package com.vndblite.dcj.domain.model

/**
 * Pure domain snapshot of user preferences persisted in DataStore (A3, A8, A12).
 * Note: the dead `autoUpdate` setting has been removed per A12.
 */
data class AppSettings(
    val themeId: ThemeId = ThemeId.DEFAULT,
    val showCoverCensor: Boolean = true,
    val showChart: Boolean = true,
    val fontSizeOffset: Float = 0.0f,
    val maxPreviewItem: Int = 20,
    val maxItemPerRowPortrait: Int = 2,
    val maxItemPerRowLandscape: Int = 4,
    val collectionStatusTabArrangement: List<CollectionStatus> = CollectionStatus.entries
)
