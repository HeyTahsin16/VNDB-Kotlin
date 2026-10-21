package com.vndblite.dcj.domain.model

enum class CharacterRole(val apiCode: String) {
    MAIN("main"),
    PRIMARY("primary"),
    SIDE("side"),
    APPEARS("appears");

    companion object {
        fun fromApiCode(code: String?): CharacterRole {
            if (code.isNullOrBlank()) return APPEARS
            return entries.firstOrNull { it.apiCode.equals(code.trim(), ignoreCase = true) } ?: APPEARS
        }
    }
}

enum class CharacterSex(val apiCode: String) {
    FEMALE("f"),
    MALE("m"),
    BOTH("b"),
    SEXLESS("n");

    companion object {
        fun fromApiCode(code: String?): CharacterSex? {
            if (code.isNullOrBlank()) return null
            return entries.firstOrNull { it.apiCode.equals(code.trim(), ignoreCase = true) }
        }
    }
}

data class CharacterBirthday(
    val month: Int,
    val day: Int
)

data class CharacterMeasurements(
    val bustCm: Int? = null,
    val waistCm: Int? = null,
    val hipsCm: Int? = null,
    val cupSize: String? = null
)

data class CharacterVnAppearance(
    val vnId: String,
    val vnTitle: String,
    val role: CharacterRole = CharacterRole.APPEARS,
    val spoilerLevel: Int = 0
)

/**
 * Pure domain model for a VNDB character without hardcoded formatting strings or `RoleSummary` PascalCase property (A6, A7, A8).
 */
data class Character(
    val id: String,
    val name: String,
    val originalName: String? = null,
    val aliases: List<String> = emptyList(),
    val description: String? = null,
    val image: VnImage? = null,
    val bloodType: String? = null,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val measurements: CharacterMeasurements? = null,
    val ageYears: Int? = null,
    val birthday: CharacterBirthday? = null,
    val sex: CharacterSex? = null,
    val vns: List<CharacterVnAppearance> = emptyList(),
    val traits: List<Trait> = emptyList()
) {
    val roleSummary: CharacterRole?
        get() = vns.firstOrNull()?.role
}
