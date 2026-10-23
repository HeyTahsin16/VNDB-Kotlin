package com.vndblite.dcj.core

import com.vndblite.dcj.BuildConfig

object AppInfo {
    const val APP_NAME: String = "VNDB Kotlin"
    const val APP_LICENSE: String = "Apache-2.0"
    const val GITHUB_REPO_SLUG: String = "HeyTahsin16/VNDB-Kotlin"
    const val GITHUB_REPO_URL: String = "https://github.com/HeyTahsin16/VNDB-Kotlin"
    const val SOURCE_REPOSITORY_URL: String = GITHUB_REPO_URL
    const val GITHUB_ISSUES_URL: String = "https://github.com/HeyTahsin16/VNDB-Kotlin/issues"
    const val UPSTREAM_FLUTTER_REPO_SLUG: String = "Daniel-C-J/vndb-lite"
    const val UPSTREAM_FLUTTER_REPO_URL: String = "https://github.com/Daniel-C-J/vndb-lite"
    const val UPSTREAM_FLUTTER_AUTHOR: String = "Daniel C.J."
    const val UPSTREAM_AUTHOR: String = UPSTREAM_FLUTTER_AUTHOR
    const val VNDB_URL: String = "https://vndb.org"
    const val VNDB_HOME_URL: String = VNDB_URL
    const val VNDB_DATA_LICENSE_URL: String = "https://vndb.org/d17#4"

    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE
    val VERSION_NAME: String = versionName
    val VERSION_CODE: Int = versionCode

    val userAgent: String = "VNDBKotlin/$versionName (+$GITHUB_REPO_URL)"
}
