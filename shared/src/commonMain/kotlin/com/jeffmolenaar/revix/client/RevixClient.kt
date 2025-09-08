package com.jeffmolenaar.revix.client

import com.jeffmolenaar.revix.domain.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Main client class for interacting with the Revix API
 * 
 * Usage example:
 * ```kotlin
 * val client = RevixClient(RevixClientConfig("http://localhost:8080"))
 * 
 * // Login
 * val authResponse = client.auth.login("user@example.com", "password")
 * 
 * // Get vehicles
 * val vehicles = client.vehicles.getAll()
 * 
 * // Create a vehicle
 * val vehicle = client.vehicles.create(CreateVehicleRequest(...))
 * ```
 */
class RevixClient(private val config: RevixClientConfig) {
    private val httpClient: HttpClient = config.createHttpClient()
    private val baseUrl = config.baseUrl.trimEnd('/')
    private val apiBaseUrl = "$baseUrl/api/v1"
    
    // Token management
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private val tokenMutex = Mutex()
    
    // Service instances
    val auth = AuthService()
    val vehicles = VehicleService()
    val parts = PartService()
    val tags = TagService()
    val maintenance = MaintenanceService()
    
    /**
     * Close the HTTP client when done
     */
    fun close() {
        httpClient.close()
    }
    
    /**
     * Make an authenticated API request
     */
    private suspend inline fun <reified T> apiRequest(
        endpoint: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null,
        requireAuth: Boolean = true
    ): T {
        val response = apiRequestResponse(endpoint, method, body, requireAuth)
        
        // Handle error responses
        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.body<ApiError>()
            } catch (e: Exception) {
                ApiError("unknown_error", "An unknown error occurred")
            }
            
