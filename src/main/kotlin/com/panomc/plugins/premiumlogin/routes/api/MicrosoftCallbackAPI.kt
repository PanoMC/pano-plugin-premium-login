package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.model.*
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
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
    override val paths = listOf(Path("/api/premium-login/microsoft/callback", RouteType.GET))

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .queryParameter(Parameters.param("code", stringSchema()))
            .queryParameter(Parameters.param("state", stringSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val code = parameters.queryParameter("code").string
        val state = parameters.queryParameter("state").string

        // Redirect to the frontend callback page — actual processing happens via MicrosoftProcessAPI
        val redirectUrl = "/premium-login/callback?code=${enc(code)}&state=${enc(state)}"
        context.response()
            .setStatusCode(302)
            .putHeader("Location", redirectUrl)
            .end()

        return Successful()
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
