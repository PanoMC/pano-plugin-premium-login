package com.panomc.plugins.premiumlogin.mc.velocity

import com.github.games647.fastlogin.velocity.FastLoginVelocity
import com.google.inject.Inject
import com.panomc.plugins.pano.core.helper.PanoPluginMain
import com.panomc.plugins.premiumlogin.mc.core.FastLoginIntegration
import com.panomc.plugins.premiumlogin.mc.core.PremiumStatusManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.ProxyServer
import java.util.logging.Logger

class PremiumLoginVelocityPlugin {

    @Inject
    private lateinit var server: ProxyServer

    @Inject
    private lateinit var logger: Logger

    private lateinit var premiumStatusManager: PremiumStatusManager
    private lateinit var fastLoginIntegration: FastLoginIntegration

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        val panoPlugin = server.pluginManager.getPlugin("pano")

        if (panoPlugin.isEmpty) {
            logger.severe("[PremiumLogin] Pano MC plugin not found! Disabling...")
            return
        }

        val panoInstance = panoPlugin.get().instance

        if (panoInstance.isEmpty) {
            logger.severe("[PremiumLogin] Pano MC plugin instance not available! Disabling...")
            return
        }

        val panoMain = panoInstance.get() as PanoPluginMain
        val platformManager = panoMain.getPano().platformManager

        // Initialize FastLogin integration if FastLogin is present
        fastLoginIntegration = FastLoginIntegration(logger)
        initFastLogin()

        premiumStatusManager = PremiumStatusManager(platformManager, logger, fastLoginIntegration)

        // Register FastLogin event listeners
        server.eventManager.register(this, PremiumToggleListener(premiumStatusManager, logger))
        server.eventManager.register(this, AutoLoginListener(premiumStatusManager, logger))

        logger.info("[PremiumLogin] Velocity plugin enabled!")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        if (::premiumStatusManager.isInitialized) {
            premiumStatusManager.clear()
        }
        logger.info("[PremiumLogin] Velocity plugin disabled.")
    }

    /**
     * Detect FastLogin plugin and initialize the compile-time API integration.
     */
    private fun initFastLogin() {
        try {
            val fastLoginPlugin = server.pluginManager.getPlugin("fastlogin")
            if (fastLoginPlugin.isPresent) {
                val instance = fastLoginPlugin.get().instance
                if (instance.isPresent) {
                    val fastLoginVelocity = instance.get() as FastLoginVelocity
                    val core = fastLoginVelocity.core
                    fastLoginIntegration.init(core)
                    logger.info("[PremiumLogin] FastLogin detected and API integration initialized!")
                }
            } else {
                logger.info("[PremiumLogin] FastLogin not found. Running without FastLogin integration.")
            }
        } catch (e: Exception) {
            logger.warning("[PremiumLogin] Failed to initialize FastLogin integration: ${e.message}")
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        if (!::premiumStatusManager.isInitialized) return

        premiumStatusManager.onPlayerDisconnect(event.player.username)
    }

    fun getStatusManager(): PremiumStatusManager? {
        return if (::premiumStatusManager.isInitialized) premiumStatusManager else null
    }
}
