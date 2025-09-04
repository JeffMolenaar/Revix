package com.jeffmolenaar.revix.server.database

import com.jeffmolenaar.revix.domain.OdoUnit
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255).nullable()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
}

object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = varchar("token_hash", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
}

object VehiclesTable : UUIDTable("vehicles") {
    val ownerId = reference("owner_id", UsersTable)
    val licensePlate = varchar("license_plate", 16).nullable()
    val vin = varchar("vin", 17).nullable()
    val manufacturer = varchar("manufacturer", 100)
    val model = varchar("model", 100)
    val buildYear = integer("build_year").nullable()
    val fuelType = varchar("fuel_type", 50).nullable()
    val odoUnit = enumerationByName("odo_unit", 10, OdoUnit::class)
    val currentOdo = long("current_odo").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object TagsTable : UUIDTable("tags") {
    val ownerId = reference("owner_id", UsersTable)
    val name = varchar("name", 50)
    val color = varchar("color", 7).nullable()
    val slug = varchar("slug", 60)
    val createdAt = timestamp("created_at")
    
    init {
        uniqueIndex("unique_tag_name_per_owner", ownerId, name)
        uniqueIndex("unique_tag_slug_per_owner", ownerId, slug)
    }
}

object PartsTable : UUIDTable("parts") {
    val ownerId = reference("owner_id", UsersTable)
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val priceCents = long("price_cents").nullable()
    val currency = varchar("currency", 3).nullable()
    val url = text("url").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object PartTagsTable : Table("part_tags") {
    val partId = reference("part_id", PartsTable)
    val tagId = reference("tag_id", TagsTable)
    
    override val primaryKey = PrimaryKey(partId, tagId)
}

object MaintenanceRecordsTable : UUIDTable("maintenance_records") {
    val vehicleId = reference("vehicle_id", VehiclesTable)
    val ownerId = reference("owner_id", UsersTable)
    val happenedAt = date("happened_at")
    val odoReading = long("odo_reading").nullable()
    val title = varchar("title", 200)
    val notes = text("notes").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object MaintenanceItemsTable : UUIDTable("maintenance_items") {
    val maintenanceId = reference("maintenance_id", MaintenanceRecordsTable)
    val partId = reference("part_id", PartsTable)
    val quantity = decimal("quantity", 10, 3)
    val unit = varchar("unit", 20).nullable()
    val unitPriceCentsOverride = long("unit_price_cents_override").nullable()
    val notes = text("notes").nullable()
}