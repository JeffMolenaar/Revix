package com.jeffmolenaar.revix.client

import com.jeffmolenaar.revix.domain.ApiError

/**
 * Base exception for all Revix API client errors
 */
sealed class RevixException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when an API request fails
 */
class RevixApiException(
    val statusCode: Int,
    val error: String,
    message: String,
    val details: Map<String, String>? = null
) : RevixException("[$statusCode] $error: $message") {
    
    companion object {
        fun fromApiError(statusCode: Int, apiError: ApiError): RevixApiException {
            return RevixApiException(
                statusCode = statusCode,
                error = apiError.error,
                message = apiError.message,
                details = apiError.details
            )
        }
    }
}

/**
 * Exception thrown when authentication fails
 */
class RevixAuthenticationException(message: String, cause: Throwable? = null) : RevixException(message, cause)

/**
 * Exception thrown when a resource is not found
 */
class RevixNotFoundException(resource: String) : RevixException("$resource not found")

/**
 * Exception thrown when validation fails
 */
class RevixValidationException(message: String, val details: Map<String, String>? = null) : RevixException(message)

/**
 * Exception thrown when the client is not configured properly
 */
class RevixConfigurationException(message: String) : RevixException(message)