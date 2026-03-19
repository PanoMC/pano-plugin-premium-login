package com.panomc.plugins.premiumlogin.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.permission.ManagePremiumLoginSettingsPermission
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository

@Endpoint
class PanelGetPremiumLoginConfigAPI(
    private val plugin: PremiumLoginPlugin
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/premium-login/config", RouteType.GET))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository).build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManagePremiumLoginSettingsPermission(), context)

        val config = configManager.config
        val result = JsonObject()
            .put("microsoftLoginEnabled", config.microsoftLoginEnabled)
            .put("microsoftClientId", config.microsoftClientId)
            .put("hasSecret", config.microsoftClientSecret.isNotBlank())
            .put("microsoftRedirectUri", config.microsoftRedirectUri)
            .put("debugLogging", config.debugLogging)

        return Successful(result.map)
    }
}
