package com.panomc.plugins.premiumlogin.frontend

import com.panomc.platform.api.config.PluginConfig
import com.panomc.platform.frontend.FallbackPageEnvironment
import com.panomc.platform.frontend.FallbackPageRegistry
import com.panomc.platform.frontend.FrontendTargetsRefusal
import com.panomc.platform.frontend.FrontendUrlInputs
import com.panomc.platform.frontend.FrontendUrlMap
import com.panomc.platform.frontend.ThemeRouteMap
import com.panomc.platform.frontend.pages.FallbackPageRenderer
import com.panomc.platform.route.FallbackPageRoute
import com.panomc.platform.ui.FrontendMode
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.util.concurrent.TimeUnit

/**
 * The Microsoft sign-in on the open front-end (doc 05 sections 10.1 to 10.3): the plugin's `frontend-targets.json` is
 * accepted by the host's URL map, the links follow the targets, the two built-in pages render from the plugin's own
 * templates with the host's route, and the Microsoft redirect URL is listed for the compatibility page.
 */
class MicrosoftFrontendTest {
    private class FakeInputs : FrontendUrlInputs {
        var mode = FrontendMode.NONE
        var frontend: Map<String, Any?> = emptyMap()

        override fun mode() = mode
        override fun siteUrl() = "https://site.example"
        override fun websiteUrl() = "https://pano.example"
        override fun overrides(): Map<String, String> = emptyMap()
        override fun frontendUrls() = frontend
        override fun themeRoutes(): ThemeRouteMap? = null
    }

    private class FakeEnvironment : FallbackPageEnvironment {
        var locale = "en-US"

        override fun ready() = true
        override suspend fun blockedByMaintenance(context: RoutingContext) = false
        override fun siteName() = "My Site"
        override fun locale() = locale
        override fun logoUrl() = "/api/v1/website-logo"
        override fun faviconUrl() = "/api/v1/favicon"
        override fun homeUrl() = "/"
    }

    private val logger = LoggerFactory.getLogger("test")
    private val inputs = FakeInputs()
    private val environment = FakeEnvironment()
    private val urls = FrontendUrlMap(inputs, logger)
    private val registry = FallbackPageRegistry(urls, logger)
    private val route = FallbackPageRoute(registry, urls, FallbackPageRenderer(environment, logger), environment, logger)

    private lateinit var plugin: PremiumLoginPlugin
    private lateinit var vertx: Vertx
    private var port = 0

    private val targetsFile: String
        get() = javaClass.classLoader.getResourceAsStream("frontend-targets.json")!!.use { it.readBytes().toString(Charsets.UTF_8) }

    /** What `PluginManager` does when it loads the plugin: the pages first, then the targets file. */
    private fun loadPlugin() {
        registry.registerPlugin(
            plugin.pluginId,
            NAMESPACE,
            listOf(SetUsernameFallbackPage(plugin), MicrosoftCallbackFallbackPage(plugin)),
            PremiumLoginPlugin::class.java.classLoader
        )

        urls.registerPlugin(plugin.pluginId, NAMESPACE, targetsFile)
    }

    @BeforeEach
    fun setUp() {
        plugin = PremiumLoginPlugin()

        set(plugin, "pluginId", "pano-plugin-premium-login")

        val context = AnnotationConfigApplicationContext()

        context.beanFactory.registerSingleton("environment", environment)
        context.beanFactory.registerSingleton("urls", urls)
        context.refresh()

        set(plugin, "applicationContext", context)

        vertx = Vertx.vertx()

        val router = Router.router(vertx)

        route.paths.forEach { path ->
            router.route(path.routeType.vertxHttpMethod, path.url).order(route.order).handler(route.getHandler())
        }

        port = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1").blockingGet().actualPort()
    }

    @AfterEach
    fun tearDown() {
        vertx.close().toCompletionStage().toCompletableFuture().get(15, TimeUnit.SECONDS)
    }

    private fun set(target: Any, field: String, value: Any) {
        val declared = generateSequence<Class<*>>(target.javaClass) { it.superclass }
            .flatMap { it.declaredFields.asSequence() }
            .first { it.name == field }

        declared.isAccessible = true
        declared.set(target, value)
    }

    private fun <T> Future<T>.blockingGet(): T = toCompletionStage().toCompletableFuture().get(15, TimeUnit.SECONDS)

    private class Answer(val status: Int, val headers: Map<String, String>, val body: String)

    private fun get(path: String): Answer =
        vertx.createHttpClient(HttpClientOptions().setKeepAlive(false)).request(HttpMethod.GET, port, "127.0.0.1", path)
            .compose { it.send() }
            .compose { response ->
                response.body().map { Answer(response.statusCode(), response.headers().entries().associate { e -> e.key.lowercase() to e.value }, it.toString()) }
            }
            .blockingGet()

    // --- the targets file --------------------------------------------------------

