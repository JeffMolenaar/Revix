# Revix Kotlin Client

Een eenvoudige Kotlin client voor de Revix API waarmee je gemakkelijk kunt aanmelden, afmelden en data kunt toevoegen, wijzigen en verwijderen.

## Installatie

Voeg de `shared` module toe aan je project afhankelijkheden in `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":shared"))
}
```

## Gebruik

### Basis setup

```kotlin
import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig

// Maak een client configuratie
val config = RevixClientConfig(
    baseUrl = "http://localhost:8080", // Vervang met je server URL
    enableLogging = true // Optioneel: zet logging aan voor debugging
)

// Maak de client
val client = RevixClient(config)
```

### Authenticatie

```kotlin
// Aanmelden (login)
val authResponse = client.auth.login("user@example.com", "password")
println("Ingelogd als: ${authResponse.user.email}")

// Registreren van nieuwe gebruiker
val authResponse = client.auth.register("newuser@example.com", "password", "Naam")

// Huidige gebruiker ophalen
val currentUser = client.auth.getCurrentUser()

// Afmelden (logout)
client.auth.logout()

// Controleren of ingelogd
if (client.isAuthenticated()) {
    println("Gebruiker is ingelogd")
}
```

### Voertuigen beheren

```kotlin
// Alle voertuigen ophalen
val vehicles = client.vehicles.getAll()
println("${vehicles.totalCount} voertuigen gevonden")

// Specifiek voertuig ophalen
val vehicle = client.vehicles.getById("vehicle-id")

// Nieuw voertuig toevoegen
val createRequest = CreateVehicleRequest(
    manufacturer = "Toyota",
    model = "Camry", 
    buildYear = 2020,
    licensePlate = "AB-123-CD",
    vin = "1HGBH41JXMN109186",
    fuelType = "Benzine",
    currentOdo = 50000
)
val newVehicle = client.vehicles.create(createRequest)

// Voertuig wijzigen
val updateRequest = UpdateVehicleRequest(
    currentOdo = 52000
)
val updatedVehicle = client.vehicles.update("vehicle-id", updateRequest)

// Voertuig verwijderen
client.vehicles.delete("vehicle-id")
```

### Onderdelen beheren

```kotlin
// Alle onderdelen ophalen
val parts = client.parts.getAll()

// Onderdelen zoeken
val searchResults = client.parts.getAll(
    query = "motorolie",
    tagIds = listOf("tag-id"),
    page = 1,
    pageSize = 20
)

// Onderdeel ophalen
val part = client.parts.getById("part-id")

// Nieuw onderdeel toevoegen
val createRequest = CreatePartRequest(
    name = "Mobil 1 5W-30 Motorolie",
    description = "Volledig synthetische motorolie",
    priceCents = 4500, // €45.00 in centen
    currency = "EUR",
    url = "https://example.com/oil",
    tagIds = listOf("tag-id")
)
val newPart = client.parts.create(createRequest)

// Onderdeel wijzigen
val updateRequest = UpdatePartRequest(
    priceCents = 4200
)
val updatedPart = client.parts.update("part-id", updateRequest)

// Onderdeel verwijderen
client.parts.delete("part-id")
```

### Tags beheren

```kotlin
// Alle tags ophalen
val tags = client.tags.getAll()

// Nieuwe tag toevoegen
val createRequest = CreateTagRequest(
    name = "Motorolie",
    color = "#FF5722"
)
val newTag = client.tags.create(createRequest)

// Tag wijzigen
val updateRequest = UpdateTagRequest(
    name = "Motor Olie",
    color = "#F44336"
)
val updatedTag = client.tags.update("tag-id", updateRequest)

// Tag verwijderen
client.tags.delete("tag-id")
```

### Onderhoud beheren

```kotlin
// Onderhoudsrecords voor voertuig ophalen
val maintenanceRecords = client.maintenance.getByVehicle("vehicle-id")

// Specifiek onderhoudsrecord ophalen
val record = client.maintenance.getById("maintenance-id")

// Nieuw onderhoudsrecord toevoegen
val createRequest = CreateMaintenanceRecordRequest(
    happenedAt = LocalDate.parse("2024-01-15"),
    odoReading = 52000,
    title = "Olie verversen",
    notes = "Reguliere oliewissel",
    items = listOf(
        CreateMaintenanceItemRequest(
            partId = "part-id",
            quantity = 1.0,
            unit = "fles",
            notes = "Motorolie vervangen"
        )
    )
)
val newRecord = client.maintenance.create("vehicle-id", createRequest)

// Onderhoudsrecord wijzigen  
val updateRequest = UpdateMaintenanceRecordRequest(
    odoReading = 52100
)
val updatedRecord = client.maintenance.update("maintenance-id", updateRequest)

// Onderhoudsrecord verwijderen
client.maintenance.delete("maintenance-id")
```

