package com.vndblite.dcj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vndblite.dcj.di.AppContainer
import com.vndblite.dcj.di.DefaultAppContainer
import com.vndblite.dcj.domain.model.AppSettings
import com.vndblite.dcj.ui.components.LoadingStateView
import com.vndblite.dcj.ui.navigation.AppNavHost
import com.vndblite.dcj.ui.theme.VndbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as? VndbApplication)?.container
            ?: DefaultAppContainer(applicationContext)
        setContent {
            VndbRootContent(container = appContainer)
        }
    }
}

@Composable
fun VndbRootContent(container: AppContainer) {
    val settings by container.settingsRepository
        .settingsFlow
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    var migrationReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(container) {
        if (!migrationReady) {
            container.legacyPrefsMigrator.migrateIfNeeded()
            migrationReady = true
        }
    }

    VndbTheme(
        themeId = settings.themeId,
        fontSizeOffset = settings.fontSizeOffset
    ) {
        if (!migrationReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingStateView(message = stringResource(R.string.migration_in_progress))
            }
        } else {
            AppNavHost(container = container)
        }
    }
}

@Composable
fun VndbApp() {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as? VndbApplication)?.container
            ?: DefaultAppContainer(context.applicationContext)
    }
    VndbRootContent(container = container)
}
