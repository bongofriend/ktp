package com.bongofriend

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.requests.NewChatGroupRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import org.junit.jupiter.api.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatGroupTest {
    private lateinit var token: UUID

    @BeforeAll
    fun prepare() {
        removeTestDb()
        token = getTokenForTestUser()
    }

    @AfterAll
    fun cleanUp() {
        removeTestDb()
    }

    @Test
    fun `Create New ChatGroup`() {
        executePost("/chatgroups", NewChatGroupRequest("test"), token) {
            assertEquals(HttpStatusCode.Created, response.status())
        }
    }

    @Test
    fun `Get ChatGroups for User`() {
        executeGet("/chatgroups", token) {
            assertSame(HttpStatusCode.Created, response.status())
            assertFalse(response.content.isNullOrEmpty())
            val chatGroups = ObjectMapper().readValue<List<ChatGroup>>(response.content!!)
            assertFalse(chatGroups.isEmpty())
        }
    }
}