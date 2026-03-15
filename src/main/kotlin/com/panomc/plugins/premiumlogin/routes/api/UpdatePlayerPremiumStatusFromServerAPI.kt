package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.error.BadRequest
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

@Endpoint
class UpdatePlayerPremiumStatusFromServerAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : Api() {
    override val paths = listOf(Path("/api/premium-login/sync", RouteType.POST))

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty("playerName", stringSchema())
                        .requiredProperty("accountType", stringSchema())
                        .optionalProperty("playerUuid", stringSchema())
                        .optionalProperty("source", stringSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val body = context.body().asJsonObject()
        val playerName = body.getString("playerName")
        val accountTypeStr = body.getString("accountType")
        val playerUuid = body.getString("playerUuid") ?: ""
        val sourceStr = body.getString("source") ?: StatusSource.FASTLOGIN.name

        val accountType = try {
            AccountType.valueOf(accountTypeStr)
        } catch (_: Exception) {
            throw BadRequest()
        }

        val source = try {
            StatusSource.valueOf(sourceStr)
        } catch (_: Exception) {
            StatusSource.FASTLOGIN
        }

        val sqlClient = getSqlClient()

        val status = PlayerPremiumStatus(
            playerUuid = playerUuid,
            playerName = playerName,
            accountType = accountType.name,
            source = source.name,
            lastUpdated = System.currentTimeMillis()
        )

        playerPremiumStatusDao.upsertByPlayerName(status, sqlClient)

        return Successful()
    }
}
