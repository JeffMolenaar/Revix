package com.jeffmolenaar.revix.server.routes

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.repository.VehicleRepository
import com.jeffmolenaar.revix.validation.ValidationRules
import com.jeffmolenaar.revix.validation.combine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.vehicleRoutes() {
    val vehicleRepository by inject<VehicleRepository>()
    
    route("/vehicles") {
        get {
            val userId = call.getUserId()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val query = call.request.queryParameters["query"]
            
            val vehicles = vehicleRepository.findByOwner(userId, page, pageSize)
            val totalCount = vehicleRepository.countByOwner(userId)
            val totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()
            
            call.respond(PaginatedResponse(
                data = vehicles,
                page = page,
                pageSize = pageSize,
                totalCount = totalCount,
                totalPages = totalPages
            ))
        }
        
        post {
            val userId = call.getUserId()
            val request = call.receive<CreateVehicleRequest>()
            
            // Validate input
            val validationResults = listOf(
                ValidationRules.validateLicensePlate(request.licensePlate),
                ValidationRules.validateVin(request.vin),
                ValidationRules.validateBuildYear(request.buildYear),
                ValidationRules.validateOdoReading(request.currentOdo)
            )
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@post
            }
            
            // Validate required fields
            if (request.manufacturer.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = "Manufacturer is required"
                ))
                return@post
            }
            
            if (request.model.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = "Model is required"
                ))
                return@post
            }
            
            val vehicle = vehicleRepository.create(userId, request)
            call.respond(HttpStatusCode.Created, vehicle)
        }
        
        get("/{id}") {
            val userId = call.getUserId()
            val vehicleId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Vehicle ID is required"
                ))
                return@get
            }
            
            val vehicle = vehicleRepository.findById(vehicleId, userId)
            if (vehicle == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "vehicle_not_found",
                    message = "Vehicle not found"
                ))
                return@get
            }
            
            call.respond(vehicle)
        }
        
        put("/{id}") {
            val userId = call.getUserId()
            val vehicleId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Vehicle ID is required"
                ))
                return@put
            }
            
            val request = call.receive<UpdateVehicleRequest>()
            
            // Validate input
            val validationResults = listOf(
                ValidationRules.validateLicensePlate(request.licensePlate),
                ValidationRules.validateVin(request.vin),
                ValidationRules.validateBuildYear(request.buildYear),
                ValidationRules.validateOdoReading(request.currentOdo)
            )
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@put
            }
            
            val vehicle = vehicleRepository.update(vehicleId, userId, request)
            if (vehicle == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "vehicle_not_found",
                    message = "Vehicle not found"
                ))
                return@put
            }
            
            call.respond(vehicle)
        }
        
        delete("/{id}") {
            val userId = call.getUserId()
            val vehicleId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Vehicle ID is required"
                ))
                return@delete
            }
            
            val deleted = vehicleRepository.delete(vehicleId, userId)
            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "vehicle_not_found",
                    message = "Vehicle not found"
                ))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}