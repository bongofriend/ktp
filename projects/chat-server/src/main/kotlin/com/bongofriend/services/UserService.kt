package com.bongofriend.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.bongofriend.data.repositories.UserRepository
import com.bongofriend.requests.AddNewUserRequest
import io.ktor.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.logging.Logger

interface UserService {
    suspend fun addNewUser(request: AddNewUserRequest): Boolean
}

class UserServiceImpl(private val userRepo: UserRepository, jwtConfig: ApplicationConfig) : UserService, BaseService(Logger.getLogger(UserService::class.simpleName)) {
    companion object {
        private const val SALT = 12

        private fun hashPassword(clearText: String) = BCrypt.withDefaults().hashToString(SALT, clearText.toCharArray())

        private fun verifyPassword(clearText: String, hash: String): Boolean {
            val result = BCrypt.verifyer().verify(clearText.toCharArray(), hash)
            return result.verified
        }
    }

    private val jwtSecret = jwtConfig.property("secret").getString()
    private val jwtAudience = jwtConfig.property("audience").getString()
    private val jwtIssuer = jwtConfig.property("issuer").getString()

    override suspend fun addNewUser(request: AddNewUserRequest): Boolean {
       if (request.password.isEmpty() || request.username.isEmpty()) {
           return false
       }
      withContext(Dispatchers.IO) {
           userRepo.addNewUser(request.username, hashPassword(request.password))
       }
       return true
    }

}