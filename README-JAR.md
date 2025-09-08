# Revix Kotlin Library JAR Generation Guide

Deze handleiding legt uit hoe je een JAR-bestand kunt genereren van de Revix Kotlin library en hoe je deze kunt gebruiken in je eigen projecten.

## 🚀 Snelle Start

### Automatisch (Windows)
```batch
# Voer de build script uit
build-jar.bat
```

### Handmatig (alle platforms)
```bash
# Linux/macOS
./gradlew :shared:build

# Windows
gradlew.bat :shared:build
```

## 📦 Gegenereerde Bestanden

Na het bouwen vind je de volgende bestanden in `shared/build/libs/`:

- **`shared-jvm-1.0.0.jar`** - De hoofdbibliotheek (dit is wat je nodig hebt)
- **`shared-jvm-1.0.0-sources.jar`** - Broncode (optioneel, nuttig voor debugging)
- **`shared-metadata-1.0.0.jar`** - Kotlin Multiplatform metadata (niet nodig voor Java/Android)

## 🔧 Gebruik in je Project

### Android Project

#### Optie 1: JAR bestand (aanbevolen voor distributie)

1. **Kopieer de JAR:**
   ```bash
   cp shared/build/libs/shared-jvm-1.0.0.jar your-android-project/app/libs/
   ```

2. **Voeg toe aan `build.gradle.kts`:**
   ```kotlin
   dependencies {
       implementation(files("libs/shared-jvm-1.0.0.jar"))
       
       // Vereiste dependencies voor Ktor op Android
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

#### Optie 2: Lokale module (aanbevolen voor ontwikkeling)

1. **Kopieer de shared folder:**
   ```bash
   cp -r shared your-android-project/
   ```

2. **Voeg toe aan `settings.gradle.kts`:**
   ```kotlin
   include(":shared")
   ```

3. **Voeg dependency toe aan `build.gradle.kts`:**
   ```kotlin
   dependencies {
       implementation(project(":shared"))
       implementation("io.ktor:ktor-client-okhttp:2.3.12")
       implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
   }
   ```

### Java/Kotlin Desktop Project

```kotlin
dependencies {
    implementation(files("libs/shared-jvm-1.0.0.jar"))
    
    // Vereiste dependencies
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-client-auth:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12") // of cio voor desktop
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("io.insert-koin:koin-core:3.5.6")
}
```

### Maven Project

1. **Installeer JAR in lokale repository:**
   ```bash
   mvn install:install-file \
     -Dfile=shared-jvm-1.0.0.jar \
     -DgroupId=com.jeffmolenaar.revix \
     -DartifactId=shared \
     -Dversion=1.0.0 \
     -Dpackaging=jar
   ```

2. **Voeg toe aan `pom.xml`:**
   ```xml
   <dependency>
       <groupId>com.jeffmolenaar.revix</groupId>
       <artifactId>shared</artifactId>
       <version>1.0.0</version>
   </dependency>
   
   <!-- Vereiste dependencies -->
   <dependency>
       <groupId>io.ktor</groupId>
       <artifactId>ktor-client-core</artifactId>
       <version>2.3.12</version>
   </dependency>
   <!-- ... andere Ktor dependencies -->
   ```

## 💻 Code Voorbeelden

### Basis gebruik

```kotlin
import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig

suspend fun main() {
    // Maak client configuratie
    val config = RevixClientConfig(
        baseUrl = "http://localhost:8080", // of je server URL
        enableLogging = true
    )
    
    // Maak de client
    val client = RevixClient(config)
    
    try {
        // Login
        client.auth.login("user@example.com", "password")
        
        // Gebruik de API
        val vehicles = client.vehicles.getAll()
        println("Gevonden ${vehicles.size} voertuigen")
        
        // Logout
        client.auth.logout()
    } finally {
        // Sluit de client
        client.close()
    }
}
```

### Android ViewModel voorbeeld

```kotlin
class MainViewModel : ViewModel() {
    private val client = RevixClient(
        RevixClientConfig("https://your-server.com")
    )
    
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                client.auth.login(email, password)
                // Handle successful login
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
```

## 🔍 JAR Inhoud Verificatie

Je kunt de inhoud van de gegenereerde JAR controleren:

```bash
# Toon JAR inhoud
jar -tf shared/build/libs/shared-jvm-1.0.0.jar

# Toon manifest
jar -xf shared/build/libs/shared-jvm-1.0.0.jar META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
```

## 🛠️ Geavanceerd: Fat JAR met dependencies

Als je een JAR wilt met alle dependencies inbegrepen, kun je het volgende toevoegen aan `shared/build.gradle.kts`:

```kotlin
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("fat")
    from(sourceSets.main.get().output)
    
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

Dan kun je een fat JAR bouwen met:
```bash
./gradlew :shared:fatJar
```

## ⚠️ Belangrijke Opmerkingen

1. **Dependencies**: De JAR bevat alleen de Revix library code. Je moet de Ktor en andere dependencies apart toevoegen aan je project.

2. **Versie**: De JAR versie is momenteel `1.0.0`. Check de `build.gradle.kts` voor de huidige versie.

3. **Platform**: De `shared-jvm-1.0.0.jar` is specifiek voor JVM platforms (Android, Desktop Java/Kotlin).

4. **Network configuratie**: Voor Android, zorg ervoor dat je internetrechten hebt in je `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

## 🐛 Probleemoplossing

### Build fouten
```bash
# Clean en rebuild
./gradlew :shared:clean :shared:build
```

### Dependency conflicten
Zorg ervoor dat alle Ktor dependencies dezelfde versie hebben (2.3.12).

### Android ProGuard/R8
Voeg toe aan `proguard-rules.pro`:
```
-keep class com.jeffmolenaar.revix.** { *; }
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
```

## 📚 Meer Informatie

- Zie `ANDROID_INTEGRATION.md` voor uitgebreide Android integratie
- Zie `shared/src/commonMain/kotlin/com/jeffmolenaar/revix/client/README.md` voor API documentatie
- Check de `server/` directory voor de backend setup