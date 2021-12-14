package com.bongofriend

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.testing.*
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

object TestUser {
    const val username = "hans_peter"
    const val password = "password"
}

private const val configName = "application_test.conf"

fun prepareDatabase() {
    val appConfig = HoconApplicationConfig(ConfigFactory.load(configName))
    val dbConfig = appConfig.config("db")
    val dbPath = dbConfig.property("url").getString().replace("jdbc:sqlite:", "")
    Path(dbPath).deleteIfExists()
}

fun withChatServerEnvironment(func: TestApplicationEngine.() -> Unit) {
    val env = createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load(configName))
    }
    withApplication(env) {
        this.func()
    }
}