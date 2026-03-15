package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformMessageHandler
import java.util.logging.Logger

/**
 * Handles PLAYER_PREMIUM_STATUS_UPDATED push messages coming from
 * the Pano backend via WebSocket.
 *
 * When an admin changes a player's premium/cracked status on the Pano panel,
 * the backend pushes this event to all connected MC servers. This handler
 * receives it and immediately updates FastLogin's database so the change
 * takes effect before the player's next login.
 *
 * Handler name derivation:
 *   "PlayerPremiumStatusUpdatedHandler" → remove "Handler" → "PlayerPremiumStatusUpdated"
 *   → convertToSnakeCase → "PLAYER_PREMIUM_STATUS_UPDATED"
 */
class PlayerPremiumStatusUpdatedHandler(
    private val fastLoginIntegration: FastLoginIntegration,
    private val premiumStatusManager: PremiumStatusManager,
    private val logger: Logger
) : PlatformMessageHandler<PlayerPremiumStatusUpdatedMessage>() {

    override suspend fun handle(response: PlayerPremiumStatusUpdatedMessage) {
        val playerName = response.playerName
        val accountType = response.accountType

        if (playerName.isBlank() || accountType.isBlank()) {
            logger.warning("[PremiumLogin] Received invalid status update push: playerName='$playerName', accountType='$accountType'")
            return
        }

        logger.info("[PremiumLogin] Received status update push from Pano: '$playerName' → '$accountType'")

        // Update FastLogin's database directly
        if (fastLoginIntegration.isAvailable()) {
            val success = when (accountType) {
                "PREMIUM" -> fastLoginIntegration.setPremiumStatus(playerName)
                "CRACKED" -> fastLoginIntegration.setCrackedStatus(playerName)
                "AUTO" -> {
                    logger.info("[PremiumLogin] Player '$playerName' set to AUTO — no FastLogin DB change needed.")
                    true
                }
                else -> {
                    logger.warning("[PremiumLogin] Unknown account type '$accountType' for '$playerName'")
                    false
                }
            }

            if (success) {
                logger.info("[PremiumLogin] FastLogin DB updated for '$playerName' → '$accountType'")
            }
        } else {
            logger.warning("[PremiumLogin] FastLogin not available, cannot update DB for '$playerName'")
        }

        // Also update the local cache in PremiumStatusManager
        premiumStatusManager.setDetectedType(playerName, accountType)
    }
}
