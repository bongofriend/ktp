package com.bongofriend.plugins

import com.bongofriend.data.db.ChatGroups
import com.bongofriend.data.db.Users
import com.bongofriend.data.db.UsersInGroups
import com.bongofriend.data.repositories.ChatGroupRepository
import com.bongofriend.data.repositories.ChatGroupRepositoryImpl
import com.bongofriend.data.repositories.UserRepository
import com.bongofriend.data.repositories.UserRepositoryImpl
import com.bongofriend.services.ChatGroupService
import com.bongofriend.services.ChatGroupServiceImpl
import com.bongofriend.services.UserService
import com.bongofriend.services.UserServiceImpl
import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.Koin

fun Application.configureKoin() {
    install(Koin) {
        modules(
            repoModule(environment),
            serviceModule(environment)
        )
    }
}

internal fun serviceModule(env: ApplicationEnvironment) = module {
    single<UserService>{ UserServiceImpl(get<UserRepository>(), env.config.config("jwt")) }
    single<ChatGroupService> { ChatGroupServiceImpl() }
}

internal fun repoModule(env: ApplicationEnvironment): Module {
    val dbConfig = env.config.config("db")
    val jdbcUrl = dbConfig.property("url").getString()
    val driver = dbConfig.property("driver").getString()
    Database.connect(jdbcUrl, driver)
    transaction {
        SchemaUtils.create(
            Users,
            ChatGroups,
            UsersInGroups
        )
    }

    return module {
        single<UserRepository> { UserRepositoryImpl() }
        single<ChatGroupRepository> { ChatGroupRepositoryImpl() }
    }
}