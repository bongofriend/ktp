package com.bongofriend

import com.bongofriend.requests.AddNewUserRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserTest {
    private val objectMapper = ObjectMapper()

    @BeforeTest
    fun clearTestDatabase() {
        prepareDatabase()
    }

    @Test
    fun testNewUserCreation() {
        withChatServerEnvironment {
            with(handleRequest(HttpMethod.Post, "/users"){
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                val request = AddNewUserRequest(TestUser.username, TestUser.password)
                setBody(objectMapper.writeValueAsString(request))
            }) {
                assertEquals(HttpStatusCode.Created, response.status())
            }
        }
    }
}