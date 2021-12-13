package com.bongofriend.plugins

import com.bongofriend.requests.AddNewUserRequest
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
            return@post call.respond(if (result) { HttpStatusCode.Created } else { HttpStatusCode.BadRequest })
        }
    }
}
