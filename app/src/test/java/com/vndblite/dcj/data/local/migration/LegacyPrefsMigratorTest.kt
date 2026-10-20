package com.vndblite.dcj.data.local.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.ThemeId
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyPrefsMigratorTest {

    private lateinit var context: Context
    private lateinit var legacyPrefs: SharedPreferences
    private lateinit var tokenPrefs: SharedPreferences
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        legacyPrefs = context.getSharedPreferences("test_legacy_vndb_prefs", Context.MODE_PRIVATE)
        legacyPrefs.edit().clear().commit()
        tokenPrefs = context.getSharedPreferences("test_migrated_token_prefs", Context.MODE_PRIVATE)
        tokenPrefs.edit().clear().commit()
        database = AppDatabase.buildInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
        legacyPrefs.edit().clear().commit()
        tokenPrefs.edit().clear().commit()
    }

    @Test
    fun `migrates valid legacy records encrypts and wipes plaintext token quarantines corrupt records and runs idempotently`() = runTest {
        val tempFile = File.createTempFile("test_migration_settings_", ".preferences_pb").apply {
            deleteOnExit()
            delete()
        }
        val settingsStore = DataStoreSettingsStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tempFile })
        )
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        val tokenStore = EncryptedTokenStore(
            sharedPreferences = tokenPrefs,
            cipher = JvmAesGcmTokenCipher(),
            dispatchers = dispatchers
        )
        val fixedClock = Clock { 1_710_000_000_000L }

        // Populate legacy SharedPreferences with valid + corrupt records
        val validRecordClean = """
            {"id":"v17","title":"Ever17","status":"finished","vote":90,"added":"2024-01-05","started":"2024-01-05","finished":"2024-01-12","lastmod":"2024-01-12","notes":"Great VN"}
        """.trimIndent()
        val validRecordNew = """
            {"id":"v92","title":"Muv-Luv","status":"playing","vote":0,"added":"2024-02-01","started":"2024-02-01","finished":"null","lastmod":"1707000000"}
        """.trimIndent()
        val corruptCollectionRecord = """
            {"id":"","title":"Broken Record Without Valid VnId","status":"playing"}
        """.trimIndent()
        val malformedJsonCollectionRecord = "{not-valid-json"

        val validP1 = """
            {"id":"v17","title":"Ever17 -the out of infinity-","rating":86.4,"votecount":7500,"olang":"ja","length":4,"released":"2002-08-29","description":"Sci-fi thriller."}
        """.trimIndent()
        val validP2 = """
            {"id":"v17","developers":["KID"],"devstatus":0,"length_minutes":2100,"aliases":["Ever17"],"minage":[12],"languages":["en","ja"],"mtl":[],"platforms":["win","psp"],"tags":[{"id":"g19","name":"Mystery","rating":2.8,"spoiler":0}],"screenshots":[]}
        """.trimIndent()
        val corruptP1 = "{broken_p1_json"

        legacyPrefs.edit()
            .putString(
                LegacyPrefsMigrator.KEY_USER_IDENTITY,
                """{"id":"u42","username":"tahsin","authToken":"secret_legacy_token_123"}"""
            )
            .putBoolean(LegacyPrefsMigrator.KEY_USER_SYNCED, true)
            .putStringSet(
                LegacyPrefsMigrator.KEY_SAVED_COLLECTION,
                setOf(validRecordClean, validRecordNew, corruptCollectionRecord, malformedJsonCollectionRecord)
            )
            .putStringSet(LegacyPrefsMigrator.KEY_ADDED_BY_APP, setOf("v92"))
            .putStringSet(LegacyPrefsMigrator.KEY_TO_BE_REMOVED, setOf("v500"))
            .putString("${LegacyPrefsMigrator.PREFIX_P1}v17", validP1)
            .putString("${LegacyPrefsMigrator.PREFIX_P2}v17", validP2)
            .putString("${LegacyPrefsMigrator.PREFIX_P1}v999", corruptP1)
            .putString(LegacyPrefsMigrator.KEY_THEME_CONF, ThemeId.AIR.code)
            .putBoolean(LegacyPrefsMigrator.KEY_COVER_CENSOR_CONF, false)
            .commit()

        val migrator = LegacyPrefsMigrator(
            legacyPrefs = legacyPrefs,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = fixedClock,
            dispatchers = dispatchers
        )

        val firstReport = migrator.migrateIfNeeded()
        assertFalse(firstReport.alreadyMigrated)
        assertEquals(2, firstReport.migratedCollectionCount)
        assertEquals(1, firstReport.migratedVnCacheCount)
        assertEquals(1, firstReport.migratedPendingDeletionsCount)
        assertTrue(firstReport.migratedToken)
        assertEquals(3, firstReport.quarantinedCount)

        // 1. Plaintext USER_IDENTITY was wiped from legacy SharedPreferences (S5)
        assertFalse(legacyPrefs.contains(LegacyPrefsMigrator.KEY_USER_IDENTITY))
        // And encrypted token is available in TokenStore
        assertEquals("secret_legacy_token_123", tokenStore.getToken("u42"))

        // 2. Valid collection records were migrated with accurate SyncState & nullable fields
        val migratedV17 = database.collectionDao().getVisibleEntryById("v17")
        assertNotNull(migratedV17)
        assertEquals(CollectionStatus.FINISHED.labelId, migratedV17.statusId)
        assertEquals(90, migratedV17.vote)
        assertEquals(SyncState.CLEAN.name, migratedV17.syncState)

        val migratedV92 = database.collectionDao().getVisibleEntryById("v92")
        assertNotNull(migratedV92)
        assertEquals(CollectionStatus.PLAYING.labelId, migratedV92.statusId)
        assertEquals(null, migratedV92.vote)
        assertEquals(SyncState.NEW.name, migratedV92.syncState)

        // 3. Pending remote deletion was scoped to u42
        val pendingDeletes = database.pendingRemoteDeletionDao().getPendingForAccount("u42")
        assertEquals(listOf("v500"), pendingDeletes.map { it.vnId })

        // 4. Corrupt records were quarantined in quarantined_records table instead of dropped silently (S6)
        val quarantinedRows = database.quarantineDao().getAll()
        assertEquals(3, quarantinedRows.size)

        // 5. Running migrateIfNeeded() a second time is idempotent and does not duplicate anything (S6)
        val secondReport = migrator.migrateIfNeeded()
        assertTrue(secondReport.alreadyMigrated)
        assertEquals(3, database.quarantineDao().count())
    }

    @Test
    fun `corrupt USER_IDENTITY payload redacts authToken before quarantining`() = runTest {
        val tempFile = File.createTempFile("test_migration_redact_", ".preferences_pb").apply {
            deleteOnExit()
            delete()
        }
        val settingsStore = DataStoreSettingsStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tempFile })
        )
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        val tokenStore = EncryptedTokenStore(
            sharedPreferences = tokenPrefs,
            cipher = JvmAesGcmTokenCipher(),
            dispatchers = dispatchers
        )

        val rawCorruptIdentity = """{"id":"","username":"broken","authToken":"super_secret_should_be_redacted"}"""
        legacyPrefs.edit()
            .putString(LegacyPrefsMigrator.KEY_USER_IDENTITY, rawCorruptIdentity)
            .commit()

        val migrator = LegacyPrefsMigrator(
            legacyPrefs = legacyPrefs,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            dispatchers = dispatchers
        )
        val report = migrator.migrateIfNeeded()
        assertEquals(1, report.quarantinedCount)

        val quarantined = database.quarantineDao().getBySourceKey(LegacyPrefsMigrator.KEY_USER_IDENTITY)
        assertEquals(1, quarantined.size)
        assertFalse(quarantined.first().rawPayload.contains("super_secret_should_be_redacted"))
        assertTrue(quarantined.first().rawPayload.contains("<REDACTED>"))
    }
}
