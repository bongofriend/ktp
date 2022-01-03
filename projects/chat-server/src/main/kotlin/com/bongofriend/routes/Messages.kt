package com.bongofriend.routes

import com.bongofriend.data.models.User
import com.bongofriend.requests.AddMessageToGroupRequest
import com.bongofriend.services.ChatGroupService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

internal fun Route.messages() {
    val chatGroupService by inject<ChatGroupService>()

    route("messages") {
        get("{groupId}") {
            val user = call.principal<User>() ?: return@get call.respond(HttpStatusCode.Forbidden)
            val groupId = call.parameters["groupId"] ?: return@get call.respond(HttpStatusCode.NotFound)
            val messages = chatGroupService.getMessages(user, groupId) ?: return@get call.respond(HttpStatusCode.NotFound)
            return@get call.respond(HttpStatusCode.OK, mapOf("messages" to messages))
        }

        post("{groupId}") {
            val user = call.principal<User>() ?: return@post call.respond(HttpStatusCode.Forbidden)
            val groupId = call.parameters["groupId"] ?: return@post call.respond(HttpStatusCode.NotFound)
            val request = call.receive<AddMessageToGroupRequest>()
            val msg = chatGroupService.addMessage(user, groupId, request) ?: return@post call.respond(HttpStatusCode.BadRequest)
            return@post call.respond(HttpStatusCode.Created, mapOf("message" to msg))
        }
    }
}