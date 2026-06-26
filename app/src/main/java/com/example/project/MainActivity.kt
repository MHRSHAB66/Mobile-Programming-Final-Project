package com.example.project

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.core.locale.LocaleManager
import com.example.project.domain.model.UserSettings
import com.example.project.domain.repository.SettingsRepository
import com.example.project.ui.MainScreen
import com.example.project.ui.auth.AuthScreen
import com.example.project.ui.theme.ProjectTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private var initialSettings: UserSettings = UserSettings()

    override fun attachBaseContext(newBase: Context) {
        // Read persisted settings once before the UI inflates: apply the language (locale +
        // RTL/LTR) and capture the initial login state so the first frame is correct.
        initialSettings = runBlocking { settingsRepository.settings.first() }
        super.attachBaseContext(LocaleManager.wrap(newBase, initialSettings.language.tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appliedLanguageTag = initialSettings.language.tag
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = initialSettings)

            // Re-create the Activity when the language changes so the new locale is applied.
            LaunchedEffect(settings.language) {
                if (settings.language.tag != appliedLanguageTag) recreate()
            }

            ProjectTheme(themeMode = settings.themeMode) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale * settings.fontSize.scale,
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        // Root switch: logged in → main app, logged out → simulated auth screen.
                        if (settings.isLoggedIn) MainScreen() else AuthScreen()
                    }
                }
            }
        }
    }
}
