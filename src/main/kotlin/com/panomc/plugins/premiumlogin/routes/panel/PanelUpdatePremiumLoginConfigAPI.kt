package com.panomc.plugins.premiumlogin.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.log.UpdatedPremiumLoginSettingsLog
import com.panomc.plugins.premiumlogin.permission.ManagePremiumLoginSettingsPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Bodies
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class PanelUpdatePremiumLoginConfigAPI(
    private val plugin: PremiumLoginPlugin
) : PanelApi() {
    override val paths = listOf(Path("/config", RouteType.PUT))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    objectSchema()
                        .optionalProperty("microsoftLoginEnabled", booleanSchema())
                        .optionalProperty("microsoftClientId", stringSchema())
                        .optionalProperty("microsoftClientSecret", stringSchema())
                        .optionalProperty("microsoftRedirectUri", stringSchema())
                        .optionalProperty("debugLogging", booleanSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManagePremiumLoginSettingsPermission(), context)

        val body = context.body().asJsonObject()
        configManager.saveConfig(body)

        // Log the activity
        val sqlClient = getSqlClient()
        val adminUserId = authProvider.getUserIdFromRoutingContext(context)
        val adminUsername = databaseManager.userDao.getUsernameFromUserId(adminUserId, sqlClient)!!

        databaseManager.panelActivityLogDao.add(
            UpdatedPremiumLoginSettingsLog(
                adminUserId,
                adminUsername,
                plugin.pluginId
            ),
            sqlClient
        )

        return Successful()
    }
}
