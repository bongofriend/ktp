package com.bongofriend

import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserTest {
    private val objectMapper = jacksonObjectMapper()

    @BeforeAll
    fun prepare() {
        removeTestDb()
    }

    @AfterAll
    fun cleanUp() {
        removeTestDb()
    }

    @Test
    @Order(1)
    fun `Create New User`() {
        withChatServerEnvironment {
            executePost("/users", AddNewUserRequest(PrimaryTestUser.username, PrimaryTestUser.password)) {
                assertEquals(HttpStatusCode.Created, response.status())
                assertFalse(response.content.isNullOrEmpty())
                val data = objectMapper.readValue<Map<String, String>>(response.content!!)
                assertTrue(data.containsKey("id"))
                val userId = data["id"]
                assertFalse(userId.isNullOrEmpty())
                assertTrue(isValidUuid(userId!!))
            }
        }
    }

    @Test
    @Order(2)
    fun `Request Authentication Token for Test User`() {
        withChatServerEnvironment {
            executePost("/users/token", GetUserTokenRequest(PrimaryTestUser.username, PrimaryTestUser.password)) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertFalse(response.content.isNullOrEmpty())
                val data = objectMapper.readValue<Map<String, String>>(response.content!!)
                assertTrue(data.containsKey("token"))
                val token = data["token"]
                assertFalse(token.isNullOrEmpty())
            }
        }
    }
}