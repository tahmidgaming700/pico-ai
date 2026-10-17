package com.example.data.model

enum class ModelFormat(val displayName: String, val extension: String) {
    MEDIA_PIPE_TASK("MediaPipe Task Bundle", ".task"),
    LITE_RT_BIN("LiteRT / TFLite Binary", ".bin"),
    GGUF("llama.cpp GGUF Format", ".gguf")
}

data class ModelInfo(
    val id: String,
    val name: String,
    val publisher: String,
    val format: ModelFormat,
    val licenseName: String,
    val licenseUrl: String,
    val approxSizeBytes: Long,
    val expectedSha256: String? = null,
    val downloadUrl: String? = null,
    val minRamMb: Int = 2048,
    val parameterCount: String = "1B",
    val description: String = "",
    val isDownloaded: Boolean = false,
    val localFileSizeBytes: Long = 0L,
    val localPath: String? = null,
    val isVerified: Boolean = false,
    val isGated: Boolean = false
) {
    val formattedSize: String
        get() {
            val bytes = if (isDownloaded && localFileSizeBytes > 0) localFileSizeBytes else approxSizeBytes
            return formatBytes(bytes)
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024)
            } else {
                String.format("%.1f MB", mb)
            }
        }
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : DownloadState()
    object VerifyingChecksum : DownloadState()
    object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
