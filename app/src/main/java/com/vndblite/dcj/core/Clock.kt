package com.vndblite.dcj.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Injectable clock abstraction so all time-dependent logic (rate limiting, cache TTLs,
 * sync conflict timestamps, and default dates) can be deterministically tested.
 */
fun interface Clock {
    fun nowMillis(): Long

    fun nowEpochSeconds(): Long = nowMillis() / 1_000L

    fun nowInstant(): Instant = Instant.ofEpochMilli(nowMillis())

    fun todayUtc(): LocalDate = nowInstant().atZone(ZoneOffset.UTC).toLocalDate()
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
