# Android Sample Project

This directory contains a complete Android sample project that demonstrates how to use the Revix Kotlin client library in an Android application.

## Project Structure

```
android-sample/
├── build.gradle.kts              # Android module configuration
├── src/main/
│   ├── AndroidManifest.xml       # Android app manifest
│   ├── java/com/jeffmolenaar/revix/android/sample/
│   │   ├── MainActivity.kt       # Main activity with Compose UI
│   │   ├── RevixViewModel.kt     # ViewModel for managing Revix client
│   │   └── ui/theme/
│   │       └── Theme.kt          # Material 3 theme
│   └── res/
│       └── values/
│           ├── strings.xml       # String resources
│           └── themes.xml        # App themes
```

## Features Demonstrated

- ✅ **Authentication** - Login/logout with email and password
- ✅ **Server Configuration** - Dynamic server URL input
- ✅ **Vehicle Management** - Load and display vehicles
- ✅ **Parts Management** - Load and display parts
- ✅ **Error Handling** - Comprehensive error display
- ✅ **Loading States** - Progress indicators during operations
- ✅ **Jetpack Compose UI** - Modern Android UI
- ✅ **ViewModel Pattern** - MVVM architecture
- ✅ **State Management** - Reactive UI with StateFlow

## How to Use This Sample

### Option 1: Copy to Your Existing Project

1. Copy the `android-sample` directory to your Android project
2. Add to your `settings.gradle.kts`:
   ```kotlin
   include(":android-sample")
   ```
3. Modify package names and customize as needed

### Option 2: Use as Reference

Study the code to understand:
- How to configure the Revix client for Android
- Proper error handling patterns
- UI integration with Compose
- ViewModel setup and lifecycle management

## Key Implementation Details

### Network Configuration

The sample uses `10.0.2.2:8080` as the default server URL, which is the correct way to access `localhost:8080` from an Android emulator.

### HTTP Client

The sample relies on the shared module's OkHttp client configuration, which is optimized for Android.

### State Management

Uses StateFlow for reactive UI updates:

```kotlin
data class RevixUiState(
    val serverUrl: String = "http://10.0.2.2:8080",
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val parts: List<Part> = emptyList()
)
```

### Lifecycle Management

Properly closes the Revix client in `onCleared()`:

```kotlin
override fun onCleared() {
    super.onCleared()
    revixClient?.close()
}
```

## Building and Running

To build this sample:

1. Ensure you have Android Studio installed
2. Open the parent Revix project
3. Sync Gradle files
4. Run the `:android-sample` module

**Note:** The Android sample is excluded from the main build because it requires the Android SDK and Gradle plugins. It's provided as a reference and can be built separately in Android Studio.

## Dependencies

The sample includes all necessary dependencies for:
- Revix client library
- Ktor HTTP client (OkHttp engine)
- Kotlin coroutines for Android
- Jetpack Compose UI
- Material 3 design system
- ViewModel and Lifecycle components

This sample serves as a complete template for integrating the Revix API client into any Android application.