package com.bongofriend.data.repositories

import com.bongofriend.data.db.UserEntity
import com.bongofriend.data.db.Users
import com.bongofriend.data.models.User
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface UserRepository {
    suspend fun addNewUser(username: String, password: String): User
    suspend fun getUserByName(username: String): User?
    suspend fun getUserById(userId: UUID): User?
}

class UserRepositoryImpl: UserRepository {
    override suspend fun addNewUser(username: String, password: String): User {
        val entity = transaction {
            UserEntity.new {
                this.username = username
                this.passwordHash = password
            }
        }
        return User(entity.id.value, entity.username, entity.passwordHash)
    }

    override suspend fun getUserByName(username: String): User? = getUserByExpression(Users.username eq username)

    override suspend fun getUserById(userId: UUID): User? = getUserByExpression(Users.id eq userId)

    private fun getUserByExpression(op: Op<Boolean>): User? {
        val userEntity = transaction {
            return@transaction UserEntity.find(op).firstOrNull()
        } ?: return null
        return User(userEntity.id.value, userEntity.username, userEntity.passwordHash)
    }

}