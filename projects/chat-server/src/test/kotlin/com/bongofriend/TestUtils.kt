package com.bongofriend

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

object TestUser {
    const val username = "hans_peter"
    const val password = "password"
}

private const val configName = "application_test.conf"



fun removeTestDb() {
    val appConfig = HoconApplicationConfig(ConfigFactory.load(configName))
    val dbConfig = appConfig.config("db")
    val dbPath = dbConfig.property("url").getString().removePrefix("jdbc:sqlite:")
    Path(dbPath).deleteIfExists()
}


fun isValidUuid(data: String): Boolean {
    return try {
        UUID.fromString(data)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}

fun<TRequestBody> executePost(path: String, body: TRequestBody, vararg headers: Pair<String,String>, func: TestApplicationCall.() -> Unit) {
   withChatServerEnvironment {
       with(handleRequest(HttpMethod.Post, path){
           addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
           headers.forEach { addHeader(it.first, it.second) }
           val objectMapper = ObjectMapper()
           setBody(objectMapper.writeValueAsString(body))
       }) {
           this.func()
       }
   }
}

fun withChatServerEnvironment(func: TestApplicationEngine.() -> Unit) {
    val env = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
    }
    withApplication(env) {
        this.func()
    }
}