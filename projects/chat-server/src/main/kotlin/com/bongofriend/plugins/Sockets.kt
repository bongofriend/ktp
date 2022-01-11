package com.bongofriend.plugins

import com.bongofriend.services.ClientConnectionManager
import com.bongofriend.services.LoginService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import org.koin.ktor.ext.inject
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        timeout = Duration.ofHours(1)
        pingPeriod = Duration.ofHours(1)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }


    routing {
        val loginService by inject<LoginService>()
        val connectionManager by inject<ClientConnectionManager>()

        webSocket("/socket") {
            val token = call.parameters["token"]
            val group = call.parameters["groupId"]

            if (token.isNullOrEmpty() || group.isNullOrEmpty()) {
                return@webSocket call.respond(HttpStatusCode.Forbidden)
            }
            val user = loginService.verifyUser(token)
            if (user == null || !loginService.isUserInGroup(user, group)) {
                return@webSocket call.respond(HttpStatusCode.Forbidden)
            }
            connectionManager.addClient(group, this)
            //send("Test")
        }
    }

}


