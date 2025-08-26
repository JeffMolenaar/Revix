package com.jeffmolenaar.revix.server.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String = "org.postgresql.Driver"
)

data class JwtConfig(
    val secret: String,
    val issuer: String = "revix",
    val audience: String = "revix",
    val realm: String = "revix",
    val accessTokenExpirationMs: Long = 15 * 60 * 1000L, // 15 minutes
    val refreshTokenExpirationMs: Long = 7 * 24 * 60 * 60 * 1000L // 7 days
)

data class ServerConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0"
)

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val server: ServerConfig
)

object ConfigLoader {
    fun load(): AppConfig {
        return AppConfig(
            database = DatabaseConfig(
                url = System.getenv("REVIX_DB_URL") ?: "jdbc:postgresql://localhost:5432/revix",
                user = System.getenv("REVIX_DB_USER") ?: "revix",
                password = System.getenv("REVIX_DB_PASS") ?: "revix"
            ),
            jwt = JwtConfig(
                secret = System.getenv("REVIX_JWT_SECRET") ?: "change-me-in-production"
            ),
            server = ServerConfig(
                port = System.getenv("REVIX_PORT")?.toIntOrNull() ?: 8080,
                host = System.getenv("REVIX_HOST") ?: "0.0.0.0"
            )
        )
    }
}