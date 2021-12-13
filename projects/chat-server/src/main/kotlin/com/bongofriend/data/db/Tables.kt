package com.bongofriend.data.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

abstract class BaseTable: UUIDTable() {
    val createdAt: Column<LocalDateTime> = datetime("created_at").default(LocalDateTime.now())
}

object Users: BaseTable() {
    val username = varchar("user_name", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
}