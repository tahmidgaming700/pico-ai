package com.example.data.model

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object ModelValidator {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    /**
     * Inspects magic bytes and headers rather than trusting file extensions alone.
     */
    fun validateModelFile(file: File): ValidationResult {
        if (!file.exists()) {
            return ValidationResult.Invalid("File does not exist: ${file.name}")
        }
        if (file.length() < 1024 * 1024) { // Less than 1MB is almost certainly a corrupted download or 404 HTML
            return ValidationResult.Invalid("File is too small (${file.length()} bytes) to be a valid LLM weight file.")
        }

        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(16)
                val bytesRead = input.read(header)
                if (bytesRead < 8) {
                    return ValidationResult.Invalid("Cannot read header bytes.")
                }

                // Check GGUF magic bytes: 'G', 'G', 'U', 'F'
                val isGguf = header[0] == 0x47.toByte() && header[1] == 0x47.toByte() &&
                        header[2] == 0x55.toByte() && header[3] == 0x46.toByte()

                // Check MediaPipe .task bundle (Standard Zip container PK\x03\x04)
                val isZipTask = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()

                // Check TFLite / LiteRT FlatBuffer (identifier 'TFL3' at offset 4..7)
                val isTflite = bytesRead >= 8 &&
                        header[4] == 'T'.code.toByte() &&
                        header[5] == 'F'.code.toByte() &&
                        header[6] == 'L'.code.toByte() &&
                        header[7] == '3'.code.toByte()

                if (isGguf) {
                    return ValidationResult.Valid
                }
                if (isZipTask) {
                    return ValidationResult.Valid
                }
                if (isTflite) {
                    return ValidationResult.Valid
                }

                // If none matched, check whether it's an unrecognized or corrupted payload
                return ValidationResult.Invalid("Unrecognized binary header. The file does not have a valid MediaPipe (.task), TFLite/LiteRT ('TFL3'), or GGUF ('GGUF') magic signature.")
            }
        } catch (e: Exception) {
            return ValidationResult.Invalid("Failed reading file: ${e.message}")
        }
    }

    /**
     * Compute SHA-256 hash of a file
     */
    fun computeSha256(file: File, onProgress: ((Int) -> Unit)? = null): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        val totalBytes = file.length()
        var readBytes = 0L

        FileInputStream(file).use { input ->
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                digest.update(buffer, 0, count)
                readBytes += count
                if (totalBytes > 0 && onProgress != null) {
                    val percent = ((readBytes * 100) / totalBytes).toInt()
                    onProgress(percent)
                }
            }
        }

        val hashBytes = digest.digest()
        val hexString = StringBuilder()
        for (b in hashBytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
