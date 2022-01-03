package com.bongofriend.data.repositories

import com.bongofriend.data.db.*
import com.bongofriend.data.models.ChatMessage
import com.bongofriend.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface ChatMessageRepository {
    suspend fun addMessage(message: String, user: User, groupId: UUID): ChatMessage?
    suspend fun getMessages(groupId: UUID): List<ChatMessage>
}

class ChatMessageRepositoryImpl: ChatMessageRepository {
    override suspend fun addMessage(message: String, user: User, groupId: UUID): ChatMessage? = withContext(Dispatchers.IO) {
        return@withContext transaction {
            val userEntity = UserEntity.find { Users.id eq user.id  }.firstOrNull() ?: return@transaction null
            val groupEntity = ChatGroupEntity.find { ChatGroups.id eq groupId }.firstOrNull() ?: return@transaction null
            val messageEntity = ChatMessageEntity.new {
                this.message = message
                this.user = userEntity
                group = groupEntity
            }
            return@transaction ChatMessage(messageEntity.message, user.username)
        }
    }

    override suspend fun getMessages(groupId: UUID): List<ChatMessage> = withContext(Dispatchers.IO) {
        transaction {
            ChatMessageEntity.find { ChatMessages.group eq groupId }.map { ChatMessage(it.message, it.user.username) }
        }
    }

}