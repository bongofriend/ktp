package com.bongofriend.data.repositories

import com.bongofriend.data.db.UserEntity
import com.bongofriend.data.db.Users
import com.bongofriend.data.models.User
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    suspend fun addNewUser(username: String, password: String): User
}

class UserRepositoryImpl: UserRepository {
    override suspend fun addNewUser(username: String, password: String): User = transaction {
        val entity = UserEntity.new {
            this.username = username
            this.passwordHash = password
        }
        return@transaction User(entity.username, entity.passwordHash)
    }

}