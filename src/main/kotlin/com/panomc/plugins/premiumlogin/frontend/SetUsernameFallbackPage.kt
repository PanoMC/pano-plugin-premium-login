package com.panomc.plugins.premiumlogin.frontend

import com.panomc.platform.annotation.FallbackPageDefinition
import com.panomc.platform.frontend.FallbackPage
import com.panomc.platform.frontend.FallbackPageEnvironment
import com.panomc.platform.frontend.FrontendUrlMap
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.ext.web.RoutingContext

/**
 * The link of the mail that asks the old owner of a user name to choose a new one (a Minecraft premium account has
 * claimed it), for a site whose front-end has not taken the target `premium-login.set-username` over (doc 05
 * section 10.3).
 */
@FallbackPageDefinition
class SetUsernameFallbackPage(
    private val plugin: PremiumLoginPlugin
) : FallbackPage("set-username", "fallback/set-username.hbs") {
    private val environment by lazy { plugin.applicationContext.getBean(FallbackPageEnvironment::class.java) }

    private val urls by lazy { plugin.applicationContext.getBean(FrontendUrlMap::class.java) }

    override suspend fun model(context: RoutingContext): Map<String, Any?> = mapOf(
        "p" to FallbackTexts.of(javaClass.classLoader, "set-username", environment.locale()),
        "token" to (context.request().getParam("token") ?: ""),
        "endpoint" to "plugin:/set-username",
        "clientConfig" to mapOf("errors" to FallbackTexts.of(javaClass.classLoader, "errors", environment.locale())),
        "loginUrl" to runCatching { urls.url("auth.login") }.getOrNull()
    )
}
