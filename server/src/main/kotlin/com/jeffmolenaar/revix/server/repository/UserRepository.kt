package com.jeffmolenaar.revix.server.repository

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.database.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserRepository {
    
    fun findByEmail(email: String): User? = transaction {
        UsersTable.select { UsersTable.email eq email }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    fun findById(id: String): User? = transaction {
        UsersTable.select { UsersTable.id eq UUID.fromString(id) }
            .map { it.toUser() }
            .singleOrNull()
    }
    
    fun create(email: String, passwordHash: String, name: String?): User = transaction {
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.name] = name
            it[UsersTable.createdAt] = Clock.System.now()
        }
        
        User(
            id = id.toString(),
            email = email,
            name = name,
            createdAt = Clock.System.now()
        )
    }
    
    fun getPasswordHash(userId: String): String? = transaction {
        UsersTable.select { UsersTable.id eq UUID.fromString(userId) }
            .map { it[UsersTable.passwordHash] }
            .singleOrNull()
    }
}

class RefreshTokenRepository {
    
    fun save(userId: String, tokenHash: String, expiresAt: kotlinx.datetime.Instant): Unit = transaction {
        RefreshTokensTable.insert {
            it[RefreshTokensTable.userId] = UUID.fromString(userId)
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[RefreshTokensTable.expiresAt] = expiresAt
            it[RefreshTokensTable.createdAt] = Clock.System.now()
        }
    }
    
    fun findByTokenHash(tokenHash: String): Pair<String, kotlinx.datetime.Instant>? = transaction {
        RefreshTokensTable.select { RefreshTokensTable.tokenHash eq tokenHash }
            .map { it[RefreshTokensTable.userId].toString() to it[RefreshTokensTable.expiresAt] }
            .singleOrNull()
    }
    
    fun deleteByTokenHash(tokenHash: String): Unit = transaction {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.tokenHash eq tokenHash }
    }
    
    fun deleteExpiredTokens(): Unit = transaction {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.expiresAt less Clock.System.now() }
    }
    
    fun deleteAllForUser(userId: String): Unit = transaction {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq UUID.fromString(userId) }
    }
}

class VehicleRepository {
    
    fun findByOwner(ownerId: String, page: Int = 1, pageSize: Int = 20): List<Vehicle> = transaction {
        VehiclesTable.select { VehiclesTable.ownerId eq UUID.fromString(ownerId) }
            .orderBy(VehiclesTable.createdAt, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { it.toVehicle() }
    }
    
    fun findById(id: String, ownerId: String): Vehicle? = transaction {
        VehiclesTable.select { 
            (VehiclesTable.id eq UUID.fromString(id)) and 
            (VehiclesTable.ownerId eq UUID.fromString(ownerId)) 
        }
            .map { it.toVehicle() }
            .singleOrNull()
    }
    
    fun create(ownerId: String, request: CreateVehicleRequest): Vehicle = transaction {
        val id = VehiclesTable.insertAndGetId {
            it[VehiclesTable.ownerId] = UUID.fromString(ownerId)
            it[VehiclesTable.licensePlate] = request.licensePlate
            it[VehiclesTable.vin] = request.vin
            it[VehiclesTable.manufacturer] = request.manufacturer
            it[VehiclesTable.model] = request.model
            it[VehiclesTable.buildYear] = request.buildYear
            it[VehiclesTable.fuelType] = request.fuelType
            it[VehiclesTable.odoUnit] = request.odoUnit
            it[VehiclesTable.currentOdo] = request.currentOdo
            it[VehiclesTable.createdAt] = Clock.System.now()
            it[VehiclesTable.updatedAt] = Clock.System.now()
        }
        
        VehiclesTable.select { VehiclesTable.id eq id }
            .map { it.toVehicle() }
            .single()
    }
    
    fun update(id: String, ownerId: String, request: UpdateVehicleRequest): Vehicle? = transaction {
        val updated = VehiclesTable.update({ 
            (VehiclesTable.id eq UUID.fromString(id)) and 
            (VehiclesTable.ownerId eq UUID.fromString(ownerId)) 
        }) {
            request.licensePlate?.let { licensePlate -> it[VehiclesTable.licensePlate] = licensePlate }
            request.vin?.let { vin -> it[VehiclesTable.vin] = vin }
            request.manufacturer?.let { manufacturer -> it[VehiclesTable.manufacturer] = manufacturer }
            request.model?.let { model -> it[VehiclesTable.model] = model }
            request.buildYear?.let { buildYear -> it[VehiclesTable.buildYear] = buildYear }
            request.fuelType?.let { fuelType -> it[VehiclesTable.fuelType] = fuelType }
            request.odoUnit?.let { odoUnit -> it[VehiclesTable.odoUnit] = odoUnit }
            request.currentOdo?.let { currentOdo -> it[VehiclesTable.currentOdo] = currentOdo }
            it[VehiclesTable.updatedAt] = Clock.System.now()
        }
        
        if (updated > 0) {
            findById(id, ownerId)
        } else {
            null
        }
    }
    
    fun delete(id: String, ownerId: String): Boolean = transaction {
        VehiclesTable.deleteWhere { 
            (VehiclesTable.id eq UUID.fromString(id)) and 
            (VehiclesTable.ownerId eq UUID.fromString(ownerId)) 
        } > 0
    }
    
    fun countByOwner(ownerId: String): Long = transaction {
        VehiclesTable.select { VehiclesTable.ownerId eq UUID.fromString(ownerId) }.count()
    }
}

// Extension functions to convert ResultRow to domain objects
private fun ResultRow.toUser(): User = User(
    id = this[UsersTable.id].toString(),
    email = this[UsersTable.email],
    name = this[UsersTable.name],
    createdAt = this[UsersTable.createdAt]
)

private fun ResultRow.toVehicle(): Vehicle = Vehicle(
    id = this[VehiclesTable.id].toString(),
    ownerId = this[VehiclesTable.ownerId].toString(),
    licensePlate = this[VehiclesTable.licensePlate],
    vin = this[VehiclesTable.vin],
    manufacturer = this[VehiclesTable.manufacturer],
    model = this[VehiclesTable.model],
    buildYear = this[VehiclesTable.buildYear],
    fuelType = this[VehiclesTable.fuelType],
    odoUnit = this[VehiclesTable.odoUnit],
    currentOdo = this[VehiclesTable.currentOdo],
    createdAt = this[VehiclesTable.createdAt],
    updatedAt = this[VehiclesTable.updatedAt]
)