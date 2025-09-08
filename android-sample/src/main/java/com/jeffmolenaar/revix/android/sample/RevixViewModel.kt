package com.jeffmolenaar.revix.android.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig
import com.jeffmolenaar.revix.client.RevixExceptions
import com.jeffmolenaar.revix.domain.Part
import com.jeffmolenaar.revix.domain.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RevixUiState(
    val serverUrl: String = "http://10.0.2.2:8080", // Android emulator localhost
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val parts: List<Part> = emptyList()
)

class RevixViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RevixUiState())
    val uiState: StateFlow<RevixUiState> = _uiState.asStateFlow()
    
    private var revixClient: RevixClient? = null
    
    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        // Create new client when URL changes
        revixClient?.close()
        revixClient = null
    }
    
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val client = getOrCreateClient()
                client.auth.login(_uiState.value.email, _uiState.value.password)
                
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    isLoading = false,
                    error = null
                )
            } catch (e: RevixExceptions.RevixAuthenticationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Authentication failed: ${e.message}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Login error: ${e.message}"
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                revixClient?.auth?.logout()
            } catch (e: Exception) {
                // Ignore logout errors
            }
            
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                vehicles = emptyList(),
                parts = emptyList(),
                error = null
            )
        }
    }
    
    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val client = getOrCreateClient()
                val vehicles = client.vehicles.getAll()
                
                _uiState.value = _uiState.value.copy(
                    vehicles = vehicles,
                    parts = emptyList(), // Clear parts when loading vehicles
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load vehicles: ${e.message}"
                )
            }
        }
    }
    
    fun loadParts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val client = getOrCreateClient()
                val parts = client.parts.getAll()
                
                _uiState.value = _uiState.value.copy(
                    parts = parts,
                    vehicles = emptyList(), // Clear vehicles when loading parts
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load parts: ${e.message}"
                )
            }
        }
    }
    
    private fun getOrCreateClient(): RevixClient {
        return revixClient ?: run {
            val config = RevixClientConfig(
                baseUrl = _uiState.value.serverUrl,
                enableLogging = true
            )
            RevixClient(config).also { revixClient = it }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        revixClient?.close()
    }
}