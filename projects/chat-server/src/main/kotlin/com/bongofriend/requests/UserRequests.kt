package com.bongofriend.requests

data class AddNewUserRequest(val username: String, val password: String)

data class GetUserTokenRequest(val username: String, val password: String)