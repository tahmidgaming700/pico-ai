package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.ModelFormat
import com.example.data.model.ModelInfo
import com.example.data.model.ModelValidator
import com.example.data.model.VerifiedModelCatalogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ModelRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("pico_ai_models", Context.MODE_PRIVATE)
    private val modelsDir: File = File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    private val _installedModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val installedModels: Flow<List<ModelInfo>> = _installedModels.asStateFlow()

    private val _activeModelId = MutableStateFlow<String?>(prefs.getString(KEY_ACTIVE_MODEL_ID, null))
    val activeModelId: Flow<String?> = _activeModelId.asStateFlow()

    init {
        refreshModels()
    }

    fun getModelsDirectory(): File = modelsDir

    fun refreshModels() {
        val files = modelsDir.listFiles() ?: emptyArray()
        val list = mutableListOf<ModelInfo>()

        for (file in files) {
            if (file.isFile && !file.name.endsWith(".tmp")) {
                val validation = ModelValidator.validateModelFile(file)
                val isValid = validation is ModelValidator.ValidationResult.Valid

                // Check if matches known catalogue
                val baseName = file.nameWithoutExtension
                val catalogueMatch = VerifiedModelCatalogue.verifiedModels.find { it.id == baseName }

                val model = if (catalogueMatch != null) {
                    catalogueMatch.copy(
                        isDownloaded = true,
                        localFileSizeBytes = file.length(),
                        localPath = file.absolutePath,
                        isVerified = isValid
                    )
                } else {
                    val format = when {
                        file.name.endsWith(".task") -> ModelFormat.MEDIA_PIPE_TASK
                        file.name.endsWith(".gguf") -> ModelFormat.GGUF
                        else -> ModelFormat.LITE_RT_BIN
                    }
                    ModelInfo(
                        id = file.name,
                        name = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').capitalizeWords(),
                        publisher = "Imported Local Model",
                        format = format,
                        licenseName = "Locally Provided",
                        licenseUrl = "",
                        approxSizeBytes = file.length(),
                        isDownloaded = true,
                        localFileSizeBytes = file.length(),
                        localPath = file.absolutePath,
                        isVerified = isValid,
                        description = "Manually imported model weight file located at ${file.name}."
                    )
                }
                list.add(model)
            }
        }

        _installedModels.value = list

        // If active model is no longer installed, update activeModelId
        val currentActive = _activeModelId.value
        if (currentActive != null && list.none { it.id == currentActive }) {
            val fallback = list.firstOrNull()?.id
            setActiveModelId(fallback)
        } else if (currentActive == null && list.isNotEmpty()) {
            setActiveModelId(list.first().id)
        }
    }

    fun setActiveModelId(modelId: String?) {
        _activeModelId.value = modelId
        prefs.edit().apply {
            if (modelId != null) putString(KEY_ACTIVE_MODEL_ID, modelId)
            else remove(KEY_ACTIVE_MODEL_ID)
            apply()
        }
    }

    fun getActiveModel(): ModelInfo? {
        val id = _activeModelId.value ?: return null
        return _installedModels.value.find { it.id == id }
    }

    fun deleteModel(modelId: String): Boolean {
        val model = _installedModels.value.find { it.id == modelId } ?: return false
        val path = model.localPath ?: return false
        val file = File(path)
        val deleted = if (file.exists()) file.delete() else false
        if (modelId == _activeModelId.value) {
            setActiveModelId(null)
        }
        refreshModels()
        return deleted
    }

    fun clearAllModels(): Long {
        var freedBytes = 0L
        modelsDir.listFiles()?.forEach {
            freedBytes += it.length()
            it.delete()
        }
        setActiveModelId(null)
        refreshModels()
        return freedBytes
    }

    suspend fun importModelFromUri(uri: Uri, fileName: String): Result<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(modelsDir, "import_${System.currentTimeMillis()}.tmp")
            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open selected file stream."))

            // Validate header
            val validation = ModelValidator.validateModelFile(tempFile)
            if (validation is ModelValidator.ValidationResult.Invalid) {
                tempFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Import rejected: ${validation.reason}"))
            }

            // Clean sanitized name
            val cleanName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(modelsDir, cleanName)
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            refreshModels()
            val imported = _installedModels.value.find { it.localPath == targetFile.absolutePath }
                ?: return@withContext Result.failure(IllegalStateException("Failed to register imported model."))

            setActiveModelId(imported.id)
            Result.success(imported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUsableSpaceBytes(): Long = modelsDir.usableSpace

    fun getTotalModelsSizeBytes(): Long {
        return modelsDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    companion object {
        private const val KEY_ACTIVE_MODEL_ID = "active_model_id"
    }
}
