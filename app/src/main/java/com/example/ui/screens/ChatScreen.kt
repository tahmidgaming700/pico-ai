package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.MessageEntity
import com.example.engine.EngineStatus
import com.example.ui.MainViewModel
import com.example.ui.components.MarkdownFormattedContent
import com.example.ui.theme.PicoCyan
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMessages by viewModel.currentMessages.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()
    val elapsedSec by viewModel.generationElapsedSec.collectAsState()
    val activeModelId by viewModel.activeModelId.collectAsState()
    val installedModels by viewModel.installedModels.collectAsState()
    val activeModel = installedModels.find { it.id == activeModelId }

    var inputText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    val isGenerating = engineStatus is EngineStatus.Generating
    val listState = rememberLazyListState()

    // Auto-scroll when messages update or during generation
    LaunchedEffect(currentMessages.size, isGenerating) {
        if (currentMessages.isNotEmpty()) {
            listState.animateScrollToItem(currentMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopChatBar(
            activeModelName = activeModel?.name,
            engineStatus = engineStatus,
            onModelChipClick = onNavigateToModels,
            onNewChatClick = { viewModel.startNewConversation() },
            onShareConversationClick = {
                val currentConvId = viewModel.currentConversationId.value
                if (currentConvId != null) {
                    kotlinx.coroutines.MainScope().run {
                        // Launch export share
                        val text = "Pico AI Conversation Export"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Conversation"))
                    }
                }
            },
            onClearChatClick = {
                viewModel.clearAllHistory()
            }
        )

        // Banner if no model loaded
        if (activeModel == null || !viewModel.inferenceEngine.isModelLoaded()) {
            NoModelBanner(onDownloadClick = onNavigateToModels)
        }

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (currentMessages.isEmpty()) {
                EmptyChatWelcome(
                    onStarterPromptClick = { prompt ->
                        inputText = prompt
                        viewModel.sendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(currentMessages, key = { it.id }) { message ->
                        val isLastAssistant = message.id == currentMessages.lastOrNull { it.role == "assistant" }?.id
                        MessageItem(
                            message = message,
                            isLastAssistant = isLastAssistant,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Pico AI Response", message.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message.content)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share response"))
                            },
                            onRegenerate = {
                                viewModel.regenerateLastResponse()
                            }
                        )
                    }

                    // Generation In Progress Indicator
                    if (isGenerating) {
                        item {
                            GeneratingIndicator(elapsedSec = elapsedSec, onStop = { viewModel.stopGeneration() })
                        }
                    }
                }
            }
        }

        // Bottom Composer
        MessageComposer(
            inputText = inputText,
            onInputTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank() && !isGenerating) {
                    val prompt = inputText
                    inputText = ""
                    viewModel.sendMessage(prompt)
                }
            },
            onStop = { viewModel.stopGeneration() },
            isGenerating = isGenerating,
            hasLoadedModel = viewModel.inferenceEngine.isModelLoaded() || activeModel != null
        )
    }
}

@Composable
private fun TopChatBar(
    activeModelName: String?,
    engineStatus: EngineStatus,
    onModelChipClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onShareConversationClick: () -> Unit,
    onClearChatClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, PicoNavyBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pico AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PicoNavyCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ON-DEVICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PicoCyan
                    )
                }
            }

            // Model status chip
            Row(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavyDarkest)
                    .clickable(onClick = onModelChipClick)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = when (engineStatus) {
                    is EngineStatus.Ready -> PicoSuccess
                    is EngineStatus.Loading, is EngineStatus.Generating -> PicoWarning
                    is EngineStatus.Error -> MaterialTheme.colorScheme.error
                    else -> PicoTextMuted
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = activeModelName ?: "No model loaded",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onNewChatClick,
            modifier = Modifier.testTag("new_chat_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Conversation",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.testTag("chat_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(PicoNavyCard)
            ) {
                DropdownMenuItem(
                    text = { Text("Share conversation", color = PicoTextPrimary) },
                    onClick = {
                        menuExpanded = false
                        onShareConversationClick()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null, tint = PicoCyan)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear all history", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        onClearChatClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun NoModelBanner(onDownloadClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            .clickable(onClick = onDownloadClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = PicoWarning,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "No on-device model active",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = PicoWarning
            )
            Text(
                text = "Tap here to download or import a lightweight model (e.g. SmolLM 135M or Gemma 3 1B) to start chatting offline.",
                style = MaterialTheme.typography.bodySmall,
                color = PicoTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download Model",
            tint = PicoCyan,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MessageItem(
    message: MessageEntity,
    isLastAssistant: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            if (!isUser) {
                // AI Avatar Icon
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PicoNavyDarkest)
                        .border(1.dp, PicoCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Pico AI",
                        tint = PicoCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(
                            if (isUser) Color(0xFF1E3A8A) // Sapphire Blue
                            else if (message.isError) Color(0xFF450A0A)
                            else PicoNavySurface
                        )
                        .border(
                            1.dp,
                            if (isUser) Color(0xFF2563EB)
                            else if (message.isError) Color(0xFFDC2626)
                            else PicoNavyBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    MarkdownFormattedContent(
                        text = message.content,
                        textColor = if (isUser) Color(0xFFEFF6FF) else PicoTextPrimary
                    )
                }

                // Metadata & Quick Actions
                Row(
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = PicoTextMuted,
                        fontSize = 10.sp
                    )
                    if (message.latencyMs > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${String.format("%.1f", message.latencyMs / 1000.0)}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = PicoTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    if (!isUser && !message.isError) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("copy_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = PicoTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("share_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share message",
                                tint = PicoTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (isLastAssistant) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("regenerate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = PicoTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratingIndicator(elapsedSec: Int, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PicoNavySurface)
                .border(1.dp, PicoCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = PicoCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generating on-device (${elapsedSec}s)...",
                    style = MaterialTheme.typography.bodySmall,
                    color = PicoCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("stop_generation_indicator")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(onStarterPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(2.dp, PicoCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = PicoCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pico AI",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = PicoTextPrimary
        )
        Text(
            text = "100% On-Device • Zero API Keys • Totally Free",
            style = MaterialTheme.typography.bodyMedium,
            color = PicoCyan,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your private on-device intelligence. No chat telemetry or cloud inference.",
            style = MaterialTheme.typography.bodySmall,
            color = PicoTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "TRY ASKING",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = PicoTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        val prompts = listOf(
            "Explain quantum computing in simple terms",
            "Write a Python script to sort files by extension",
            "Give me a 3-day full body workout plan",
            "Help me write a concise follow-up email"
        )

        prompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PicoNavySurface)
                    .border(1.dp, PicoNavyBorder, RoundedCornerShape(12.dp))
                    .clickable { onStarterPromptClick(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PicoTextPrimary
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    hasLoadedModel: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PicoNavyBorder)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = if (hasLoadedModel) "Message Pico AI..." else "Select/download a model first...",
                        color = PicoTextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input_field"),
                shape = RoundedCornerShape(20.dp),
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

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                        .testTag("stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop generation",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) PicoCyan else PicoNavyCard)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (inputText.isNotBlank()) PicoNavyDarkest else PicoTextMuted
                    )
                }
            }
        }
    }
}
