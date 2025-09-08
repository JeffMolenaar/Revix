package com.jeffmolenaar.revix.client

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Configuration for the Revix API client
 */
data class RevixClientConfig(
    /**
     * Base URL of the Revix API server
     * Example: "https://your-revix-server.com" or "http://localhost:8080"
     */
    val baseUrl: String,
    
    /**
     * Enable debug logging for HTTP requests/responses
     */
    val enableLogging: Boolean = false,
    
    /**
     * Custom HTTP client engine (optional)
     */
    val engine: HttpClientEngine? = null,
    
    /**
     * Custom JSON configuration
     */
    val json: Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
) {
    internal fun createHttpClient(): HttpClient {
        return if (engine != null) {
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(this@RevixClientConfig.json)
                }
                
                if (enableLogging) {
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = LogLevel.INFO
                    }
                }
            }
        } else {
            createPlatformHttpClient(this)
        }
    }
}