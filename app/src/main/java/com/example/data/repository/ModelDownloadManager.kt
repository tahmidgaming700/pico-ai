package com.example.data.repository

import android.content.Context
import com.example.data.model.DownloadState
import com.example.data.model.ModelInfo
import com.example.data.model.ModelValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ModelDownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadState = _downloadState.asStateFlow()

    private val activeCalls = mutableMapOf<String, okhttp3.Call>()

    suspend fun downloadModel(
        model: ModelInfo,
        destinationDir: File,
        onSuccess: suspend (File) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val url = model.downloadUrl ?: return@withContext Result.failure(
            IllegalArgumentException("Model has no download URL configured.")
        )

        // 1. Free space check
        val usableSpace = destinationDir.usableSpace
        val requiredSpace = (model.approxSizeBytes * 1.15).toLong()
        if (usableSpace < requiredSpace) {
            val neededMb = (requiredSpace - usableSpace) / (1024 * 1024)
            val error = "Insufficient storage space. Free up at least $neededMb MB to install ${model.name}."
            updateState(model.id, DownloadState.Failed(error))
            return@withContext Result.failure(IllegalStateException(error))
        }

        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val tempFile = File(destinationDir, "${model.id}_${System.currentTimeMillis()}.tmp")
        val extension = model.format.extension
        val finalFile = File(destinationDir, "${model.id}$extension")

        updateState(model.id, DownloadState.Downloading(0, 0, model.approxSizeBytes))

        try {
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            activeCalls[model.id] = call

            val response = call.execute()
            if (!response.isSuccessful) {
                val error = "Server returned error: HTTP ${response.code} ${response.message}"
                updateState(model.id, DownloadState.Failed(error))
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException(error))
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from server.")
            val contentLength = if (body.contentLength() > 0) body.contentLength() else model.approxSizeBytes

            var bytesDownloaded = 0L
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L
            var speedBps = 0L

            body.byteStream().use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastTime >= 500) {
                            val elapsedSec = (now - lastTime) / 1000.0
                            speedBps = if (elapsedSec > 0) ((bytesDownloaded - lastBytes) / elapsedSec).toLong() else 0L
                            lastBytes = bytesDownloaded
                            lastTime = now

                            val progress = if (contentLength > 0) {
                                ((bytesDownloaded * 100) / contentLength).toInt().coerceIn(0, 99)
                            } else 0

                            updateState(
                                model.id,
                                DownloadState.Downloading(progress, bytesDownloaded, contentLength, speedBps)
                            )
                        }
                    }
                    output.flush()
                }
            }

            // 2. Checksum validation if available
            if (model.expectedSha256 != null) {
                updateState(model.id, DownloadState.VerifyingChecksum)
                val computedHash = ModelValidator.computeSha256(tempFile)
                if (!computedHash.equals(model.expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    val error = "Checksum mismatch: download is corrupted or tampered."
                    updateState(model.id, DownloadState.Failed(error))
                    return@withContext Result.failure(IllegalStateException(error))
                }
            }

            // 3. Binary header validation
            when (val validation = ModelValidator.validateModelFile(tempFile)) {
                is ModelValidator.ValidationResult.Invalid -> {
                    tempFile.delete()
                    val error = "Model verification failed: ${validation.reason}"
                    updateState(model.id, DownloadState.Failed(error))
                    return@withContext Result.failure(IllegalStateException(error))
                }
                is ModelValidator.ValidationResult.Valid -> {
                    // Passed validation!
                }
            }

            // 4. Atomic installation rename
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tempFile.renameTo(finalFile)
            if (!renamed) {
                // Fallback copy if cross-filesystem rename fails
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            updateState(model.id, DownloadState.Completed)
            onSuccess(finalFile)
            Result.success(finalFile)
        } catch (e: Exception) {
            tempFile.delete()
            val error = e.localizedMessage ?: "Download interrupted or cancelled"
            updateState(model.id, DownloadState.Failed(error))
            Result.failure(e)
        } finally {
            activeCalls.remove(model.id)
        }
    }

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
        activeCalls.remove(modelId)
        updateState(modelId, DownloadState.Failed("Download cancelled by user"))
    }

    private fun updateState(modelId: String, state: DownloadState) {
        val current = _downloadState.value.toMutableMap()
        current[modelId] = state
        _downloadState.value = current
    }
}
