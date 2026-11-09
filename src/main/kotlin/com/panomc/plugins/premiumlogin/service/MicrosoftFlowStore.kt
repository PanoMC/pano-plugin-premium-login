package com.panomc.plugins.premiumlogin.service

import com.panomc.platform.access.AccessContext
import io.vertx.core.http.Cookie
import io.vertx.core.http.CookieSameSite
import io.vertx.ext.web.RoutingContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * The OAuth `state` of a Microsoft round trip: a random one-time token kept server-side together with
 * what the flow is for (`login`, `register`, `link`), the user who started a link, and the hash of a
 * random value held in an HttpOnly cookie of the browser that started it.
 *
 * [MicrosoftProcessAPI][com.panomc.plugins.premiumlogin.routes.api.MicrosoftProcessAPI] only runs a flow
 * whose state it [consume]s here, from the same browser (cookie) and, for a link, for the same user. So
 * an authorization code obtained by someone else cannot be replayed in another person's browser.
 *
 * Kept in memory: a round trip lasts minutes, and a restart in the middle only means "start again".
 * A front-end server with a stored front-end key cannot hold our cookie, so its flows are bound to the
 * key instead (a flow it started can only be finished by a stored-key request).
 */
class MicrosoftFlowStore(private val clock: () -> Long = System::currentTimeMillis) {

    data class Flow(
        val action: String,
        val userId: Long?,
        /** SHA-256 of the browser cookie value, or null for a flow started with a stored front-end key. */
        val browserHash: String?,
        val expiresAt: Long
    )

    companion object {
        const val COOKIE_NAME = "pano_premium_login_flow"
        const val TTL_MS = 10 * 60 * 1000L
        private const val MAX_FLOWS = 10_000

        private val random = SecureRandom()

        fun randomToken(): String {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun sha256(value: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))

        /** A request that carries a stored front-end key (not the internal key of Pano's own UIs). */
        fun hasStoredKey(context: RoutingContext): Boolean =
            AccessContext.of(context)?.key?.let { !it.isInternal } == true

        private fun isSecure(context: RoutingContext): Boolean {
            val request = context.request()
            if (request.isSSL) return true
            return request.getHeader("X-Forwarded-Proto")
                ?.split(",")?.any { it.trim().equals("https", ignoreCase = true) } == true
        }

        private fun cookie(context: RoutingContext, value: String, maxAgeSeconds: Long): Cookie =
            Cookie.cookie(COOKIE_NAME, value).apply {
                path = "/"
                isHttpOnly = true
                isSecure = isSecure(context)
                sameSite = CookieSameSite.LAX
                maxAge = maxAgeSeconds
            }
    }

    private val flows = ConcurrentHashMap<String, Flow>()

    /** Starts a flow: returns the `state` to send to Microsoft and, for a browser, sets the binding cookie. */
    fun create(action: String, userId: Long?, context: RoutingContext): String {
        val now = clock()
        flows.entries.removeIf { it.value.expiresAt < now }
        if (flows.size >= MAX_FLOWS) {
            flows.entries.minByOrNull { it.value.expiresAt }?.let { flows.remove(it.key) }
        }

        val browserHash = if (hasStoredKey(context)) {
            null
        } else {
            val browserValue = randomToken()
            context.response().addCookie(cookie(context, browserValue, TTL_MS / 1000))
            sha256(browserValue)
        }

        val state = randomToken()
        flows[state] = Flow(action, userId, browserHash, now + TTL_MS)
        return state
    }

    /**
     * Takes the flow of [state] out of the store (one time) and returns it when it is still valid and
     * this request comes from the browser (or stored-key front-end) that started it; null otherwise.
     */
    fun consume(state: String, context: RoutingContext): Flow? {
        val flow = flows.remove(state) ?: return null
        if (flow.expiresAt < clock()) return null

        if (flow.browserHash == null) {
            return if (hasStoredKey(context)) flow else null
        }

        val browserValue = context.request().getCookie(COOKIE_NAME)?.value ?: return null
        if (!MessageDigest.isEqual(sha256(browserValue).toByteArray(), flow.browserHash.toByteArray())) return null

        context.response().addCookie(cookie(context, "", 0))
        return flow
    }
}
