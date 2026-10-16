package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.ConversationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ConversationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ConversationRepository(db.conversationDao(), db.messageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context returns Pico AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pico AI", appName)
    }

    @Test
    fun `test conversation creation and message persistence`() = runBlocking {
        val conv = repo.createNewConversation(initialTitle = "Test Chat", modelUsed = "SmolLM")
        assertNotNull(conv.id)

        repo.addMessage(
            conversationId = conv.id,
            role = "user",
            content = "Explain binary trees"
        )
        repo.addMessage(
            conversationId = conv.id,
            role = "assistant",
            content = "A binary tree is a hierarchical data structure."
        )

        val messages = repo.getMessagesList(conv.id)
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("Explain binary trees", messages[0].content)
        assertEquals("assistant", messages[1].role)
    }

    @Test
    fun `test conversation cascade deletion`() = runBlocking {
        val conv = repo.createNewConversation(initialTitle = "To Delete")
        repo.addMessage(conversationId = conv.id, role = "user", content = "Hello")

        assertEquals(1, repo.getMessagesList(conv.id).size)

        repo.deleteConversation(conv.id)
        assertNull(repo.getConversationById(conv.id))
        assertEquals(0, repo.getMessagesList(conv.id).size)
    }

    @Test
    fun `test conversation auto-titling from user message`() = runBlocking {
        val conv = repo.createNewConversation(initialTitle = "New Chat")
        repo.addMessage(
            conversationId = conv.id,
            role = "user",
            content = "What is the capital of Bangladesh?"
        )

        val updated = repo.getConversationById(conv.id)
        assertNotNull(updated)
        assertEquals("What is the capital of Bangladesh?", updated!!.title)
    }
}
