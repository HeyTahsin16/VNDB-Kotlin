package com.vndblite.dcj.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.ApiError
import com.vndblite.dcj.core.AppResult
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.data.remote.VndbApiImpl
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.sync.SyncEngine
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsAndSyncRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.buildInMemory(context)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    @Test
    fun `SettingsRepository updates DataStore and SyncRepository verifies permissions and logout choice`() = runTest {
        val tempFile = File.createTempFile("settings_sync_repo_test_", ".preferences_pb").apply {
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
        val tokenPrefs = context.getSharedPreferences("test_sync_repo_prefs", Context.MODE_PRIVATE)
        tokenPrefs.edit().clear().commit()
        val tokenStore = EncryptedTokenStore(tokenPrefs, JvmAesGcmTokenCipher(), dispatchers)
        val clock = Clock { 1_750_000_000_000L }
        val api = VndbApiImpl(
            httpClient = HttpClientFactory.create(),
            rateLimiter = RateLimiter(clock = clock, sleeper = {}),
            dispatchers = dispatchers,
            clock = clock,
            baseKanaUrl = server.url("/kana").toString()
        )
        val syncEngine = SyncEngine(
            api = api,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
        val settingsRepo = SettingsRepositoryImpl(settingsStore)
        val syncRepo = SyncRepositoryImpl(syncEngine, settingsStore, tokenStore)

        // 1. SettingsRepository updates and resets
        settingsRepo.updateSettings { it.copy(themeId = ThemeId.SAYA, showCoverCensor = false) }
        assertEquals(ThemeId.SAYA, settingsRepo.getSettingsSnapshot().themeId)
        assertFalse(settingsRepo.getSettingsSnapshot().showCoverCensor)
        settingsRepo.resetSettingsToDefaults()
        assertEquals(ThemeId.DEFAULT, settingsRepo.getSettingsSnapshot().themeId)
        assertTrue(settingsRepo.getSettingsSnapshot().showCoverCensor)

        // 2. SyncRepository rejects token missing listwrite
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"u42","username":"ReaderOnly","permissions":["listread"]}""")
        )
        val missingPermRes = syncRepo.authenticateAndSync("token_missing_write")
        val failure = assertIs<AppResult.Failure>(missingPermRes)
        assertEquals(ApiError.MissingPermission(setOf("listwrite")), failure.error)
        assertFalse(syncRepo.hasStoredToken())

        // 3. SyncRepository authenticates valid token with listread + listwrite and syncs
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"u42","username":"ValidUser","permissions":["listread","listwrite"]}""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"more":false,"results":[{"id":"v17","lastmod":1740000000,"vote":90,"labels":[{"id":2,"label":"Finished"}],"vn":{"id":"v17","title":"Ever17"}}]}"""
                )
        )
        val authRes = syncRepo.authenticateAndSync("valid_token")
        assertIs<AppResult.Success<*>>(authRes)
        assertTrue(syncRepo.hasStoredToken())
        assertEquals("ValidUser", syncRepo.activeAccountFlow.first()?.username)
        assertNotNull(database.collectionDao().getVisibleEntryById("v17"))

        // 4. Logout with clearLocalCollection = false keeps local collection
        syncRepo.logout(clearLocalCollection = false)
        assertFalse(syncRepo.hasStoredToken())
        assertNull(syncRepo.activeAccountFlow.first())
        assertNotNull(database.collectionDao().getVisibleEntryById("v17"))
    }
}