### Foutafhandeling

```kotlin
try {
    val vehicles = client.vehicles.getAll()
} catch (e: RevixAuthenticationException) {
    println("Authenticatie fout: ${e.message}")
    client.auth.logout()
} catch (e: RevixNotFoundException) {
    println("Niet gevonden: ${e.message}")
} catch (e: RevixValidationException) {
    println("Validatie fout: ${e.message}")
    e.details?.forEach { (field, error) ->
        println("  $field: $error")
    }
} catch (e: RevixApiException) {
    println("API fout [${e.statusCode}]: ${e.message}")
} catch (e: Exception) {
    println("Onverwachte fout: ${e.message}")
}
```

### Resources opruimen

```kotlin
// Vergeet niet om de client te sluiten wanneer je klaar bent
client.close()
```

## Volledige voorbeeld

```kotlin
import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = RevixClient(RevixClientConfig("http://localhost:8080"))
    
    try {
        // Inloggen
        client.auth.login("user@example.com", "password")
        
        // Voertuig toevoegen
        val vehicle = client.vehicles.create(CreateVehicleRequest(
            manufacturer = "Toyota",
            model = "Prius",
            buildYear = 2023
        ))
        
        // Tag en onderdeel toevoegen
        val tag = client.tags.create(CreateTagRequest("Filters"))
        val part = client.parts.create(CreatePartRequest(
            name = "Luchtfilter",
            tagIds = listOf(tag.id)
        ))
        
        // Onderhoudsrecord toevoegen
        val maintenance = client.maintenance.create(vehicle.id, 
            CreateMaintenanceRecordRequest(
                happenedAt = LocalDate.now(),
                title = "Luchtfilter vervangen",
                items = listOf(CreateMaintenanceItemRequest(
                    partId = part.id,
                    quantity = 1.0
                ))
            )
        )
        
        println("Succesvol onderhoud toegevoegd: ${maintenance.title}")
        
    } catch (e: Exception) {
        println("Fout: ${e.message}")
    } finally {
        client.close()
    }
}
```

## Functies

- ✅ **Eenvoudig te gebruiken**: Intuïtieve API met duidelijke methoden
- ✅ **Automatische authenticatie**: Token refresh wordt automatisch afgehandeld  
- ✅ **Type-safe**: Gebruik van Kotlin data classes voor alle API modellen
- ✅ **Uitgebreide foutafhandeling**: Specifieke exceptions voor verschillende fouttypes
- ✅ **Async/await**: Coroutines ondersteuning voor non-blocking calls
- ✅ **Configureerbaar**: Aanpasbare HTTP client en logging
- ✅ **Complete API dekking**: Alle Revix API endpoints worden ondersteund

## API Endpoints

De client ondersteunt alle beschikbare Revix API endpoints:

### Authenticatie
- `POST /api/v1/auth/register` - Registreer nieuwe gebruiker
- `POST /api/v1/auth/login` - Inloggen  
- `POST /api/v1/auth/refresh` - Token vernieuwen
- `GET /api/v1/auth/me` - Huidige gebruiker ophalen

### Voertuigen  
- `GET /api/v1/vehicles` - Voertuigen lijst
- `POST /api/v1/vehicles` - Voertuig toevoegen
- `GET /api/v1/vehicles/{id}` - Voertuig ophalen
- `PUT /api/v1/vehicles/{id}` - Voertuig wijzigen
- `DELETE /api/v1/vehicles/{id}` - Voertuig verwijderen

### Onderdelen
- `GET /api/v1/parts` - Onderdelen lijst (met zoeken/filtering)
- `POST /api/v1/parts` - Onderdeel toevoegen  
- `GET /api/v1/parts/{id}` - Onderdeel ophalen
- `PUT /api/v1/parts/{id}` - Onderdeel wijzigen
- `DELETE /api/v1/parts/{id}` - Onderdeel verwijderen

### Tags
- `GET /api/v1/tags` - Tags lijst
- `POST /api/v1/tags` - Tag toevoegen
- `PUT /api/v1/tags/{id}` - Tag wijzigen  
- `DELETE /api/v1/tags/{id}` - Tag verwijderen

### Onderhoud
- `GET /api/v1/vehicles/{vehicleId}/maintenance` - Onderhoudsrecords voor voertuig
- `POST /api/v1/vehicles/{vehicleId}/maintenance` - Onderhoudsrecord toevoegen
- `GET /api/v1/maintenance/{id}` - Onderhoudsrecord ophalen
- `PUT /api/v1/maintenance/{id}` - Onderhoudsrecord wijzigen
- `DELETE /api/v1/maintenance/{id}` - Onderhoudsrecord verwijderen