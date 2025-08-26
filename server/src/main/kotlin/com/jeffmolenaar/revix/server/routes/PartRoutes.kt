package com.jeffmolenaar.revix.server.routes

import com.jeffmolenaar.revix.domain.*
import com.jeffmolenaar.revix.server.repository.PartRepository
import com.jeffmolenaar.revix.server.repository.TagRepository
import com.jeffmolenaar.revix.validation.ValidationRules
import com.jeffmolenaar.revix.validation.combine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.tagRoutes() {
    val tagRepository by inject<TagRepository>()
    
    route("/tags") {
        get {
            val userId = call.getUserId()
            val tags = tagRepository.findByOwner(userId)
            call.respond(tags)
        }
        
        post {
            val userId = call.getUserId()
            val request = call.receive<CreateTagRequest>()
            
            // Validate input
            val validationResult = ValidationRules.validateTagName(request.name)
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@post
            }
            
            // Check if tag name already exists for this user
            if (tagRepository.existsByName(request.name, userId)) {
                call.respond(HttpStatusCode.Conflict, ApiError(
                    error = "tag_exists",
                    message = "A tag with this name already exists"
                ))
                return@post
            }
            
            val tag = tagRepository.create(userId, request)
            call.respond(HttpStatusCode.Created, tag)
        }
        
        put("/{id}") {
            val userId = call.getUserId()
            val tagId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Tag ID is required"
                ))
                return@put
            }
            
            val request = call.receive<UpdateTagRequest>()
            
            // Validate input if name is provided
            request.name?.let { name ->
                val validationResult = ValidationRules.validateTagName(name)
                if (!validationResult.isValid) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(
                        error = "validation_error",
                        message = validationResult.errorMessage!!
                    ))
                    return@put
                }
                
                // Check if new name conflicts with existing tag
                if (tagRepository.existsByName(name, userId)) {
                    val existingTag = tagRepository.findById(tagId, userId)
                    if (existingTag?.name != name) {
                        call.respond(HttpStatusCode.Conflict, ApiError(
                            error = "tag_exists",
                            message = "A tag with this name already exists"
                        ))
                        return@put
                    }
                }
            }
            
            val tag = tagRepository.update(tagId, userId, request)
            if (tag == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "tag_not_found",
                    message = "Tag not found"
                ))
                return@put
            }
            
            call.respond(tag)
        }
        
        delete("/{id}") {
            val userId = call.getUserId()
            val tagId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Tag ID is required"
                ))
                return@delete
            }
            
            val deleted = tagRepository.delete(tagId, userId)
            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "tag_not_found",
                    message = "Tag not found"
                ))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

fun Route.partRoutes() {
    val partRepository by inject<PartRepository>()
    val tagRepository by inject<TagRepository>()
    
    route("/parts") {
        get {
            val userId = call.getUserId()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val query = call.request.queryParameters["query"]
            val tagIds = call.request.queryParameters["tags"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            
            val parts = partRepository.findByOwner(userId, tagIds, query, page, pageSize)
            val totalCount = partRepository.countByOwner(userId)
            val totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()
            
            call.respond(PaginatedResponse(
                data = parts,
                page = page,
                pageSize = pageSize,
                totalCount = totalCount,
                totalPages = totalPages
            ))
        }
        
        post {
            val userId = call.getUserId()
            val request = call.receive<CreatePartRequest>()
            
            // Validate input
            val validationResults = listOf(
                ValidationRules.validatePartName(request.name),
                ValidationRules.validatePriceCents(request.priceCents),
                ValidationRules.validateCurrency(request.currency)
            )
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@post
            }
            
            // Validate that all tag IDs exist and belong to the user
            if (request.tagIds.isNotEmpty()) {
                val tags = tagRepository.findByIds(request.tagIds, userId)
                if (tags.size != request.tagIds.size) {
                    call.respond(HttpStatusCode.BadRequest, ApiError(
                        error = "invalid_tags",
                        message = "One or more tag IDs are invalid"
                    ))
                    return@post
                }
            }
            
            val part = partRepository.create(userId, request)
            call.respond(HttpStatusCode.Created, part)
        }
        
        get("/{id}") {
            val userId = call.getUserId()
            val partId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Part ID is required"
                ))
                return@get
            }
            
            val part = partRepository.findById(partId, userId)
            if (part == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "part_not_found",
                    message = "Part not found"
                ))
                return@get
            }
            
            call.respond(part)
        }
        
        put("/{id}") {
            val userId = call.getUserId()
            val partId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Part ID is required"
                ))
                return@put
            }
            
            val request = call.receive<UpdatePartRequest>()
            
            // Validate input
            val validationResults = listOfNotNull(
                request.name?.let { ValidationRules.validatePartName(it) },
                request.priceCents?.let { ValidationRules.validatePriceCents(it) },
                request.currency?.let { ValidationRules.validateCurrency(it) }
            )
            
            val validationResult = validationResults.combine()
            if (!validationResult.isValid) {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "validation_error",
                    message = validationResult.errorMessage!!
                ))
                return@put
            }
            
            // Validate tag IDs if provided
            request.tagIds?.let { tagIds ->
                if (tagIds.isNotEmpty()) {
                    val tags = tagRepository.findByIds(tagIds, userId)
                    if (tags.size != tagIds.size) {
                        call.respond(HttpStatusCode.BadRequest, ApiError(
                            error = "invalid_tags",
                            message = "One or more tag IDs are invalid"
                        ))
                        return@put
                    }
                }
            }
            
            val part = partRepository.update(partId, userId, request)
            if (part == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "part_not_found",
                    message = "Part not found"
                ))
                return@put
            }
            
            call.respond(part)
        }
        
        delete("/{id}") {
            val userId = call.getUserId()
            val partId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiError(
                    error = "missing_parameter",
                    message = "Part ID is required"
                ))
                return@delete
            }
            
            val deleted = partRepository.delete(partId, userId)
            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, ApiError(
                    error = "part_not_found",
                    message = "Part not found"
                ))
                return@delete
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}