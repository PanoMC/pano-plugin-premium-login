package com.panomc.plugins.premiumlogin.routes

import com.panomc.platform.model.Api
import com.panomc.platform.model.BrowserAccess
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.db.impl.PlayerPremiumStatusDaoImpl
import com.panomc.plugins.premiumlogin.routes.api.GetPlayerPremiumStatusAPI
import com.panomc.plugins.premiumlogin.routes.api.GetPremiumLoginConfigPublicAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftCallbackAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftLinkInitAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftLoginInitAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftProcessAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftRegisterInitAPI
import com.panomc.plugins.premiumlogin.routes.api.MicrosoftUnlinkAPI
import com.panomc.plugins.premiumlogin.routes.api.SetUsernameAPI
import com.panomc.plugins.premiumlogin.routes.api.UpdateOwnPremiumStatusAPI
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The public endpoints describe their answers (doc 04 section 5), and the sign-in answer matches the host's session shape. */
class EndpointDocsTest {
    private val plugin = PremiumLoginPlugin()
    private val dao = PlayerPremiumStatusDaoImpl()

    private val publicEndpoints: List<Api> = listOf(
        GetPlayerPremiumStatusAPI(plugin, dao),
        GetPremiumLoginConfigPublicAPI(plugin),
        MicrosoftCallbackAPI(plugin),
        MicrosoftLinkInitAPI(plugin),
        MicrosoftLoginInitAPI(plugin),
        MicrosoftProcessAPI(plugin, dao),
        MicrosoftRegisterInitAPI(plugin),
        MicrosoftUnlinkAPI(plugin, dao),
        SetUsernameAPI(plugin),
        UpdateOwnPremiumStatusAPI(plugin, dao)
    )

    @Test
    fun `every public endpoint has a usable doc`() {
        publicEndpoints.forEach { api ->
            val doc = api.doc

            assertNotNull(doc, "${api.javaClass.simpleName} has no doc")
            assertNull(doc!!.problem(), api.javaClass.simpleName)
        }
    }

    @Test
    fun `the process answer carries the action and a cookie session or a site session`() {
        val doc = MicrosoftProcessAPI(plugin, dao).doc!!

        assertEquals(emptyList<String>(), doc.check(JsonObject().put("action", "login").put("mcUsername", "Steve").put("csrfToken", "c")))
        assertEquals(
            emptyList<String>(),
            doc.check(JsonObject().put("action", "login").put("mcUsername", "Steve").put("sessionToken", "eyJ").put("expiresAt", 1791400000000L))
        )
        assertEquals(emptyList<String>(), doc.check(JsonObject().put("action", "link").put("mcUsername", "Steve")))
        assertEquals(emptyList<String>(), doc.check(JsonObject().put("action", "complete-required").put("pendingToken", "p").put("mcUsername", "Steve")))
        assertTrue(doc.check(JsonObject().put("csrfToken", "c")).isNotEmpty())
    }

    @Test
    fun `the Microsoft callback sits below the plugin and takes a browser from any origin`() {
        val callback = MicrosoftCallbackAPI(plugin)

        assertEquals(listOf("/microsoft/callback"), callback.paths.map { it.url })
        assertEquals(BrowserAccess.ANY_ORIGIN, callback.browserAccess)
        assertEquals(
            "/api/plugins/pano-plugin-premium-login/microsoft/callback",
            com.panomc.platform.route.ApiPaths.plugin("pano-plugin-premium-login", "/microsoft/callback")
        )
    }
}
