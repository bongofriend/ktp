package com.bongofriend

import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

internal object TestUser {
    const val username = "hans_peter"
    const val password = "password"
}

private const val configName = "application_test.conf"



internal fun removeTestDb() {
    val appConfig = HoconApplicationConfig(ConfigFactory.load(configName))
    val dbConfig = appConfig.config("db")
    val dbPath = dbConfig.property("url").getString().removePrefix("jdbc:sqlite:")
    Path(dbPath).deleteIfExists()
}


internal fun isValidUuid(data: String): Boolean {
    return try {
        UUID.fromString(data)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}

internal fun getTokenForTestUser(): UUID {
    executePost<AddNewUserRequest, Nothing>("/users", AddNewUserRequest(TestUser.username, TestUser.password))
    val token = executePost<GetUserTokenRequest, String>("/users/token", GetUserTokenRequest(TestUser.username, TestUser.password)) {
        val mapper = ObjectMapper()
        val data = mapper.readValue<Map<String, String>>(response.content!!)
        return@executePost data["token"]!!
    }
    return UUID.fromString(token)
}


private fun<TResult> executeCall(method: HttpMethod, path: String, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    return withChatServerEnvironment {
        with(handleRequest(method, path) {
            if (setup != null) this.setup()
        }) {
            return@withChatServerEnvironment if(func != null) this.func() else null
        }
    }
}

private fun<TResult> executeAuthenticatedCall(method: HttpMethod, path: String, token: UUID, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    val authSetup: TestApplicationRequest.() -> Unit = {
        if (setup != null) this.setup()
        addHeader(HttpHeaders.Authorization, "Bearer $token")
    }
    return executeCall(method, path, authSetup, func)
}

internal fun <TBody, TResult> executePost(path: String, data: TBody, token: UUID? = null, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    val postSetup: TestApplicationRequest.() -> Unit = {
        if (setup != null) this.setup()
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        when(data) {
            is String -> setBody(data)
            is ByteArray -> setBody(data)
            else -> {
                val mapper = ObjectMapper()
                setBody(mapper.writeValueAsString(data))
            }
        }
    }
    return if (token == null) executeCall(HttpMethod.Post, path, postSetup, func) else executeAuthenticatedCall(
        HttpMethod.Post, path, token, postSetup, func)
}

internal fun <TResult> executeGet(path: String, token: UUID? = null, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    return if (token == null) executeCall(HttpMethod.Get, path, setup, func) else executeAuthenticatedCall(HttpMethod.Get, path, token, setup, func)
}

fun <R> withChatServerEnvironment(func: TestApplicationEngine.() -> R): R {
    val env = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
    }
    return withApplication<R>(env) {
        this.func()
    }
}