package com.vndblite.dcj.domain.model

/**
 * VNDB play-length bucket (`1..5`).
 * Unknown/unvoted length is represented as `null` rather than `0` (A7, A8).
 */
enum class VnLengthBucket(val apiCode: Int) {
    VERY_SHORT(1),
    SHORT(2),
    MEDIUM(3),
    LONG(4),
    VERY_LONG(5);

    companion object {
        fun fromApiCode(code: Int?): VnLengthBucket? =
            entries.firstOrNull { it.apiCode == code }
    }
}
