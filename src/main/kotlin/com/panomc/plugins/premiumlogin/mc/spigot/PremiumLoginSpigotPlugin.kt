package com.panomc.plugins.premiumlogin.mc.spigot

import com.github.games647.fastlogin.bukkit.FastLoginBukkit
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent
import com.panomc.plugins.pano.core.helper.PanoPluginMain
import com.panomc.plugins.pano.core.platform.PlatformManager
import com.panomc.plugins.premiumlogin.mc.core.FastLoginIntegration
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class PremiumLoginSpigotPlugin : JavaPlugin(), Listener {

    private lateinit var premiumStatusManager: PremiumStatusManager
    private lateinit var platformManager: PlatformManager
    private val fastLoginIntegration = FastLoginIntegration(logger)

    private var preLoginListener: PreLoginListener? = null
    private var connectionMonitorTaskId: Int = -1
    private var wasConnected = false

    override fun onEnable() {
        val panoPlugin = Bukkit.getPluginManager().getPlugin("Pano")

        if (panoPlugin == null) {
            logger.severe("[PremiumLogin] Pano MC plugin not found! Disabling...")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        val panoMain = panoPlugin as PanoPluginMain
        platformManager = panoMain.getPano().platformManager

        // Initialize FastLogin integration if FastLogin is present
        initFastLogin()

        premiumStatusManager = PremiumStatusManager(platformManager, logger, fastLoginIntegration)

        // Register only the quit listener (always active)
        Bukkit.getPluginManager().registerEvents(this, this)

        // Start monitoring platform connection state
        startConnectionMonitor()

        logger.info("[PremiumLogin] Spigot plugin enabled!")
    }

    override fun onDisable() {
        stopConnectionMonitor()
        unregisterPreLoginListener()

        if (::premiumStatusManager.isInitialized) {
            premiumStatusManager.clear()
        }
        logger.info("[PremiumLogin] Spigot plugin disabled.")
    }

    /**
     * Monitor platform connection state every second.
     * Registers PreLogin listener when connected, unregisters when disconnected.
     */
    private fun startConnectionMonitor() {
        connectionMonitorTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, {
            val isConnected = ::premiumStatusManager.isInitialized &&
                    platformManager.isPlatformConfigured() &&
                    platformManager.getWebSocket() != null

            if (isConnected && !wasConnected) {
                // Just connected → register PreLogin listener
                registerPreLoginListener()
                wasConnected = true
            } else if (!isConnected && wasConnected) {
                // Just disconnected → unregister PreLogin listener
                unregisterPreLoginListener()
                wasConnected = false
            }
        }, 20L, 20L) // Check every second (20 ticks)
    }

    private fun stopConnectionMonitor() {
        if (connectionMonitorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(connectionMonitorTaskId)
            connectionMonitorTaskId = -1
        }
    }

    private fun registerPreLoginListener() {
        if (preLoginListener != null) return // Already registered

        preLoginListener = PreLoginListener(premiumStatusManager, fastLoginIntegration, logger)
        Bukkit.getPluginManager().registerEvents(preLoginListener!!, this)
        logger.info("[PremiumLogin] Platform connected → PreLogin listener registered.")
    }

    private fun unregisterPreLoginListener() {
        preLoginListener?.let {
            HandlerList.unregisterAll(it)
            preLoginListener = null
            logger.info("[PremiumLogin] Platform disconnected → PreLogin listener unregistered.")
        }
    }

    /**
     * Detect FastLogin plugin and initialize the compile-time API integration.
     */
    private fun initFastLogin() {
        try {
            val fastLogin = Bukkit.getPluginManager().getPlugin("FastLogin")
            if (fastLogin != null && fastLogin.isEnabled) {
                val fastLoginBukkit = fastLogin as FastLoginBukkit
                val core = fastLoginBukkit.core
                fastLoginIntegration.init(core)
                logger.info("[PremiumLogin] FastLogin detected and API integration initialized!")
            } else {
                logger.info("[PremiumLogin] FastLogin not found. Running without FastLogin integration.")
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to initialize FastLogin integration: ${e.message}")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!::premiumStatusManager.isInitialized) return

        premiumStatusManager.onPlayerDisconnect(event.player.name)
    }

    fun getStatusManager(): PremiumStatusManager? {
        return if (::premiumStatusManager.isInitialized) premiumStatusManager else null
    }
}

/**
 * Separate listener for BukkitFastLoginPreLoginEvent.
 * Registered only when platform is connected, unregistered on disconnect.
 */
class PreLoginListener(
    private val premiumStatusManager: PremiumStatusManager,
    private val fastLoginIntegration: FastLoginIntegration,
    private val logger: java.util.logging.Logger
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