    @Test
    fun `the targets file declares set-username and callback with their fallback pages`() {
        loadPlugin()

        val setUsername = urls.targets().first { it.id == "premium-login.set-username" }
        val callback = urls.targets().first { it.id == "premium-login.callback" }

        assertEquals("pano-plugin-premium-login", setUsername.owner)
        assertEquals("/set-username?token={token}", setUsername.defaultPath)
        assertTrue(setUsername.fallback)
        assertFalse(setUsername.createsSession)

        assertEquals("/premium-login/callback", callback.defaultPath)
        assertTrue(callback.fallback)
        assertTrue(callback.createsSession)

        assertTrue(registry.has("premium-login.set-username"))
        assertTrue(registry.has("premium-login.callback"))
    }

    @Test
    fun `a plugin without its fallback pages fails the load, naming the target`() {
        val refusal = assertThrows<FrontendTargetsRefusal> { urls.registerPlugin("pano-plugin-premium-login", NAMESPACE, targetsFile) }

        assertTrue(refusal.message!!.contains("premium-login.set-username"))
    }

    @Test
    fun `unloading the plugin drops both pages`() {
        loadPlugin()

        registry.unregisterPlugin(plugin.pluginId)
        urls.unregisterPlugin(plugin.pluginId)

        assertEquals(404, get("/_pano/premium-login.callback?code=c&state=s").status)
        assertEquals(404, get("/_pano/premium-login.set-username?token=t").status)
    }

    // --- the links ------------------------------------------------------------------

    @Test
    fun `the callback redirect carries code and state to the built-in page, to a theme path or to a front-end`() {
        loadPlugin()

        val params = linkedMapOf("code" to "M.C5_abc", "state" to "bG9naW4")

        assertEquals("https://pano.example/_pano/premium-login.callback?code=M.C5_abc&state=bG9naW4", urls.url("premium-login.callback", params))

        inputs.mode = FrontendMode.THEME

        assertEquals("https://site.example/premium-login/callback?code=M.C5_abc&state=bG9naW4", urls.url("premium-login.callback", params))

        inputs.mode = FrontendMode.CUSTOM_APP
        inputs.frontend = mapOf("premium-login.callback" to "/ms/done")

        assertEquals("https://site.example/ms/done?code=M.C5_abc&state=bG9naW4", urls.url("premium-login.callback", params))
    }

    @Test
    fun `the set-username mail link carries the token`() {
        loadPlugin()

        assertEquals("https://pano.example/_pano/premium-login.set-username?token=tok", urls.url("premium-login.set-username", mapOf("token" to "tok")))

        inputs.mode = FrontendMode.THEME

        assertEquals("https://site.example/set-username?token=tok", urls.url("premium-login.set-username", mapOf("token" to "tok")))
    }

    // --- the pages ------------------------------------------------------------------

    @Test
    fun `the set-username page posts the token and the new name to the plugin`() {
        loadPlugin()

        val answer = get("/_pano/premium-login.set-username?token=abc123")

        assertEquals(200, answer.status)
        assertTrue(answer.body.contains("""data-pano-endpoint="plugin:/set-username""""))
        assertTrue(answer.body.contains("""name="token" value="abc123""""))
        assertTrue(answer.body.contains("""name="newUsername""""))
        assertFalse(answer.body.contains("data-pano-auto"))
        assertTrue(answer.body.contains("Choose a new username"))
        assertTrue(answer.body.contains("""href="https://pano.example/_pano/auth.login""""))
    }

    @Test
    fun `the callback page posts code and state once on load`() {
        loadPlugin()

        val answer = get("/_pano/premium-login.callback?code=M.C5_abc&state=bG9naW4%3D")

        assertEquals(200, answer.status)
        assertTrue(answer.body.contains("""data-pano-endpoint="plugin:/microsoft/process""""))
        assertTrue(answer.body.contains("data-pano-auto"))
        assertTrue(answer.body.contains("""name="code" value="M.C5_abc""""))
        // Handlebars writes "=" as a character reference; the browser reads it back as "=".
        assertTrue(answer.body.contains("""name="state" value="bG9naW4&#x3D;""""))
        assertTrue(answer.body.contains("Signing you in"))
    }

    @Test
    fun `the callback page without a code shows a message instead of posting`() {
        loadPlugin()

        val answer = get("/_pano/premium-login.callback")

        assertEquals(200, answer.status)
        assertFalse(answer.body.contains("data-pano-auto"))
        assertFalse(answer.body.contains("data-pano-endpoint"))
        assertTrue(answer.body.contains("This sign-in link is not valid"))
    }

    @Test
    fun `the pages escape the query and speak the site language`() {
        loadPlugin()

        environment.locale = "tr"

        val answer = get("/_pano/premium-login.callback?code=%22%3E%3Cscript%3E&state=s")

        assertEquals(200, answer.status)
        assertFalse(answer.body.contains("\"><script>"))
        assertTrue(answer.body.contains("Giriş yapılıyor"))
    }

    @Test
    fun `a front-end that claims the callback gets the visitor with code and state`() {
        loadPlugin()

        inputs.frontend = mapOf("premium-login.callback" to "/ms/done")

        val answer = get("/_pano/premium-login.callback?code=c1&state=s1")

        assertEquals(302, answer.status)
        assertEquals("https://site.example/ms/done?code=c1&state=s1", answer.headers["location"])
    }

    private companion object {
        const val NAMESPACE = "premium-login"
    }
}
