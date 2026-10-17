package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadState
import com.example.data.model.ModelInfo
import com.example.data.model.VerifiedModelCatalogue
import com.example.engine.EngineStatus
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

@Composable
fun ModelManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedModels by viewModel.installedModels.collectAsState()
    val activeModelId by viewModel.activeModelId.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Installed, 1: Catalogue
    var deleteConfirmModel by remember { mutableStateOf<ModelInfo?>(null) }
    var ramWarningModel by remember { mutableStateOf<ModelInfo?>(null) }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported_model.bin"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.importModel(uri, fileName)
        }
    }

    val storageReport = remember { viewModel.getStorageReport() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, PicoNavyBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Model Manager",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage on-device neural model weights",
                    style = MaterialTheme.typography.bodySmall,
                    color = PicoTextSecondary
                )
            }

            OutlinedButton(
                onClick = { importLauncher.launch("*/*") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PicoCyan),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PicoCyan)),
                modifier = Modifier.testTag("import_model_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import")
            }
        }

        // Storage & RAM Summary Card
        StorageMetricsCard(
            usableSpace = storageReport.usableSpaceBytes,
            modelsSize = storageReport.modelsSizeBytes,
            totalRamMb = storageReport.totalRamMb,
            availRamMb = storageReport.availableRamMb
        )

        // Active Engine Status Bar
        ActiveEngineBar(
            engineStatus = engineStatus,
            activeModel = installedModels.find { it.id == activeModelId },
            onUnload = { viewModel.unloadModel() }
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PicoNavySurface,
            contentColor = PicoCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PicoCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Installed (${installedModels.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Model Catalogue", fontWeight = FontWeight.SemiBold) }
            )
        }

        // Content
        if (selectedTab == 0) {
            InstalledModelsList(
                installedModels = installedModels,
                activeModelId = activeModelId,
                engineStatus = engineStatus,
                onLoad = { model ->
                    if (model.minRamMb > storageReport.totalRamMb) {
                        ramWarningModel = model
                    } else {
                        viewModel.loadModel(model.id)
                    }
                },
                onDelete = { deleteConfirmModel = it },
                onNavigateToCatalogue = { selectedTab = 1 }
            )
        } else {
            CatalogueModelsList(
                catalogue = VerifiedModelCatalogue.verifiedModels,
                installedModels = installedModels,
                downloadStates = downloadStates,
                onDownload = { model ->
                    viewModel.downloadModel(model)
                },
                onCancel = { modelId ->
                    viewModel.cancelDownload(modelId)
                }
            )
        }
    }

    // Delete Confirmation Dialog
    deleteConfirmModel?.let { model ->
        AlertDialog(
            onDismissRequest = { deleteConfirmModel = null },
            title = { Text("Delete Model?", color = PicoTextPrimary) },
            text = { Text("Are you sure you want to delete ${model.name}? This will free up ${model.formattedSize} of storage.", color = PicoTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteModel(model.id)
                        deleteConfirmModel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmModel = null }) {
                    Text("Cancel", color = PicoTextSecondary)
                }
            },
            containerColor = PicoNavyCard
        )
    }

    // RAM Warning Dialog
    ramWarningModel?.let { model ->
        AlertDialog(
            onDismissRequest = { ramWarningModel = null },
            title = { Text("High Memory Requirement", color = PicoWarning) },
            text = {
                Text(
                    "This model recommends at least ${model.minRamMb} MB of RAM, while your device has ${storageReport.totalRamMb} MB total RAM. Running it may cause performance slowdowns or memory eviction on devices like the Huawei Y9 2019.\n\nDo you want to proceed loading anyway?",
                    color = PicoTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadModel(model.id)
                        ramWarningModel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PicoWarning, contentColor = PicoNavyDarkest)
                ) {
                    Text("Load Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { ramWarningModel = null }) {
                    Text("Cancel", color = PicoTextSecondary)
                }
            },
            containerColor = PicoNavyCard
        )
    }
}

