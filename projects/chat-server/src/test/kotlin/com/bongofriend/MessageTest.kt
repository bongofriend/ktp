package com.bongofriend

import com.bongofriend.data.models.ChatMessage
import com.bongofriend.requests.AddMessageToGroupRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.selects.select
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import kotlin.test.*

@Suppress("NON_EXHAUSTIVE_WHEN_STATEMENT")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MessageTest {

    companion object {
        const val TEST_CHAT_GROUP = "testing-group"
        const val TEST_MESSAGE = "Hello"
    }

    private val objectMapper = jacksonObjectMapper()

    @BeforeAll
    fun prepare() {
        removeTestDb()
        withChatServerEnvironment {
            addPrimaryUser()
            val token = getPrimaryUserToken()
            createChatGroup(TEST_CHAT_GROUP, token)
        }
    }

    @AfterAll
    fun cleanUp() {
        removeTestDb()
    }

    @Order(1)
    @Test
    fun `Send message to group`() {
        withChatServerEnvironment {
            val token = getPrimaryUserToken()
            val chatGroup = getChatGroups(token).find { g -> g.name == TEST_CHAT_GROUP }
            assertTrue(chatGroup != null)
            executePost("/messages/${chatGroup.id.toString()}", AddMessageToGroupRequest(TEST_MESSAGE), token) {
                assertSame(response.status(), HttpStatusCode.Created)
                assertFalse(response.content.isNullOrEmpty())
                val message = objectMapper.readValue<ChatMessage>(response.content!!)
                assertTrue(message.message.isNotEmpty() && message.message == TEST_MESSAGE)
            }
        }
    }

    @Order(2)
    @Test
    fun `Get Chat Messages for Group`() {
        withChatServerEnvironment {
            val token = getPrimaryUserToken()
            val chatGroup = getChatGroups(token).first()
            sendMessage(TEST_MESSAGE, chatGroup.id.toString(), token)
            executeGet("/messages/${chatGroup.id.toString()}", token) {
                assertSame(response.status(), HttpStatusCode.OK)
                assertFalse(response.content.isNullOrEmpty())
                val messages = objectMapper.readValue<Map<String, List<ChatMessage>>>(response.content!!)["messages"]
                assertNotNull(messages)
                assertFalse(messages.isEmpty())
                assertTrue(messages.any { g -> g.message == TEST_MESSAGE })
            }
        }
    }

    @Order(3)
    @Test
    fun `Receive messages from socket`() {
        withChatServerEnvironment {
            val token = getPrimaryUserToken()
            val chatGroup = getChatGroups(token).first()
            handleWebSocketConversation("/socket?token=${token}&groupId=${chatGroup.id}") { incoming, outcoming ->
                sendMessage(TEST_MESSAGE, chatGroup.id.toString(), token)
                select<Unit> {
                    incoming.onReceive { f ->
                        when (f) {
                            is Frame.Text -> {
                                val chatMessage = objectMapper.readValue<ChatMessage>(f.readBytes())
                                assertEquals(TEST_MESSAGE, chatMessage.message)
                            }
                        }
                    }
                }
            }
        }
    }
}