package com.bongofriend

import io.ktor.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

fun prepareDatabase(config: ApplicationConfig) {
    val dbConfig = config.config("db")
    val dbPath = dbConfig.property("url").getString().replace("jdbc:sqlite:", "")
    Path(dbPath).deleteIfExists()
}