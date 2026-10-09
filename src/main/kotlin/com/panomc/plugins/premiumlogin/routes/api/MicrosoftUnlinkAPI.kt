package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import java.util.*
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*

/**
 * Unlinks the premium Minecraft account from the logged-in user.
 * Resets premiumLinked to false and generates a new offline UUID.
 */
@Endpoint
class MicrosoftUnlinkAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : LoggedInApi() {
    override val paths = listOf(Path("/microsoft/unlink", RouteType.POST))

    override val doc = EndpointDoc(
        summary = "Unlinks the premium account of the signed-in user.",
        tag = "microsoft",
        response = objectSchema()
    )

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository).build()

    override suspend fun handle(context: RoutingContext): Result {
        val sqlClient = getSqlClient()
        val userId = authProvider.getUserIdFromRoutingContext(context)
        val username = databaseManager.userDao.getUsernameFromUserId(userId, sqlClient)!!

        // Generate offline UUID (UUIDv3 from "OfflinePlayer:<username>")
        val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString()

        // Update user's mcUuid to offline UUID
        databaseManager.userDao.setMcUuidById(userId, offlineUuid, sqlClient)

        // Update premiumLinked to false in plugin table
        val existingStatus = playerPremiumStatusDao.getByPlayerName(username, sqlClient)
        if (existingStatus != null) {
            playerPremiumStatusDao.upsertByPlayerName(
                PlayerPremiumStatus(
                    playerUuid = existingStatus.playerUuid,
                    playerName = existingStatus.playerName,
                    accountType = existingStatus.accountType,
                    source = existingStatus.source,
                    premiumLinked = false,
                    lastUpdated = System.currentTimeMillis()
                ),
                sqlClient
            )
        }

        return Successful()
    }
}
