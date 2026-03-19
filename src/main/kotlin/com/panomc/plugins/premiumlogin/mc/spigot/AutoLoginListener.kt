package com.panomc.plugins.premiumlogin.mc.spigot

import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginAutoLoginEvent
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.logging.Logger

/**
 * Listens for FastLogin's AutoLoginEvent which fires when FastLogin
 * automatically detects and authenticates a premium player.
 * Pushes the detected premium status to Pano backend.
 */
class AutoLoginListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAutoLogin(event: BukkitFastLoginAutoLoginEvent) {
        if (event.isCancelled) return

        val profile = event.profile
        val session = event.session
        val playerName = session.username ?: profile.name ?: return
        val playerUuid = session.uuid?.toString() ?: profile.optId?.map { it.toString() }?.orElse("") ?: ""

        // AutoLogin only fires for premium players that are auto-authenticated
        logger.info("[PremiumLogin] Auto-login detected: '$playerName' → PREMIUM")

        try {
            runBlocking {
                premiumStatusManager.forceUpdateStatus(
                    playerName = playerName,
                    playerUuid = playerUuid,
                    accountType = "PREMIUM",
                    source = "FASTLOGIN"
                )
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to push auto-login status to Pano for '$playerName': ${e.message}")
        }
    }
}
