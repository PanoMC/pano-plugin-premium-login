package com.panomc.plugins.premiumlogin.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MicrosoftLoginErrorsTest {
    @Test
    fun `declared errors answer 4xx`() {
        assertEquals(409, MicrosoftLoginNotEnabled().getStatusCode())
        assertEquals("MICROSOFT_LOGIN_NOT_ENABLED", MicrosoftLoginNotEnabled().code)
        assertEquals(400, MicrosoftLoginFailed().getStatusCode())
        assertEquals(409, MinecraftAccountAlreadyLinked().getStatusCode())
        assertEquals(404, MinecraftAccountNotFound().getStatusCode())
        assertEquals(409, MinecraftAccountNotLinked().getStatusCode())
        assertEquals(403, InvalidPassword().getStatusCode())
        assertEquals(403, PremiumStatusSetByStaff().getStatusCode())
    }

    @Test
    fun `explicit status still wins`() {
        assertEquals(400, MicrosoftLoginNotEnabled("x", 400).getStatusCode())
    }
}
