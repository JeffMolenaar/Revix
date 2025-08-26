package com.jeffmolenaar.revix.server

import com.jeffmolenaar.revix.server.config.ConfigLoader
import com.jeffmolenaar.revix.validation.ValidationRules
import kotlin.test.*

class ApplicationTest {
    
    @Test
    fun testConfigLoader() {
        val config = ConfigLoader.load()
        assertNotNull(config)
        assertNotNull(config.database)
        assertNotNull(config.jwt)
        assertNotNull(config.server)
    }
    
    @Test
    fun testValidationRules() {
        // Test email validation
        assertTrue(ValidationRules.validateEmail("test@example.com").isValid)
        assertFalse(ValidationRules.validateEmail("invalid-email").isValid)
        
        // Test password validation
        assertTrue(ValidationRules.validatePassword("SecurePass123").isValid)
        assertFalse(ValidationRules.validatePassword("weak").isValid)
        
        // Test price validation
        assertTrue(ValidationRules.validatePriceCents(1000).isValid)
        assertFalse(ValidationRules.validatePriceCents(-100).isValid)
        
        // Test currency validation
        assertTrue(ValidationRules.validateCurrency("USD").isValid)
        assertFalse(ValidationRules.validateCurrency("us").isValid)
    }
}