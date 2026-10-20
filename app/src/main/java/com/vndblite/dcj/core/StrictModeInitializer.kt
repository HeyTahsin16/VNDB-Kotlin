package com.vndblite.dcj.core

import android.os.StrictMode
import com.vndblite.dcj.BuildConfig

/**
 * Configures Android [StrictMode] policies in debug builds to detect accidental main-thread
 * disk reads, disk writes, or network operations (A10, S3).
 */
object StrictModeInitializer {

    fun enableIfDebug(isDebugBuild: Boolean = BuildConfig.DEBUG) {
        if (!isDebugBuild) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
        AppLog.d("StrictMode", "Debug StrictMode thread and VM policies enabled")
    }
}
