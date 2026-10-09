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
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import java.util.*
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class MicrosoftLoginInitAPI(
    private val plugin: PremiumLoginPlugin
) : Api() {
    override val paths = listOf(Path("/microsoft/login", RouteType.GET))

    override val doc = EndpointDoc(
        summary = "Returns the Microsoft address to send the browser to for signing in.",
        tag = "microsoft",
        response = objectSchema()
            .requiredProperty("redirectUrl", stringSchema()),
        errors = listOf(MicrosoftLoginNotEnabled::class)
    )

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
        val state = Base64.getUrlEncoder().encodeToString("login".toByteArray())
        val authUrl = oauthService.buildAuthorizationUrl(state)

        return Successful(mapOf("redirectUrl" to authUrl))
    }
}
