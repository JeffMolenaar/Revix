package com.jeffmolenaar.revix.server.repository

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.database.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class MaintenanceRepository {
    
    fun findByVehicle(
        vehicleId: String,
        ownerId: String,
        from: LocalDate? = null,
        to: LocalDate? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<MaintenanceRecord> = transaction {
        var query = MaintenanceRecordsTable
            .select { 
                (MaintenanceRecordsTable.vehicleId eq UUID.fromString(vehicleId)) and
                (MaintenanceRecordsTable.ownerId eq UUID.fromString(ownerId))
            }
        
        from?.let { fromDate ->
            query = query.andWhere { MaintenanceRecordsTable.happenedAt greaterEq fromDate }
        }
        
        to?.let { toDate ->
            query = query.andWhere { MaintenanceRecordsTable.happenedAt lessEq toDate }
        }
        
        val records = query
            .orderBy(MaintenanceRecordsTable.happenedAt, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { it.toMaintenanceRecord() }
        
        // Load maintenance items for each record
        records.map { record ->
            record.copy(items = findMaintenanceItems(record.id))
        }
    }
    
    fun findById(id: String, ownerId: String): MaintenanceRecord? = transaction {
        val record = MaintenanceRecordsTable
            .select { 
                (MaintenanceRecordsTable.id eq UUID.fromString(id)) and
                (MaintenanceRecordsTable.ownerId eq UUID.fromString(ownerId))
            }
            .map { it.toMaintenanceRecord() }
            .singleOrNull()
        
        record?.copy(items = findMaintenanceItems(id))
    }
    
    fun create(vehicleId: String, ownerId: String, request: CreateMaintenanceRecordRequest): MaintenanceRecord = transaction {
        val recordId = MaintenanceRecordsTable.insertAndGetId {
            it[MaintenanceRecordsTable.vehicleId] = UUID.fromString(vehicleId)
            it[MaintenanceRecordsTable.ownerId] = UUID.fromString(ownerId)
            it[MaintenanceRecordsTable.happenedAt] = request.happenedAt
            it[MaintenanceRecordsTable.odoReading] = request.odoReading
            it[MaintenanceRecordsTable.title] = request.title
            it[MaintenanceRecordsTable.notes] = request.notes
            it[MaintenanceRecordsTable.createdAt] = Clock.System.now()
            it[MaintenanceRecordsTable.updatedAt] = Clock.System.now()
        }
        
        // Create maintenance items
        val items = request.items.map { itemRequest ->
            createMaintenanceItem(recordId.toString(), itemRequest)
        }
        
        MaintenanceRecord(
            id = recordId.toString(),
            vehicleId = vehicleId,
            ownerId = ownerId,
            happenedAt = request.happenedAt,
            odoReading = request.odoReading,
            title = request.title,
            notes = request.notes,
            items = items,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    fun update(id: String, ownerId: String, request: UpdateMaintenanceRecordRequest): MaintenanceRecord? = transaction {
        val updated = MaintenanceRecordsTable.update({ 
            (MaintenanceRecordsTable.id eq UUID.fromString(id)) and 
            (MaintenanceRecordsTable.ownerId eq UUID.fromString(ownerId)) 
        }) {
            request.happenedAt?.let { happenedAt -> it[MaintenanceRecordsTable.happenedAt] = happenedAt }
            request.odoReading?.let { odoReading -> it[MaintenanceRecordsTable.odoReading] = odoReading }
            request.title?.let { title -> it[MaintenanceRecordsTable.title] = title }
            request.notes?.let { notes -> it[MaintenanceRecordsTable.notes] = notes }
            it[MaintenanceRecordsTable.updatedAt] = Clock.System.now()
        }
        
        if (updated > 0) {
            // Update items if provided
            request.items?.let { itemRequests ->
                // Delete existing items
                MaintenanceItemsTable.deleteWhere { MaintenanceItemsTable.maintenanceId eq UUID.fromString(id) }
                
                // Create new items
                itemRequests.forEach { itemRequest ->
                    createMaintenanceItem(id, itemRequest)
                }
            }
            
            findById(id, ownerId)
        } else {
            null
        }
    }
    
    fun delete(id: String, ownerId: String): Boolean = transaction {
        MaintenanceRecordsTable.deleteWhere { 
            (MaintenanceRecordsTable.id eq UUID.fromString(id)) and 
            (MaintenanceRecordsTable.ownerId eq UUID.fromString(ownerId)) 
        } > 0
    }
    
    fun countByVehicle(vehicleId: String, ownerId: String): Long = transaction {
        MaintenanceRecordsTable.select { 
            (MaintenanceRecordsTable.vehicleId eq UUID.fromString(vehicleId)) and
            (MaintenanceRecordsTable.ownerId eq UUID.fromString(ownerId))
        }.count()
    }
    
    private fun findMaintenanceItems(maintenanceId: String): List<MaintenanceItem> = transaction {
        MaintenanceItemsTable
            .join(PartsTable, JoinType.LEFT, MaintenanceItemsTable.partId, PartsTable.id)
            .leftJoin(PartTagsTable)
            .leftJoin(TagsTable)
            .select { MaintenanceItemsTable.maintenanceId eq UUID.fromString(maintenanceId) }
            .groupBy { it[MaintenanceItemsTable.id] }
            .map { (_, rows) ->
                val firstRow = rows.first()
                val tags = rows.mapNotNull { row ->
                    if (row.hasValue(TagsTable.id) && row[TagsTable.id] != null) {
                        Tag(
                            id = row[TagsTable.id].toString(),
                            ownerId = row[TagsTable.ownerId].toString(),
                            name = row[TagsTable.name],
                            color = row[TagsTable.color],
                            slug = row[TagsTable.slug],
                            createdAt = row[TagsTable.createdAt]
                        )
                    } else null
                }.distinctBy { it.id }
                
                val part = if (firstRow.hasValue(PartsTable.id) && firstRow[PartsTable.id] != null) {
                    Part(
                        id = firstRow[PartsTable.id].toString(),
                        ownerId = firstRow[PartsTable.ownerId].toString(),
                        name = firstRow[PartsTable.name],
                        description = firstRow[PartsTable.description],
                        priceCents = firstRow[PartsTable.priceCents],
                        currency = firstRow[PartsTable.currency],
                        url = firstRow[PartsTable.url],
                        tags = tags,
                        createdAt = firstRow[PartsTable.createdAt],
                        updatedAt = firstRow[PartsTable.updatedAt]
                    )
                } else null
                
                MaintenanceItem(
                    id = firstRow[MaintenanceItemsTable.id].toString(),
                    maintenanceId = firstRow[MaintenanceItemsTable.maintenanceId].toString(),
                    partId = firstRow[MaintenanceItemsTable.partId].toString(),
                    part = part,
                    quantity = firstRow[MaintenanceItemsTable.quantity].toDouble(),
                    unit = firstRow[MaintenanceItemsTable.unit],
                    unitPriceCentsOverride = firstRow[MaintenanceItemsTable.unitPriceCentsOverride],
                    notes = firstRow[MaintenanceItemsTable.notes]
                )
            }
    }
    
    private fun createMaintenanceItem(maintenanceId: String, request: CreateMaintenanceItemRequest): MaintenanceItem = transaction {
        val itemId = MaintenanceItemsTable.insertAndGetId {
            it[MaintenanceItemsTable.maintenanceId] = UUID.fromString(maintenanceId)
            it[MaintenanceItemsTable.partId] = UUID.fromString(request.partId)
            it[MaintenanceItemsTable.quantity] = request.quantity.toBigDecimal()
            it[MaintenanceItemsTable.unit] = request.unit
            it[MaintenanceItemsTable.unitPriceCentsOverride] = request.unitPriceCentsOverride
            it[MaintenanceItemsTable.notes] = request.notes
        }
        
        MaintenanceItem(
            id = itemId.toString(),
            maintenanceId = maintenanceId,
            partId = request.partId,
            part = null, // Will be loaded separately if needed
            quantity = request.quantity,
            unit = request.unit,
            unitPriceCentsOverride = request.unitPriceCentsOverride,
            notes = request.notes
        )
    }
}

// Extension functions
private fun ResultRow.toMaintenanceRecord(): MaintenanceRecord = MaintenanceRecord(
    id = this[MaintenanceRecordsTable.id].toString(),
    vehicleId = this[MaintenanceRecordsTable.vehicleId].toString(),
    ownerId = this[MaintenanceRecordsTable.ownerId].toString(),
    happenedAt = this[MaintenanceRecordsTable.happenedAt],
    odoReading = this[MaintenanceRecordsTable.odoReading],
    title = this[MaintenanceRecordsTable.title],
    notes = this[MaintenanceRecordsTable.notes],
    items = emptyList(), // Will be loaded separately
    createdAt = this[MaintenanceRecordsTable.createdAt],
    updatedAt = this[MaintenanceRecordsTable.updatedAt]
)