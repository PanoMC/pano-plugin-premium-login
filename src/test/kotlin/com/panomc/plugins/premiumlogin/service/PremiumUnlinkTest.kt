package com.panomc.plugins.premiumlogin.service

import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PremiumUnlinkTest {
    @Test
    fun `an unlink clears the link and drops the PREMIUM type the link set`() {
        val linked = PlayerPremiumStatus(
            playerUuid = "uuid-notch", playerName = "Notch", accountType = AccountType.PREMIUM.name,
            source = StatusSource.MICROSOFT.name, premiumLinked = true
        )

        val after = PremiumUnlink.unlinkedStatus(linked, AccountType.AUTO, 5L)

        assertFalse(after.premiumLinked)
        assertEquals(AccountType.AUTO.name, after.accountType)
        assertEquals("Notch", after.playerName)
        assertEquals("uuid-notch", after.playerUuid)
        assertEquals(5L, after.lastUpdated)
    }

    @Test
    fun `a type set by staff stays after an unlink`() {
        val panel = PlayerPremiumStatus(
            playerUuid = "u", playerName = "bob", accountType = AccountType.CRACKED.name,
            source = StatusSource.PANEL.name, premiumLinked = true
        )

        val after = PremiumUnlink.unlinkedStatus(panel, AccountType.AUTO, 5L)

        assertFalse(after.premiumLinked)
        assertEquals(AccountType.CRACKED.name, after.accountType)
        assertEquals(StatusSource.PANEL.name, after.source)
    }
}
