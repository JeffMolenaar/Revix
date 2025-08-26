package com.jeffmolenaar.revix.server.routes

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.repository.MaintenanceRepository
import com.jeffmolenaar.revix.server.repository.PartRepository
import com.jeffmolenaar.revix.server.repository.VehicleRepository
import com.jeffmolenaar.revix.validation.ValidationRules
import com.jeffmolenaar.revix.validation.combine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import org.koin.ktor.ext.inject

fun Route.maintenanceRoutes() {
    val maintenanceRepository by inject<MaintenanceRepository>()
    val vehicleRepository by inject<VehicleRepository>()
    val partRepository by inject<PartRepository>()
    
    route("/vehicles/{vehicleId}/maintenance") {
        get {
            val userId = call.getUserId()
            val vehicleId = call.parameters["vehicleId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Vehicle ID is required"
                ))
                return@get
            }
            
            // Verify vehicle exists and belongs to user
            val vehicle = vehicleRepository.findById(vehicleId, userId)
            if (vehicle == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "vehicle_not_found",
                    message = "Vehicle not found"
                ))
                return@get
            }
            
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val from = call.request.queryParameters["from"]?.let { LocalDate.parse(it) }
            val to = call.request.queryParameters["to"]?.let { LocalDate.parse(it) }
            
            val records = maintenanceRepository.findByVehicle(vehicleId, userId, from, to, page, pageSize)
            val totalCount = maintenanceRepository.countByVehicle(vehicleId, userId)
            val totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()
            
            call.respond(PaginatedResponse(
                data = records,
                page = page,
                pageSize = pageSize,
                totalCount = totalCount,
                totalPages = totalPages
            ))
        }
        
        post {
            val userId = call.getUserId()
            val vehicleId = call.parameters["vehicleId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Vehicle ID is required"
                ))
                return@post
            }
            
            // Verify vehicle exists and belongs to user
            val vehicle = vehicleRepository.findById(vehicleId, userId)
            if (vehicle == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "vehicle_not_found",
                    message = "Vehicle not found"
                ))
                return@post
            }
            
            val request = call.receive<CreateMaintenanceRecordRequest>()
            
            // Validate input
            val validationResults = mutableListOf(
                ValidationRules.validateMaintenanceTitle(request.title),
                ValidationRules.validateOdoReading(request.odoReading)
            )
            
            // Validate maintenance items
            request.items.forEach { item ->
                validationResults.add(ValidationRules.validateQuantity(item.quantity))
                item.unitPriceCentsOverride?.let { price ->
                    validationResults.add(ValidationRules.validatePriceCents(price))
                }
            }
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@post
            }
            
            // Validate that all part IDs exist and belong to the user
            val partIds = request.items.map { it.partId }.distinct()
            if (partIds.isNotEmpty()) {
                val parts = partIds.mapNotNull { partId ->
                    partRepository.findById(partId, userId)
                }
                if (parts.size != partIds.size) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(
                        error = "invalid_parts",
                        message = "One or more part IDs are invalid"
                    ))
                    return@post
                }
            }
            
            val record = maintenanceRepository.create(vehicleId, userId, request)
            call.respond(HttpStatusCode.Created, record)
        }
    }
    
    route("/maintenance/{id}") {
        get {
            val userId = call.getUserId()
            val recordId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Maintenance record ID is required"
                ))
                return@get
            }
            
            val record = maintenanceRepository.findById(recordId, userId)
            if (record == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "maintenance_not_found",
                    message = "Maintenance record not found"
                ))
                return@get
            }
            
            call.respond(record)
        }
        
        put {
            val userId = call.getUserId()
            val recordId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Maintenance record ID is required"
                ))
                return@put
            }
            
            val request = call.receive<UpdateMaintenanceRecordRequest>()
            
            // Validate input
            val validationResults = mutableListOf<com.jeffmolenaar.revix.validation.ValidationResult>()
            
            request.title?.let { title ->
                validationResults.add(ValidationRules.validateMaintenanceTitle(title))
            }
            
            request.odoReading?.let { odoReading ->
                validationResults.add(ValidationRules.validateOdoReading(odoReading))
            }
            
            // Validate maintenance items if provided
            request.items?.forEach { item ->
                validationResults.add(ValidationRules.validateQuantity(item.quantity))
                item.unitPriceCentsOverride?.let { price ->
                    validationResults.add(ValidationRules.validatePriceCents(price))
                }
            }
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@put
            }
            
            // Validate part IDs if items are provided
            request.items?.let { items ->
                val partIds = items.map { it.partId }.distinct()
                if (partIds.isNotEmpty()) {
                    val parts = partIds.mapNotNull { partId ->
                        partRepository.findById(partId, userId)
                    }
                    if (parts.size != partIds.size) {
                        call.respond(HttpStatusCode.BadRequest, ApiError(
                            error = "invalid_parts",
                            message = "One or more part IDs are invalid"
                        ))
                        return@put
                    }
                }
            }
            
            val record = maintenanceRepository.update(recordId, userId, request)
            if (record == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "maintenance_not_found",
                    message = "Maintenance record not found"
                ))
                return@put
            }
            
            call.respond(record)
        }
        
        delete {
            val userId = call.getUserId()
            val recordId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Maintenance record ID is required"
                ))
                return@delete
            }
            
            val deleted = maintenanceRepository.delete(recordId, userId)
            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "maintenance_not_found",
                    message = "Maintenance record not found"
                ))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}