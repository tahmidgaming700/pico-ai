package com.example.engine

import kotlinx.coroutines.flow.StateFlow
import java.io.File

sealed class EngineStatus {
    object Unloaded : EngineStatus()
    data class Loading(val modelName: String) : EngineStatus()
    data class Ready(val modelName: String, val memoryUsedMb: Long) : EngineStatus()
    data class Generating(val modelName: String) : EngineStatus()
    data class Error(val message: String) : EngineStatus()
}

interface LocalInferenceEngine {
    val status: StateFlow<EngineStatus>

    suspend fun loadModel(modelFile: File, settings: EngineSettings): Result<Unit>
    suspend fun generateResponse(prompt: String): Result<String>
    fun cancelGeneration()
    fun unloadModel()
    fun isModelLoaded(): Boolean
    fun getLoadedModelName(): String?
}
