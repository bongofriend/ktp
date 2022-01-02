package com.bongofriend.routes

import com.bongofriend.requests.AddNewUserRequest
import com.bongofriend.requests.GetUserTokenRequest
import com.bongofriend.services.LoginService
import com.bongofriend.services.UserService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoute() {
    val userService by inject<UserService>()
    val loginService by inject<LoginService>()

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
            val token = loginService.generateToken(data)
            return@post if (token == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(HttpStatusCode.OK, mapOf("token" to token))
            }
        }
    }
}