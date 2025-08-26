package com.jeffmolenaar.revix.server.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.jeffmolenaar.revix.server.config.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.*
import kotlin.time.Duration.Companion.days

class AuthService(private val jwtConfig: JwtConfig) {
    
    private val algorithm = Algorithm.HMAC256(jwtConfig.secret)
    
    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }
    
    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
    
    fun generateAccessToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + jwtConfig.accessTokenExpirationMs)
        
        return JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", userId)
            .withClaim("type", "access")
            .withExpiresAt(expiration)
            .withIssuedAt(now)
            .sign(algorithm)
    }
    
    fun generateRefreshToken(userId: String): String {
        val now = Date()
        val expiration = Date(now.time + jwtConfig.refreshTokenExpirationMs)
        
        return JWT.create()
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(expiration)
            .withIssuedAt(now)
            .sign(algorithm)
    }
    
    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getUserIdFromToken(token: String): String? {
        return verifyToken(token)?.getClaim("userId")?.asString()
    }
    
    fun getTokenType(token: String): String? {
        return verifyToken(token)?.getClaim("type")?.asString()
    }
    
    fun hashRefreshToken(refreshToken: String): String {
        return BCrypt.withDefaults().hashToString(12, refreshToken.toCharArray())
    }
    
    fun verifyRefreshTokenHash(refreshToken: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(refreshToken.toCharArray(), hash).verified
    }
    
    fun getRefreshTokenExpiration(): Instant {
        return Clock.System.now().plus(7.days)
    }
}