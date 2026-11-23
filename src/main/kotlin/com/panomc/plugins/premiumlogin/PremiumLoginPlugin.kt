package com.panomc.plugins.premiumlogin

import com.panomc.platform.api.PanoPlugin
import com.panomc.platform.api.PluginDatabaseManager
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.server.ServerManager
import com.panomc.platform.setup.SetupManager
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.event.server.GetPlayerPremiumStatusEvent
import com.panomc.plugins.premiumlogin.event.server.UpdatePlayerPremiumStatusEvent
import com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService
import io.vertx.ext.web.client.WebClient

class PremiumLoginPlugin : PanoPlugin() {
    private val pluginDatabaseManager by lazy {
        applicationContext.getBean(PluginDatabaseManager::class.java)
    }

    private val setupManager by lazy {
        applicationContext.getBean(SetupManager::class.java)
    }

    private val serverManager by lazy {
        applicationContext.getBean(ServerManager::class.java)
    }

    private val databaseManager by lazy {
        applicationContext.getBean(DatabaseManager::class.java)
    }

    private var isInitialized = false

    private lateinit var getPlayerPremiumStatusEvent: GetPlayerPremiumStatusEvent
    private lateinit var updatePlayerPremiumStatusEvent: UpdatePlayerPremiumStatusEvent

    override suspend fun onStart() {
        logger.info("Starting...")

        startPlugin()
    }

    internal suspend fun startPlugin() {
        if (isInitialized) return

        if (!setupManager.isSetupDone()) {
            logger.info("Setup is not finished, waiting for setup completion...")
            return
        }

        isInitialized = true

        try {
            val configManager = registerIfAbsent(PluginConfigManager::class.java.name) {
                PluginConfigManager(this, PremiumLoginConfig::class.java)
            }

            // Register Microsoft OAuth service
            registerIfAbsent(MicrosoftOAuthService::class.java.name) {
                MicrosoftOAuthService(configManager, applicationContext.getBean(WebClient::class.java))
            }
            registerIfAbsent(com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore::class.java.name) {
                com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore()
            }

            pluginDatabaseManager.initialize(this)
        } catch (e: Throwable) {
            // Leave the plugin startable again: a later start retries instead of returning early.
            isInitialized = false
            throw e
        }

        // Register WebSocket event handlers for MC plugin communication
        registerServerEvents()

        logger.info("Started!")
    }

    /**
     * Registers [create]'s bean under [name] unless a previous (possibly failed) start already did, so a retry
     * does not fail on an existing singleton.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> registerIfAbsent(name: String, create: () -> T): T {
        val beanFactory = pluginBeanContext.beanFactory
        if (beanFactory.containsSingleton(name)) return beanFactory.getSingleton(name) as T

        return create().also { beanFactory.registerSingleton(name, it) }
    }

    private fun registerServerEvents() {
        val playerPremiumStatusDao = pluginBeanContext.getBean(PlayerPremiumStatusDao::class.java)

        getPlayerPremiumStatusEvent = GetPlayerPremiumStatusEvent(databaseManager, playerPremiumStatusDao)
        updatePlayerPremiumStatusEvent = UpdatePlayerPremiumStatusEvent(databaseManager, playerPremiumStatusDao)

        serverManager.registerEvent(getPlayerPremiumStatusEvent)
        serverManager.registerEvent(updatePlayerPremiumStatusEvent)

        logger.info("Registered WebSocket event handlers: GET_PLAYER_PREMIUM_STATUS, UPDATE_PLAYER_PREMIUM_STATUS")
    }

    private fun unregisterServerEvents() {
        if (::getPlayerPremiumStatusEvent.isInitialized) {
            serverManager.unregisterEvent(getPlayerPremiumStatusEvent)
        }
        if (::updatePlayerPremiumStatusEvent.isInitialized) {
            serverManager.unregisterEvent(updatePlayerPremiumStatusEvent)
        }

        logger.info("Unregistered WebSocket event handlers.")
    }

    override suspend fun onEnable() {
        logger.info("Enabled!")
    }

    override suspend fun onDisable() {
        unregisterServerEvents()
        isInitialized = false
    }

    override suspend fun onUninstall() {
        logger.info("Uninstalling...")
        unregisterServerEvents()
        pluginDatabaseManager.uninstall(this)
    }
}
