package com.vndblite.dcj.domain.model

data class Trait(
    val id: String,
    val name: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val description: String? = null,
    val aliases: List<String> = emptyList(),
    val characterCount: Int = 0,
    val spoilerLevel: Int = 0
)

/**
 * Predefined popular character trait shortcuts for the Character Explorer (A6, A8).
 */
enum class PopularTraitPreset(
    val traitId: String,
    val defaultName: String,
    val defaultGroupName: String
) {
    TSUNDERE("i154", "Tsundere", "Personality"),
    KUUDERE("i162", "Kuudere", "Personality"),
    YANDERE("i155", "Yandere", "Personality"),
    DANDERE("i159", "Dandere", "Personality"),
    CHILDHOOD_FRIEND("i232", "Childhood Friend", "Role"),
    MAID("i125", "Maid", "Role"),
    GLASSES("i47", "Glasses", "Clothes"),
    BLONDE_HAIR("i5", "Blonde Hair", "Hair"),
    BLACK_HAIR("i2", "Black Hair", "Hair"),
    SILVER_WHITE_HAIR("i9", "Silver/White Hair", "Hair"),
    TWIN_TAILS("i17", "Twin Tails", "Hair"),
    PONYTAIL("i18", "Ponytail", "Hair");

    fun toTrait(): Trait = Trait(
        id = traitId,
        name = defaultName,
        groupName = defaultGroupName
    )
}
