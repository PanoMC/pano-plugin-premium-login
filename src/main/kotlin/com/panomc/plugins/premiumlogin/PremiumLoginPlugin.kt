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
import io.vertx.core.Vertx

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

        val configManager = PluginConfigManager(this, PremiumLoginConfig::class.java)
        pluginBeanContext.beanFactory.registerSingleton(PluginConfigManager::class.java.name, configManager)

        // Register Microsoft OAuth service
        val vertx = applicationContext.getBean(Vertx::class.java)
        val oauthService = MicrosoftOAuthService(configManager, vertx)
        pluginBeanContext.beanFactory.registerSingleton(MicrosoftOAuthService::class.java.name, oauthService)
        pluginBeanContext.beanFactory.registerSingleton(
            com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore::class.java.name,
            com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore()
        )

        pluginDatabaseManager.initialize(this)

        // Register WebSocket event handlers for MC plugin communication
        registerServerEvents()

        logger.info("Started!")
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
