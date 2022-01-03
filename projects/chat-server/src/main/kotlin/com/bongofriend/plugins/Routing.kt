package com.bongofriend.plugins

import com.bongofriend.routes.chatGroupRoute
import com.bongofriend.routes.messages
import com.bongofriend.routes.userRoute
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*

fun Application.configureRouting() {

    routing {
        userRoute()
        authenticate("auth-jwt") {
            chatGroupRoute()
            messages()
        }
    }
}

