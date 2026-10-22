package com.vndblite.dcj.di

import android.content.Context
import com.vndblite.dcj.core.Clock
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import com.vndblite.dcj.core.SystemClock
import com.vndblite.dcj.data.local.db.AppDatabase
import com.vndblite.dcj.data.local.db.dao.CollectionDao
import com.vndblite.dcj.data.local.db.dao.HomeSectionCacheDao
import com.vndblite.dcj.data.local.db.dao.PendingRemoteDeletionDao
import com.vndblite.dcj.data.local.db.dao.QuarantineDao
import com.vndblite.dcj.data.local.db.dao.QuoteCacheDao
import com.vndblite.dcj.data.local.db.dao.SiteStatsCacheDao
import com.vndblite.dcj.data.local.db.dao.SyncMetadataDao
import com.vndblite.dcj.data.local.db.dao.VnCacheDao
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
import com.vndblite.dcj.data.repository.SyncRepositoryImpl
import com.vndblite.dcj.data.repository.VnRepositoryImpl
import com.vndblite.dcj.domain.repository.CollectionRepository
import com.vndblite.dcj.domain.repository.SettingsRepository
import com.vndblite.dcj.domain.repository.SyncRepository
import com.vndblite.dcj.domain.repository.VnRepository
import com.vndblite.dcj.sync.SyncEngine
import com.vndblite.dcj.sync.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * Manual dependency injection container owning singletons for the application lifecycle.
 *
 * Ensures that a single [OkHttpClient] and a single [RateLimiter] are shared between
 * [VndbApi] and [ReviewSource] so the 200 requests / 5 minutes budget is never multiplied.
 */
interface AppContainer {
    val dispatchers: DispatcherProvider
    val clock: Clock
    val applicationScope: CoroutineScope
    val httpClient: OkHttpClient
    val rateLimiter: RateLimiter
    val vndbApi: VndbApi
    val reviewSource: ReviewSource
    val database: AppDatabase
    val collectionDao: CollectionDao
    val vnCacheDao: VnCacheDao
    val homeSectionCacheDao: HomeSectionCacheDao
    val quoteCacheDao: QuoteCacheDao
    val siteStatsCacheDao: SiteStatsCacheDao
    val pendingRemoteDeletionDao: PendingRemoteDeletionDao
    val syncMetadataDao: SyncMetadataDao
    val quarantineDao: QuarantineDao
    val settingsStore: SettingsStore
    val tokenStore: TokenStore
    val legacyPrefsMigrator: LegacyPrefsMigrator
    val syncService: SyncService
    val vnRepository: VnRepository
    val collectionRepository: CollectionRepository
    val settingsRepository: SettingsRepository
    val syncRepository: SyncRepository
}

class DefaultAppContainer(
    context: Context,
    override val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    override val clock: Clock = SystemClock
) : AppContainer {

    private val appContext: Context = context.applicationContext

    override val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + dispatchers.io)
    }

    override val httpClient: OkHttpClient by lazy {
        HttpClientFactory.create()
    }

    override val rateLimiter: RateLimiter by lazy {
        RateLimiter(clock = clock)
    }

    override val vndbApi: VndbApi by lazy {
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

    override val collectionDao: CollectionDao by lazy {
        database.collectionDao()
    }

    override val vnCacheDao: VnCacheDao by lazy {
        database.vnCacheDao()
    }

    override val homeSectionCacheDao: HomeSectionCacheDao by lazy {
        database.homeSectionCacheDao()
    }

    override val quoteCacheDao: QuoteCacheDao by lazy {
        database.quoteCacheDao()
    }

    override val siteStatsCacheDao: SiteStatsCacheDao by lazy {
        database.siteStatsCacheDao()
    }

    override val pendingRemoteDeletionDao: PendingRemoteDeletionDao by lazy {
        database.pendingRemoteDeletionDao()
    }

    override val syncMetadataDao: SyncMetadataDao by lazy {
        database.syncMetadataDao()
    }

    override val quarantineDao: QuarantineDao by lazy {
        database.quarantineDao()
    }

    override val settingsStore: SettingsStore by lazy {
        DataStoreSettingsStore.create(appContext, applicationScope)
    }

    override val tokenStore: TokenStore by lazy {
        EncryptedTokenStore.create(
            context = appContext,
            dispatchers = dispatchers
        )
    }

    override val legacyPrefsMigrator: LegacyPrefsMigrator by lazy {
        LegacyPrefsMigrator.create(
            context = appContext,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val syncService: SyncService by lazy {
        SyncEngine(
            api = vndbApi,
            database = database,
            settingsStore = settingsStore,
            tokenStore = tokenStore,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val vnRepository: VnRepository by lazy {
        VnRepositoryImpl(
            api = vndbApi,
            reviewSource = reviewSource,
            database = database,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(
            database = database,
            settingsStore = settingsStore,
            clock = clock,
            dispatchers = dispatchers
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsStore = settingsStore)
    }

    override val syncRepository: SyncRepository by lazy {
        SyncRepositoryImpl(
            syncService = syncService,
            settingsStore = settingsStore,
            tokenStore = tokenStore
        )
    }
}
