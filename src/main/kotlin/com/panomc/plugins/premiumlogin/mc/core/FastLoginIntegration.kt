package com.panomc.plugins.premiumlogin.mc.core

import com.github.games647.fastlogin.core.shared.FastLoginCore
import com.github.games647.fastlogin.core.storage.StoredProfile
import java.util.logging.Logger

/**
 * Platform-agnostic integration with FastLogin's StoredProfile / AuthStorage API.
 *
 * This class directly manipulates FastLogin's internal database to set
 * premium or cracked status **before** the player connects, ensuring
 * FastLogin's ProtocolLib interceptor reads the correct value at LOGIN_START.
 *
 * Usage flow:
 * 1. Pano admin panel changes a player's status → Pano backend pushes a
 *    PLAYER_PREMIUM_STATUS_UPDATED message to all connected MC servers.
 * 2. The MC-side handler receives the message and calls
 *    [setPremiumStatus] / [setCrackedStatus] on this integration.
 * 3. FastLogin's StoredProfile in the database is updated immediately.
 * 4. Next time the player connects, FastLogin reads the updated profile
 *    and applies the correct authentication mode.
 */
class FastLoginIntegration(
    private val logger: Logger
) {
    private var fastLoginCore: FastLoginCore<*, *, *>? = null

    /**
     * Initialize the integration with a reference to FastLoginCore.
     * Call this from the platform-specific plugin after detecting FastLogin.
     */
    fun init(core: FastLoginCore<*, *, *>) {
        this.fastLoginCore = core
        logger.info("[PremiumLogin] FastLogin API integration initialized (compile-time API).")
    }

    /**
     * @return true if FastLogin is available and the integration is initialized
     */
    fun isAvailable(): Boolean = fastLoginCore != null

    /**
     * Set a player's FastLogin profile to PREMIUM (online-mode preferred).
     *
     * @param playerName the player's Minecraft username
     * @return true if the profile was updated successfully
     */
    fun setPremiumStatus(playerName: String): Boolean {
        return setOnlineModePreferred(playerName, true)
    }

    /**
     * Set a player's FastLogin profile to CRACKED (offline-mode).
     *
     * @param playerName the player's Minecraft username
     * @return true if the profile was updated successfully
     */
    fun setCrackedStatus(playerName: String): Boolean {
        return setOnlineModePreferred(playerName, false)
    }

    /**
     * Query FastLogin's database for a player's current premium status.
     *
     * @param playerName the player's Minecraft username
     * @return true if premium, false if cracked, null if no profile / error
     */
    fun isPremium(playerName: String): Boolean? {
        val core = fastLoginCore ?: return null
        val storage = core.storage ?: return null

        return try {
            val profile: StoredProfile? = storage.loadProfile(playerName)
            profile?.isOnlinemodePreferred
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to query FastLogin profile for '$playerName': ${e.message}")
            null
        }
    }

    /**
     * Core helper: loads a StoredProfile from FastLogin's DB,
     * modifies the online-mode preference, and saves it back.
     *
     * Uses FastLogin's recommended [StoredProfile.setOnlinemodePreferred] method.
     */
    private fun setOnlineModePreferred(playerName: String, premium: Boolean): Boolean {
        val core = fastLoginCore ?: run {
            logger.warning("[PremiumLogin] FastLogin not available, cannot update '$playerName'.")
            return false
        }

        val storage = core.storage ?: run {
            logger.warning("[PremiumLogin] FastLogin storage not available, cannot update '$playerName'.")
            return false
        }

        return try {
            val profile: StoredProfile? = storage.loadProfile(playerName)

            if (profile != null) {
                profile.setOnlinemodePreferred(premium)
                storage.save(profile)
                val status = if (premium) "PREMIUM" else "CRACKED"
                logger.info("[PremiumLogin] Updated FastLogin DB: '$playerName' → $status")
                true
            } else {
                logger.warning("[PremiumLogin] No FastLogin profile found for '$playerName', cannot update.")
                false
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to update FastLogin profile for '$playerName': ${e.message}")
            false
        }
    }
}
