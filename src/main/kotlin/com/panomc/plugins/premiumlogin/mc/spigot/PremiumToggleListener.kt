package com.panomc.plugins.premiumlogin.mc.spigot

import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPremiumToggleEvent
import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent.PremiumToggleReason
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.logging.Logger

/**
 * Listens for FastLogin's PremiumToggleEvent which fires when a player
 * uses /premium or /cracked commands (or an admin uses it on them).
 * Pushes the status change to Pano backend.
 */
class PremiumToggleListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPremiumToggle(event: BukkitFastLoginPremiumToggleEvent) {
        val profile = event.profile
        val playerName = profile.name ?: return
        val playerUuid = profile.optId?.map { it.toString() }?.orElse("") ?: ""
        val isPremium = profile.isOnlinemodePreferred
        val accountType = if (isPremium) "PREMIUM" else "CRACKED"
        val source = when (event.reason) {
            PremiumToggleReason.COMMAND_SELF -> "COMMAND_SELF"
            PremiumToggleReason.COMMAND_OTHER -> "COMMAND_OTHER"
        }

        logger.info("[PremiumLogin] Premium toggle detected: '$playerName' → $accountType (reason: $source)")

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
