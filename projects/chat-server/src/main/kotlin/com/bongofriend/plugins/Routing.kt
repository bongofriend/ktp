package com.bongofriend.plugins

import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import com.bongofriend.services.UserService
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    routing {
       userRoute()
    }
}

internal fun Route.userRoute() {
    val userService by inject<UserService>()

    route("/users") {
        post {
            val userData = call.receive<AddNewUserRequest>()
            val result = userService.addNewUser(userData)
            return@post if (result == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(HttpStatusCode.Created, mapOf("id" to result.toString()))
            }
        }

        post("/token") {
            val data = call.receive<GetUserTokenRequest>()
            val token = userService.createUserToken(data)
            return@post if (token == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("token" to token))
            }
        }
    }
}

