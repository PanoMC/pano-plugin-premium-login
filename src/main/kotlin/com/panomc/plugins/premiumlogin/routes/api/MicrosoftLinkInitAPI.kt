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
class MicrosoftLinkInitAPI(
    private val plugin: PremiumLoginPlugin
) : LoggedInApi() {
    override val paths = listOf(Path("/microsoft/link", RouteType.GET))

    override val doc = EndpointDoc(
        summary = "Returns the Microsoft address to send the browser to for linking a premium account to the signed-in user.",
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

    private val flowStore by lazy {
        plugin.pluginBeanContext.getBean(com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(com.panomc.platform.auth.AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository).build()

    override suspend fun handle(context: RoutingContext): Result {
        if (!configManager.config.microsoftLoginEnabled) {
            throw MicrosoftLoginNotEnabled()
        }

        // A random one-time state, bound to this browser and user (see MicrosoftFlowStore).
        val state = flowStore.create("link", authProvider.getUserIdFromRoutingContext(context), context)
        val authUrl = oauthService.buildAuthorizationUrl(state)

        return Successful(mapOf("redirectUrl" to authUrl))
    }
}
