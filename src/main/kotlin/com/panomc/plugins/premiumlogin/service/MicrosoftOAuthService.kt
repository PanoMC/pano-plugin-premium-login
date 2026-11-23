package com.panomc.plugins.premiumlogin.service

import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import java.net.URLEncoder

class MicrosoftOAuthService(
    private val configManager: PluginConfigManager<PremiumLoginConfig>,
    // Core's shared client: never closed here, the host owns it.
    private val webClient: WebClient
) {
    companion object {
        private const val MICROSOFT_AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
        private const val MICROSOFT_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        private const val XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        private const val MINECRAFT_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        private const val MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
        private const val MICROSOFT_GRAPH_ME_URL = "https://graph.microsoft.com/v1.0/me"
        private const val SCOPE = "XboxLive.signin offline_access"
    }

    data class MinecraftProfile(
        val uuid: String,
        val username: String,
        val email: String? = null
    )

    fun buildAuthorizationUrl(state: String): String {
        val config = configManager.config
        val params = mapOf(
            "client_id" to config.microsoftClientId,
            "response_type" to "code",
            "redirect_uri" to config.microsoftRedirectUri,
            "scope" to SCOPE,
            "state" to state,
            "response_mode" to "query"
        )
        return MICROSOFT_AUTH_URL + "?" + params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
    }

    private val logger = org.slf4j.LoggerFactory.getLogger("MicrosoftOAuthService")

    private fun debugLog(msg: String) {
        if (configManager.config.debugLogging) {
            logger.info(msg)
        }
    }

    suspend fun exchangeCodeForMinecraftProfile(code: String): MinecraftProfile {
        // Step 1: Exchange authorization code for Microsoft access token
        debugLog("Step 1: Exchanging authorization code for Microsoft token...")
        val msToken = exchangeCodeForMicrosoftToken(code)
        debugLog("Step 1: Success - got Microsoft access token")

        // Step 2: Authenticate with Xbox Live
        debugLog("Step 2: Authenticating with Xbox Live...")
        val xboxToken = authenticateWithXboxLive(msToken)
        debugLog("Step 2: Success - Xbox token obtained, userHash=${xboxToken.userHash}")

        // Step 3: Get XSTS token
        debugLog("Step 3: Getting XSTS token...")
        val xstsResult = authenticateWithXSTS(xboxToken.token)
        debugLog("Step 3: Success - XSTS token obtained, userHash=${xstsResult.userHash}")

        // Step 4: Authenticate with Minecraft
        debugLog("Step 4: Authenticating with Minecraft Services...")
        val mcToken = authenticateWithMinecraft(xstsResult.userHash, xstsResult.token)
        debugLog("Step 4: Success - Minecraft access token obtained")

        // Step 5: Get Minecraft profile
        debugLog("Step 5: Getting Minecraft profile...")
        val profile = getMinecraftProfile(mcToken)
        debugLog("Step 5: Success - Minecraft username=${profile.username}, uuid=${profile.uuid}")

        return profile
    }

    private data class XboxAuthResult(val token: String, val userHash: String)

    private suspend fun exchangeCodeForMicrosoftToken(code: String): String {
        val config = configManager.config

        val formBody = listOf(
            "client_id" to config.microsoftClientId,
            "client_secret" to config.microsoftClientSecret,
            "code" to code,
            "grant_type" to "authorization_code",
            "redirect_uri" to config.microsoftRedirectUri
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val response = webClient
            .postAbs(MICROSOFT_TOKEN_URL)
            .putHeader("Content-Type", "application/x-www-form-urlencoded")
            .sendBuffer(Buffer.buffer(formBody))
            .coAwait()

        val body = response.bodyAsJsonObject()

        if (body.containsKey("error")) {
            throw RuntimeException("Microsoft token exchange failed: ${body.getString("error_description", body.getString("error"))}")
        }

        return body.getString("access_token")
    }

    private suspend fun authenticateWithXboxLive(microsoftToken: String): XboxAuthResult {
        val requestBody = JsonObject()
            .put("Properties", JsonObject()
                .put("AuthMethod", "RPS")
                .put("SiteName", "user.auth.xboxlive.com")
                .put("RpsTicket", "d=$microsoftToken")
            )
            .put("RelyingParty", "http://auth.xboxlive.com")
            .put("TokenType", "JWT")

        val response = webClient
            .postAbs(XBOX_AUTH_URL)
            .putHeader("Content-Type", "application/json")
            .putHeader("Accept", "application/json")
            .sendJsonObject(requestBody)
            .coAwait()

        val body = response.bodyAsJsonObject()

        if (response.statusCode() != 200) {
            throw RuntimeException("Xbox Live authentication failed: ${response.statusCode()}")
        }

        val token = body.getString("Token")
        val userHash = body.getJsonObject("DisplayClaims")
            .getJsonArray("xui")
            .getJsonObject(0)
            .getString("uhs")

        return XboxAuthResult(token, userHash)
    }

    private suspend fun authenticateWithXSTS(xboxToken: String): XboxAuthResult {
        val requestBody = JsonObject()
            .put("Properties", JsonObject()
                .put("SandboxId", "RETAIL")
                .put("UserTokens", io.vertx.core.json.JsonArray().add(xboxToken))
            )
            .put("RelyingParty", "rp://api.minecraftservices.com/")
            .put("TokenType", "JWT")

        val response = webClient
            .postAbs(XSTS_AUTH_URL)
            .putHeader("Content-Type", "application/json")
            .putHeader("Accept", "application/json")
            .sendJsonObject(requestBody)
            .coAwait()

        if (response.statusCode() != 200) {
            val bodyStr = response.bodyAsString() ?: "no body"
            val body = try { response.bodyAsJsonObject() } catch (_: Exception) { null }
            val xErr = body?.getLong("XErr", 0L) ?: 0L
            val message = when (xErr) {
                2148916233L -> "This Microsoft account does not have an Xbox account."
                2148916235L -> "Xbox Live is not available in this country."
                2148916236L, 2148916237L -> "The account requires adult verification (South Korea)."
                2148916238L -> "This is a child account. An adult needs to add it to a family."
                else -> "XSTS authentication failed: ${response.statusCode()} (XErr: $xErr) Body: $bodyStr"
            }
            throw RuntimeException(message)
        }

        val body = response.bodyAsJsonObject()
        val token = body.getString("Token")
        val uhs = body.getJsonObject("DisplayClaims")
            .getJsonArray("xui")
            .getJsonObject(0)
            .getString("uhs")

        return XboxAuthResult(token, uhs)
    }

    private suspend fun authenticateWithMinecraft(userHash: String, xstsToken: String): String {
        val identityToken = "XBL3.0 x=$userHash;$xstsToken"

        val requestBody = JsonObject()
            .put("identityToken", identityToken)

        val response = webClient
            .postAbs(MINECRAFT_AUTH_URL)
            .putHeader("Content-Type", "application/json")
            .putHeader("Accept", "application/json")
            .sendJsonObject(requestBody)
            .coAwait()

        if (response.statusCode() != 200) {
            val bodyStr = response.bodyAsString() ?: "no body"
            throw RuntimeException("Minecraft authentication failed: ${response.statusCode()} - $bodyStr")
        }

        val body = response.bodyAsJsonObject()
        return body.getString("access_token")
    }

    private suspend fun getMinecraftProfile(minecraftToken: String): MinecraftProfile {
        val response = webClient
            .getAbs(MINECRAFT_PROFILE_URL)
            .putHeader("Authorization", "Bearer $minecraftToken")
            .send()
            .coAwait()

        if (response.statusCode() == 404) {
            throw RuntimeException("This Microsoft account does not own Minecraft.")
        }

        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to get Minecraft profile: ${response.statusCode()}")
        }

        val body = response.bodyAsJsonObject()
        val rawUuid = body.getString("id") // UUID without hyphens
        val uuid = rawUuid.replaceFirst(
            Regex("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})"),
            "$1-$2-$3-$4-$5"
        )
        val username = body.getString("name")

        return MinecraftProfile(uuid, username)
    }

    /**
     * Fetch the Microsoft account email using the Graph API.
     * Requires User.Read scope.
     */
    private suspend fun getMicrosoftEmail(microsoftToken: String): String? {
        val response = webClient
            .getAbs(MICROSOFT_GRAPH_ME_URL)
            .putHeader("Authorization", "Bearer $microsoftToken")
            .send()
            .coAwait()

        if (response.statusCode() != 200) {
            logger.warn("Graph API /me failed: ${response.statusCode()} - ${response.bodyAsString()}")
            return null
        }

        val body = response.bodyAsJsonObject()
        // Try 'mail' first, then 'userPrincipalName' as fallback
        return body.getString("mail")
            ?: body.getString("userPrincipalName")?.takeIf { it.contains("@") }
    }
}
