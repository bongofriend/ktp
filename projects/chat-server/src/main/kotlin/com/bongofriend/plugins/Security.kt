package com.bongofriend.plugins

import io.ktor.auth.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bongofriend.data.repositories.UserRepository
import io.ktor.application.*
import org.koin.ktor.ext.inject
import java.util.*

fun Application.configureSecurity() {

    authentication {
        jwt("auth-jwt") {
            val jwtConfig = environment.config.config("jwt")
            val jwtAudience = jwtConfig.property("audience").getString()
            val jwtSecret = jwtConfig.property("secret").getString()
            val jwtIssuer = jwtConfig.property("issuer").getString()
            val jwtRealm = jwtConfig.property("realm").getString()

            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val userRepository by inject<UserRepository>()
                val userId = credential.getClaim("id", String::class) ?: return@validate null
                return@validate userRepository.getUserById(UUID.fromString(userId))
            }
        }
    }
}
