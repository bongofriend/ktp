package com.bongofriend.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import com.bongofriend.data.models.User
import com.bongofriend.data.repositories.ChatGroupRepository
import com.bongofriend.data.repositories.UserRepository
import com.bongofriend.requests.GetUserTokenRequest
import io.ktor.auth.jwt.*
import io.ktor.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.logging.Logger

interface LoginService {
    suspend fun generateToken(request: GetUserTokenRequest): String?
    fun hashPassword(password: String): String
    suspend fun verifyUser(token: String): User?
    suspend fun verifyUser(credential: JWTCredential): User?
    suspend fun isUserInGroup(user: User, groupId: String): Boolean
}

class LoginServiceImpl(jwtConfig: ApplicationConfig, private val userRepo: UserRepository, private val groupRepo: ChatGroupRepository): LoginService, BaseService(
    Logger.getLogger(LoginService::class.simpleName)) {

    companion object {
        private const val SALT = 12
    }

    private val jwtSecret = jwtConfig.property("secret").getString()
    private val jwtAudience = jwtConfig.property("audience").getString()
    private val jwtIssuer = jwtConfig.property("issuer").getString()

    private val jwtVerifier =  JWT
        .require(Algorithm.HMAC256(jwtSecret))
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .build()

    private fun verifyPassword(clearText: String, hash: String): Boolean {
        val result = BCrypt.verifyer().verify(clearText.toCharArray(), hash)
        return result.verified
    }

    override suspend fun generateToken(request: GetUserTokenRequest): String? {
        val user = userRepo.getUserByName(request.username) ?: return null
        if (!verifyPassword(request.password, user.passwordHash)) return null
        return JWT
            .create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("id", user.id.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 6000000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    override fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(SALT, password.toCharArray())

    override suspend fun verifyUser(token: String): User? = verifyPayload(jwtVerifier.verify(token))

    override suspend fun verifyUser(credential: JWTCredential): User? = verifyPayload(credential.payload)

    private suspend fun verifyPayload(payload: Payload): User? {
        val userIdClaim = payload.getClaim("id")
        if (userIdClaim.isNull) {
            return null
        }
        val id = UUID.fromString(userIdClaim.asString())
        return withContext(Dispatchers.IO) {
            return@withContext userRepo.getUserById(id)
        }
    }

    override suspend fun isUserInGroup(user: User, groupId: String): Boolean {
        if (groupId.isEmpty()) return false
        val id = UUID.fromString(groupId)
        return groupRepo.isUserInGroup(user, id)
    }

}