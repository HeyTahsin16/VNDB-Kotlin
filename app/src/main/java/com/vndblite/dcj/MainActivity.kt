package com.vndblite.dcj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vndblite.dcj.core.StrictModeInitializer
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.ui.navigation.AppNavHost
import com.vndblite.dcj.ui.theme.AppBackground
import com.vndblite.dcj.ui.theme.VndbTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StrictModeInitializer.installIfDebug()
        enableEdgeToEdge()

        val appContainer = (application as? VndbApplication)?.container
            ?: DefaultAppContainer(applicationContext)

        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                VndbApp()
            }
        }
    }
}

/**
 * Root application shell collecting only global theme/font settings and delegating screen state
 * to per-destination ViewModels inside [AppNavHost] (R10, A2, A3, A5, B7, U2).
 */
@Composable
fun VndbApp() {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )

    VndbTheme(
        themeId = settings.themeId,
        fontSizeOffset = settings.fontSizeOffset,
        includeBackground = false
    ) {
        AppBackground(themeId = settings.themeId) {
            AppNavHost(settings = settings)
        }
    }
}
