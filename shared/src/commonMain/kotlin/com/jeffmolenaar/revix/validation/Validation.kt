package com.jeffmolenaar.revix.validation

import com.jeffmolenaar.revix.domain.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

object ValidationRules {
    private val emailRegex = Regex(
        "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$"
    )
    
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Error("Email is required")
            !emailRegex.matches(email) -> ValidationResult.Error("Invalid email format")
            else -> ValidationResult.Success
        }
    }
    
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.length < 8 -> ValidationResult.Error("Password must be at least 8 characters")
            password.length > 128 -> ValidationResult.Error("Password must be less than 128 characters")
            !password.any { it.isUpperCase() } -> ValidationResult.Error("Password must contain at least one uppercase letter")
            !password.any { it.isLowerCase() } -> ValidationResult.Error("Password must contain at least one lowercase letter")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one digit")
            else -> ValidationResult.Success
        }
    }
    
    fun validateLicensePlate(licensePlate: String?): ValidationResult {
        if (licensePlate == null) return ValidationResult.Success
        return when {
            licensePlate.length > 16 -> ValidationResult.Error("License plate must be 16 characters or less")
            else -> ValidationResult.Success
        }
    }
    
    fun validateVin(vin: String?): ValidationResult {
        if (vin == null) return ValidationResult.Success
        return when {
            vin.length != 17 -> ValidationResult.Error("VIN must be exactly 17 characters")
            else -> ValidationResult.Success
        }
    }
    
    fun validateBuildYear(year: Int?): ValidationResult {
        if (year == null) return ValidationResult.Success
        val currentYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year
        return when {
            year < 1950 -> ValidationResult.Error("Build year must be 1950 or later")
            year > currentYear + 1 -> ValidationResult.Error("Build year cannot be more than one year in the future")
            else -> ValidationResult.Success
        }
    }
    
    fun validateCurrency(currency: String): ValidationResult {
        return when {
            currency.length != 3 -> ValidationResult.Error("Currency must be a 3-letter ISO code")
            !currency.all { it.isUpperCase() } -> ValidationResult.Error("Currency must be uppercase")
            else -> ValidationResult.Success
        }
    }
    
    fun validatePriceCents(priceCents: Long): ValidationResult {
        return when {
            priceCents < 0 -> ValidationResult.Error("Price cannot be negative")
            else -> ValidationResult.Success
        }
    }
    
    fun validateOdoReading(reading: Long?): ValidationResult {
        if (reading == null) return ValidationResult.Success
        return when {
            reading < 0 -> ValidationResult.Error("Odometer reading cannot be negative")
            else -> ValidationResult.Success
        }
    }
    
    fun validateTagName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Tag name is required")
            name.length > 50 -> ValidationResult.Error("Tag name must be 50 characters or less")
            else -> ValidationResult.Success
        }
    }
    
    fun validatePartName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Part name is required")
            name.length > 200 -> ValidationResult.Error("Part name must be 200 characters or less")
            else -> ValidationResult.Success
        }
    }
    
    fun validateMaintenanceTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Error("Maintenance title is required")
            title.length > 200 -> ValidationResult.Error("Maintenance title must be 200 characters or less")
            else -> ValidationResult.Success
        }
    }
    
    fun validateQuantity(quantity: Double): ValidationResult {
        return when {
            quantity <= 0 -> ValidationResult.Error("Quantity must be greater than 0")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isValid: Boolean get() = this is Success
    val errorMessage: String? get() = (this as? Error)?.message
}

fun List<ValidationResult>.combine(): ValidationResult {
    val errors = this.mapNotNull { it.errorMessage }
    return if (errors.isEmpty()) {
        ValidationResult.Success
    } else {
        ValidationResult.Error(errors.joinToString("; "))
    }
}

object TagUtils {
    fun createSlug(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}