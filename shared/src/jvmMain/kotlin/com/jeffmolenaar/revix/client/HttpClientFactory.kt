package com.jeffmolenaar.revix.client

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*

/**
 * JVM-specific HTTP client factory using OkHttp (same as Android)
 */
internal actual fun createPlatformHttpClient(config: RevixClientConfig): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(config.json)
        }
        
        if (config.enableLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }
}