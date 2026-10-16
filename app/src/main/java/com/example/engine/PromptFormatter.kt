package com.example.engine

import com.example.data.local.MessageEntity

object PromptFormatter {

    enum class FormatType {
        GEMMA,
        CHAT_ML,
        RAW_INSTRUCTION
    }

    /**
     * Constructs a safe, context-bounded conversation prompt for the local language model.
     * Prevents unbounded prompt growth while preserving recent multi-turn context.
     */
    fun buildPrompt(
        history: List<MessageEntity>,
        currentMessage: String,
        systemPrompt: String = EngineSettings.DEFAULT_SYSTEM_PROMPT,
        formatType: FormatType = FormatType.GEMMA,
        maxContextChars: Int = 6000
    ): String {
        // Filter out system or error messages from turns
        val validHistory = history.filter { !it.isError && (it.role == "user" || it.role == "assistant") }

        // Take recent turns that fit within context budget
        val selectedHistory = mutableListOf<MessageEntity>()
        var accumulatedChars = currentMessage.length + systemPrompt.length + 200

        for (i in validHistory.indices.reversed()) {
            val msg = validHistory[i]
            if (accumulatedChars + msg.content.length > maxContextChars) {
                break
            }
            accumulatedChars += msg.content.length
            selectedHistory.add(0, msg)
        }

        return when (formatType) {
            FormatType.GEMMA -> buildGemmaPrompt(selectedHistory, currentMessage, systemPrompt)
            FormatType.CHAT_ML -> buildChatMlPrompt(selectedHistory, currentMessage, systemPrompt)
            FormatType.RAW_INSTRUCTION -> buildRawInstructionPrompt(selectedHistory, currentMessage, systemPrompt)
        }
    }

    private fun buildGemmaPrompt(
        history: List<MessageEntity>,
        currentMessage: String,
        systemPrompt: String
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("<start_of_turn>system\n")
            sb.append(systemPrompt.trim())
            sb.append("<end_of_turn>\n")
        }

        for (msg in history) {
            val roleTag = if (msg.role == "user") "user" else "model"
            sb.append("<start_of_turn>$roleTag\n")
            sb.append(msg.content.trim())
            sb.append("<end_of_turn>\n")
        }

        sb.append("<start_of_turn>user\n")
        sb.append(currentMessage.trim())
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")

        return sb.toString()
    }

    private fun buildChatMlPrompt(
        history: List<MessageEntity>,
        currentMessage: String,
        systemPrompt: String
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("<|im_start|>system\n")
            sb.append(systemPrompt.trim())
            sb.append("<|im_end|>\n")
        }

        for (msg in history) {
            val roleTag = if (msg.role == "user") "user" else "assistant"
            sb.append("<|im_start|>$roleTag\n")
            sb.append(msg.content.trim())
            sb.append("<|im_end|>\n")
        }

        sb.append("<|im_start|>user\n")
        sb.append(currentMessage.trim())
        sb.append("<|im_end|>\n")
        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }

    private fun buildRawInstructionPrompt(
        history: List<MessageEntity>,
        currentMessage: String,
        systemPrompt: String
    ): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append("System: ").append(systemPrompt.trim()).append("\n\n")
        }

        for (msg in history) {
            val label = if (msg.role == "user") "User" else "Assistant"
            sb.append("$label: ").append(msg.content.trim()).append("\n")
        }

        sb.append("User: ").append(currentMessage.trim()).append("\n")
        sb.append("Assistant: ")

        return sb.toString()
    }
}
