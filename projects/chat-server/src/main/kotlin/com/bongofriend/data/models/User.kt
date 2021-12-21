package com.bongofriend.data.models

import io.ktor.auth.*
import java.util.*

data class User (val id: UUID, val username: String, val passwordHash: String ): Principal