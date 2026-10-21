package com.vndblite.dcj

import android.app.Application

/**
 * Custom [Application] building [AppContainer] and launching the one-time idempotent
 * legacy preferences migration on a background dispatcher (Section 2, Section 5, A3, S6).
 */
class VndbApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val defaultContainer = DefaultAppContainer(this)
        container = defaultContainer
        defaultContainer.startBackgroundMigrationIfNeeded()
    }
}
