package com.panomc.plugins.premiumlogin.frontend

import com.panomc.platform.annotation.FallbackPageDefinition
import com.panomc.platform.frontend.FallbackPage
import com.panomc.platform.frontend.FallbackPageEnvironment
import com.panomc.platform.frontend.FrontendUrlMap
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.ext.web.RoutingContext

/**
 * Where the Microsoft callback sends the browser (`code` and `state` in the query), for a site whose front-end has
 * not taken the target `premium-login.callback` over (doc 05 section 10.3). The page posts both to the plugin, which
 * opens a cookie session on Pano's host (or links the account when the visitor started from their settings).
 */
@FallbackPageDefinition
class MicrosoftCallbackFallbackPage(
    private val plugin: PremiumLoginPlugin
) : FallbackPage("callback", "fallback/callback.hbs") {
    private val environment by lazy { plugin.applicationContext.getBean(FallbackPageEnvironment::class.java) }

    private val urls by lazy { plugin.applicationContext.getBean(FrontendUrlMap::class.java) }

    override suspend fun model(context: RoutingContext): Map<String, Any?> = mapOf(
        "p" to FallbackTexts.of(javaClass.classLoader, "callback", environment.locale()),
        "code" to (context.request().getParam("code") ?: ""),
        "state" to (context.request().getParam("state") ?: ""),
        "endpoint" to "plugin:/microsoft/process",
        "homeUrl" to environment.homeUrl(),
        "clientConfig" to mapOf("errors" to FallbackTexts.of(javaClass.classLoader, "errors", environment.locale())),
        "loginUrl" to runCatching { urls.url("auth.login") }.getOrNull()
    )
}
