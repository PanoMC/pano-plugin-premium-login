package com.panomc.plugins.premiumlogin.mc.bungee

import com.github.games647.fastlogin.bungee.event.BungeeFastLoginAutoLoginEvent
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.logging.Logger

/**
 * Listens for FastLogin's AutoLoginEvent on BungeeCord.
 * Fires when FastLogin auto-detects and authenticates a premium player.
 */
class AutoLoginListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) : Listener {

    @EventHandler
    fun onAutoLogin(event: BungeeFastLoginAutoLoginEvent) {
        if (event.isCancelled) return

        val profile = event.profile
        val session = event.session
        val playerName = session.username ?: profile.name ?: return
        val playerUuid = session.uuid?.toString() ?: profile.optId?.map { it.toString() }?.orElse("") ?: ""

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
