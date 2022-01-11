package com.bongofriend.requests

data class CreateChatGroupRequest(val name: String)
data class AddToChatGroupRequest(val groupId: String? = null, val name: String? = null)
data class AddMessageToGroupRequest(val message: String)