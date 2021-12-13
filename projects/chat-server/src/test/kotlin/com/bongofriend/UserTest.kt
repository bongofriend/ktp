package com.bongofriend

import com.bongofriend.plugins.configureKoin
import com.bongofriend.plugins.configureRouting
import com.bongofriend.plugins.configureSerialization
import com.bongofriend.requests.AddNewUserRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.netty.handler.codec.http.HttpHeaders.addHeader
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.io.path.*

class UserTest {
    private val testUser = object {
        val username = "hans_peter"
        val password = "password"
    }

    private val testConfigName = "application_test.conf"
    private val objectMapper = ObjectMapper()
    private val testEnv = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(testConfigName))
    }

    @BeforeTest
    fun clearTestDatabase() {
        prepareDatabase(testEnv.config)
    }

    @Test
    fun testNewUserCreation() {
        withApplication(testEnv) {
            with(handleRequest(HttpMethod.Post, "/users"){
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                val request = AddNewUserRequest(testUser.username, testUser.password)
                setBody(objectMapper.writeValueAsString(request))
            }) {
                assertEquals(HttpStatusCode.Created, response.status())
            }
        }
    }
}