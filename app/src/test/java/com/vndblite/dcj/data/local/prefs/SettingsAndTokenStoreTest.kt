package com.vndblite.dcj.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.JvmAesGcmTokenCipher
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.domain.model.CollectionFilters
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Language
import com.vndblite.dcj.domain.model.Platform
import com.vndblite.dcj.domain.model.SortField
import com.vndblite.dcj.domain.model.SortOption
import com.vndblite.dcj.domain.model.ThemeId
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsAndTokenStoreTest {

    @Test
    fun `DataStoreSettingsStore persists AppSettings CollectionFilters SortOption and activeAccountId`() = runTest {
        val tempFile = File.createTempFile("test_vndb_settings_", ".preferences_pb").apply {
            deleteOnExit()
            delete()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFile }
        )
        val store = DataStoreSettingsStore(dataStore)

        val customSettings = AppSettings(
            themeId = ThemeId.EV17,
            showCoverCensor = false,
            showChart = false,
            fontSizeOffset = 2.0f,
            maxPreviewItem = 15,
            maxItemPerRowPortrait = 3,
            maxItemPerRowLandscape = 5,
            collectionStatusTabArrangement = listOf(
                CollectionStatus.WISHLIST,
                CollectionStatus.PLAYING,
                CollectionStatus.FINISHED,
                CollectionStatus.STALLED,
                CollectionStatus.DROPPED,
                CollectionStatus.BLACKLIST
            )
        )
        store.saveSettings(customSettings)
        assertEquals(customSettings, store.getSettingsSnapshot())

        val customFilters = CollectionFilters(
            searchQuery = "Ever17",
            status = CollectionStatus.FINISHED,
            minAge = 18,
            languages = listOf(Language.EN, Language.JA),
            devStatuses = listOf(DevStatus.FINISHED),
            originalLanguages = listOf(Language.JA),
            platforms = listOf(Platform.WIN, Platform.SWI)
        )
        store.saveCollectionFilters(customFilters)
        assertEquals(customFilters, store.collectionFiltersFlow.first())

        val customSort = SortOption(field = SortField.USER_VOTE, reverse = true)
        store.saveCollectionSort(customSort)
        assertEquals(customSort, store.collectionSortFlow.first())

        assertNull(store.getActiveAccountId())
        store.setActiveAccountId("u42")
        assertEquals("u42", store.getActiveAccountId())
        store.setActiveAccountId(null)
        assertNull(store.getActiveAccountId())
    }

    @Test
    fun `EncryptedTokenStore encrypts token at rest and never stores plaintext in SharedPreferences`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_secure_token_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        val cipher = JvmAesGcmTokenCipher()
        val tokenStore = EncryptedTokenStore(
            sharedPreferences = prefs,
            cipher = cipher,
            dispatchers = dispatchers
        )

        val secretToken = "vndb_pat_super_secret_987654321"
        tokenStore.saveToken(accountId = "u42", token = secretToken)

        // Verify plaintext token is NEVER present anywhere in the underlying SharedPreferences
        val rawStoredValues = prefs.all.values.map { it.toString() }
        assertTrue(rawStoredValues.none { it.contains(secretToken) })

        // Verify round-trip decryption works for both account-specific and active token lookups
        assertTrue(tokenStore.hasToken("u42"))
        assertEquals(secretToken, tokenStore.getToken("u42"))
        assertEquals(secretToken, tokenStore.getToken(null))

        // Verify querying a different account whose key is missing does NOT fall back to the active account's token
        assertFalse(tokenStore.hasToken("u777"))
        assertNull(tokenStore.getToken("u777"))

        // Verify corrupt ciphertext is handled gracefully and returns null instead of crashing
        prefs.edit().putString("token_account_u99", "not-valid-base64-ciphertext!!").commit()
        assertNull(tokenStore.getToken("u99"))

        // Verify clearing token removes it completely
        tokenStore.clearToken("u42")
        assertFalse(tokenStore.hasToken("u42"))
        assertNull(tokenStore.getToken("u42"))
        assertNull(tokenStore.getToken(null))
    }
}
