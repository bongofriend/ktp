package com.bongofriend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bongofriend.services.LoginService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val loginService by inject<LoginService>()

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
            validate { credential -> loginService.verifyUser(credential) }
        }
    }
}
