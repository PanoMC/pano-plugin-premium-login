package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.error.BadRequest
import com.panomc.platform.frontend.FrontendUrlMap
import com.panomc.platform.model.*
import com.panomc.platform.schema.EndpointDoc
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.ext.web.RoutingContext
import java.net.URLEncoder

/**
 * Microsoft OAuth callback endpoint — receives the authorization code from Microsoft,
 * then redirects to the frontend callback page for client-side processing.
 * The actual heavy OAuth exchange happens in MicrosoftProcessAPI.
 */
@Endpoint
class MicrosoftCallbackAPI(
    private val plugin: PremiumLoginPlugin
) : Api() {
    override val paths = listOf(Path("/microsoft/callback", RouteType.GET))

    // Microsoft's browser arrives from a foreign origin.
    override val browserAccess: BrowserAccess = BrowserAccess.ANY_ORIGIN

    override val doc = EndpointDoc(
        summary = "The address registered at Microsoft: receives the authorization code and redirects the browser to the callback page of the front-end.",
        tag = "microsoft",
        binary = true
    )

    private val frontendUrlMap by lazy {
        plugin.applicationContext.getBean(FrontendUrlMap::class.java)
    }

    override suspend fun handle(context: RoutingContext): Result {
        val request = context.request()
        val code = request.getParam("code")
        val state = request.getParam("state")
        val error = request.getParam("error")

        // Microsoft always sends `code` and `state`, or `error`. Anything else is not a callback, and it is not
        // forwarded to the front-end either (the redirect target is only ever a real Microsoft answer).
        if (error == null && (code.isNullOrBlank() || state.isNullOrBlank())) throw BadRequest("Missing code or state")

        // Redirect to the front-end's callback page (target `premium-login.callback` of the URL map, Pano's own page
        // at /_pano/premium-login.callback until a front-end takes it over) — the processing happens via MicrosoftProcessAPI
        val params = if (error != null) linkedMapOf("error" to error) else linkedMapOf("code" to code!!, "state" to state!!)
        val redirectUrl = frontendUrlMap.url("premium-login.callback", params)
            ?: ("/premium-login/callback?" + params.entries.joinToString("&") { "${it.key}=${enc(it.value)}" })
        context.response()
            .setStatusCode(302)
            .putHeader("Location", redirectUrl)
            .end()

        return Successful()
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
