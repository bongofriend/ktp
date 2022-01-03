package com.bongofriend

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.requests.AddToChatGroupRequest
import com.bongofriend.requests.CreateChatGroupRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ChatGroupTest {

    @BeforeAll
    fun prepare() {
        removeTestDb()
        withChatServerEnvironment {
            addPrimaryUser()
            addSecondaryUser()
        }
    }

    @AfterAll
    fun clean() {
        removeTestDb()
    }

    @Test
    @Order(1)
    fun `Create New ChatGroup`() {
        withChatServerEnvironment {
            val token = getPrimaryUserToken()
            executePost("/chatgroups/create", CreateChatGroupRequest(testChatGroup), token) {
                assertEquals(HttpStatusCode.Created, response.status())
            }
        }
    }


    @Test
    @Order(2)
    fun `Get ChatGroups for User`() {
        withChatServerEnvironment {
            val token = getPrimaryUserToken()
            executeGet("/chatgroups", token) {
                assertSame(HttpStatusCode.OK, response.status())
                assertFalse(response.content.isNullOrEmpty())
                val chatGroups = jacksonObjectMapper().readValue<List<ChatGroup>>(response.content!!)
                assertFalse(chatGroups.isEmpty())
            }
        }
    }

    @Test
    @Order(3)
    fun `Add User to Chat Group`() {
        withChatServerEnvironment {
            val primaryUser = getPrimaryUserToken()
            val secondaryUser = getSecondaryUserToken()
            val primaryUserChatGroups = getChatGroups(primaryUser)
            val groupId = primaryUserChatGroups.first().id.toString()
            executePost("/chatgroups/add", AddToChatGroupRequest(groupId), secondaryUser) {
                assertSame(HttpStatusCode.OK, response.status())
                val secondaryUserChatGroups = getChatGroups(secondaryUser)
                assertTrue(secondaryUserChatGroups.any { g -> g.id.toString() == groupId })
            }
        }
    }
}