package com.bongofriend

import com.bongofriend.plugins.*
import io.ktor.application.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureKoin()
    configureSecurity()
    configureRouting()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
}
