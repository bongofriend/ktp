package com.bongofriend

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

internal object PrimaryTestUser {
    const val username = "hans_peter"
    const val password = "password"
}

internal object SecondaryTestUser {
    const val username = "gunther"
    const val password = "password"
}

internal const val testChatGroup = "MyTestingGroup"

private const val configName = "application_test.conf"

internal val objectMapper = jacksonObjectMapper()


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

private fun TestApplicationEngine.addUser(username: String, password: String) {
    executePost<AddNewUserRequest, Nothing>("/users", AddNewUserRequest(username, password))
}

internal fun TestApplicationEngine.addPrimaryUser() = addUser(PrimaryTestUser.username, PrimaryTestUser.password)
internal fun TestApplicationEngine.addSecondaryUser() = addUser(SecondaryTestUser.username, SecondaryTestUser.password)

private fun TestApplicationEngine.getTokenForTestUser(username: String, password: String): String {
        val token = executePost("/users/token", GetUserTokenRequest(username, password)) {
            val data = objectMapper.readValue<Map<String, String>>(response.content!!)
            return@executePost data["token"]!!
        }
        return token!!
}

internal fun TestApplicationEngine.getPrimaryUserToken(): String = getTokenForTestUser(PrimaryTestUser.username, PrimaryTestUser.password)

internal fun TestApplicationEngine.getSecondaryUserToken(): String = getTokenForTestUser(SecondaryTestUser.username, SecondaryTestUser.password)

internal fun TestApplicationEngine.getChatGroups(token: String): List<ChatGroup> {
    val data = executeGet("/chatgroups", token) {
        return@executeGet objectMapper.readValue<List<ChatGroup>>(response.content!!)
    }
    return data!!
}

private fun<TResult> TestApplicationEngine.executeCall(method: HttpMethod, path: String, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    return with(handleRequest(method, path) {
            if (setup != null) this.setup()
        }) {
            return@with if(func != null) this.func() else null
    }

}

private fun<TResult> TestApplicationEngine.executeAuthenticatedCall(method: HttpMethod, path: String, token: String, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    val authSetup: TestApplicationRequest.() -> Unit = {
        if (setup != null) this.setup()
        addHeader(HttpHeaders.Authorization, "Bearer $token")
    }
    return executeCall(method, path, authSetup, func)
}

internal fun <TBody, TResult> TestApplicationEngine.executePost(path: String, data: TBody, token: String? = null, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    val postSetup: TestApplicationRequest.() -> Unit = {
        if (setup != null) this.setup()
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        when(data) {
            is String -> setBody(data)
            is ByteArray -> setBody(data)
            else -> setBody(objectMapper.writeValueAsString(data))
        }
    }
    return if (token == null) executeCall(HttpMethod.Post, path, postSetup, func) else executeAuthenticatedCall(
        HttpMethod.Post, path, token, postSetup, func)
}

internal fun <TResult> TestApplicationEngine.executeGet(path: String, token: String? = null, setup: (TestApplicationRequest.() -> Unit)? = null, func: (TestApplicationCall.() -> TResult)? = null): TResult? {
    return if (token == null) executeCall(HttpMethod.Get, path, setup, func) else executeAuthenticatedCall(HttpMethod.Get, path, token, setup, func)
}

internal fun <R> withChatServerEnvironment(func: TestApplicationEngine.() -> R): R {
    val env = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
    }
    return withApplication(env) {
        this.func()
    }
}

