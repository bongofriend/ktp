package com.bongofriend.routes

import com.bongofriend.data.models.User
import com.bongofriend.requests.AddToChatGroupRequest
import com.bongofriend.requests.CreateChatGroupRequest
import com.bongofriend.services.ChatGroupService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

internal fun Route.chatGroupRoute() {
    val chatGroupService by inject<ChatGroupService>()

    route("/chatgroups") {
        post("/create") {
            val request = call.receive<CreateChatGroupRequest>()
            val user = call.principal<User>() ?: return@post call.respond(HttpStatusCode.Forbidden)
            val newGroup = chatGroupService.createNewChatGroup(user, request)
            return@post if (newGroup == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                call.respond(HttpStatusCode.Created, newGroup)
            }
        }

        post("/add") {
            val user = call.principal<User>() ?:return@post call.respond(HttpStatusCode.Forbidden)
            val request = call.receive<AddToChatGroupRequest>()
            val result = chatGroupService.addUserToChatGroup(user, request)
            return@post if (result) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.BadRequest)
        }

        get {
            val user = call.principal<User>() ?: return@get call.respond(HttpStatusCode.Forbidden)
            val groups = chatGroupService.getChatGroups(user)
            return@get call.respond(groups)
        }
    }
}