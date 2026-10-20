package com.vndblite.dcj.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vndblite.dcj.data.remote.dto.KanaJson
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.VnImage
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnSummary
import java.time.Instant
import kotlinx.serialization.encodeToString

/**
 * Cached list of VN IDs per [com.vndblite.dcj.domain.model.HomeSection] with its fetch timestamp
 * for per-section TTL and stale-while-revalidate (S1, S4, F2).
 */
@Entity(tableName = "home_section_cache")
data class HomeSectionCacheEntity(
    @PrimaryKey
    val sectionCode: String,
    val vnIdsJson: String,
    val fetchedAtEpochSeconds: Long
) {
    fun decodeVnIds(): List<String> =
        runCatching { KanaJson.decodeFromString<List<String>>(vnIdsJson) }.getOrDefault(emptyList())

    companion object {
        fun create(sectionCode: String, vnIds: List<String>, fetchedAtEpochSeconds: Long): HomeSectionCacheEntity =
            HomeSectionCacheEntity(
                sectionCode = sectionCode,
                vnIdsJson = KanaJson.encodeToString(vnIds),
                fetchedAtEpochSeconds = fetchedAtEpochSeconds
            )
    }
}

/**
 * Cached quote row in `quote_cache` (S1, S4).
 */
@Entity(
    tableName = "quote_cache",
    indices = [Index(value = ["displayOrder"])]
)
data class QuoteCacheEntity(
    @PrimaryKey
    val quoteId: String,
    val text: String,
    val score: Int = 0,
    val vnId: String? = null,
    val vnTitle: String? = null,
    val vnImageUrl: String? = null,
    val vnImageThumbnailUrl: String? = null,
    val vnImageSexual: Double = 0.0,
    val vnImageViolence: Double = 0.0,
    val vnRating: Double? = null,
    val vnVoteCount: Int = 0,
    val vnOriginalLanguage: String? = null,
    val vnLengthBucketCode: Int? = null,
    val vnReleasedRaw: String? = null,
    val characterId: String? = null,
    val characterName: String? = null,
    val characterOriginalName: String? = null,
    val displayOrder: Int = 0,
    val fetchedAtEpochSeconds: Long = 0L
) {
    fun toDomain(): Quote {
        val vnSummary = vnId?.takeIf { it.isNotBlank() }?.let { id ->
            val img = vnImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                VnImage(
                    url = url,
                    thumbnailUrl = vnImageThumbnailUrl,
                    sexual = vnImageSexual,
                    violence = vnImageViolence
                )
            }
            VnSummary(
                id = id,
                title = vnTitle.orEmpty(),
                image = img,
                rating = vnRating,
                voteCount = vnVoteCount,
                originalLanguage = vnOriginalLanguage,
                lengthBucket = VnLengthBucket.fromApiCode(vnLengthBucketCode),
                released = ReleaseDate.parse(vnReleasedRaw)
            )
        }
        return Quote(
            id = quoteId,
            text = text,
            score = score,
            vn = vnSummary,
            characterId = characterId,
            characterName = characterName,
            characterOriginalName = characterOriginalName
        )
    }

    companion object {
        fun fromDomain(quote: Quote, displayOrder: Int, fetchedAtEpochSeconds: Long): QuoteCacheEntity =
            QuoteCacheEntity(
                quoteId = quote.id,
                text = quote.text,
                score = quote.score,
                vnId = quote.vn?.id,
                vnTitle = quote.vn?.title,
                vnImageUrl = quote.vn?.image?.url,
                vnImageThumbnailUrl = quote.vn?.image?.thumbnailUrl,
                vnImageSexual = quote.vn?.image?.sexual ?: 0.0,
                vnImageViolence = quote.vn?.image?.violence ?: 0.0,
                vnRating = quote.vn?.rating,
                vnVoteCount = quote.vn?.voteCount ?: 0,
                vnOriginalLanguage = quote.vn?.originalLanguage,
                vnLengthBucketCode = quote.vn?.lengthBucket?.apiCode,
                vnReleasedRaw = quote.vn?.released?.toApiString(),
                characterId = quote.characterId,
                characterName = quote.characterName,
                characterOriginalName = quote.characterOriginalName,
                displayOrder = displayOrder,
                fetchedAtEpochSeconds = fetchedAtEpochSeconds
            )
    }
}

/**
 * Singleton row caching live VNDB database statistics from `GET /kana/stats` (S1, F6).
 * Never populated with fake placeholder numbers.
 */
@Entity(tableName = "site_stats_cache")
data class SiteStatsCacheEntity(
    @PrimaryKey
    val singletonId: Int = SINGLETON_ID,
    val chars: Int,
    val releases: Int,
    val vn: Int,
    val staff: Int,
    val tags: Int,
    val producers: Int,
    val traits: Int,
    val fetchedAtEpochSeconds: Long
) {
    fun toDomain(): SiteStats = SiteStats(
        chars = chars,
        releases = releases,
        vn = vn,
        staff = staff,
        tags = tags,
        producers = producers,
        traits = traits,
        fetchedAt = Instant.ofEpochSecond(fetchedAtEpochSeconds)
    )

    companion object {
        const val SINGLETON_ID: Int = 1

        fun fromDomain(stats: SiteStats): SiteStatsCacheEntity = SiteStatsCacheEntity(
            singletonId = SINGLETON_ID,
            chars = stats.chars,
            releases = stats.releases,
            vn = stats.vn,
            staff = stats.staff,
            tags = stats.tags,
            producers = stats.producers,
            traits = stats.traits,
            fetchedAtEpochSeconds = stats.fetchedAt.epochSecond
        )
    }
}

/**
 * Persistent queue of remote userlist deletions (`DELETE /ulist/<id>`) scoped by `accountId` (Y4, Y6).
 * Items are only removed after a 2xx or 404 response from VNDB or when the owning account logs out.
 */
@Entity(
    tableName = "pending_remote_deletions",
    primaryKeys = ["accountId", "vnId"],
    indices = [Index(value = ["accountId"])]
)
data class PendingRemoteDeletionEntity(
    val accountId: String,
    val vnId: String,
    val queuedAtEpochSeconds: Long
)

/**
 * Account-scoped synchronization metadata (S1, Y1, Y6).
 * Note: the API token itself is NEVER stored in Room; it lives in `TokenStore` (S5).
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val accountId: String,
    val username: String,
    val permissionsPipe: String = "",
    val isSynced: Boolean = false,
    val lastSyncedAtEpochSeconds: Long? = null
) {
    fun toUserAccount(): UserAccount = UserAccount(
        id = accountId,
        username = username,
        permissions = VnCacheEntity.decodePipeList(permissionsPipe).toSet()
    )

    companion object {
        fun fromUserAccount(
            account: UserAccount,
            isSynced: Boolean,
            lastSyncedAtEpochSeconds: Long?
        ): SyncMetadataEntity = SyncMetadataEntity(
            accountId = account.id,
            username = account.username,
            permissionsPipe = VnCacheEntity.encodePipeList(account.permissions),
            isSynced = isSynced,
            lastSyncedAtEpochSeconds = lastSyncedAtEpochSeconds
        )
    }
}

/**
 * Quarantined legacy record that failed to parse during `LegacyPrefsMigrator` execution (S6).
 * Preserves the unparsable raw payload (with any auth token redacted) rather than silently dropping it.
 */
@Entity(
    tableName = "quarantined_records",
    indices = [Index(value = ["sourceKey"])]
)
data class QuarantinedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceKey: String,
    val rawPayload: String,
    val errorReason: String,
    val quarantinedAtEpochSeconds: Long
)
