package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository

@Endpoint
class GetPremiumLoginConfigPublicAPI(
    private val plugin: PremiumLoginPlugin
) : Api() {
    override val paths = listOf(Path("/api/premium-login/config", RouteType.GET))

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository).build()

    override suspend fun handle(context: RoutingContext): Result {
        val config = configManager.config

        // Only expose safe public fields - never expose client secret
        return Successful(
            mapOf(
                "microsoftLoginEnabled" to config.microsoftLoginEnabled
            )
        )
    }
}
