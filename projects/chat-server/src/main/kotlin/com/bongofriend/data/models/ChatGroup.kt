package com.bongofriend.data.models

import java.util.*

data class ChatGroup (val id: UUID, val name: String)

data class ChatMessage(val message: String, val username: String)