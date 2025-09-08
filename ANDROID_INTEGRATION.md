# Android Integration Guide

This guide shows how to integrate the Revix Kotlin client library into your Android project.

## Step 1: Add the Revix Library to Your Android Project

### Option A: Local Module Dependency (Recommended for Development)

1. Copy the `shared` folder from this repository to your Android project root
2. Add the module to your `settings.gradle.kts`:
```kotlin
include(":shared")
```

3. Add the dependency in your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":shared"))
    
    // Required for Ktor on Android
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### Option B: JAR File Dependency

1. Build the shared module:
```bash
./gradlew :shared:build
```

2. Copy the generated JAR from `shared/build/libs/shared-jvm-1.0.0.jar` to your Android project's `libs` folder

3. Add to your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(files("libs/shared-jvm-1.0.0.jar"))
    
    // Required dependencies
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-client-auth:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("io.insert-koin:koin-core:3.5.6")
}
```

## Step 2: Add Internet Permission

Add the INTERNET permission to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Step 3: Usage Examples

### Basic Setup and Authentication

```kotlin
import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    private lateinit var revixClient: RevixClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize client
        val config = RevixClientConfig(
            baseUrl = "http://10.0.2.2:8080", // Use 10.0.2.2 for emulator localhost
            enableLogging = true
        )
        revixClient = RevixClient(config)
        
        // Login
        lifecycleScope.launch {
            try {
                val auth = revixClient.auth.login("user@example.com", "password")
                // Login successful - auth contains user and tokens
                handleLoginSuccess(auth)
            } catch (e: Exception) {
                // Handle login error
                handleLoginError(e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        revixClient.close()
    }
}
```

### Using with Android ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RevixViewModel : ViewModel() {
    private val revixClient = RevixClient(RevixClientConfig("http://10.0.2.2:8080"))
    
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                revixClient.auth.login(email, password)
                loadVehicles()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadVehicles() {
        viewModelScope.launch {
            try {
                val vehicleList = revixClient.vehicles.getAll()
                _vehicles.value = vehicleList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        revixClient.close()
    }
}
```

### Using with Jetpack Compose

```kotlin
@Composable
fun VehicleList() {
    val viewModel: RevixViewModel = viewModel()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Column {
        Button(
            onClick = { viewModel.loadVehicles() },
            enabled = !isLoading
        ) {
            Text("Load Vehicles")
        }
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(vehicles) { vehicle ->
                    VehicleItem(vehicle = vehicle)
                }
            }
        }
    }
}

@Composable
fun VehicleItem(vehicle: Vehicle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${vehicle.manufacturer} ${vehicle.model}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Year: ${vehicle.buildYear}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### Complete Android Application Example

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RevixApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevixApp() {
    val viewModel: RevixViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Server URL input
        OutlinedTextField(
            value = uiState.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Authentication
        if (!uiState.isAuthenticated) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = viewModel::loadVehicles,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load Vehicles")
                }
                
                Button(
                    onClick = viewModel::loadParts,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load Parts")
                }
            }
            
            Button(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
        
        // Error display
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Results
        if (uiState.vehicles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                item {
                    Text(
                        text = "Vehicles (${uiState.vehicles.size})",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(uiState.vehicles) { vehicle ->
                    VehicleItem(vehicle = vehicle)
                }
            }
        }
    }
}
```

## Important Notes for Android

### Network Configuration

1. **Emulator**: Use `10.0.2.2` instead of `localhost` for the emulator
2. **Physical Device**: Use your computer's IP address on the same network
3. **Production**: Use your actual server URL

### Proguard Rules

If you use code obfuscation, add these rules to your `proguard-rules.pro`:

```proguard
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Revix domain classes
-keep class com.jeffmolenaar.revix.domain.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
```

### Error Handling

Always handle network operations properly in Android:

```kotlin
try {
    val result = revixClient.vehicles.getAll()
    // Handle success
} catch (e: RevixExceptions.RevixAuthenticationException) {
    // Handle authentication errors - redirect to login
} catch (e: RevixExceptions.RevixNotFoundException) {
    // Handle 404 errors
} catch (e: ConnectException) {
    // Handle network connection errors
} catch (e: Exception) {
    // Handle other errors
}
```

## Complete Working Example

The `android-sample` directory in this repository contains a complete working Android application that demonstrates all the features of the Revix client library. You can use it as a reference or starting point for your own application.

To run the sample:
1. Open Android Studio
2. Import the `android-sample` project
3. Make sure your Revix server is running
4. Update the server URL in the app
5. Run the app on an emulator or device

This integration guide makes the Revix Kotlin client library fully usable in Android Studio with complete examples and setup instructions.