package com.vndblite.dcj.core

import android.util.Log

/**
 * Structured logging interface that guarantees secrets (such as personal access tokens)
 * are redacted before any message is written to logcat or test sinks (Rule 5, Rule 6, A10).
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object AppLog : Logger {
    private val tokenHeaderRegex = Regex("(?i)(authorization\\s*[:=]\\s*token\\s+)([^\\s,;\"']+)")
    private val tokenParamRegex = Regex("(?i)(token\\s+)([a-z0-9\\-_]{8,})")

    @Volatile
    var delegate: Logger = AndroidLogger

    fun redactSecrets(input: String): String {
        return input
            .replace(tokenHeaderRegex, "$1[REDACTED]")
            .replace(tokenParamRegex, "$1[REDACTED]")
    }

    override fun d(tag: String, message: String) {
        delegate.d(tag, redactSecrets(message))
    }

    override fun i(tag: String, message: String) {
        delegate.i(tag, redactSecrets(message))
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        delegate.w(tag, redactSecrets(message), throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        delegate.e(tag, redactSecrets(message), throwable)
    }
}

private object AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: RuntimeException) {
            // Ignored in plain JVM unit test environments where android.util.Log is stubbed.
        }
    }

    override fun i(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: RuntimeException) {
            // Ignored in plain JVM unit test environments.
        }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        try {
            if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
        } catch (_: RuntimeException) {
            // Ignored in plain JVM unit test environments.
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        try {
            if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        } catch (_: RuntimeException) {
            // Ignored in plain JVM unit test environments.
        }
    }
}
