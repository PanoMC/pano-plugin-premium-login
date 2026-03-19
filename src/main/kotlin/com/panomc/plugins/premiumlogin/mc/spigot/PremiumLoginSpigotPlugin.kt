package com.panomc.plugins.premiumlogin.mc.spigot

import com.github.games647.fastlogin.bukkit.FastLoginBukkit
import com.panomc.plugins.pano.core.helper.PanoPluginMain
import com.panomc.plugins.pano.core.platform.PlatformManager
import com.panomc.plugins.premiumlogin.mc.core.FastLoginIntegration
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
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
    private var premiumToggleListener: PremiumToggleListener? = null
    private var autoLoginListener: AutoLoginListener? = null
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
        unregisterFastLoginListeners()

        if (::premiumStatusManager.isInitialized) {
            premiumStatusManager.clear()
        }
        logger.info("[PremiumLogin] Spigot plugin disabled.")
    }

    /**
     * Monitor platform connection state every second.
     * Registers FastLogin listeners when connected, unregisters when disconnected.
     */
    private fun startConnectionMonitor() {
        connectionMonitorTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, {
            val isConnected = ::premiumStatusManager.isInitialized &&
                    platformManager.isPlatformConfigured() &&
                    platformManager.getWebSocket() != null

            if (isConnected && !wasConnected) {
                // Just connected → register FastLogin listeners
                registerFastLoginListeners()
                wasConnected = true
            } else if (!isConnected && wasConnected) {
                // Just disconnected → unregister FastLogin listeners
                unregisterFastLoginListeners()
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

    private fun registerFastLoginListeners() {
        if (preLoginListener != null) return // Already registered

        preLoginListener = PreLoginListener(premiumStatusManager, fastLoginIntegration, logger)
        premiumToggleListener = PremiumToggleListener(premiumStatusManager, logger)
        autoLoginListener = AutoLoginListener(premiumStatusManager, logger)

        Bukkit.getPluginManager().registerEvents(preLoginListener!!, this)
        Bukkit.getPluginManager().registerEvents(premiumToggleListener!!, this)
        Bukkit.getPluginManager().registerEvents(autoLoginListener!!, this)

        logger.info("[PremiumLogin] Platform connected → FastLogin listeners registered.")
    }

    private fun unregisterFastLoginListeners() {
        preLoginListener?.let {
            HandlerList.unregisterAll(it)
            preLoginListener = null
        }
        premiumToggleListener?.let {
            HandlerList.unregisterAll(it)
            premiumToggleListener = null
        }
        autoLoginListener?.let {
            HandlerList.unregisterAll(it)
            autoLoginListener = null
        }
        logger.info("[PremiumLogin] Platform disconnected → FastLogin listeners unregistered.")
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

