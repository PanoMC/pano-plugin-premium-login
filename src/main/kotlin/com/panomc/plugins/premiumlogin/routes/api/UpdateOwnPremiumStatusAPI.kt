package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.BadRequest
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.error.PremiumStatusSetByStaff
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Bodies
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class UpdateOwnPremiumStatusAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : LoggedInApi() {
    override val paths = listOf(Path("/me", RouteType.PUT))

    override val doc = EndpointDoc(
        summary = "Sets the account type (AUTO, PREMIUM or CRACKED) of the signed-in user.",
        tag = "player",
        response = objectSchema(),
        errors = listOf(BadRequest::class, PremiumStatusSetByStaff::class)
    )

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty("accountType", stringSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val userId = authProvider.getUserIdFromRoutingContext(context)
        val data = getParameters(context).body().jsonObject
        val accountTypeStr = data.getString("accountType")

        val accountType = try {
            AccountType.valueOf(accountTypeStr)
        } catch (_: Exception) {
            throw BadRequest()
        }

        val sqlClient = getSqlClient()

        val username = databaseManager.userDao.getUsernameFromUserId(userId, sqlClient)!!

        // Get existing status to preserve UUID if available
        val existing = playerPremiumStatusDao.getByPlayerName(username, sqlClient)

        // A type staff set in the panel stays until staff change it.
        if (existing?.source == StatusSource.PANEL.name) {
            throw PremiumStatusSetByStaff()
        }

        val status = PlayerPremiumStatus(
            playerUuid = existing?.playerUuid ?: "",
            playerName = username,
            accountType = accountType.name,
            source = StatusSource.USER.name,
            // Changing the type does not touch a Microsoft link.
            premiumLinked = existing?.premiumLinked ?: false,
            lastUpdated = System.currentTimeMillis()
        )

        playerPremiumStatusDao.upsertByPlayerName(status, sqlClient)

        return Successful()
    }
}
