package com.panomc.plugins.premiumlogin.mc.velocity

import com.github.games647.fastlogin.velocity.event.VelocityFastLoginAutoLoginEvent
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import com.velocitypowered.api.event.Subscribe
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

/**
 * Listens for FastLogin's AutoLoginEvent on Velocity.
 * Fires when FastLogin auto-detects and authenticates a premium player.
 */
class AutoLoginListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) {

    @Subscribe
    fun onAutoLogin(event: VelocityFastLoginAutoLoginEvent) {
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