@Composable
private fun StorageMetricsCard(
    usableSpace: Long,
    modelsSize: Long,
    totalRamMb: Long,
    availRamMb: Long
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PicoNavySurface)
            .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = PicoCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Free Storage", style = MaterialTheme.typography.labelMedium, color = PicoTextMuted)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ModelInfo.formatBytes(usableSpace),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PicoTextPrimary
                )
                Text(
                    text = "Models: ${ModelInfo.formatBytes(modelsSize)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PicoTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = PicoViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RAM Footprint", style = MaterialTheme.typography.labelMedium, color = PicoTextMuted)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$availRamMb MB / $totalRamMb MB",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PicoTextPrimary
                )
                Text(
                    text = "Available / Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = PicoTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ActiveEngineBar(
    engineStatus: EngineStatus,
    activeModel: ModelInfo?,
    onUnload: () -> Unit
) {
    val isLoaded = engineStatus is EngineStatus.Ready || engineStatus is EngineStatus.Generating
    val statusText = when (engineStatus) {
        is EngineStatus.Ready -> "Ready: ${activeModel?.name ?: "Model"} (~${engineStatus.memoryUsedMb}MB RAM)"
        is EngineStatus.Loading -> "Loading ${engineStatus.modelName} into memory..."
        is EngineStatus.Generating -> "Generating response..."
        is EngineStatus.Error -> "Error: ${engineStatus.message}"
        is EngineStatus.Unloaded -> "Engine Idle (No model in memory)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PicoNavyCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotColor = when (engineStatus) {
            is EngineStatus.Ready -> PicoSuccess
            is EngineStatus.Loading, is EngineStatus.Generating -> PicoWarning
            is EngineStatus.Error -> PicoError
            else -> PicoTextMuted
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = PicoTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isLoaded) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onUnload,
                modifier = Modifier.testTag("unload_model_button")
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PicoWarning)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Unload", color = PicoWarning, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InstalledModelsList(
    installedModels: List<ModelInfo>,
    activeModelId: String?,
    engineStatus: EngineStatus,
    onLoad: (ModelInfo) -> Unit,
    onDelete: (ModelInfo) -> Unit,
    onNavigateToCatalogue: () -> Unit
) {
    if (installedModels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = PicoTextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No models installed yet", style = MaterialTheme.typography.titleMedium, color = PicoTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Download a verified model from our curated catalogue or import a local file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PicoTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToCatalogue,
                    colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest)
                ) {
                    Text("Explore Catalogue")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            items(installedModels, key = { it.id }) { model ->
                val isActive = model.id == activeModelId
                val isLoaded = isActive && (engineStatus is EngineStatus.Ready || engineStatus is EngineStatus.Generating)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PicoNavySurface)
                        .border(
                            1.dp,
                            if (isActive) PicoCyan else PicoNavyBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isActive) PicoCyan else PicoTextPrimary
                                )
                                Text(
                                    text = "${model.publisher} • ${model.formattedSize} • ${model.format.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PicoTextSecondary
                                )
                            }

                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isLoaded) PicoSuccess.copy(alpha = 0.2f) else PicoNavyCard)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isLoaded) "ACTIVE & LOADED" else "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLoaded) PicoSuccess else PicoCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = PicoTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isLoaded) {
                                Button(
                                    onClick = { onLoad(model) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("load_model_${model.id}")
                                ) {
                                    Text("Load into Memory")
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = { onDelete(model) },
                                modifier = Modifier.testTag("delete_model_${model.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete model", tint = PicoError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogueModelsList(
    catalogue: List<ModelInfo>,
    installedModels: List<ModelInfo>,
    downloadStates: Map<String, DownloadState>,
    onDownload: (ModelInfo) -> Unit,
    onCancel: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PicoCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SmolLM 135M & Qwen 1.5B download directly without authentication. Gated models (Gemma) require a free Hugging Face token in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PicoTextSecondary
                    )
                }
            }
        }

        items(catalogue, key = { it.id }) { model ->
            val isInstalled = installedModels.any { it.id == model.id }
            val downloadState = downloadStates[model.id] ?: DownloadState.Idle

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PicoTextPrimary
                            )
                            Text(
                                text = "${model.publisher} • ~${model.formattedSize} • Parameters: ${model.parameterCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PicoCyan
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (model.isGated) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33F59E0B))
                                        .border(1.dp, PicoWarning, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gated",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PicoWarning
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PicoNavyCard)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = model.licenseName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PicoTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = PicoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = PicoTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recommended RAM: ${model.minRamMb} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = PicoTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { downloadState.progressPercent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = PicoCyan,
                                        trackColor = PicoNavyBorder
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${downloadState.progressPercent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PicoCyan
                                    )
                                    IconButton(
                                        onClick = { onCancel(model.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = PicoError, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = "${ModelInfo.formatBytes(downloadState.bytesDownloaded)} / ~${model.formattedSize} (${ModelInfo.formatBytes(downloadState.speedBytesPerSec)}/s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PicoTextMuted
                                )
                            }
                        }
                        is DownloadState.VerifyingChecksum -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PicoCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying binary checksum & header...", style = MaterialTheme.typography.bodySmall, color = PicoCyan)
                            }
                        }
                        is DownloadState.Failed -> {
                            Column {
                                Text("Failed: ${downloadState.error}", style = MaterialTheme.typography.bodySmall, color = PicoError)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onDownload(model) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry Download")
                                }
                            }
                        }
                        else -> {
                            if (isInstalled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PicoSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Installed", style = MaterialTheme.typography.labelMedium, color = PicoSuccess)
                                }
                            } else {
                                Button(
                                    onClick = { onDownload(model) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PicoCyan, contentColor = PicoNavyDarkest),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("download_model_${model.id}")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download (${model.formattedSize})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
