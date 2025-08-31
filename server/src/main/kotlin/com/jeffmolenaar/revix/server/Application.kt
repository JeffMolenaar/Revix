package com.jeffmolenaar.revix.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.jeffmolenaar.revix.domain.ApiError
import com.jeffmolenaar.revix.server.auth.AuthService
import com.jeffmolenaar.revix.server.config.AppConfig
import com.jeffmolenaar.revix.server.config.ConfigLoader
import com.jeffmolenaar.revix.server.database.*
import com.jeffmolenaar.revix.server.repository.*
import com.jeffmolenaar.revix.server.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val config = ConfigLoader.load()
    
    embeddedServer(Netty, port = config.server.port, host = config.server.host, module = { module(config) })
        .start(wait = true)
}

fun Application.module(config: AppConfig = ConfigLoader.load()) {
    // Configure serialization first to handle responses properly
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(
                    error = "internal_error",
                    message = "An unexpected error occurred"
                )
            )
        }
        
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiError(
                    error = "not_found",
                    message = "The requested resource was not found"
                )
            )
        }
    }

    // Add basic health check that doesn't depend on database
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "timestamp" to System.currentTimeMillis()))
        }
    }

    // Initialize database in a try-catch block
    try {
        initDatabase(config)
        
        // Configure Koin DI only after successful database initialization
        install(Koin) {
            slf4jLogger()
            modules(appModule(config))
        }
        
        // Add database-dependent routes
        configureDependentRoutes(config)
    } catch (e: Exception) {
        log.error("Failed to initialize database", e)
        // Add a degraded mode route
        routing {
            route("/api/v1") {
                get {
                    call.respond(HttpStatusCode.ServiceUnavailable, ApiError(
                        error = "service_unavailable",
                        message = "Database is not available"
                    ))
                }
            }
        }
    }
}

fun Application.configureDependentRoutes(config: AppConfig) {
    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // @TODO: In production, configure specific hosts
    }
    
    // Configure JWT authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.jwt.realm
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "authentication_required",
                    message = "Authentication required"
                ))
            }
        }
    }
    
    routing {
        // Public routes
        route("/api/v1") {
            authRoutes()
            
            // Protected routes
            authenticate("auth-jwt") {
                vehicleRoutes()
                tagRoutes()
                partRoutes()
                maintenanceRoutes()
                
                // System meta endpoint
                get("/meta") {
                    call.respond(mapOf(
                        "version" to "1.0.0",
                        "buildCommit" to (System.getenv("BUILD_COMMIT") ?: "unknown")
                    ))
                }
            }
        }
    }
}

fun initDatabase(config: AppConfig) {
    // Connect to database
    Database.connect(
        url = config.database.url,
        driver = config.database.driver,
        user = config.database.user,
        password = config.database.password
    )
    
    // Run migrations
    val flyway = Flyway.configure()
        .dataSource(config.database.url, config.database.user, config.database.password)
        .load()
    
    flyway.migrate()
}

fun appModule(config: AppConfig) = module {
    single { config }
    single { config.jwt }
    single { config.database }
    single { config.server }
    
    // Services
    singleOf(::AuthService)
    
    // Repositories
    singleOf(::UserRepository)
    singleOf(::RefreshTokenRepository)
    singleOf(::VehicleRepository)
    singleOf(::TagRepository)
    singleOf(::PartRepository)
    singleOf(::MaintenanceRepository)
}