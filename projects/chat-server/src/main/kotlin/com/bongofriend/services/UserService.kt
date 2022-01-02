package com.bongofriend.services

import com.bongofriend.data.repositories.UserRepository
import com.bongofriend.requests.AddNewUserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.logging.Logger

interface UserService {
    suspend fun addNewUser(request: AddNewUserRequest): UUID?
}

class UserServiceImpl(private val userRepo: UserRepository, private val loginService: LoginService) : UserService, BaseService(Logger.getLogger(UserService::class.simpleName)) {
    override suspend fun addNewUser(request: AddNewUserRequest): UUID? {
       logger.info("Inserting new user")
       if (request.password.isEmpty() || request.username.isEmpty()) {
           return null
       }
      val user = withContext(Dispatchers.IO) {
           userRepo.addNewUser(request.username, loginService.hashPassword(request.password))
       }
       return user.id
    }


}