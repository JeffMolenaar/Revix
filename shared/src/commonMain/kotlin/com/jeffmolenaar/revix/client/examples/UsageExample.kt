package com.jeffmolenaar.revix.client.examples

import com.jeffmolenaar.revix.client.RevixClient
import com.jeffmolenaar.revix.client.RevixClientConfig
import com.jeffmolenaar.revix.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

/**
 * Example usage of the RevixClient
 * 
 * This demonstrates how to use the client to interact with the Revix API
 */
fun main() = runBlocking {
    // Create client configuration
    val config = RevixClientConfig(
        baseUrl = "http://localhost:8080",
        enableLogging = true
    )
    
    // Create the client
    val client = RevixClient(config)
    
    try {
        // Login
        println("Logging in...")
        val authResponse = client.auth.login("user@example.com", "password")
        println("Logged in as: ${authResponse.user.email}")
        
        // Get current user
        val currentUser = client.auth.getCurrentUser()
        println("Current user: ${currentUser.name} (${currentUser.email})")
        
        // Create a vehicle
        println("\nCreating a vehicle...")
        val createVehicleRequest = CreateVehicleRequest(
            manufacturer = "Toyota",
            model = "Camry",
            buildYear = 2020,
            licensePlate = "ABC-123",
            vin = "1HGBH41JXMN109186",
            fuelType = "Gasoline",
            currentOdo = 50000
        )
        val vehicle = client.vehicles.create(createVehicleRequest)
        println("Created vehicle: ${vehicle.manufacturer} ${vehicle.model} (${vehicle.id})")
        
        // Get all vehicles
        println("\nGetting all vehicles...")
        val vehiclesResponse = client.vehicles.getAll()
        println("Found ${vehiclesResponse.totalCount} vehicles")
        
        // Create a tag
        println("\nCreating a tag...")
        val createTagRequest = CreateTagRequest(
            name = "Engine Oil",
            color = "#FF5722"
        )
        val tag = client.tags.create(createTagRequest)
        println("Created tag: ${tag.name} (${tag.id})")
        
        // Create a part
        println("\nCreating a part...")
        val createPartRequest = CreatePartRequest(
            name = "Mobil 1 5W-30 Engine Oil",
            description = "Full synthetic motor oil",
            priceCents = 4500,
            currency = "USD",
            url = "https://example.com/oil",
            tagIds = listOf(tag.id)
        )
        val part = client.parts.create(createPartRequest)
        println("Created part: ${part.name} (${part.id})")
        
        // Create a maintenance record
        println("\nCreating a maintenance record...")
        val createMaintenanceRequest = CreateMaintenanceRecordRequest(
            happenedAt = LocalDate.parse("2024-01-15"),
            odoReading = 52000,
            title = "Oil Change",
            notes = "Regular oil change service",
            items = listOf(
                CreateMaintenanceItemRequest(
                    partId = part.id,
                    quantity = 1.0,
                    unit = "bottle",
                    notes = "Changed engine oil"
                )
            )
        )
        val maintenanceRecord = client.maintenance.create(vehicle.id, createMaintenanceRequest)
        println("Created maintenance record: ${maintenanceRecord.title} (${maintenanceRecord.id})")
        
        // Get maintenance records for the vehicle
        println("\nGetting maintenance records...")
        val maintenanceResponse = client.maintenance.getByVehicle(vehicle.id)
        println("Found ${maintenanceResponse.totalCount} maintenance records")
        
        // Update the vehicle
        println("\nUpdating vehicle...")
        val updateVehicleRequest = UpdateVehicleRequest(
            currentOdo = 53000
        )
        val updatedVehicle = client.vehicles.update(vehicle.id, updateVehicleRequest)
        println("Updated vehicle odometer to: ${updatedVehicle.currentOdo}")
        
        // Search parts
        println("\nSearching parts...")
        val partsResponse = client.parts.getAll(query = "oil", tagIds = listOf(tag.id))
        println("Found ${partsResponse.totalCount} parts matching 'oil' with tag '${tag.name}'")
        
        println("\nExample completed successfully!")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        // Clean up
        client.close()
    }
}

/**
 * Example of error handling and token refresh
 */
fun exampleErrorHandling() = runBlocking {
    val client = RevixClient(RevixClientConfig("http://localhost:8080"))
    
    try {
        // Login
        client.auth.login("user@example.com", "password")
        
        // The client automatically handles token refresh
        // If a request returns 401 Unauthorized, it will try to refresh the token
        
        // Example of checking authentication status
        if (client.isAuthenticated()) {
            println("Client is authenticated")
            
            // Make API calls...
            val vehicles = client.vehicles.getAll()
            println("Retrieved ${vehicles.totalCount} vehicles")
        } else {
            println("Client is not authenticated")
        }
        
    } catch (e: Exception) {
        println("API Error: ${e.message}")
        
        // Handle different types of errors
        when {
            e.message?.contains("401") == true -> {
                println("Authentication error - please login again")
                client.auth.logout()
            }
            e.message?.contains("404") == true -> {
                println("Resource not found")
            }
            e.message?.contains("400") == true -> {
                println("Bad request - check input data")
            }
            else -> {
                println("Unexpected error occurred")
            }
        }
    } finally {
        client.close()
    }
}