package com.panomc.plugins.premiumlogin.mc.spigot

import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent
import com.panomc.plugins.premiumlogin.mc.core.FastLoginIntegration
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.logging.Logger

/**
 * Separate listener for BukkitFastLoginPreLoginEvent.
 * Registered only when platform is connected, unregistered on disconnect.
 */
class PreLoginListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val fastLoginIntegration: FastLoginIntegration,
    private val logger: Logger
) : Listener {

    /**
     * Intercepts FastLogin's pre-login event, which fires BEFORE FastLogin
     * checks isOnlinemodePreferred() to decide premium vs cracked auth.
     *
     * This runs on ProtocolLib's async thread (LOGIN_START packet handler),
     * so it's safe to do blocking calls here.
     *
     * Flow in JoinManagement.onLogin():
     *   1. storage.loadProfile(username)     → loads profile into memory
     *   2. callFastLoginPreLoginEvent(...)    → THIS LISTENER RUNS HERE
     *   3. profile.isOnlinemodePreferred()   → reads OUR updated value
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onFastLoginPreLogin(event: BukkitFastLoginPreLoginEvent) {
        val username = event.username
        val profile = event.profile

        logger.info("[PremiumLogin] Pre-login check for '$username' (current: ${if (profile.isOnlinemodePreferred) "PREMIUM" else "CRACKED"})")

        try {
            val panoStatus = runBlocking {
                try {
                    premiumStatusManager.queryPanoStatus(username)
                } catch (e: Exception) {
                    logger.warning("[PremiumLogin] Backend query failed for '$username': ${e.message}")
                    null
                }
            }

            if (panoStatus != null && panoStatus != "AUTO") {
                val shouldBePremium = panoStatus == "PREMIUM"

                if (shouldBePremium != profile.isOnlinemodePreferred) {
                    profile.setOnlinemodePreferred(shouldBePremium)
                    logger.info("[PremiumLogin] Pre-login: Updated profile for '$username' → $panoStatus (from Pano backend)")

                    // Also persist to FastLogin DB for future logins
                    if (fastLoginIntegration.isAvailable()) {
                        if (shouldBePremium) {
                            fastLoginIntegration.setPremiumStatus(username)
                        } else {
                            fastLoginIntegration.setCrackedStatus(username)
                        }
                    }
                } else {
                    logger.info("[PremiumLogin] Pre-login: '$username' already correct ($panoStatus)")
                }
            } else {
                logger.info("[PremiumLogin] Pre-login: No definitive status from backend for '$username', using FastLogin default")
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Pre-login check failed for '$username': ${e.message}")
        }
    }
}
