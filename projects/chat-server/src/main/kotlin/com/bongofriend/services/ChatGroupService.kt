package com.bongofriend.services

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.data.models.User
import com.bongofriend.data.repositories.ChatGroupRepository
import com.bongofriend.requests.AddToChatGroupRequest
import com.bongofriend.requests.CreateChatGroupRequest
import java.util.*
import java.util.logging.Logger

interface ChatGroupService {
    suspend fun createNewChatGroup(user: User, request: CreateChatGroupRequest): ChatGroup?
    suspend fun addUserToChatGroup(userToAdd: User, request: AddToChatGroupRequest)
    suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup)
    suspend fun getChatGroups(user: User): List<ChatGroup>
}

class ChatGroupServiceImpl(private val chatGroupRepo: ChatGroupRepository): ChatGroupService, BaseService(Logger.getLogger(ChatGroupService::class.simpleName)) {
    override suspend fun createNewChatGroup(user: User, request: CreateChatGroupRequest): ChatGroup? {
        if (request.name.isEmpty()) {
            return null
        }
        val group = chatGroupRepo.createNewChatGroup(request.name)
        chatGroupRepo.addUserToChatGroup(user, group)
        return group
    }

    override suspend fun addUserToChatGroup(userToAdd: User, request: AddToChatGroupRequest) {
        if (request.groupId.isEmpty()) {
            return
        }
        val group = chatGroupRepo.getChatGroupById(UUID.fromString(request.groupId)) ?: return
        chatGroupRepo.addUserToChatGroup(userToAdd, group)
    }

    override suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup) = chatGroupRepo.addUserToChatGroup(userToAdd, chatGroup)

    override suspend fun getChatGroups(user: User): List<ChatGroup> = chatGroupRepo.getChatGroupsForUser(user)


}