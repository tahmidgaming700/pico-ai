package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineSettings
import com.example.ui.AppThemeMode
import com.example.ui.MainViewModel
import com.example.ui.theme.PicoCyan
import com.example.ui.theme.PicoError
import com.example.ui.theme.PicoNavyBorder
import com.example.ui.theme.PicoNavyCard
import com.example.ui.theme.PicoNavyDarkest
import com.example.ui.theme.PicoNavySurface
import com.example.ui.theme.PicoSuccess
import com.example.ui.theme.PicoTextMuted
import com.example.ui.theme.PicoTextPrimary
import com.example.ui.theme.PicoTextSecondary
import com.example.ui.theme.PicoViolet
import com.example.ui.theme.PicoWarning
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onShowAboutPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by viewModel.engineSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val savedHfToken by viewModel.hfToken.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()

    var temperature by remember(currentSettings) { mutableFloatStateOf(currentSettings.temperature) }
    var topK by remember(currentSettings) { mutableIntStateOf(currentSettings.topK) }
    var maxTokens by remember(currentSettings) { mutableIntStateOf(currentSettings.maxTokens) }
    var systemPrompt by remember(currentSettings) { mutableStateOf(currentSettings.systemPrompt) }
    var hfTokenInput by remember(savedHfToken) { mutableStateOf(savedHfToken) }

    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showClearModelsConfirm by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, PicoNavyBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings & Updates",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    viewModel.updateSettings(
                        EngineSettings(
                            temperature = temperature,
                            topK = topK,
                            maxTokens = maxTokens,
                            systemPrompt = systemPrompt
                        )
                    )
                    Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Weekly App Updates Section
            SectionHeader(title = "WEEKLY APP UPDATES", icon = Icons.Default.SystemUpdate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pico AI v0.1.0", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PicoTextPrimary)
                            Text("Automatic weekly CI/CD updates from GitHub", style = MaterialTheme.typography.bodySmall, color = PicoTextSecondary)
                        }

                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PicoCyan)
                        } else {
                            Button(
                                onClick = { viewModel.checkForAppUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("check_updates_button")
                            ) {
                                Text("Check Updates")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "New releases are packaged and published every week with model optimizations, engine updates, and stability fixes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PicoTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hugging Face Authentication Section (Fixes 403 Errors)
            SectionHeader(title = "HUGGING FACE TOKEN (FIXES 403/401 ERRORS)", icon = Icons.Default.Key)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Hugging Face Access Token (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PicoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gated models (such as Google Gemma) require accepting their license terms on Hugging Face. Adding your free Read token here authorizes direct in-app downloads without 403 Forbidden errors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PicoTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hfTokenInput,
                        onValueChange = { hfTokenInput = it },
                        placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxx", color = PicoTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("hf_token_field"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PicoCyan,
                            unfocusedBorderColor = PicoNavyBorder,
                            focusedTextColor = PicoTextPrimary,
                            unfocusedTextColor = PicoTextPrimary,
                            focusedContainerColor = PicoNavyDarkest,
                            unfocusedContainerColor = PicoNavyDarkest
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row {
                        Button(
                            onClick = {
                                viewModel.saveHfToken(hfTokenInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_hf_token_button")
                        ) {
                            Text("Save Token")
                        }

                        if (hfTokenInput.isNotBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    hfTokenInput = ""
                                    viewModel.saveHfToken("")
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme Section
            SectionHeader(title = "APPEARANCE", icon = Icons.Default.ColorLens)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(AppThemeMode.DARK) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            colors = RadioButtonDefaults.colors(selectedColor = PicoCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark (Deep Navy & Cyan)", color = PicoTextPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            colors = RadioButtonDefaults.colors(selectedColor = PicoCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Light", color = PicoTextPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                            colors = RadioButtonDefaults.colors(selectedColor = PicoCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Follow System", color = PicoTextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inference Parameters Section
            SectionHeader(title = "INFERENCE PARAMETERS", icon = Icons.Default.Tune)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temperature", style = MaterialTheme.typography.bodyMedium, color = PicoTextPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = String.format("%.2f", temperature),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PicoCyan
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = (it * 100).roundToInt() / 100f },
                        valueRange = 0.1f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = PicoCyan, activeTrackColor = PicoCyan)
                    )
                    Text("Controls response randomness. Lower values are more factual; higher are creative.", style = MaterialTheme.typography.labelSmall, color = PicoTextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Top-K
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top-K Sampling", style = MaterialTheme.typography.bodyMedium, color = PicoTextPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$topK",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PicoCyan
                        )
                    }
                    Slider(
                        value = topK.toFloat(),
                        onValueChange = { topK = it.toInt() },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = PicoCyan, activeTrackColor = PicoCyan)
                    )
                    Text("Filters the next token options to top K probabilities.", style = MaterialTheme.typography.labelSmall, color = PicoTextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Max Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Max Generation Tokens", style = MaterialTheme.typography.bodyMedium, color = PicoTextPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$maxTokens",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PicoCyan
                        )
                    }
                    Slider(
                        value = maxTokens.toFloat(),
                        onValueChange = { maxTokens = it.toInt() },
                        valueRange = 128f..2048f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = PicoCyan, activeTrackColor = PicoCyan)
                    )
                    Text("Upper limit of generated tokens per response turn.", style = MaterialTheme.typography.labelSmall, color = PicoTextMuted)

                    Spacer(modifier = Modifier.height(16.dp))

                    // System Prompt
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("System Prompt", style = MaterialTheme.typography.bodyMedium, color = PicoTextPrimary)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { systemPrompt = EngineSettings.DEFAULT_SYSTEM_PROMPT }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = PicoCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp, color = PicoCyan)
                        }
                    }
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PicoCyan,
                            unfocusedBorderColor = PicoNavyBorder,
                            focusedTextColor = PicoTextPrimary,
                            unfocusedTextColor = PicoTextPrimary,
                            focusedContainerColor = PicoNavyDarkest,
                            unfocusedContainerColor = PicoNavyDarkest
                        ),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hardware & Diagnostics Section
            SectionHeader(title = "HARDWARE & DIAGNOSTICS", icon = Icons.Default.BugReport)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    DiagnosticRow(label = "Device", value = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                    DiagnosticRow(label = "Target Test Platform", value = "Huawei Y9 2019 (JKM-LX2, Kirin 710)")
                    DiagnosticRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    DiagnosticRow(label = "Primary ABI", value = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
                    DiagnosticRow(label = "App Version", value = "0.1.0 (Build 1)")
                    DiagnosticRow(label = "License", value = "Apache-2.0")

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val report = viewModel.generateDiagnosticReport()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Diagnostic Report"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PicoNavyCard, contentColor = PicoCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_diagnostics_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Privacy-Safe Diagnostic Report")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Data & Storage Management
            SectionHeader(title = "STORAGE MANAGEMENT", icon = Icons.Default.Delete)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    OutlinedButton(
                        onClick = { showClearHistoryConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PicoError),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Chat History")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showClearModelsConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PicoError),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete All Downloaded Models")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About & Privacy
            SectionHeader(title = "ABOUT & PRIVACY", icon = Icons.Default.Policy)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onShowAboutPrivacy)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Guarantee & Licenses", style = MaterialTheme.typography.titleMedium, color = PicoTextPrimary)
                        Text("100% on-device AI. Zero telemetry, no cloud logging.", style = MaterialTheme.typography.bodySmall, color = PicoCyan)
                    }
                    Icon(Icons.Default.Info, contentDescription = null, tint = PicoCyan)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // App Update Dialog
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = PicoCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (info.hasUpdate) "New Update Available!" else "Up to Date",
                        color = PicoTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Current: v${info.currentVersion} • Latest: v${info.latestVersion}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PicoCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = info.releaseTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PicoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = PicoTextSecondary
                    )
                }
            },
            confirmButton = {
                if (info.hasUpdate && info.downloadUrl != null) {
                    Button(
                        onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                            context.startActivity(browserIntent)
                            viewModel.dismissUpdateDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest)
                    ) {
                        Text("Download APK")
                    }
                } else {
                    Button(
                        onClick = { viewModel.dismissUpdateDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest)
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (info.hasUpdate) {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Later", color = PicoTextSecondary)
                    }
                }
            },
            containerColor = PicoNavyCard
        )
    }

    // Clear History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All Chat History?", color = PicoTextPrimary) },
            text = { Text("This will permanently delete all stored conversations from your device.", color = PicoTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = PicoTextSecondary)
                }
            },
            containerColor = PicoNavyCard
        )
    }

    // Clear Models Dialog
    if (showClearModelsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearModelsConfirm = false },
            title = { Text("Delete All Models?", color = PicoTextPrimary) },
            text = { Text("This will remove all downloaded model weights and free up disk space.", color = PicoTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllModelFiles()
                        showClearModelsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Models")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearModelsConfirm = false }) {
                    Text("Cancel", color = PicoTextSecondary)
                }
            },
            containerColor = PicoNavyCard
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PicoCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = PicoCyan
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PicoTextMuted,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = PicoTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
