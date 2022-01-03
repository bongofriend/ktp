package com.bongofriend.requests

data class CreateChatGroupRequest(val name: String)
data class AddToChatGroupRequest(val groupId: String)
data class AddMessageToGroupRequest(val message: String)