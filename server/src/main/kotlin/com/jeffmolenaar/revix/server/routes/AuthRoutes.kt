package com.jeffmolenaar.revix.server.routes

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.auth.AuthService
import com.jeffmolenaar.revix.server.repository.RefreshTokenRepository
import com.jeffmolenaar.revix.server.repository.UserRepository
import com.jeffmolenaar.revix.validation.ValidationRules
import com.jeffmolenaar.revix.validation.combine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.koin.ktor.ext.inject
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Route.authRoutes() {
    val userRepository by inject<UserRepository>()
    val refreshTokenRepository by inject<RefreshTokenRepository>()
    val authService by inject<AuthService>()
    
    route("/auth") {
        post("/register") {
            val request = call.receive<CreateUserRequest>()
            
            // Validate input
            val validationResults = listOf(
                ValidationRules.validateEmail(request.email),
                ValidationRules.validatePassword(request.password)
            )
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@post
            }
            
            // Check if email already exists
            if (userRepository.findByEmail(request.email) != null) {
                call.respond(HttpStatusCode.Conflict, ApiError(
                    error = "email_exists",
                    message = "A user with this email already exists"
                ))
                return@post
            }
            
            // Create user
            val passwordHash = authService.hashPassword(request.password)
            val user = userRepository.create(request.email, passwordHash, request.name)
            
            // Generate tokens
            val accessToken = authService.generateAccessToken(user.id)
            val refreshToken = authService.generateRefreshToken(user.id)
            
            // Store refresh token
            val refreshTokenHash = authService.hashRefreshToken(refreshToken)
            refreshTokenRepository.save(user.id, refreshTokenHash, authService.getRefreshTokenExpiration())
            
            call.respond(HttpStatusCode.Created, AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = user
            ))
        }
        
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            // Find user
            val user = userRepository.findByEmail(request.email)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "invalid_credentials",
                    message = "Invalid email or password"
                ))
                return@post
            }
            
            // Verify password
            val passwordHash = userRepository.getPasswordHash(user.id)!!
            if (!authService.verifyPassword(request.password, passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "invalid_credentials",
                    message = "Invalid email or password"
                ))
                return@post
            }
            
            // Generate tokens
            val accessToken = authService.generateAccessToken(user.id)
            val refreshToken = authService.generateRefreshToken(user.id)
            
            // Store refresh token
            val refreshTokenHash = authService.hashRefreshToken(refreshToken)
            refreshTokenRepository.save(user.id, refreshTokenHash, authService.getRefreshTokenExpiration())
            
            call.respond(AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = user
            ))
        }
        
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            
            // Verify refresh token
            val userId = authService.getUserIdFromToken(request.refreshToken)
            val tokenType = authService.getTokenType(request.refreshToken)
            
            if (userId == null || tokenType != "refresh") {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "invalid_token",
                    message = "Invalid refresh token"
                ))
                return@post
            }
            
            // Check if token exists in database and is not expired
            val refreshTokenHash = authService.hashRefreshToken(request.refreshToken)
            val (storedUserId, expiresAt) = refreshTokenRepository.findByTokenHash(refreshTokenHash) ?: run {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "invalid_token",
                    message = "Refresh token not found"
                ))
                return@post
            }
            
            if (expiresAt < Clock.System.now()) {
                refreshTokenRepository.deleteByTokenHash(refreshTokenHash)
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "token_expired",
                    message = "Refresh token has expired"
                ))
                return@post
            }
            
            if (storedUserId != userId) {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    error = "invalid_token",
                    message = "Token user mismatch"
                ))
                return@post
            }
            
            // Generate new access token
            val accessToken = authService.generateAccessToken(userId)
            
            call.respond(mapOf("accessToken" to accessToken))
        }
        
        get("/me") {
            val userId = call.getUserId()
            val user = userRepository.findById(userId)
            
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "user_not_found",
                    message = "User not found"
                ))
                return@get
            }
            
            call.respond(user)
        }
    }
}

// Extension function to get user ID from JWT token
fun ApplicationCall.getUserId(): String {
    return principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
        ?: throw IllegalStateException("User ID not found in token")
}