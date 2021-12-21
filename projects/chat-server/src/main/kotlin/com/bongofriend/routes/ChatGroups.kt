package com.bongofriend.routes

import com.bongofriend.data.models.User
import com.bongofriend.requests.NewChatGroupRequest
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

    post {
        val request = call.receive<NewChatGroupRequest>()
        val user = call.principal<User>() ?: return@post call.respond(HttpStatusCode.Forbidden)
        val newGroup = chatGroupService.createNewChatGroup(user, request)
        return@post if (newGroup == null) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            call.respond(HttpStatusCode.Created, newGroup)
        }
    }

    get {
        val user = call.principal<User>() ?: return@get call.respond(HttpStatusCode.Forbidden)
        val groups = chatGroupService.getChatGroups(user)
        return@get call.respond(groups)
    }
}