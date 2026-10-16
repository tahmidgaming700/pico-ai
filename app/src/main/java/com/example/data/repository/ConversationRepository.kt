package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun getAllConversations(): Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations()

    suspend fun getConversationById(id: String): ConversationEntity? =
        conversationDao.getConversationById(id)

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.searchConversations(query)

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun getMessagesList(conversationId: String): List<MessageEntity> =
        messageDao.getMessagesList(conversationId)

    suspend fun createNewConversation(initialTitle: String = "New Chat", modelUsed: String = ""): ConversationEntity {
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = initialTitle,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelUsed = modelUsed
        )
        conversationDao.insertConversation(entity)
        return entity
    }

    suspend fun renameConversation(conversationId: String, newTitle: String) {
        val existing = conversationDao.getConversationById(conversationId) ?: return
        conversationDao.updateConversation(existing.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateConversationModel(conversationId: String, modelName: String) {
        val existing = conversationDao.getConversationById(conversationId) ?: return
        conversationDao.updateConversation(existing.copy(modelUsed = modelName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        latencyMs: Long = 0L,
        tokenCount: Int = 0,
        isError: Boolean = false
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            latencyMs = latencyMs,
            tokenCount = tokenCount,
            isError = isError
        )
        messageDao.insertMessage(message)

        // Update conversation timestamp & auto-generate title from first user message if default
        val conversation = conversationDao.getConversationById(conversationId)
        if (conversation != null) {
            val title = if (conversation.title == "New Chat" && role == "user") {
                val clean = content.trim().replace("\n", " ")
                if (clean.length > 36) clean.take(36) + "…" else clean
            } else conversation.title

            conversationDao.updateConversation(
                conversation.copy(
                    title = title,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        return message
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
    }

    suspend fun clearAll() {
        messageDao.clearAllMessages()
        conversationDao.clearAllConversations()
    }

    suspend fun exportConversationAsText(conversationId: String): String {
        val conversation = conversationDao.getConversationById(conversationId) ?: return ""
        val messages = messageDao.getMessagesList(conversationId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.appendLine("=== Pico AI Conversation: ${conversation.title} ===")
        sb.appendLine("Date: ${dateFormat.format(Date(conversation.createdAt))}")
        if (conversation.modelUsed.isNotBlank()) {
            sb.appendLine("Model: ${conversation.modelUsed}")
        }
        sb.appendLine("Inference: 100% On-Device (Private & Offline)")
        sb.appendLine("============================================")
        sb.appendLine()

        for (msg in messages) {
            val sender = when (msg.role) {
                "user" -> "User"
                "assistant" -> "Pico AI"
                else -> "System"
            }
            sb.appendLine("[$sender] (${dateFormat.format(Date(msg.timestamp))}):")
            sb.appendLine(msg.content)
            sb.appendLine()
        }

        return sb.toString()
    }
}