            when (response.status) {
                HttpStatusCode.Unauthorized -> throw RevixAuthenticationException(errorBody.message)
                HttpStatusCode.NotFound -> throw RevixNotFoundException(errorBody.message)
                HttpStatusCode.BadRequest -> throw RevixValidationException(errorBody.message, errorBody.details)
                else -> throw RevixApiException.fromApiError(response.status.value, errorBody)
            }
        }
        
        return response.body()
    }
    
    /**
     * Make an authenticated API request and return the response
     */
    private suspend fun apiRequestResponse(
        endpoint: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null,
        requireAuth: Boolean = true
    ): HttpResponse {
        val url = "$apiBaseUrl$endpoint"
        
        return tokenMutex.withLock {
            val response = httpClient.request(url) {
                this.method = method
                
                if (requireAuth && accessToken != null) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                }
                
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            
            // Handle token refresh if needed
            if (response.status == HttpStatusCode.Unauthorized && requireAuth && refreshToken != null) {
                refreshAccessToken()
                return@withLock httpClient.request(url) {
                    this.method = method
                    
                    if (accessToken != null) {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $accessToken")
                        }
                    }
                    
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
            }
            
            response
        }
    }
    
    /**
     * Refresh the access token using the refresh token
     */
    private suspend fun refreshAccessToken() {
        val currentRefreshToken = refreshToken ?: return
        
        try {
            val response: Map<String, String> = apiRequest(
                endpoint = "/auth/refresh",
                method = HttpMethod.Post,
                body = RefreshTokenRequest(currentRefreshToken),
                requireAuth = false
            )
            
            accessToken = response["accessToken"]
        } catch (e: Exception) {
            // If refresh fails, clear tokens
            accessToken = null
            refreshToken = null
            throw e
        }
    }
    
    /**
     * Set authentication tokens
     */
    private fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }
    
    /**
     * Clear authentication tokens
     */
    private fun clearTokens() {
        accessToken = null
        refreshToken = null
    }
    
    /**
     * Check if the client is authenticated
     */
    fun isAuthenticated(): Boolean = accessToken != null
    
    /**
     * Authentication service
     */
    inner class AuthService {
        /**
         * Register a new user account
         */
        suspend fun register(email: String, password: String, name: String? = null): AuthResponse {
            val request = CreateUserRequest(email, password, name)
            val response: AuthResponse = apiRequest("/auth/register", HttpMethod.Post, request, requireAuth = false)
            setTokens(response.accessToken, response.refreshToken)
            return response
        }
        
        /**
         * Login with email and password
         */
        suspend fun login(email: String, password: String): AuthResponse {
            val request = LoginRequest(email, password)
            val response: AuthResponse = apiRequest("/auth/login", HttpMethod.Post, request, requireAuth = false)
            setTokens(response.accessToken, response.refreshToken)
            return response
        }
        
        /**
         * Logout the current user
         */
        fun logout() {
            clearTokens()
        }
        
        /**
         * Get the current user profile
         */
        suspend fun getCurrentUser(): User {
            return apiRequest("/auth/me")
        }
        
        /**
         * Manually refresh the access token
         */
        suspend fun refresh(): String? {
            refreshAccessToken()
            return accessToken
        }
    }
    
    /**
     * Vehicle management service
     */
    inner class VehicleService {
        /**
         * Get all vehicles for the current user
         */
        suspend fun getAll(page: Int = 1, pageSize: Int = 20): PaginatedResponse<Vehicle> {
            return apiRequest("/vehicles?page=$page&pageSize=$pageSize")
        }
        
        /**
         * Get a specific vehicle by ID
         */
        suspend fun getById(id: String): Vehicle {
            return apiRequest("/vehicles/$id")
        }
        
        /**
         * Create a new vehicle
         */
        suspend fun create(request: CreateVehicleRequest): Vehicle {
            return apiRequest("/vehicles", HttpMethod.Post, request)
        }
        
        /**
         * Update an existing vehicle
         */
        suspend fun update(id: String, request: UpdateVehicleRequest): Vehicle {
            return apiRequest("/vehicles/$id", HttpMethod.Put, request)
        }
        
        /**
         * Delete a vehicle
         */
        suspend fun delete(id: String) {
            apiRequestResponse("/vehicles/$id", HttpMethod.Delete)
        }
    }
    
    /**
     * Parts management service
     */
    inner class PartService {
        /**
         * Get all parts for the current user
         */
        suspend fun getAll(
            page: Int = 1, 
            pageSize: Int = 20, 
            query: String? = null, 
            tagIds: List<String>? = null
        ): PaginatedResponse<Part> {
            val params = mutableListOf<String>()
            params.add("page=$page")
            params.add("pageSize=$pageSize")
            query?.let { params.add("query=$it") }
            tagIds?.let { if (it.isNotEmpty()) params.add("tags=${it.joinToString(",")}") }
            
            val queryString = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
            return apiRequest("/parts$queryString")
        }
        
        /**
         * Get a specific part by ID
         */
        suspend fun getById(id: String): Part {
            return apiRequest("/parts/$id")
        }
        
        /**
         * Create a new part
         */
        suspend fun create(request: CreatePartRequest): Part {
            return apiRequest("/parts", HttpMethod.Post, request)
        }
        
        /**
         * Update an existing part
         */
        suspend fun update(id: String, request: UpdatePartRequest): Part {
            return apiRequest("/parts/$id", HttpMethod.Put, request)
        }
        
        /**
         * Delete a part
         */
        suspend fun delete(id: String) {
            apiRequestResponse("/parts/$id", HttpMethod.Delete)
        }
    }
    
    /**
     * Tags management service
     */
    inner class TagService {
        /**
         * Get all tags for the current user
         */
        suspend fun getAll(): List<Tag> {
            return apiRequest("/tags")
        }
        
        /**
         * Create a new tag
         */
        suspend fun create(request: CreateTagRequest): Tag {
            return apiRequest("/tags", HttpMethod.Post, request)
        }
        
        /**
         * Update an existing tag
         */
        suspend fun update(id: String, request: UpdateTagRequest): Tag {
            return apiRequest("/tags/$id", HttpMethod.Put, request)
        }
        
        /**
         * Delete a tag
         */
        suspend fun delete(id: String) {
            apiRequestResponse("/tags/$id", HttpMethod.Delete)
        }
    }
    
    /**
     * Maintenance records management service
     */
    inner class MaintenanceService {
        /**
         * Get maintenance records for a specific vehicle
         */
        suspend fun getByVehicle(
            vehicleId: String, 
            page: Int = 1, 
            pageSize: Int = 20,
            from: String? = null,
            to: String? = null
        ): PaginatedResponse<MaintenanceRecord> {
            val params = mutableListOf<String>()
            params.add("page=$page")
            params.add("pageSize=$pageSize")
            from?.let { params.add("from=$it") }
            to?.let { params.add("to=$it") }
            
            val queryString = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
            return apiRequest("/vehicles/$vehicleId/maintenance$queryString")
        }
        
        /**
         * Get a specific maintenance record by ID
         */
        suspend fun getById(id: String): MaintenanceRecord {
            return apiRequest("/maintenance/$id")
        }
        
        /**
         * Create a new maintenance record
         */
        suspend fun create(vehicleId: String, request: CreateMaintenanceRecordRequest): MaintenanceRecord {
            return apiRequest("/vehicles/$vehicleId/maintenance", HttpMethod.Post, request)
        }
        
        /**
         * Update an existing maintenance record
         */
        suspend fun update(id: String, request: UpdateMaintenanceRecordRequest): MaintenanceRecord {
            return apiRequest("/maintenance/$id", HttpMethod.Put, request)
        }
        
        /**
         * Delete a maintenance record
         */
        suspend fun delete(id: String) {
            apiRequestResponse("/maintenance/$id", HttpMethod.Delete)
        }
    }
}