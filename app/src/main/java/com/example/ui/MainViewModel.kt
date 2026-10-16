package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.model.DownloadState
import com.example.data.model.ModelInfo
import com.example.data.model.VerifiedModelCatalogue
import com.example.data.repository.ConversationRepository
import com.example.data.repository.ModelDownloadManager
import com.example.data.repository.ModelRepository
import com.example.engine.EngineSettings
import com.example.engine.EngineStatus
import com.example.engine.LocalInferenceEngine
import com.example.engine.MediaPipeInferenceEngine
import com.example.engine.PromptFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppThemeMode {
    SYSTEM, DARK, LIGHT
}

data class StorageReport(
    val usableSpaceBytes: Long,
    val modelsSizeBytes: Long,
    val totalRamMb: Long,
    val availableRamMb: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val conversationRepo = ConversationRepository(db.conversationDao(), db.messageDao())
    val modelRepo = ModelRepository(application)
    val downloadManager = ModelDownloadManager(application)
    val inferenceEngine: LocalInferenceEngine = MediaPipeInferenceEngine(application)

    private val prefs = application.getSharedPreferences("pico_ai_settings", Context.MODE_PRIVATE)

    // Current Conversation
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    // History & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        conversationRepo.getAllConversations(),
        _searchQuery
    ) { all, query ->
        if (query.isBlank()) all
        else all.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Models & Downloads
    val installedModels: StateFlow<List<ModelInfo>> = modelRepo.installedModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModelId: StateFlow<String?> = modelRepo.activeModelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadStates: StateFlow<Map<String, DownloadState>> = downloadManager.downloadState

    val engineStatus: StateFlow<EngineStatus> = inferenceEngine.status

    // Settings
    private val _engineSettings = MutableStateFlow(loadSettings())
    val engineSettings: StateFlow<EngineSettings> = _engineSettings.asStateFlow()

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Timers & UI feedback
    private val _generationElapsedSec = MutableStateFlow(0)
    val generationElapsedSec: StateFlow<Int> = _generationElapsedSec.asStateFlow()

    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var messagesJob: Job? = null

    init {
        // Observe conversation changes
        viewModelScope.launch {
            _currentConversationId.collectLatest { id ->
                messagesJob?.cancel()
                if (id != null) {
                    messagesJob = launch {
                        conversationRepo.getMessagesForConversation(id).collect { msgs ->
                            _currentMessages.value = msgs
                        }
                    }
                } else {
                    _currentMessages.value = emptyList()
                }
            }
        }

        // Auto-select latest conversation or create first conversation
        viewModelScope.launch {
            val convs = conversationRepo.getAllConversations()
            convs.collectLatest { list ->
                if (_currentConversationId.value == null) {
                    if (list.isNotEmpty()) {
                        _currentConversationId.value = list.first().id
                    } else {
                        startNewConversation()
                    }
                }
            }
        }

        // Try auto-loading active model on startup if present
        viewModelScope.launch {
            val active = modelRepo.getActiveModel()
            if (active?.localPath != null && !inferenceEngine.isModelLoaded()) {
                val file = File(active.localPath)
                if (file.exists()) {
                    inferenceEngine.loadModel(file, _engineSettings.value)
                }
            }
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val activeModel = modelRepo.getActiveModel()
            val newConv = conversationRepo.createNewConversation(
                initialTitle = "New Chat",
                modelUsed = activeModel?.name ?: "None"
            )
            _currentConversationId.value = newConv.id
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            conversationRepo.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        val convId = _currentConversationId.value ?: return

        generationJob?.cancel()
        timerJob?.cancel()

        generationJob = viewModelScope.launch {
            // Check model status
            if (!inferenceEngine.isModelLoaded()) {
                val active = modelRepo.getActiveModel()
                if (active?.localPath != null) {
                    val file = File(active.localPath)
                    if (file.exists()) {
                        val loadRes = inferenceEngine.loadModel(file, _engineSettings.value)
                        if (loadRes.isFailure) {
                            showNotice("Failed to load model: ${loadRes.exceptionOrNull()?.message}")
                            return@launch
                        }
                    } else {
                        showNotice("Model file not found. Please install a model in Model Manager.")
                        return@launch
                    }
                } else {
                    showNotice("No local model selected. Please download or import a model first.")
                    return@launch
                }
            }

            // 1. Add user message
            conversationRepo.addMessage(
                conversationId = convId,
                role = "user",
                content = trimmed
            )

            // Start timer
            _generationElapsedSec.value = 0
            timerJob = launch {
                while (isActive) {
                    delay(1000)
                    _generationElapsedSec.value += 1
                }
            }

            val startTime = System.currentTimeMillis()

            // 2. Build prompt with history
            val history = _currentMessages.value
            val prompt = PromptFormatter.buildPrompt(
                history = history,
                currentMessage = trimmed,
                systemPrompt = _engineSettings.value.systemPrompt
            )

            // 3. Generate genuine model response
            val result = inferenceEngine.generateResponse(prompt)
            timerJob?.cancel()

            val latency = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val output = result.getOrNull() ?: ""
                conversationRepo.addMessage(
                    conversationId = convId,
                    role = "assistant",
                    content = output,
                    latencyMs = latency
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Inference error occurred."
                conversationRepo.addMessage(
                    conversationId = convId,
                    role = "assistant",
                    content = "⚠️ $error",
                    latencyMs = latency,
                    isError = true
                )
            }
        }
    }

    fun stopGeneration() {
        inferenceEngine.cancelGeneration()
        generationJob?.cancel()
        timerJob?.cancel()
        _generationElapsedSec.value = 0
    }

    fun regenerateLastResponse() {
        val msgs = _currentMessages.value
        val lastUserMsg = msgs.lastOrNull { it.role == "user" } ?: return
        sendMessage(lastUserMsg.content)
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            val model = modelRepo.installedModels.stateIn(viewModelScope).value.find { it.id == modelId }
            if (model?.localPath == null) {
                showNotice("Model path not found.")
                return@launch
            }
            val file = File(model.localPath)
            if (!file.exists()) {
                showNotice("Model file missing from disk.")
                return@launch
            }
            modelRepo.setActiveModelId(modelId)
            val res = inferenceEngine.loadModel(file, _engineSettings.value)
            if (res.isSuccess) {
                showNotice("Successfully loaded ${model.name} into memory.")
                _currentConversationId.value?.let { cid ->
                    conversationRepo.updateConversationModel(cid, model.name)
                }
            } else {
                showNotice("Load failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun unloadModel() {
        inferenceEngine.unloadModel()
        showNotice("Model unloaded from memory.")
    }

    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            downloadManager.downloadModel(
                model = model,
                destinationDir = modelRepo.getModelsDirectory(),
                onSuccess = {
                    modelRepo.refreshModels()
                    showNotice("Successfully installed ${model.name}!")
                    // Auto-load if no model currently active
                    if (!inferenceEngine.isModelLoaded()) {
                        loadModel(model.id)
                    }
                }
            )
        }
    }

    fun cancelDownload(modelId: String) {
        downloadManager.cancelDownload(modelId)
        showNotice("Download cancelled.")
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            if (modelRepo.getActiveModel()?.id == modelId) {
                inferenceEngine.unloadModel()
            }
            val deleted = modelRepo.deleteModel(modelId)
            if (deleted) showNotice("Model deleted.")
            else showNotice("Failed to delete model file.")
        }
    }

    fun importModel(uri: Uri, fileName: String) {
        viewModelScope.launch {
            showNotice("Validating and importing model binary...")
            val result = modelRepo.importModelFromUri(uri, fileName)
            if (result.isSuccess) {
                val model = result.getOrNull()!!
                showNotice("Imported ${model.name} successfully!")
                loadModel(model.id)
            } else {
                showNotice("Import failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            conversationRepo.clearAll()
            startNewConversation()
            showNotice("All conversation history cleared.")
        }
    }

    fun clearAllModelFiles() {
        viewModelScope.launch {
            inferenceEngine.unloadModel()
            val freed = modelRepo.clearAllModels()
            showNotice("Cleared ${ModelInfo.formatBytes(freed)} of model files.")
        }
    }

    fun updateSettings(newSettings: EngineSettings) {
        _engineSettings.value = newSettings
        prefs.edit().apply {
            putFloat("temp", newSettings.temperature)
            putInt("top_k", newSettings.topK)
            putInt("max_tokens", newSettings.maxTokens)
            putString("system_prompt", newSettings.systemPrompt)
            apply()
        }
        showNotice("Inference settings saved.")
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getStorageReport(): StorageReport {
        val usable = modelRepo.getUsableSpaceBytes()
        val modelsSize = modelRepo.getTotalModelsSizeBytes()

        val actManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        return StorageReport(
            usableSpaceBytes = usable,
            modelsSizeBytes = modelsSize,
            totalRamMb = memInfo.totalMem / (1024 * 1024),
            availableRamMb = memInfo.availMem / (1024 * 1024)
        )
    }

    fun generateDiagnosticReport(): String {
        val report = getStorageReport()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val activeModel = modelRepo.getActiveModel()

        return buildString {
            appendLine("=== PICO AI DIAGNOSTIC REPORT ===")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine("App Version: 0.1.0 (Build 1)")
            appendLine("Device Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine("Total Device RAM: ${report.totalRamMb} MB")
            appendLine("Available RAM: ${report.availableRamMb} MB")
            appendLine("Free Internal Storage: ${ModelInfo.formatBytes(report.usableSpaceBytes)}")
            appendLine("Models Storage: ${ModelInfo.formatBytes(report.modelsSizeBytes)}")
            appendLine("Active Engine: MediaPipe LLM Inference (Native on-device)")
            appendLine("Active Model: ${activeModel?.name ?: "None"} (${activeModel?.format?.displayName ?: "N/A"})")
            appendLine("Engine Status: ${_engineStatusDisplay(inferenceEngine.status.value)}")
            appendLine("Privacy Guarantee: 100% On-Device, zero telemetry, zero remote inference.")
            appendLine("=================================")
        }
    }

    private fun _engineStatusDisplay(status: EngineStatus): String = when (status) {
        is EngineStatus.Unloaded -> "Unloaded"
        is EngineStatus.Loading -> "Loading ${status.modelName}..."
        is EngineStatus.Ready -> "Ready (${status.modelName}, ~${status.memoryUsedMb}MB active)"
        is EngineStatus.Generating -> "Generating response..."
        is EngineStatus.Error -> "Error: ${status.message}"
    }

    fun clearNotice() {
        _uiNotice.value = null
    }

    private fun showNotice(msg: String) {
        _uiNotice.value = msg
    }

    private fun loadSettings(): EngineSettings {
        return EngineSettings(
            temperature = prefs.getFloat("temp", 0.7f),
            topK = prefs.getInt("top_k", 40),
            maxTokens = prefs.getInt("max_tokens", 1024),
            systemPrompt = prefs.getString("system_prompt", EngineSettings.DEFAULT_SYSTEM_PROMPT)
                ?: EngineSettings.DEFAULT_SYSTEM_PROMPT
        )
    }
}
