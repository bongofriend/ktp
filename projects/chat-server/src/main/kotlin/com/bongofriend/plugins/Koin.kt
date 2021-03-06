package com.bongofriend.plugins

import com.bongofriend.data.db.ChatGroups
import com.bongofriend.data.db.ChatMessages
import com.bongofriend.data.db.Users
import com.bongofriend.data.db.UsersInGroups
import com.bongofriend.data.repositories.*
import com.bongofriend.services.*
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
    single<LoginService> { LoginServiceImpl(env.config.config("jwt"), get<UserRepository>(), get<ChatGroupRepository>()) }
    single<ClientConnectionManager> { ClientConnectionManagerImpl() }
    single<UserService>{ UserServiceImpl(get<UserRepository>(), get<LoginService>()) }
    single<ChatGroupService> { ChatGroupServiceImpl(get<ChatGroupRepository>(), get<ChatMessageRepository>(), get<LoginService>(), get<ClientConnectionManager>()) }
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
            UsersInGroups,
            ChatMessages
        )
    }

    return module {
        single<UserRepository> { UserRepositoryImpl() }
        single<ChatGroupRepository> { ChatGroupRepositoryImpl() }
        single<ChatMessageRepository> { ChatMessageRepositoryImpl() }
    }
}