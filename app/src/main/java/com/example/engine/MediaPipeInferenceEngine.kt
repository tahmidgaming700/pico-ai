package com.example.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaPipeInferenceEngine(private val context: Context) : LocalInferenceEngine {

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Unloaded)
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private var llmInference: LlmInference? = null
    private var loadedModelName: String? = null
    private var isGenerating = false

    override fun isModelLoaded(): Boolean = llmInference != null

    override fun getLoadedModelName(): String? = loadedModelName

    override suspend fun loadModel(modelFile: File, settings: EngineSettings): Result<Unit> = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            val err = "Model file does not exist at ${modelFile.absolutePath}"
            _status.value = EngineStatus.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        unloadModel()
        val modelName = modelFile.nameWithoutExtension
        _status.value = EngineStatus.Loading(modelName)

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(settings.maxTokens)
                .setMaxTopK(settings.topK)
                .build()

            val instance = LlmInference.createFromOptions(context, options)
            llmInference = instance
            loadedModelName = modelName

            val runtime = Runtime.getRuntime()
            val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            _status.value = EngineStatus.Ready(modelName, usedMemMb)

            Result.success(Unit)
        } catch (oom: OutOfMemoryError) {
            unloadModel()
            System.gc()
            val err = "Out of Memory while loading model '$modelName'. The device RAM was insufficient."
            _status.value = EngineStatus.Error(err)
            Result.failure(IllegalStateException(err, oom))
        } catch (linkError: UnsatisfiedLinkError) {
            unloadModel()
            val err = "Native library failure: MediaPipe GenAI binary not supported on this device CPU/ABI architecture (${linkError.message})."
            _status.value = EngineStatus.Error(err)
            Result.failure(IllegalStateException(err, linkError))
        } catch (t: Throwable) {
            unloadModel()
            val err = "Failed to initialize local model: ${t.localizedMessage ?: t.javaClass.simpleName}"
            _status.value = EngineStatus.Error(err)
            Result.failure(t)
        }
    }

    override suspend fun generateResponse(prompt: String): Result<String> = withContext(Dispatchers.Default) {
        val instance = llmInference ?: return@withContext Result.failure(
            IllegalStateException("No model loaded. Please load a verified model from Model Manager first.")
        )
        val modelName = loadedModelName ?: "Local Model"

        _status.value = EngineStatus.Generating(modelName)
        isGenerating = true

        try {
            val rawOutput = instance.generateResponse(prompt)
            if (!isGenerating) {
                _status.value = EngineStatus.Ready(modelName, getCurrentMemoryMb())
                return@withContext Result.failure(CancellationException("Generation was stopped by user."))
            }

            // Post-process tokens (trim stop tokens)
            val cleaned = cleanResponse(rawOutput)
            _status.value = EngineStatus.Ready(modelName, getCurrentMemoryMb())
            Result.success(cleaned)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            _status.value = EngineStatus.Error("Out of Memory during token generation.")
            Result.failure(IllegalStateException("Out of memory during inference.", oom))
        } catch (c: CancellationException) {
            _status.value = EngineStatus.Ready(modelName, getCurrentMemoryMb())
            Result.failure(c)
        } catch (t: Throwable) {
            _status.value = EngineStatus.Ready(modelName, getCurrentMemoryMb())
            Result.failure(t)
        } finally {
            isGenerating = false
        }
    }

    override fun cancelGeneration() {
        isGenerating = false
        val modelName = loadedModelName
        if (modelName != null && isModelLoaded()) {
            _status.value = EngineStatus.Ready(modelName, getCurrentMemoryMb())
        }
    }

    override fun unloadModel() {
        isGenerating = false
        try {
            llmInference?.close()
        } catch (_: Exception) {
        }
        llmInference = null
        loadedModelName = null
        _status.value = EngineStatus.Unloaded
        System.gc()
    }

    private fun cleanResponse(raw: String): String {
        var text = raw.trim()
        val stopTokens = listOf(
            "<end_of_turn>",
            "<|im_end|>",
            "<start_of_turn>",
            "<|im_start|>",
            "</s>",
            "[INST]",
            "[/INST]"
        )
        for (stop in stopTokens) {
            text = text.replace(stop, "")
        }
        return text.trim()
    }

    private fun getCurrentMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
}
