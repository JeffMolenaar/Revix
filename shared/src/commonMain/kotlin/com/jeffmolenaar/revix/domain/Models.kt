package com.jeffmolenaar.revix.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val createdAt: Instant
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
enum class OdoUnit {
    KM, HOURS
}

@Serializable
data class Vehicle(
    val id: String,
    val ownerId: String,
    val licensePlate: String? = null,
    val vin: String? = null,
    val manufacturer: String,
    val model: String,
    val buildYear: Int? = null,
    val fuelType: String? = null,
    val odoUnit: OdoUnit = OdoUnit.KM,
    val currentOdo: Long? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CreateVehicleRequest(
    val licensePlate: String? = null,
    val vin: String? = null,
    val manufacturer: String,
    val model: String,
    val buildYear: Int? = null,
    val fuelType: String? = null,
    val odoUnit: OdoUnit = OdoUnit.KM,
    val currentOdo: Long? = null
)

@Serializable
data class UpdateVehicleRequest(
    val licensePlate: String? = null,
    val vin: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val buildYear: Int? = null,
    val fuelType: String? = null,
    val odoUnit: OdoUnit? = null,
    val currentOdo: Long? = null
)

@Serializable
data class Tag(
    val id: String,
    val ownerId: String,
    val name: String,
    val color: String? = null,
    val slug: String,
    val createdAt: Instant
)

@Serializable
data class CreateTagRequest(
    val name: String,
    val color: String? = null
)

@Serializable
data class UpdateTagRequest(
    val name: String? = null,
    val color: String? = null
)

@Serializable
data class Part(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val priceCents: Long,
    val currency: String = "EUR",
    val url: String? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CreatePartRequest(
    val name: String,
    val description: String? = null,
    val priceCents: Long,
    val currency: String = "EUR",
    val url: String? = null,
    val tagIds: List<String> = emptyList()
)

@Serializable
data class UpdatePartRequest(
    val name: String? = null,
    val description: String? = null,
    val priceCents: Long? = null,
    val currency: String? = null,
    val url: String? = null,
    val tagIds: List<String>? = null
)

@Serializable
data class MaintenanceItem(
    val id: String,
    val maintenanceId: String,
    val partId: String,
    val part: Part? = null,
    val quantity: Double,
    val unit: String? = null,
    val unitPriceCentsOverride: Long? = null,
    val notes: String? = null
)

@Serializable
data class CreateMaintenanceItemRequest(
    val partId: String,
    val quantity: Double,
    val unit: String? = null,
    val unitPriceCentsOverride: Long? = null,
    val notes: String? = null
)

@Serializable
data class MaintenanceRecord(
    val id: String,
    val vehicleId: String,
    val ownerId: String,
    val happenedAt: LocalDate,
    val odoReading: Long? = null,
    val title: String,
    val notes: String? = null,
    val items: List<MaintenanceItem> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CreateMaintenanceRecordRequest(
    val happenedAt: LocalDate,
    val odoReading: Long? = null,
    val title: String,
    val notes: String? = null,
    val items: List<CreateMaintenanceItemRequest> = emptyList()
)

@Serializable
data class UpdateMaintenanceRecordRequest(
    val happenedAt: LocalDate? = null,
    val odoReading: Long? = null,
    val title: String? = null,
    val notes: String? = null,
    val items: List<CreateMaintenanceItemRequest>? = null
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Long,
    val totalPages: Int
)

@Serializable
data class ApiError(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)