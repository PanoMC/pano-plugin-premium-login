package com.panomc.plugins.premiumlogin.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.BadRequest
import com.panomc.platform.error.NotFound
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import com.panomc.plugins.premiumlogin.log.UpdatedPlayerPremiumStatusLog
import com.panomc.plugins.premiumlogin.permission.ManagePremiumStatusPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

@Endpoint
class PanelUpdatePlayerPremiumStatusAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/premium-login/player/:username", RouteType.PUT))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(Parameters.param("username", stringSchema()))
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty("accountType", stringSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManagePremiumStatusPermission(), context)

        val parameters = getParameters(context)
        val username = parameters.pathParameter("username").string
        val data = parameters.body().jsonObject
        val accountTypeStr = data.getString("accountType")

        val accountType = try {
            AccountType.valueOf(accountTypeStr)
        } catch (_: Exception) {
            throw BadRequest()
        }

        val sqlClient = getSqlClient()

        // Verify user exists
        databaseManager.userDao.getUserIdFromUsername(username, sqlClient) ?: throw NotFound()

        // Get existing status to preserve UUID if available
        val existing = playerPremiumStatusDao.getByPlayerName(username, sqlClient)

        val status = PlayerPremiumStatus(
            playerUuid = existing?.playerUuid ?: "",
            playerName = username,
            accountType = accountType.name,
            source = StatusSource.PANEL.name,
            lastUpdated = System.currentTimeMillis()
        )

        playerPremiumStatusDao.upsertByPlayerName(status, sqlClient)

        // Log the activity
        val adminUserId = authProvider.getUserIdFromRoutingContext(context)
        val adminUsername = databaseManager.userDao.getUsernameFromUserId(adminUserId, sqlClient)!!

        databaseManager.panelActivityLogDao.add(
            UpdatedPlayerPremiumStatusLog(
                adminUserId,
                adminUsername,
                username,
                accountType.name,
                plugin.pluginId
            ),
            sqlClient
        )

        return Successful()
    }
}
