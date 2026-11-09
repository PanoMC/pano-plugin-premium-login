package com.panomc.plugins.premiumlogin.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.NotFound
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.service.PremiumUnlink
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import com.panomc.plugins.premiumlogin.permission.ManagePremiumStatusPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import java.util.*

/**
 * Admin endpoint to unlink a player's premium Minecraft account.
 */
@Endpoint
class PanelUnlinkPlayerPremiumAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : PanelApi() {
    override val paths = listOf(Path("/player/:username/unlink", RouteType.POST))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private val unlinkConfigManager by lazy {
        plugin.pluginBeanContext.getBean(com.panomc.platform.api.config.PluginConfigManager::class.java)
            as com.panomc.platform.api.config.PluginConfigManager<com.panomc.plugins.premiumlogin.config.PremiumLoginConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(Parameters.param("username", stringSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManagePremiumStatusPermission(), context)

        val parameters = getParameters(context)
        val username = parameters.pathParameter("username").string

        val sqlClient = getSqlClient()

        // Verify user exists
        val userId = databaseManager.userDao.getUserIdFromUsername(username, sqlClient) ?: throw NotFound()

        // Generate offline UUID
        val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString()

        // The premium UUID that was linked, read before it is reset: the status row is stored under it.
        val linkedMcUuid = databaseManager.userDao.getById(userId, sqlClient)?.mcUuid

        // Update user's mcUuid to offline UUID
        databaseManager.userDao.setMcUuidById(userId, offlineUuid, sqlClient)

        // Update premiumLinked to false in plugin table
        PremiumUnlink.clearLink(
            playerPremiumStatusDao,
            linkedMcUuid,
            username,
            unlinkConfigManager.config.defaultAccountType,
            sqlClient
        )

        return Successful()
    }
}
