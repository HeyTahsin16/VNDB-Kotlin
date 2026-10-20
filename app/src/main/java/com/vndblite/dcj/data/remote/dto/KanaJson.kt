package com.vndblite.dcj.data.remote.dto

import kotlinx.serialization.json.Json

/**
 * Shared [Json] configuration for all VNDB Kana API v2 requests and responses (N3).
 */
val KanaJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = false
}
