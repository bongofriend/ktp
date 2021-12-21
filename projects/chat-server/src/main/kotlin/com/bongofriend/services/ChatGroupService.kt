package com.bongofriend.services

import com.bongofriend.data.models.ChatGroup
import com.bongofriend.data.models.User
import com.bongofriend.requests.NewChatGroupRequest
import java.util.logging.Logger

interface ChatGroupService {
    suspend fun createNewChatGroup(user: User, request: NewChatGroupRequest): ChatGroup?
    suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup): Boolean
    suspend fun getChatGroups(user: User): List<ChatGroup>
}

class ChatGroupServiceImpl(): ChatGroupService, BaseService(Logger.getLogger(ChatGroupService::class.simpleName)) {
    override suspend fun createNewChatGroup(user: User, request: NewChatGroupRequest): ChatGroup? {
        TODO("Not yet implemented")
    }

    override suspend fun addUserToChatGroup(userToAdd: User, chatGroup: ChatGroup): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getChatGroups(user: User): List<ChatGroup> {
        TODO("Not yet implemented")
    }


}