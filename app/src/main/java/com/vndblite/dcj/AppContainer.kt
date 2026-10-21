package com.vndblite.dcj

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.migration.LegacyPrefsMigrator
import com.vndblite.dcj.data.local.prefs.DataStoreSettingsStore
import com.vndblite.dcj.data.local.prefs.SettingsStore
import com.vndblite.dcj.data.local.secure.EncryptedTokenStore
import com.vndblite.dcj.data.local.secure.TokenStore
import com.vndblite.dcj.data.remote.VndbApi
import com.vndblite.dcj.data.remote.VndbApiImpl
import com.vndblite.dcj.data.remote.http.HttpClientFactory
import com.vndblite.dcj.data.remote.http.RateLimiter
import com.vndblite.dcj.data.remote.scraper.JsoupReviewSource
import com.vndblite.dcj.data.remote.scraper.ReviewSource
import com.vndblite.dcj.data.repository.CollectionRepositoryImpl
import com.vndblite.dcj.data.repository.SettingsRepositoryImpl
import com.vndblite.dcj.data.repository.VnRepositoryImpl
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.sync.SyncEngine
import com.vndblite.dcj.sync.SyncService
import com.vndblite.dcj.ui.navigation.SearchCommandBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Manual dependency injection container holding application singletons and repositories
 * behind interfaces (Section 2, Section 5, A3).
 */
interface AppContainer {
    val clock: Clock
    val dispatchers: DispatcherProvider
    val api: VndbApi
    val reviewSource: ReviewSource
    val database: AppDatabase
    val settingsStore: SettingsStore
    val tokenStore: TokenStore
    val syncService: SyncService
    val settingsRepository: SettingsRepository
    val collectionRepository: CollectionRepository
    val vnRepository: VnRepository
    val searchCommandBus: SearchCommandBus
}

class DefaultAppContainer(
    context: Context,
    override val clock: Clock = SystemClock,
    override val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : AppContainer {

    private val appContext: Context = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val httpClient: OkHttpClient by lazy {
        HttpClientFactory.create()
    }

    private val rateLimiter: RateLimiter by lazy {
        RateLimiter(clock = clock)
    }

    override val api: VndbApi by lazy {
        VndbApiImpl(
            httpClient = httpClient,
            rateLimiter = rateLimiter,
            dispatchers = dispatchers,
            clock = clock
        )
    }

    override val reviewSource: ReviewSource by lazy {
        JsoupReviewSource(
            httpClient = httpClient,
            rateLimiter = rateLimiter,
            dispatchers = dispatchers
        )
    }

    override val database: AppDatabase by lazy {
        AppDatabase.buildPersistent(appContext)
    }

    override val settingsStore: SettingsStore by lazy {
        DataStoreSettingsStore.create(appContext, appScope)
    }

    override val tokenStore: TokenStore by lazy {
        EncryptedTokenStore.create(
            context = appContext,
            dispatchers = dispatchers
        )
    }

    override val syncService: SyncService by lazy {
        SyncEngine(
            api = api,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsStore = settingsStore)
    }

    override val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(
            database = database,
            api = api,
            settingsStore = settingsStore,
            syncService = syncService,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val vnRepository: VnRepository by lazy {
        VnRepositoryImpl(
            api = api,
            reviewSource = reviewSource,
            database = database,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val searchCommandBus: SearchCommandBus by lazy {
        SearchCommandBus()
    }

    private val legacyPrefsMigrator: LegacyPrefsMigrator by lazy {
        LegacyPrefsMigrator.create(
            context = appContext,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    fun startBackgroundMigrationIfNeeded() {
        appScope.launch {
            legacyPrefsMigrator.migrateIfNeeded()
        }
    }

    companion object {
        @Volatile
        private var fallbackInstance: DefaultAppContainer? = null

        fun resolve(context: Context): AppContainer {
            val app = context.applicationContext
            if (app is VndbApplication) {
                return app.container
            }
            return fallbackInstance ?: synchronized(this) {
                fallbackInstance ?: DefaultAppContainer(app).also {
                    it.startBackgroundMigrationIfNeeded()
                    fallbackInstance = it
                }
            }
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}
