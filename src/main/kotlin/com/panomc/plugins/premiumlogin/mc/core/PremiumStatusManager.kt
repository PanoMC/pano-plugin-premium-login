package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformManager
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Core manager that handles premium/cracked status synchronization
 * between Minecraft servers and the Pano platform via WebSocket.
 *
 * Uses pano-mc-plugin's PlatformManager.sendMessageAwaitResponse()
 * for encrypted WebSocket communication with the Pano backend.
 *
 * When FastLogin is available, also keeps FastLogin's database in sync
 * so that the status is applied at the ProtocolLib packet level before
 * Bukkit events fire.
 */
class PremiumStatusManager(
    private val platformManager: PlatformManager,
    private val logger: Logger,
    private val fastLoginIntegration: FastLoginIntegration? = null
) {
    /** Cache: lowercase player name → account type */
    private val playerTypes = ConcurrentHashMap<String, String>()

    /** Locally detected types (UUID analysis / FastLogin) */
    private val detectedTypes = ConcurrentHashMap<String, String>()

    /**
     * Query Pano for the player's account type and sync if needed.
     *
     * 1. Queries Pano via WebSocket for the authoritative account type.
     * 2. If Pano says AUTO and we have a locally detected type, push it.
     * 3. Also updates FastLogin's DB if the integration is available.
     * 4. Returns the final account type.
     */
    suspend fun syncPlayerStatus(playerName: String, playerUuid: String): String? {
        if (platformManager.getWebSocket() == null) {
            logger.warning("[PremiumLogin] No platform connection, cannot sync '$playerName'.")
            return null
        }

        return try {
            val response = platformManager.sendMessageAwaitResponse<GetPlayerPremiumStatusMessage>(
                GetPlayerPremiumStatusRequest(playerName),
                GetPlayerPremiumStatusMessage::class.java
            )

            val panoAccountType = response.accountType
            val detectedType = detectedTypes[playerName.lowercase()]

            if (panoAccountType == "AUTO" && detectedType != null && detectedType != "AUTO") {
                logger.info("[PremiumLogin] Pushing detected type '$detectedType' for '$playerName' to Pano.")

                val updateResponse = platformManager.sendMessageAwaitResponse<UpdatePlayerPremiumStatusMessage>(
                    UpdatePlayerPremiumStatusRequest(
                        playerName = playerName,
                        playerUuid = playerUuid,
                        accountType = detectedType,
                        source = "SERVER"
                    ),
                    UpdatePlayerPremiumStatusMessage::class.java
                )

                if (updateResponse.success) {
                    playerTypes[playerName.lowercase()] = detectedType
                    updateFastLoginDb(playerName, detectedType)
                    logger.info("[PremiumLogin] Updated '$playerName' to '$detectedType' on Pano.")
                    return detectedType
                } else {
                    logger.warning("[PremiumLogin] Failed to update '$playerName': ${updateResponse.error}")
                }
            }

            playerTypes[playerName.lowercase()] = panoAccountType

            // Sync FastLogin DB with the authoritative Pano status
            if (panoAccountType != "AUTO") {
                updateFastLoginDb(playerName, panoAccountType)
            }

            logger.info("[PremiumLogin] '$playerName' account type is '$panoAccountType' (source: ${response.source}).")
            panoAccountType
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to sync '$playerName': ${e.message}")
            null
        }
    }

    /**
     * Force update a player's premium status to Pano via WebSocket.
     */
    suspend fun forceUpdateStatus(
        playerName: String,
        playerUuid: String,
        accountType: String,
        source: String = "FASTLOGIN"
    ): Boolean {
        if (platformManager.getWebSocket() == null) {
            logger.warning("[PremiumLogin] No platform connection, cannot update '$playerName'.")
            return false
        }

        return try {
            detectedTypes[playerName.lowercase()] = accountType

            val response = platformManager.sendMessageAwaitResponse<UpdatePlayerPremiumStatusMessage>(
                UpdatePlayerPremiumStatusRequest(
                    playerName = playerName,
                    playerUuid = playerUuid,
                    accountType = accountType,
                    source = source
                ),
                UpdatePlayerPremiumStatusMessage::class.java
            )

            if (response.success) {
                playerTypes[playerName.lowercase()] = accountType
                updateFastLoginDb(playerName, accountType)
                logger.info("[PremiumLogin] Force-updated '$playerName' to '$accountType' (source: $source).")
                true
            } else {
                logger.warning("[PremiumLogin] Failed to force-update '$playerName': ${response.error}")
                false
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Error force-updating '$playerName': ${e.message}")
            false
        }
    }

    fun setDetectedType(playerName: String, accountType: String) {
        detectedTypes[playerName.lowercase()] = accountType
    }

    fun detectTypeFromUuid(playerUuid: String): String? {
        return try {
            val uuid = java.util.UUID.fromString(playerUuid)
            when (uuid.version()) {
                4 -> "PREMIUM"
                3 -> "CRACKED"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getAccountType(playerName: String): String? = playerTypes[playerName.lowercase()]

    fun isPremium(playerName: String): Boolean? {
        val type = getAccountType(playerName) ?: return null
        return type == "PREMIUM"
    }

    fun onPlayerDisconnect(playerName: String) {
        playerTypes.remove(playerName.lowercase())
        detectedTypes.remove(playerName.lowercase())
    }

    fun clear() {
        playerTypes.clear()
        detectedTypes.clear()
    }

    /**
     * Check if the Pano platform is configured and connected via WebSocket.
     */
    fun isPlatformConnected(): Boolean {
        return platformManager.isPlatformConfigured() && platformManager.getWebSocket() != null
    }

    /**
     * Update FastLogin's database with the given account type.
     * This ensures FastLogin reads the correct status at LOGIN_START
     * via ProtocolLib, before any Bukkit events fire.
     */
    private fun updateFastLoginDb(playerName: String, accountType: String) {
        val integration = fastLoginIntegration ?: return
        if (!integration.isAvailable()) return

        when (accountType) {
            "PREMIUM" -> integration.setPremiumStatus(playerName)
            "CRACKED" -> integration.setCrackedStatus(playerName)
        }
    }

    /**
     * Query Pano backend for a player's premium status via WebSocket.
     * Returns the account type string (PREMIUM/CRACKED/AUTO) or null if unavailable.
     */
    suspend fun queryPanoStatus(playerName: String): String? {
        if (platformManager.getWebSocket() == null) {
            logger.warning("[PremiumLogin] No platform connection, cannot query '$playerName'.")
            return null
        }

        return try {
            val response = platformManager.sendMessageAwaitResponse<GetPlayerPremiumStatusMessage>(
                GetPlayerPremiumStatusRequest(playerName),
                GetPlayerPremiumStatusMessage::class.java
            )
            logger.info("[PremiumLogin] Backend query for '$playerName': accountType=${response.accountType}, source=${response.source}")
            response.accountType
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to query backend for '$playerName': ${e.message}")
            null
        }
    }
}
