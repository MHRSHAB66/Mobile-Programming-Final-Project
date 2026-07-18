package com.example.project.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.project.R
import com.example.project.domain.model.AppLanguage
import com.example.project.domain.model.FontSize
import com.example.project.domain.model.ThemeMode
import com.example.project.ui.components.DetailTopBar
import com.example.project.ui.theme.LocalDimens
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.Surface

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dimens = LocalDimens.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = dimens.spaceM),
        ) {
            SettingsGroup(stringResource(R.string.settings_language)) {
                AppLanguage.entries.forEach { language ->
                    OptionRow(
                        label = stringResource(
                            if (language == AppLanguage.ENGLISH) R.string.settings_language_en
                            else R.string.settings_language_fa
                        ),
                        selected = settings.language == language,
                        onSelect = { viewModel.setLanguage(language) },
                    )
                }
            }

            SettingsGroup(stringResource(R.string.settings_theme)) {
                val themeLabels = mapOf(
                    ThemeMode.SYSTEM to R.string.settings_theme_system,
                    ThemeMode.LIGHT to R.string.settings_theme_light,
                    ThemeMode.DARK to R.string.settings_theme_dark,
                )
                ThemeMode.entries.forEach { mode ->
                    OptionRow(
                        label = stringResource(themeLabels.getValue(mode)),
                        selected = settings.themeMode == mode,
                        onSelect = { viewModel.setTheme(mode) },
                    )
                }
            }

            SettingsGroup(stringResource(R.string.settings_font_size)) {
                val fontLabels = mapOf(
                    FontSize.SMALL to R.string.settings_font_small,
                    FontSize.NORMAL to R.string.settings_font_normal,
                    FontSize.LARGE to R.string.settings_font_large,
                )
                FontSize.entries.forEach { size ->
                    OptionRow(
                        label = stringResource(fontLabels.getValue(size)),
                        selected = settings.fontSize == size,
                        onSelect = { viewModel.setFontSize(size) },
                    )
                }
                FontSizePreview()
            }

            SettingsGroup(stringResource(R.string.settings_account)) {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(
                        text = stringResource(R.string.settings_logout),
                        modifier = Modifier.padding(start = dimens.spaceS),
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLoggedOut()
                }) { Text(stringResource(R.string.settings_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val dimens = LocalDimens.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = dimens.spaceL, vertical = dimens.spaceS),
    )
    content()
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = dimens.spaceL, vertical = dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = dimens.spaceM),
        )
    }
}

@Composable
private fun FontSizePreview() {
    val dimens = LocalDimens.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.spaceL,
                vertical = dimens.spaceS,
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(dimens.spaceM),
        ) {
            Text(
                text = stringResource(R.string.settings_font_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.settings_font_preview_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spaceS),
            )
        }
    }
}
