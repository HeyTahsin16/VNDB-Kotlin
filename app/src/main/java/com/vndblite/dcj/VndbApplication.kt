package com.vndblite.dcj

import android.app.Application
import com.vndblite.dcj.di.AppContainer
import com.vndblite.dcj.di.DefaultAppContainer

/**
 * Application entry point holding the lazy [AppContainer] composition root.
 */
class VndbApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    internal fun overrideContainerForTesting(testContainer: AppContainer) {
        container = testContainer
    }
}
