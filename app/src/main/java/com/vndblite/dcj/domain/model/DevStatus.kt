package com.vndblite.dcj.domain.model

/**
 * VNDB development status (0 = Finished, 1 = In Development, 2 = Cancelled).
 * Unknown/absent status is represented as `null` rather than `-1` (A6, A7, A8).
 */
enum class DevStatus(val apiCode: Int) {
    FINISHED(0),
    IN_DEVELOPMENT(1),
    CANCELLED(2);

    val apiValue: Int
        get() = apiCode

    companion object {
        fun fromApiCode(code: Int?): DevStatus? =
            entries.firstOrNull { it.apiCode == code }
    }
}
