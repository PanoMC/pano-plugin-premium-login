package com.panomc.plugins.premiumlogin.mc.bungee

import com.github.games647.fastlogin.bungee.FastLoginBungee
import com.panomc.plugins.pano.core.helper.PanoPluginMain
import com.panomc.plugins.premiumlogin.mc.core.FastLoginIntegration
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler

class PremiumLoginBungeePlugin : Plugin(), Listener {

    private lateinit var premiumStatusManager: PremiumStatusManager
    private val fastLoginIntegration = FastLoginIntegration(logger)

    override fun onEnable() {
        val panoPlugin = proxy.pluginManager.getPlugin("Pano")

        if (panoPlugin == null) {
            logger.severe("[PremiumLogin] Pano MC plugin not found! Disabling...")
            return
        }

        val panoMain = panoPlugin as PanoPluginMain
        val platformManager = panoMain.getPano().platformManager

        // Initialize FastLogin integration if FastLogin is present
        initFastLogin()

        premiumStatusManager = PremiumStatusManager(platformManager, logger, fastLoginIntegration)

        proxy.pluginManager.registerListener(this, this)

        logger.info("[PremiumLogin] BungeeCord plugin enabled!")
    }

    override fun onDisable() {
        if (::premiumStatusManager.isInitialized) {
            premiumStatusManager.clear()
        }
        logger.info("[PremiumLogin] BungeeCord plugin disabled.")
    }

    /**
     * Detect FastLogin plugin and initialize the compile-time API integration.
     */
    private fun initFastLogin() {
        try {
            val fastLogin = proxy.pluginManager.getPlugin("FastLogin")
            if (fastLogin != null) {
                val fastLoginBungee = fastLogin as FastLoginBungee
                val core = fastLoginBungee.core
                fastLoginIntegration.init(core)
                logger.info("[PremiumLogin] FastLogin detected and API integration initialized!")
            } else {
                logger.info("[PremiumLogin] FastLogin not found. Running without FastLogin integration.")
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to initialize FastLogin integration: ${e.message}")
        }
    }

    @EventHandler
    fun onPlayerDisconnect(event: PlayerDisconnectEvent) {
        if (!::premiumStatusManager.isInitialized) return

        premiumStatusManager.onPlayerDisconnect(event.player.name)
    }

    fun getStatusManager(): PremiumStatusManager? {
        return if (::premiumStatusManager.isInitialized) premiumStatusManager else null
    }
}
