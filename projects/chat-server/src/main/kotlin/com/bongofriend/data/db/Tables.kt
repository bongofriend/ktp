package com.bongofriend.data.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

abstract class BaseTable: UUIDTable() {
}

object Users: BaseTable() {
    val username = varchar("user_name", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
}

object ChatGroups: BaseTable() {
    val name = varchar("chat_group_name", 255).uniqueIndex()
}

object UsersInGroups: Table() {
    val user = reference("user", Users)
    val group = reference("chat_group", ChatGroups)
    override val primaryKey = PrimaryKey(user, group)
}

object ChatMessages: BaseTable() {
    val message = varchar("message", 255)
    val user = reference("user", Users)
    val group = reference("chat_group", ChatGroups)
}