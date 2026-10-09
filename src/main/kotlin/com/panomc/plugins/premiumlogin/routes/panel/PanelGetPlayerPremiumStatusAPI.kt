package com.panomc.plugins.premiumlogin.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.NotFound
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

@Endpoint
class PanelGetPlayerPremiumStatusAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : PanelApi() {
    override val paths = listOf(Path("/player/:username", RouteType.GET))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(Parameters.param("username", stringSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val username = parameters.pathParameter("username").string

        val sqlClient = getSqlClient()

        // Verify user exists
        databaseManager.userDao.getUserIdFromUsername(username, sqlClient) ?: throw NotFound()

        val status = playerPremiumStatusDao.getByPlayerName(username, sqlClient)

        val result = if (status != null) {
            JsonObject()
                .put("accountType", status.accountType)
                .put("source", status.source)
                .put("lastUpdated", status.lastUpdated)
                .put("playerUuid", status.playerUuid)
                .put("premiumLinked", status.premiumLinked)
        } else {
            JsonObject()
                .put("accountType", AccountType.AUTO.name)
                .put("source", "")
                .put("lastUpdated", 0L)
                .put("playerUuid", "")
                .put("premiumLinked", false)
        }

        return Successful(result.map)
    }
}
