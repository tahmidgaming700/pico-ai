package com.example.engine

data class EngineSettings(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val maxTokens: Int = 1024,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Pico AI, an intelligent, helpful, and completely private personal AI assistant running 100% locally on this Android device. Provide clear, accurate, and concise responses."
    }
}
