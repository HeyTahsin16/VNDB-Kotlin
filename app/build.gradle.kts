import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (!versionPropsFile.isFile) {
        throw GradleException("Missing required version.properties at ${versionPropsFile.absolutePath}")
    }
    versionPropsFile.inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: throw GradleException("VERSION_NAME missing in version.properties")
val appVersionCode: Int = versionProps.getProperty("VERSION_CODE")?.trim()?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: throw GradleException("VERSION_CODE must be a positive integer in version.properties")

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.isFile) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

val releaseStoreFilePath: String? = keystoreProps.getProperty("storeFile")?.trim()
    ?: System.getenv("KEYSTORE_FILE")?.trim()
val releaseStorePassword: String? = keystoreProps.getProperty("storePassword")?.trim()
    ?: System.getenv("KEYSTORE_PASSWORD")?.trim()
val releaseKeyAlias: String? = keystoreProps.getProperty("keyAlias")?.trim()
    ?: System.getenv("KEY_ALIAS")?.trim()
val releaseKeyPassword: String? = keystoreProps.getProperty("keyPassword")?.trim()
    ?: System.getenv("KEY_PASSWORD")?.trim()

val resolvedReleaseKeystoreFile = releaseStoreFilePath
    ?.takeIf { it.isNotEmpty() }
    ?.let { path ->
        val candidate = file(path)
        if (candidate.isAbsolute) candidate else rootProject.file(path)
    }

val hasReleaseSigning: Boolean = resolvedReleaseKeystoreFile != null &&
    resolvedReleaseKeystoreFile.isFile &&
    !releaseStorePassword.isNullOrEmpty() &&
    !releaseKeyAlias.isNullOrEmpty() &&
    !releaseKeyPassword.isNullOrEmpty()

android {
    namespace = "com.vndblite.dcj"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vndblite.dcj"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = resolvedReleaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Uses AGP's default debug signing configuration (B2)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = null
                logger.lifecycle(
                    "WARNING: Release signing is not configured (no keystore.properties or KEYSTORE_* env vars found). " +
                        "Release artifacts will be unsigned."
                )
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            assets.directories.add("../assets")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.org.json)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
