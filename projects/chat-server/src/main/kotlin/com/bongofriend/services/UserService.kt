package com.bongofriend.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bongofriend.data.repositories.UserRepository
import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import io.ktor.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.logging.Logger

interface UserService {
    suspend fun addNewUser(request: AddNewUserRequest): UUID?
    suspend fun createUserToken(request: GetUserTokenRequest): String?
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

    override suspend fun addNewUser(request: AddNewUserRequest): UUID? {
       logger.info("Inserting new user")
       if (request.password.isEmpty() || request.username.isEmpty()) {
           return null
       }
      val user = withContext(Dispatchers.IO) {
           userRepo.addNewUser(request.username, hashPassword(request.password))
       }
       return user.id
    }

    override suspend fun createUserToken(request: GetUserTokenRequest): String? {
        val user = withContext(Dispatchers.IO) { userRepo.getUserByName(request.username) } ?: return null
        if (!verifyPassword(request.password, user.passwordHash)) return null
        return JWT
            .create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("id", user.id.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 6000000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }


}