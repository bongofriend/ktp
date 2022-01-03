package com.bongofriend.services

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.data.models.ChatMessage
import com.bongofriend.data.models.User
import com.bongofriend.data.repositories.ChatGroupRepository
import com.bongofriend.data.repositories.ChatMessageRepository
import com.bongofriend.requests.AddMessageToGroupRequest
import com.bongofriend.requests.AddToChatGroupRequest
import com.bongofriend.requests.CreateChatGroupRequest
import java.util.*
import java.util.logging.Logger

interface ChatGroupService {
    suspend fun createNewChatGroup(user: User, request: CreateChatGroupRequest): ChatGroup?
    suspend fun addUserToChatGroup(userToAdd: User, request: AddToChatGroupRequest): Boolean
    suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup): Boolean
    suspend fun getChatGroups(user: User): List<ChatGroup>
    suspend fun getMessages(user: User, groupId: String): List<ChatMessage>?
    suspend fun addMessage(user: User, groupId: String, request: AddMessageToGroupRequest): ChatMessage?
}

class ChatGroupServiceImpl(private val chatGroupRepo: ChatGroupRepository, private val messageRepo: ChatMessageRepository, private val loginService: LoginService): ChatGroupService, BaseService(Logger.getLogger(ChatGroupService::class.simpleName)) {
    override suspend fun createNewChatGroup(user: User, request: CreateChatGroupRequest): ChatGroup? {
        if (request.name.isEmpty()) {
            return null
        }
        val group = chatGroupRepo.createNewChatGroup(request.name)
        chatGroupRepo.addUserToChatGroup(user, group)
        return group
    }

    override suspend fun addUserToChatGroup(userToAdd: User, request: AddToChatGroupRequest): Boolean {
        if (request.groupId.isEmpty()) {
            return false
        }
        val group = chatGroupRepo.getChatGroupById(UUID.fromString(request.groupId)) ?: return false
        return chatGroupRepo.addUserToChatGroup(userToAdd, group)
    }

    override suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup) = chatGroupRepo.addUserToChatGroup(userToAdd, chatGroup)

    override suspend fun getChatGroups(user: User): List<ChatGroup> = chatGroupRepo.getChatGroupsForUser(user)

    override suspend fun getMessages(user: User, groupId: String): List<ChatMessage>? {
        if (!loginService.isUserInGroup(user, groupId)) {
            return null
        }
        return messageRepo.getMessages(UUID.fromString(groupId))
    }

    override suspend fun addMessage(user: User, groupId: String, request: AddMessageToGroupRequest): ChatMessage? {
        if(request.message.isEmpty() || !loginService.isUserInGroup(user, groupId)) return null
        return messageRepo.addMessage(request.message, user, UUID.fromString(groupId))
    }



}