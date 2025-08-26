package com.jeffmolenaar.revix.server.repository

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.database.*
import com.jeffmolenaar.revix.validation.TagUtils
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TagRepository {
    
    fun findByOwner(ownerId: String): List<Tag> = transaction {
        TagsTable.select { TagsTable.ownerId eq UUID.fromString(ownerId) }
            .orderBy(TagsTable.name)
            .map { it.toTag() }
    }
    
    fun findById(id: String, ownerId: String): Tag? = transaction {
        TagsTable.select { 
            (TagsTable.id eq UUID.fromString(id)) and 
            (TagsTable.ownerId eq UUID.fromString(ownerId)) 
        }
            .map { it.toTag() }
            .singleOrNull()
    }
    
    fun findByIds(ids: List<String>, ownerId: String): List<Tag> = transaction {
        if (ids.isEmpty()) return@transaction emptyList()
        
        TagsTable.select { 
            (TagsTable.id inList ids.map { UUID.fromString(it) }) and 
            (TagsTable.ownerId eq UUID.fromString(ownerId)) 
        }
            .map { it.toTag() }
    }
    
    fun create(ownerId: String, request: CreateTagRequest): Tag = transaction {
        val slug = TagUtils.createSlug(request.name)
        
        val id = TagsTable.insertAndGetId {
            it[TagsTable.ownerId] = UUID.fromString(ownerId)
            it[name] = request.name
            it[color] = request.color
            it[TagsTable.slug] = slug
            it[createdAt] = Clock.System.now()
        }
        
        Tag(
            id = id.toString(),
            ownerId = ownerId,
            name = request.name,
            color = request.color,
            slug = slug,
            createdAt = Clock.System.now()
        )
    }
    
    fun update(id: String, ownerId: String, request: UpdateTagRequest): Tag? = transaction {
        val updates = mutableMapOf<Column<*>, Any>()
        
        request.name?.let { name ->
            updates[TagsTable.name] = name
            updates[TagsTable.slug] = TagUtils.createSlug(name)
        }
        request.color?.let { color -> updates[TagsTable.color] = color }
        
        if (updates.isNotEmpty()) {
            val updated = TagsTable.update({ 
                (TagsTable.id eq UUID.fromString(id)) and 
                (TagsTable.ownerId eq UUID.fromString(ownerId)) 
            }) {
                updates.forEach { (column, value) ->
                    it[column as Column<Any>] = value
                }
            }
            
            if (updated > 0) {
                findById(id, ownerId)
            } else null
        } else {
            findById(id, ownerId)
        }
    }
    
    fun delete(id: String, ownerId: String): Boolean = transaction {
        TagsTable.deleteWhere { 
            (TagsTable.id eq UUID.fromString(id)) and 
            (TagsTable.ownerId eq UUID.fromString(ownerId)) 
        } > 0
    }
    
    fun existsByName(name: String, ownerId: String): Boolean = transaction {
        TagsTable.select { 
            (TagsTable.name eq name) and 
            (TagsTable.ownerId eq UUID.fromString(ownerId)) 
        }.count() > 0
    }
}

class PartRepository {
    
