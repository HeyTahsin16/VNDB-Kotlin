package com.vndblite.dcj.domain.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Typed representation of a VNDB release date (`"YYYY-MM-DD"`, `"YYYY-MM"`, `"YYYY"`, or `"TBA"`).
 * Unknown or missing dates are represented by `null` rather than `"Unknown"` sentinels (A7, N8).
 */
sealed interface ReleaseDate : Comparable<ReleaseDate> {
    val sortKey: String

    data class Exact(val date: LocalDate) : ReleaseDate {
        override val sortKey: String = date.toString()
    }

    data class YearMonthOnly(val year: Int, val month: Int) : ReleaseDate {
        override val sortKey: String = "%04d-%02d".format(year, month)
    }

    data class YearOnly(val year: Int) : ReleaseDate {
        override val sortKey: String = "%04d".format(year)
    }

    data object Tba : ReleaseDate {
        override val sortKey: String = "9999-99-99"
    }

    override fun compareTo(other: ReleaseDate): Int = sortKey.compareTo(other.sortKey)

    fun toApiString(): String = when (this) {
        is Exact -> date.toString()
        is YearMonthOnly -> "%04d-%02d".format(year, month)
        is YearOnly -> "%04d".format(year)
        Tba -> "TBA"
    }

    companion object {
        private val yearMonthRegex = Regex("^(\\d{4})-(\\d{2})$")
        private val yearOnlyRegex = Regex("^(\\d{4})$")

        fun parse(raw: String?): ReleaseDate? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty() ||
                trimmed.equals("unknown", ignoreCase = true) ||
                trimmed.equals("null", ignoreCase = true) ||
                trimmed == "--"
            ) {
                return null
            }
            if (trimmed.equals("TBA", ignoreCase = true)) {
                return Tba
            }
            yearOnlyRegex.matchEntire(trimmed)?.let { match ->
                val year = match.groupValues[1].toIntOrNull() ?: return null
                return YearOnly(year)
            }
            yearMonthRegex.matchEntire(trimmed)?.let { match ->
                val year = match.groupValues[1].toIntOrNull() ?: return null
                val month = match.groupValues[2].toIntOrNull() ?: return null
                if (month in 1..12) return YearMonthOnly(year, month)
            }
            return try {
                Exact(LocalDate.parse(trimmed.take(10)))
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
