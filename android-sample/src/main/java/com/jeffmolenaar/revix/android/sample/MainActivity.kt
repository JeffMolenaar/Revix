package com.jeffmolenaar.revix.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeffmolenaar.revix.android.sample.ui.theme.RevixSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RevixSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RevixSampleApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevixSampleApp() {
    val viewModel: RevixViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Revix Android Sample",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Server URL input
        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            label = { Text("Server URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("http://localhost:8080") }
        )
        
        // Login section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (!uiState.isAuthenticated) {
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Login")
                        }
                    }
                } else {
                    Text(
                        text = "✓ Authenticated as ${uiState.email}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Button(
                        onClick = viewModel::logout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
        
        // Actions section
        if (uiState.isAuthenticated) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::loadVehicles,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Load Vehicles")
                        }
                        
                        Button(
                            onClick = viewModel::loadParts,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Load Parts")
                        }
                    }
                }
            }
        }
        
        // Error display
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Results section
        if (uiState.vehicles.isNotEmpty() || uiState.parts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn {
                        if (uiState.vehicles.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Vehicles (${uiState.vehicles.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(uiState.vehicles) { vehicle ->
                                Text(
                                    text = "• ${vehicle.manufacturer} ${vehicle.model} (${vehicle.buildYear})",
                                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                                )
                            }
                        }
                        
                        if (uiState.parts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Parts (${uiState.parts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(uiState.parts) { part ->
                                Text(
                                    text = "• ${part.name} - ${part.partNumber}",
                                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}