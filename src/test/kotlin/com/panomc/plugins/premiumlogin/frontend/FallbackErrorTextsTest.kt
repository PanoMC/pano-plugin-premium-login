package com.panomc.plugins.premiumlogin.frontend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The error texts of the built-in pages are the `fallback.errors` group of the plugin's own locale files. */
class FallbackErrorTextsTest {
    private val loader = javaClass.classLoader

    private val declared = setOf(
        "INVALID_TOKEN", "REGISTER_USERNAME_TOO_SHORT", "REGISTER_USERNAME_TOO_LONG", "REGISTER_INVALID_USERNAME",
        "REGISTER_USERNAME_NOT_AVAILABLE", "MICROSOFT_LOGIN_NOT_ENABLED", "MICROSOFT_LOGIN_FAILED",
        "MINECRAFT_ACCOUNT_ALREADY_LINKED", "BAD_REQUEST"
    )

    @Test
    fun `every locale carries every declared code`() {
        listOf("en-US", "tr", "ru").forEach { locale ->
            val errors = FallbackTexts.of(loader, "errors", locale)

            assertEquals(declared, errors.keys, locale)
            assertTrue(errors.values.all { (it as String).isNotBlank() }, locale)
        }
    }

    @Test
    fun `a language speaks its own words and a missing one falls back to English`() {
        assertFalse(FallbackTexts.of(loader, "errors", "tr")["REGISTER_USERNAME_NOT_AVAILABLE"] == FallbackTexts.of(loader, "errors", "en-US")["REGISTER_USERNAME_NOT_AVAILABLE"])
        assertEquals(FallbackTexts.of(loader, "errors", "en-US"), FallbackTexts.of(loader, "errors", "de"))
    }
}