    fun findByOwner(
        ownerId: String,
        tagIds: List<String> = emptyList(),
        query: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<Part> = transaction {
        var selectQuery = PartsTable.leftJoin(PartTagsTable).leftJoin(TagsTable)
            .select { PartsTable.ownerId eq UUID.fromString(ownerId) }
        
        // Filter by tags
        if (tagIds.isNotEmpty()) {
            selectQuery = selectQuery.andWhere { 
                TagsTable.id inList tagIds.map { UUID.fromString(it) }
            }
        }
        
        // Filter by query text
        query?.let { q ->
            selectQuery = selectQuery.andWhere {
                (PartsTable.name like "%$q%") or
                (PartsTable.description like "%$q%")
            }
        }
        
        val parts = selectQuery
            .orderBy(PartsTable.createdAt, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .groupBy { it[PartsTable.id] }
            .map { (_, rows) ->
                val firstRow = rows.first()
                val tags = rows.mapNotNull { row ->
                    if (row[TagsTable.id] != null) {
                        row.toTag()
                    } else null
                }.distinctBy { it.id }
                
                firstRow.toPart(tags)
            }
        
        parts
    }
    
    fun findById(id: String, ownerId: String): Part? = transaction {
        val rows = PartsTable.leftJoin(PartTagsTable).leftJoin(TagsTable)
            .select { 
                (PartsTable.id eq UUID.fromString(id)) and 
                (PartsTable.ownerId eq UUID.fromString(ownerId)) 
            }
            .toList()
        
        if (rows.isEmpty()) return@transaction null
        
        val tags = rows.mapNotNull { row ->
            if (row[TagsTable.id] != null) {
                row.toTag()
            } else null
        }.distinctBy { it.id }
        
        rows.first().toPart(tags)
    }
    
    fun create(ownerId: String, request: CreatePartRequest): Part = transaction {
        val partId = PartsTable.insertAndGetId {
            it[PartsTable.ownerId] = UUID.fromString(ownerId)
            it[name] = request.name
            it[description] = request.description
            it[priceCents] = request.priceCents
            it[currency] = request.currency
            it[url] = request.url
            it[createdAt] = Clock.System.now()
            it[updatedAt] = Clock.System.now()
        }
        
        // Add tag associations
        if (request.tagIds.isNotEmpty()) {
            PartTagsTable.batchInsert(request.tagIds) { tagId ->
                this[PartTagsTable.partId] = partId
                this[PartTagsTable.tagId] = UUID.fromString(tagId)
            }
        }
        
        findById(partId.toString(), ownerId)!!
    }
    
    fun update(id: String, ownerId: String, request: UpdatePartRequest): Part? = transaction {
        val updated = PartsTable.update({ 
            (PartsTable.id eq UUID.fromString(id)) and 
            (PartsTable.ownerId eq UUID.fromString(ownerId)) 
        }) {
            request.name?.let { name -> it[PartsTable.name] = name }
            request.description?.let { description -> it[PartsTable.description] = description }
            request.priceCents?.let { priceCents -> it[PartsTable.priceCents] = priceCents }
            request.currency?.let { currency -> it[PartsTable.currency] = currency }
            request.url?.let { url -> it[PartsTable.url] = url }
            it[updatedAt] = Clock.System.now()
        }
        
        if (updated > 0) {
            // Update tag associations if provided
            request.tagIds?.let { tagIds ->
                PartTagsTable.deleteWhere { PartTagsTable.partId eq UUID.fromString(id) }
                if (tagIds.isNotEmpty()) {
                    PartTagsTable.batchInsert(tagIds) { tagId ->
                        this[PartTagsTable.partId] = UUID.fromString(id)
                        this[PartTagsTable.tagId] = UUID.fromString(tagId)
                    }
                }
            }
            
            findById(id, ownerId)
        } else {
            null
        }
    }
    
    fun delete(id: String, ownerId: String): Boolean = transaction {
        PartsTable.deleteWhere { 
            (PartsTable.id eq UUID.fromString(id)) and 
            (PartsTable.ownerId eq UUID.fromString(ownerId)) 
        } > 0
    }
    
    fun countByOwner(ownerId: String): Long = transaction {
        PartsTable.select { PartsTable.ownerId eq UUID.fromString(ownerId) }.count()
    }
}

// Extension functions
private fun ResultRow.toTag(): Tag = Tag(
    id = this[TagsTable.id].toString(),
    ownerId = this[TagsTable.ownerId].toString(),
    name = this[TagsTable.name],
    color = this[TagsTable.color],
    slug = this[TagsTable.slug],
    createdAt = this[TagsTable.createdAt]
)

private fun ResultRow.toPart(tags: List<Tag> = emptyList()): Part = Part(
    id = this[PartsTable.id].toString(),
    ownerId = this[PartsTable.ownerId].toString(),
    name = this[PartsTable.name],
    description = this[PartsTable.description],
    priceCents = this[PartsTable.priceCents],
    currency = this[PartsTable.currency],
    url = this[PartsTable.url],
    tags = tags,
    createdAt = this[PartsTable.createdAt],
    updatedAt = this[PartsTable.updatedAt]
)