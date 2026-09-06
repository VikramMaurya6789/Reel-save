package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ReelTopBar
import com.example.ui.theme.StatusError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ReelTopBar(title = "Settings")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Section: Downloads
                SettingsSectionHeader("Downloads")

                SettingsClickableItem(
                    title = "Default quality",
                    subtitle = uiState.settings.defaultQuality,
                    onClick = { showQualityDialog = true }
                )

                SettingsInfoItem(
                    title = "Save location",
                    subtitle = uiState.settings.saveLocation
                )

                SettingsSwitchItem(
                    title = "Auto-save to Gallery",
                    subtitle = "Store videos directly in system media gallery",
                    checked = uiState.settings.autoSaveToGallery,
                    onCheckedChange = { viewModel.updateAutoSave(it) }
                )

                SettingsSwitchItem(
                    title = "Download over Wi-Fi only",
                    subtitle = "Avoid using mobile data for Reel downloads",
                    checked = uiState.settings.wifiOnly,
                    onCheckedChange = { viewModel.updateWifiOnly(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Section: Behavior
                SettingsSectionHeader("Behavior")

                SettingsSwitchItem(
                    title = "Clipboard link detection",
                    subtitle = "Suggest download when a copied Reel link is detected",
                    checked = uiState.settings.clipboardDetection,
                    onCheckedChange = { viewModel.updateClipboardDetection(it) }
                )

                SettingsSwitchItem(
                    title = "Confirm before download",
                    subtitle = "Prompt for confirmation before starting download",
                    checked = uiState.settings.confirmBeforeDownload,
                    onCheckedChange = { viewModel.updateConfirmBeforeDownload(it) }
                )

                SettingsSwitchItem(
                    title = "Open downloaded Reel automatically",
                    subtitle = "Launch video in viewer as soon as download completes",
                    checked = uiState.settings.openAutomatically,
                    onCheckedChange = { viewModel.updateOpenAutomatically(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Section: Appearance
                SettingsSectionHeader("Appearance")

                SettingsClickableItem(
                    title = "Theme",
                    subtitle = when (uiState.settings.themeMode) {
                        "LIGHT" -> "Light"
                        "DARK" -> "Dark"
                        else -> "System default"
                    },
                    onClick = { showThemeDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Section: Storage
                SettingsSectionHeader("Storage")

                SettingsInfoItem(
                    title = "Storage used by cache",
                    subtitle = uiState.storageUsedFormatted
                )

                SettingsClickableItem(
                    title = "Clear temporary cache",
                    subtitle = "Free temporary thumbnails and network buffer files",
                    onClick = {
                        viewModel.clearCache()
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsClickableItem(
                    title = "Clear history",
                    subtitle = "Remove past search and download records",
                    onClick = { viewModel.showClearHistoryDialog(true) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                // Section: About & Legal
                SettingsSectionHeader("About")

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ReelSave",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Save Reels. Keep them handy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Version 1.0.0 (Build 100)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ReelSave is an independent application and is not affiliated with, sponsored by, or endorsed by Instagram. Users are responsible for ensuring that they have the necessary rights or permission to download, save, or reuse content.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsClickableItem(
                    title = "Privacy Policy",
                    subtitle = "Read our data handling practices",
                    onClick = { viewModel.showPrivacyDialog(true) }
                )

                SettingsClickableItem(
                    title = "Terms of Service",
                    subtitle = "Acceptable use and copyright terms",
                    onClick = { viewModel.showTermsDialog(true) }
                )

                SettingsClickableItem(
                    title = "Open-source licenses",
                    subtitle = "Third-party libraries used in ReelSave",
                    onClick = { viewModel.showLicensesDialog(true) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        val qualities = listOf("Original", "1080p", "720p", "480p")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default Quality") },
            text = {
                Column {
                    qualities.forEach { q ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = uiState.settings.defaultQuality == q,
                                onClick = {
                                    viewModel.updateDefaultQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(q)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        val themes = listOf(
            "SYSTEM" to "System default",
            "LIGHT" to "Light",
            "DARK" to "Dark"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    themes.forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = uiState.settings.themeMode == mode,
                                onClick = {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear History Dialog
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearHistoryDialog(false) },
            title = { Text("Clear all history?", fontWeight = FontWeight.Bold) },
            text = { Text("Your search and download history log will be removed. Saved media files in your Gallery will remain safe.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearHistoryDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (uiState.showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showPrivacyDialog(false) },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "ReelSave values your privacy:\n\n" +
                            "• No account or login required.\n" +
                            "• No tracking cookies or personal data stored.\n" +
                            "• ReelSave never accesses or saves your Instagram credentials.\n" +
                            "• Clipboard text is only inspected locally on device when a supported URL is present and never transmitted without your explicit interaction.\n" +
                            "• All downloaded content is stored locally in your device Gallery under Movies/ReelSave.\n" +
                            "• No personal data is sold or shared."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.showPrivacyDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (uiState.showTermsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showTermsDialog(false) },
            title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Terms of Use:\n\n" +
                            "• ReelSave is a tool designed solely for saving publicly accessible content that users are authorized to download.\n" +
                            "• You agree not to use ReelSave to infringe intellectual property rights or download content for unauthorized commercial reproduction.\n" +
                            "• ReelSave is an independent application and is not affiliated with, sponsored by, or endorsed by Instagram.\n" +
                            "• Users assume full responsibility for respecting creators' copyrights and terms."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.showTermsDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (uiState.showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLicensesDialog(false) },
            title = { Text("Open-Source Licenses", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "ReelSave utilizes the following open-source software:\n\n" +
                            "• Android Jetpack Compose (Apache 2.0)\n" +
                            "• AndroidX Room & WorkManager (Apache 2.0)\n" +
                            "• AndroidX Media3 / ExoPlayer (Apache 2.0)\n" +
                            "• OkHttp (Square, Inc. - Apache 2.0)\n" +
                            "• Coil Image Loader (Apache 2.0)\n" +
                            "• Kotlin Coroutines (Apache 2.0)"
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.showLicensesDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
