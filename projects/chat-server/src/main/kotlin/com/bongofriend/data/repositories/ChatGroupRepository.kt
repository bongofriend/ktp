package com.bongofriend.data.repositories

import com.bongofriend.data.db.ChatGroupEntity
import com.bongofriend.data.db.ChatGroups
import com.bongofriend.data.db.UserEntity
import com.bongofriend.data.db.Users
import com.bongofriend.data.models.ChatGroup
import com.bongofriend.data.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface ChatGroupRepository {
    suspend fun createNewChatGroup(name: String): ChatGroup
    suspend fun addUserToChatGroup(user: User, group: ChatGroup)
    suspend fun getChatGroupById(id: UUID): ChatGroup?
    suspend fun getChatGroupsForUser(user: User): List<ChatGroup>
}

class ChatGroupRepositoryImpl: ChatGroupRepository {
    override suspend fun createNewChatGroup(name: String): ChatGroup {
        val entity = withContext(Dispatchers.IO) {
            transaction {
                ChatGroupEntity.new {
                    this.name =  name
                    this.members = SizedCollection(emptyList())
                }
            }
        }
        return ChatGroup(entity.id.value, entity.name)
    }

    override suspend fun addUserToChatGroup(user: User, group: ChatGroup) {
        withContext(Dispatchers.IO) {
            transaction {
                val userEntity = UserEntity.find { Users.id eq user.id }.first()
                val groupEntity = ChatGroupEntity.find { ChatGroups.id eq group.id }.first()
                groupEntity.members = SizedCollection(groupEntity.members + userEntity)
            }
        }
    }

    override suspend fun getChatGroupById(id: UUID): ChatGroup? = withContext(Dispatchers.IO) {
        val entity = transaction {
            ChatGroupEntity.find { ChatGroups.id eq id }.firstOrNull()
        } ?: return@withContext null
        return@withContext ChatGroup(entity.id.value, entity.name)

    }

    override suspend fun getChatGroupsForUser(user: User): List<ChatGroup> = withContext(Dispatchers.IO) {
        transaction {
            val entity = UserEntity.find { Users.id eq user.id }.first()
            return@transaction entity.groups.toList().map { g -> ChatGroup(g.id.value, g.name) }
        }
    }


}