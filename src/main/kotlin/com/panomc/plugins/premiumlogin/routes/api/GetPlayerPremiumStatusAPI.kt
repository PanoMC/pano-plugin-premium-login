package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

@Endpoint
class GetPlayerPremiumStatusAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : Api() {
    override val paths = listOf(Path("/api/premium-login/player/:username", RouteType.GET))

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(Parameters.param("username", stringSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val username = parameters.pathParameter("username").string

        val sqlClient = getSqlClient()

        val status = playerPremiumStatusDao.getByPlayerName(username, sqlClient)

        val result = if (status != null) {
            JsonObject()
                .put("accountType", status.accountType)
                .put("source", status.source)
                .put("lastUpdated", status.lastUpdated)
                .put("playerUuid", status.playerUuid)
                .put("playerName", status.playerName)
                .put("premiumLinked", status.premiumLinked)
        } else {
            JsonObject()
                .put("accountType", AccountType.AUTO.name)
                .put("source", "")
                .put("lastUpdated", 0L)
                .put("playerUuid", "")
                .put("playerName", username)
                .put("premiumLinked", false)
        }

        return Successful(result.map)
    }
}
