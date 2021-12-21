package com.bongofriend.data.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

class UserEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<UserEntity>(Users)

    var username by Users.username
    var passwordHash by Users.passwordHash
    var groups by ChatGroupEntity via UsersInGroups
}

class ChatGroupEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<ChatGroupEntity>(ChatGroups)

    var name by ChatGroups.name
    var members by UserEntity via UsersInGroups
}