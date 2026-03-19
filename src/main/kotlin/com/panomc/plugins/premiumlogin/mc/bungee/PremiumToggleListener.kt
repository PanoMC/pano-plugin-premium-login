package com.panomc.plugins.premiumlogin.mc.bungee

import com.github.games647.fastlogin.bungee.event.BungeeFastLoginPremiumToggleEvent
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.logging.Logger

/**
 * Listens for FastLogin's PremiumToggleEvent on BungeeCord.
 * Fires when /premium or /cracked commands are used.
 */
class PremiumToggleListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) : Listener {

    @EventHandler
    fun onPremiumToggle(event: BungeeFastLoginPremiumToggleEvent) {
        val profile = event.profile
        val playerName = profile.name ?: return
        val playerUuid = profile.optId?.map { it.toString() }?.orElse("") ?: ""
        val isPremium = profile.isOnlinemodePreferred
        val accountType = if (isPremium) "PREMIUM" else "CRACKED"

        logger.info("[PremiumLogin] Premium toggle detected: '$playerName' → $accountType (reason: ${event.reason})")

        try {
            runBlocking {
                premiumStatusManager.forceUpdateStatus(
                    playerName = playerName,
                    playerUuid = playerUuid,
                    accountType = accountType,
                    source = "FASTLOGIN"
                )
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to push premium toggle to Pano for '$playerName': ${e.message}")
        }
    }
}
