package com.bongofriend.services

import com.bongofriend.data.models.ChatMessage
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

interface ClientConnectionManager {
    suspend fun addClient(groupId: String, session: DefaultWebSocketSession)
    suspend fun publishMessage(groupId: String, msg: ChatMessage)
}


class ClientConnectionManagerImpl: ClientConnectionManager, BaseService(Logger.getLogger(ClientConnectionManager::class.simpleName)) {
    private val currentClientConnections = Collections.synchronizedMap<String, List<DefaultWebSocketSession>>(HashMap())
    private val mapper = jacksonObjectMapper()

    override suspend fun addClient(groupId: String, session: DefaultWebSocketSession) {
        if (!currentClientConnections.containsKey(groupId)) {
            currentClientConnections[groupId] = emptyList()
        }
        val cons = currentClientConnections[groupId]!!.toMutableList()
        cons += session
        currentClientConnections[groupId] = cons
       for (i in session.incoming) {
            when(i) {
                is Frame.Ping -> {
                    session.send(Frame.Pong(byteArrayOf()))
                }
            }
        }
    }

    override suspend fun publishMessage(groupId: String, msg: ChatMessage) {
        if(!currentClientConnections.containsKey(groupId)) return

            withContext(Dispatchers.IO) {
                val connections = currentClientConnections[groupId]!!
                launch {
                    val data = mapper.writeValueAsString(msg)
                    connections.forEach { c ->
                        try {
                            c.send(data)
                        } catch (e: ClosedReceiveChannelException) {
                            logger.log(Level.WARNING, "${c.closeReason.await()}")
                            val cons = connections.toMutableList()
                            cons -= c
                            currentClientConnections[groupId] = cons
                        } catch (e: Throwable) {
                            logger.log(Level.WARNING, "${c.closeReason.await()}")
                            val cons = connections.toMutableList()
                            cons -= c
                            currentClientConnections[groupId] = cons
                        }
                    }
                }
            }
    }


}

