package com.panomc.plugins.premiumlogin.service

import com.panomc.platform.access.AccessContext
import com.panomc.platform.access.FrontendKeyRef
import io.vertx.core.http.Cookie
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> proxy(crossinline handler: (String, Array<Any?>) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        val result = handler(method.name, args ?: arrayOf())
        when {
            result != null || !method.returnType.isPrimitive -> result
            method.returnType == java.lang.Boolean.TYPE -> false
            method.returnType == Integer.TYPE -> 0
            else -> 0L
        }
    } as T

/** The Microsoft OAuth state only finishes in the browser (or stored-key front-end) that started it, once. */
class MicrosoftFlowStoreTest {

    /** A minimal request: the cookies it sends, the cookies its response sets, and an optional front-end key. */
    private class FakeExchange(
        val sentCookies: MutableMap<String, String> = mutableMapOf(),
        val key: FrontendKeyRef? = null
    ) {
        val setCookies = mutableListOf<Cookie>()

        val request: HttpServerRequest = proxy { name, args ->
            when (name) {
                "getCookie" -> sentCookies[args[0] as String]?.let { Cookie.cookie(args[0] as String, it) }
                "isSSL" -> false
                "getHeader" -> null
                else -> null
            }
        }

        val response: HttpServerResponse = proxy { name, args ->
            if (name == "addCookie") setCookies += args[0] as Cookie
            null
        }

        val context: RoutingContext = proxy { name, args ->
            when (name) {
                "request" -> request
                "response" -> response
                "get" -> if (args[0] == AccessContext.CONTEXT_KEY && key != null) AccessContext(key, "127.0.0.1") else null
                else -> null
            }
        }

        /** The browser keeps what the response set. */
        fun browserAfter(): FakeExchange = FakeExchange(setCookies.associate { it.name to it.value }.toMutableMap())
    }

    @Test
    fun `the starting browser finishes its own flow once`() {
        val store = MicrosoftFlowStore()
        val start = FakeExchange()
        val state = store.create("link", 7L, start.context)

        val cookie = start.setCookies.single()
        assertEquals(MicrosoftFlowStore.COOKIE_NAME, cookie.name)
        assertEquals(true, cookie.isHttpOnly)

        val flow = store.consume(state, start.browserAfter().context)
        assertNotNull(flow)
        assertEquals("link", flow!!.action)
        assertEquals(7L, flow.userId)

        assertNull(store.consume(state, start.browserAfter().context), "a state is single-use")
    }

    @Test
    fun `another browser cannot finish someone else's flow`() {
        val store = MicrosoftFlowStore()
        val attacker = FakeExchange()
        val state = store.create("link", 1L, attacker.context)

        val victim = FakeExchange(mutableMapOf(MicrosoftFlowStore.COOKIE_NAME to "something-else"))
        assertNull(store.consume(state, victim.context))
        assertNull(store.consume(state, FakeExchange().context))
    }

    @Test
    fun `an unknown, forged or expired state is refused`() {
        var now = 1_000L
        val store = MicrosoftFlowStore { now }
        val start = FakeExchange()
        assertNull(store.consume("bGluaw==", start.context))

        val state = store.create("login", null, start.context)
        now += MicrosoftFlowStore.TTL_MS + 1
        assertNull(store.consume(state, start.browserAfter().context))
    }

    @Test
    fun `a stored-key flow is finished only by a stored-key request`() {
        val store = MicrosoftFlowStore()
        val key = FrontendKeyRef(5L, "headless")
        val start = FakeExchange(key = key)
        val state = store.create("login", null, start.context)
        assertEquals(0, start.setCookies.size)

        assertNull(store.consume(state, FakeExchange().context))

        val state2 = store.create("login", null, start.context)
        assertNotNull(store.consume(state2, FakeExchange(key = key).context))
    }
}
