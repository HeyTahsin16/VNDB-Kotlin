package com.vndblite.dcj.ui.components

import com.vndblite.dcj.domain.model.Character
import com.vndblite.dcj.domain.model.CharacterBirthday
import com.vndblite.dcj.domain.model.CharacterRole
import com.vndblite.dcj.domain.model.CharacterSex
import com.vndblite.dcj.domain.model.CharacterVnAppearance
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.DevStatus
import com.vndblite.dcj.domain.model.Producer
import com.vndblite.dcj.domain.model.Quote
import com.vndblite.dcj.domain.model.ReleaseDate
import com.vndblite.dcj.domain.model.Review
import com.vndblite.dcj.domain.model.SiteStats
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.Tag
import com.vndblite.dcj.domain.model.Trait
import com.vndblite.dcj.domain.model.UserAccount
import com.vndblite.dcj.domain.model.VnDetails
import com.vndblite.dcj.domain.model.VnImage
import com.vndblite.dcj.domain.model.VnLengthBucket
import com.vndblite.dcj.domain.model.VnSummary
import java.time.Instant
import java.time.LocalDate

/**
 * Sample domain data used exclusively by Compose `@Preview` functions across components and screens (U6).
 */
object PreviewData {

    val sampleVnSummary = VnSummary(
        id = "v17",
        title = "Ever17 -the out of infinity-",
        image = VnImage(
            url = "https://t.vndb.org/cv/00/17.jpg",
            thumbnailUrl = "https://t.vndb.org/cv.t/00/17.jpg",
            sexual = 0.0,
            violence = 0.1
        ),
        rating = 86.4,
        voteCount = 7540,
        originalLanguage = "ja",
        lengthBucket = VnLengthBucket.LONG,
        released = ReleaseDate.Exact(LocalDate.of(2002, 8, 29)),
        description = "Seven people are trapped 51 meters underwater in the marine theme park [b]LeMU[/b]."
    )

    val sampleExplicitVnSummary = sampleVnSummary.copy(
        id = "v92",
        title = "Muv-Luv Alternative",
        image = VnImage(
            url = "https://t.vndb.org/cv/00/92.jpg",
            thumbnailUrl = "https://t.vndb.org/cv.t/00/92.jpg",
            sexual = 0.6,
            violence = 0.8
        ),
        rating = 90.2,
        voteCount = 12450
    )

    val sampleVnDetails = VnDetails(
        id = "v17",
        aliases = listOf("Ever17", "E17"),
        devStatus = DevStatus.FINISHED,
        lengthMinutes = 2100,
        minAges = listOf(15),
        languages = listOf("en", "ja", "zh-Hans"),
        machineTranslatedLanguages = emptyList(),
        platforms = listOf("win", "psp", "swi"),
        developers = listOf(Producer(id = "p14", name = "KID")),
        tags = listOf(
            Tag(id = "g19", name = "Mystery", rating = 2.9, spoilerLevel = 0),
            Tag(id = "g104", name = "Sci-fi", rating = 2.8, spoilerLevel = 0)
        )
    )

    val sampleCollectionEntry = CollectionEntry(
        vnId = "v17",
        title = "Ever17 -the out of infinity-",
        status = CollectionStatus.FINISHED,
        vote = 90,
        notes = "Unforgettable true route ending.",
        addedDate = LocalDate.of(2025, 1, 10),
        startedDate = LocalDate.of(2025, 1, 10),
        finishedDate = LocalDate.of(2025, 1, 24),
        lastModifiedAt = Instant.parse("2025-01-24T20:00:00Z"),
        remoteLastMod = Instant.parse("2025-01-24T20:00:00Z"),
        syncState = SyncState.CLEAN
    )

    val sampleCharacter = Character(
        id = "c12",
        name = "Tsugumi Komachi",
        originalName = "小町 つぐみ",
        description = "A mysterious girl encountered inside LeMU.",
        bloodType = "AB",
        heightCm = 156,
        weightKg = 45,
        ageYears = 17,
        birthday = CharacterBirthday(month = 4, day = 7),
        sex = CharacterSex.FEMALE,
        vns = listOf(
            CharacterVnAppearance(
                vnId = "v17",
                vnTitle = "Ever17 -the out of infinity-",
                role = CharacterRole.MAIN,
                spoilerLevel = 0
            )
        ),
        traits = listOf(
            Trait(id = "i10", name = "Long", groupId = "i1", groupName = "Hair")
        )
    )

    val sampleQuote = Quote(
        id = "q42",
        text = "Who am I? And who are you, watching from above?",
        score = 32,
        vn = sampleVnSummary,
        characterId = "c12",
        characterName = "Tsugumi Komachi"
    )

    val sampleReview = Review(
        id = "w101",
        vnId = "v17",
        username = "KanaReader",
        ratingText = "9.5",
        date = LocalDate.of(2024, 11, 15),
        text = "A landmark sci-fi mystery visual novel with a brilliant payoff.",
        pointsText = "+13",
        commentsCountText = "2",
        hasReadMore = false
    )

    val sampleSiteStats = SiteStats(
        chars = 148200,
        releases = 112500,
        vn = 52400,
        staff = 38100,
        tags = 3100,
        producers = 19400,
        traits = 3250,
        fetchedAt = Instant.parse("2026-03-15T12:00:00Z")
    )

    val sampleUserAccount = UserAccount(
        id = "u42",
        username = "Tahsin",
        permissions = setOf("listread", "listwrite")
    )
}
