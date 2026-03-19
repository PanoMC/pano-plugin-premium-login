package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.error.MicrosoftLoginNotEnabled
import com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import java.util.*

@Endpoint
class MicrosoftLinkInitAPI(
    private val plugin: PremiumLoginPlugin
) : LoggedInApi() {
    override val paths = listOf(Path("/api/premium-login/microsoft/link", RouteType.GET))

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    private val oauthService by lazy {
        plugin.pluginBeanContext.getBean(MicrosoftOAuthService::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository).build()

    override suspend fun handle(context: RoutingContext): Result {
        if (!configManager.config.microsoftLoginEnabled) {
            throw MicrosoftLoginNotEnabled()
        }

        // State contains the action type
        val state = Base64.getUrlEncoder().encodeToString("link".toByteArray())
        val authUrl = oauthService.buildAuthorizationUrl(state)

        return Successful(mapOf("redirectUrl" to authUrl))
    }
}
