package com.example

import com.example.data.local.MessageEntity
import com.example.data.model.ModelFormat
import com.example.data.model.ModelInfo
import com.example.data.model.ModelValidator
import com.example.engine.PromptFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class ExampleUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPromptFormatter_gemmaFormat() {
        val history = listOf(
            MessageEntity(id = "1", conversationId = "c1", role = "user", content = "Hello"),
            MessageEntity(id = "2", conversationId = "c1", role = "assistant", content = "Hi! How can I help?")
        )

        val prompt = PromptFormatter.buildPrompt(
            history = history,
            currentMessage = "What is 2+2?",
            systemPrompt = "You are a math tutor.",
            formatType = PromptFormatter.FormatType.GEMMA
        )

        assertTrue(prompt.contains("<start_of_turn>system\nYou are a math tutor.<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>user\nHello<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>model\nHi! How can I help?<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>user\nWhat is 2+2?<end_of_turn>"))
        assertTrue(prompt.endsWith("<start_of_turn>model\n"))
    }

    @Test
    fun testPromptFormatter_unicodeAndBengali() {
        val bengaliText = "কেমন আছেন? বাংলায় একটি কবিতা লিখুন।"
        val prompt = PromptFormatter.buildPrompt(
            history = emptyList(),
            currentMessage = bengaliText,
            systemPrompt = "You are Pico AI.",
            formatType = PromptFormatter.FormatType.GEMMA
        )

        assertTrue(prompt.contains(bengaliText))
    }

    @Test
    fun testPromptFormatter_contextBounding() {
        // Create 20 long messages
        val history = (1..20).map { i ->
            MessageEntity(
                id = "$i",
                conversationId = "c1",
                role = if (i % 2 == 1) "user" else "assistant",
                content = "Message turn $i: " + "a".repeat(500)
            )
        }

        val prompt = PromptFormatter.buildPrompt(
            history = history,
            currentMessage = "Final query",
            systemPrompt = "System",
            maxContextChars = 2000
        )

        // Bounded context must not exceed the limit excessively
        assertTrue(prompt.length < 3000)
        assertTrue(prompt.contains("Final query"))
    }

    @Test
    fun testModelValidator_rejectsSmallOrInvalidFile() {
        val emptyFile = tempFolder.newFile("corrupt.bin")
        val res = ModelValidator.validateModelFile(emptyFile)
        assertTrue(res is ModelValidator.ValidationResult.Invalid)
    }

    @Test
    fun testModelValidator_acceptsGgufHeader() {
        val ggufFile = tempFolder.newFile("test.gguf")
        FileOutputStream(ggufFile).use { out ->
            // GGUF magic bytes: 'G', 'G', 'U', 'F'
            out.write(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x01, 0x00, 0x00, 0x00))
            out.write(ByteArray(1024 * 1024 + 10)) // Pad to > 1MB
        }

        val res = ModelValidator.validateModelFile(ggufFile)
        assertTrue(res is ModelValidator.ValidationResult.Valid)
    }

    @Test
    fun testModelValidator_acceptsMediaPipeZipTaskHeader() {
        val taskFile = tempFolder.newFile("model.task")
        FileOutputStream(taskFile).use { out ->
            // ZIP magic bytes: 'P', 'K', 0x03, 0x04
            out.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            out.write(ByteArray(1024 * 1024 + 10))
        }

        val res = ModelValidator.validateModelFile(taskFile)
        assertTrue(res is ModelValidator.ValidationResult.Valid)
    }

    @Test
    fun testModelValidator_sha256() {
        val testFile = tempFolder.newFile("hello.txt")
        testFile.writeText("pico-ai-test")
        val hash = ModelValidator.computeSha256(testFile)
        // Known SHA-256 for "pico-ai-test"
        assertEquals(64, hash.length)
    }

    @Test
    fun testModelInfo_formatBytes() {
        assertEquals("0 MB", ModelInfo.formatBytes(0L))
        assertEquals("500.0 MB", ModelInfo.formatBytes(500L * 1024 * 1024))
        assertEquals("1.50 GB", ModelInfo.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }
}
