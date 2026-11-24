package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.config.ConfigManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.api.event.AuthEventListener
import com.panomc.platform.error.BadRequest
import com.panomc.platform.error.IpIsBanned
import com.panomc.platform.error.LoginUserIsBanned
import com.panomc.platform.error.UsernameRequired
import com.panomc.platform.util.BanUtil
import com.panomc.platform.error.NotLoggedIn
import com.panomc.platform.frontend.FrontendUrlMap
import com.panomc.platform.mail.MailManager
import com.panomc.platform.model.*
import com.panomc.platform.schema.CoreSchemas
import com.panomc.platform.schema.EndpointDoc
import com.panomc.platform.token.TokenProvider
import com.panomc.platform.token.SetUsernameTokenType
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import com.panomc.plugins.premiumlogin.error.MicrosoftLoginFailed
import com.panomc.plugins.premiumlogin.error.MicrosoftLoginNotEnabled
import com.panomc.plugins.premiumlogin.error.MinecraftAccountAlreadyLinked
import com.panomc.plugins.premiumlogin.mail.UsernameChangeMail
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Bodies
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import io.vertx.kotlin.coroutines.coAwait
import java.util.*

/**
 * POST endpoint called from the frontend callback page.
 * Does the actual heavy OAuth exchange + scenario routing and returns JSON.
 */
@Endpoint
class MicrosoftProcessAPI(
    private val plugin: PremiumLoginPlugin,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : Api() {
    override val paths = listOf(Path("/microsoft/process", RouteType.POST))

    override val doc = EndpointDoc(
        summary = "Finishes a Microsoft sign-in: exchanges the code from the callback for a Minecraft profile and signs in, registers or links the account.",
        tag = "microsoft",
        response = CoreSchemas.sessionFields(
            objectSchema()
                .requiredProperty("action", stringSchema())
                .optionalProperty("mcUsername", stringSchema())
                .optionalProperty("pendingToken", stringSchema())
        ),
        errors = listOf(BadRequest::class, NotLoggedIn::class, IpIsBanned::class, LoginUserIsBanned::class, UsernameRequired::class, MicrosoftLoginNotEnabled::class, MicrosoftLoginFailed::class, MinecraftAccountAlreadyLinked::class)
    )

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>
    }

    private val oauthService by lazy {
        plugin.pluginBeanContext.getBean(com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val flowStore by lazy {
        plugin.pluginBeanContext.getBean(com.panomc.plugins.premiumlogin.service.MicrosoftFlowStore::class.java)
    }

    private val tokenProvider by lazy {
        plugin.applicationContext.getBean(TokenProvider::class.java)
    }

    private val mailManager by lazy {
        plugin.applicationContext.getBean(MailManager::class.java)
    }

    private val platformConfigManager by lazy {
        plugin.applicationContext.getBean(ConfigManager::class.java)
    }

    private val frontendUrlMap by lazy {
        plugin.applicationContext.getBean(FrontendUrlMap::class.java)
    }

    private fun debugLog(msg: String) {
        if (configManager.config.debugLogging) {
            plugin.logger.info(msg)
        }
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty("code", stringSchema())
                        .requiredProperty("state", stringSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val body = try {
            context.body().asJsonObject()
        } catch (_: Exception) {
            null
        }
        val code = try {
            body?.getString("code")
        } catch (_: Exception) {
            null
        }
        val stateRaw = try {
            body?.getString("state")
        } catch (_: Exception) {
            null
        }

        if (code.isNullOrBlank() || stateRaw.isNullOrBlank()) {
            throw BadRequest()
        }

        if (!configManager.config.microsoftLoginEnabled) {
            throw MicrosoftLoginNotEnabled(statusCode = 400)
        }

        // The state must be one this site handed out, to this browser, and not used yet. An unknown,
        // expired or foreign state stops here, before the code is redeemed.
        val flow = flowStore.consume(stateRaw, context)
            ?: throw MicrosoftLoginFailed("STATE_INVALID", 400)
        val action = flow.action

        // Exchange code for Minecraft profile
        val mcProfile = try {
            oauthService.exchangeCodeForMinecraftProfile(code)
        } catch (e: com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService.MicrosoftAuthException) {
            plugin.logger.error("Microsoft OAuth failed: ${e.message}", e)
            throw MicrosoftLoginFailed(e.reason, 400)
        } catch (e: Exception) {
            // The text may hold a provider's raw answer: log it, never return it.
            plugin.logger.error("Microsoft OAuth failed: ${e.message}", e)
            throw MicrosoftLoginFailed(statusCode = 400)
        }

        val sqlClient = getSqlClient()

        // Upsert premium status in plugin's own table
        val status = PlayerPremiumStatus(
            playerUuid = mcProfile.uuid,
            playerName = mcProfile.username,
            accountType = AccountType.PREMIUM.name,
            source = StatusSource.MICROSOFT.name,
            premiumLinked = true,
            lastUpdated = System.currentTimeMillis()
        )
        playerPremiumStatusDao.upsertByPlayerName(status, sqlClient)

        // === LINK ACTION: Update existing logged-in user's mcUuid ===
        if (action == "link") {
            return handleLinkAccount(context, flow.userId, mcProfile, sqlClient)
        }

        // === SCENARIO ROUTING ===
        // A banned address signs in nowhere, and gets no new account either (same rule as the password sign-in).
        val activeIpBan = databaseManager.bannedIpDao.getActiveByIp(authProvider.getRemoteIP(context), sqlClient)
        if (activeIpBan != null) {
            throw IpIsBanned(extras = mapOf("reason" to activeIpBan.reason, "bannedUntil" to activeIpBan.bannedUntil))
        }

        debugLog("MC Login: username=${mcProfile.username}, uuid=${mcProfile.uuid}, email=${mcProfile.email}")

        // Check if a user with this premium UUID already exists
        val existingByUuid = databaseManager.userDao.getByMcUuid(mcProfile.uuid, sqlClient)

        if (existingByUuid != null) {
            // SCENARIO C: Existing user with matching premium UUID → direct login
            debugLog("MC Login → SCENARIO C: UUID match found for user '${existingByUuid.username}' (id=${existingByUuid.id})")
            return handleDirectLogin(context, existingByUuid.username, mcProfile.username, sqlClient)
        }

        // Check if a user with this MC username exists (case-insensitive)
        val existingByUsername = findUserByUsernameIgnoreCase(mcProfile.username, sqlClient)

        if (existingByUsername != null) {
            // SCENARIO B: Existing user with same username but different UUID
            debugLog("MC Login → SCENARIO B: Username conflict with '${existingByUsername.username}' (id=${existingByUsername.id}, existingUuid=${existingByUsername.mcUuid})")
            return handleUsernameConflict(context, existingByUsername, mcProfile, sqlClient)
        }

        // SCENARIO A: No existing user → auto-register + auto-login
        debugLog("MC Login → SCENARIO A: No existing user, creating new account for '${mcProfile.username}'")
        return handleNewUserRegister(context, mcProfile, sqlClient)
    }

    private suspend fun findUserByUsernameIgnoreCase(
        username: String,
        sqlClient: io.vertx.sqlclient.SqlClient
    ): com.panomc.platform.db.model.User? {
        val exact = databaseManager.userDao.getByUsername(username, sqlClient)
        if (exact != null) return exact

        val query = "SELECT * FROM `${databaseManager.getTablePrefix()}user` WHERE LOWER(`username`) = LOWER(?)"
        val rows = sqlClient.preparedQuery(query)
            .execute(io.vertx.sqlclient.Tuple.of(username))
            .coAwait()

        if (rows.size() == 0) return null

        val row = rows.toList()[0]
        return com.panomc.platform.db.model.User(
            id = row.getLong("id"),
            username = row.getString("username"),
            email = row.getString("email"),
            registeredIp = row.getString("registeredIp") ?: "",
            registerDate = row.getLong("registerDate"),
            lastLoginDate = row.getLong("lastLoginDate"),
            emailVerified = row.getInteger("emailVerified") == 1,
            banned = row.getInteger("banned") == 1,
            canCreateTicket = row.getInteger("canCreateTicket") == 1,
            lastActivityTime = row.getLong("lastActivityTime"),
            lastPanelActivityTime = row.getLong("lastPanelActivityTime"),
            localeCode = row.getString("localeCode"),
            mcUuid = row.getString("mcUuid")
        )
    }

    /**
     * SCENARIO C: Direct login — user's premium UUID matches an existing account
     */
    private suspend fun handleDirectLogin(
        context: RoutingContext,
        panoUsername: String,
        mcUsername: String,
        sqlClient: io.vertx.sqlclient.SqlClient
    ): Result {
        val user = databaseManager.userDao.getByUsername(panoUsername, sqlClient)
            ?: throw MicrosoftLoginFailed(statusCode = 400)

        if (BanUtil.isBanned(user)) {
            throw LoginUserIsBanned(extras = mapOf("reason" to user.banMessage, "until" to user.bannedUntil))
        }

        // The same pre-login hooks as the password sign-in (two-factor and other checks of plugins). A step-up
        // is finished through the core pending session (POST /api/v1/auth/complete-pending), which runs them again
        // with the user's answer; no session is opened here.
        when (val decision = authProvider.runOnBeforeLogin(user, context, sqlClient)) {
            is AuthEventListener.LoginDecision.Deny -> {
                val pendingToken = authProvider.createPendingSession(user.id, "premium-login:microsoft", sqlClient)

                return Successful(
                    mapOf(
                        "action" to "complete-required",
                        "pendingToken" to pendingToken,
                        "mcUsername" to mcUsername
                    )
                )
            }
            is AuthEventListener.LoginDecision.RequireUsername ->
                throw UsernameRequired(extras = mapOf("userId" to decision.userId))
            else -> { /* proceed */ }
        }

        // A cookie session answers `csrfToken`; a front-end with a stored key gets `sessionToken` + `expiresAt` and no cookies.
        val session = authProvider.issueSession(user.username, context, sqlClient)
        databaseManager.userDao.updateLastLoginDate(user.id, sqlClient)
        authProvider.runOnAfterLogin(databaseManager.userDao.getById(user.id, sqlClient) ?: user, context, sqlClient)

        return Successful(
            mapOf(
                "action" to "login",
                "mcUsername" to mcUsername
            ) + session
        )
    }

    /**
     * SCENARIO B: Username conflict — existing user has same username but different UUID
     */
    private suspend fun handleUsernameConflict(
        context: RoutingContext,
        existingUser: com.panomc.platform.db.model.User,
        mcProfile: com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService.MinecraftProfile,
        sqlClient: io.vertx.sqlclient.SqlClient
    ): Result {
        val oldUsername = existingUser.username
        val shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6)
        val tempUsername = "tmp_$shortUuid"

        // 1. Rename old user to temp-user
        databaseManager.userDao.setUsernameById(existingUser.id, tempUsername, sqlClient)

        // 2. Create new user with MC username + premium UUID + MS email (if available)
        val remoteIP = authProvider.getRemoteIP(context)
        val newUser = com.panomc.platform.db.model.User(
            username = mcProfile.username,
            email = mcProfile.email,
            registeredIp = remoteIP,
            mcUuid = mcProfile.uuid,
            emailVerified = mcProfile.email != null
        )
        databaseManager.userDao.add(newUser, null, sqlClient, false)

        // 3. Send email to old user if they have an email
        if (!existingUser.email.isNullOrEmpty()) {
            val locale = existingUser.localeCode ?: platformConfigManager.config.locale

            tokenProvider.invalidateTokensBySubjectAndType(
                existingUser.id.toString(),
                SetUsernameTokenType,
                sqlClient
            )
            val (token, expireDate) = tokenProvider.generateToken(
                existingUser.id.toString(),
                SetUsernameTokenType
            )
            tokenProvider.saveToken(
                token,
                existingUser.id.toString(),
                SetUsernameTokenType,
                expireDate,
                sqlClient
            )

            // Target `premium-login.set-username` of the front-end URL map (frontend-targets.json).
            val setUsernameLink = frontendUrlMap.url("premium-login.set-username", mapOf("token" to token))
                ?: "${platformConfigManager.config.websiteUrl}/set-username?token=$token"

            val mail = UsernameChangeMail(
                oldUsername = oldUsername,
                newMcUsername = mcProfile.username,
                setUsernameLink = setUsernameLink,
                expireMinutes = ((expireDate - System.currentTimeMillis()).coerceAtLeast(0) + 59_999) / 60_000
            )

            try {
                mailManager.sendTemplate(sqlClient, existingUser.id, mail)
            } catch (e: Exception) {
                plugin.logger.error("Failed to send username change email to ${existingUser.email}: ${e.message}", e)
            }
        }

        // 4. Log in the new MC user
        return handleDirectLogin(context, mcProfile.username, mcProfile.username, sqlClient)
    }

    /**
     * SCENARIO A: No existing user → auto-register + auto-login
     */
    private suspend fun handleNewUserRegister(
        context: RoutingContext,
        mcProfile: com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService.MinecraftProfile,
        sqlClient: io.vertx.sqlclient.SqlClient
    ): Result {
        val remoteIP = authProvider.getRemoteIP(context)

        val newUser = com.panomc.platform.db.model.User(
            username = mcProfile.username,
            email = mcProfile.email,
            registeredIp = remoteIP,
            mcUuid = mcProfile.uuid,
            emailVerified = mcProfile.email != null
        )
        databaseManager.userDao.add(newUser, null, sqlClient, false)

        return handleDirectLogin(context, mcProfile.username, mcProfile.username, sqlClient)
    }

    /**
     * LINK ACTION: A logged-in user links their premium Minecraft account.
     */
    private suspend fun handleLinkAccount(
        context: RoutingContext,
        flowUserId: Long?,
        mcProfile: com.panomc.plugins.premiumlogin.service.MicrosoftOAuthService.MinecraftProfile,
        sqlClient: io.vertx.sqlclient.SqlClient
    ): Result {
        val isLoggedIn = authProvider.isLoggedIn(context)
        if (!isLoggedIn) {
            throw NotLoggedIn()
        }

        val userId = authProvider.getUserIdFromRoutingContext(context)

        // A link only lands on the account that started it.
        if (flowUserId == null || flowUserId != userId) {
            throw MicrosoftLoginFailed("OTHER_ACCOUNT", 400)
        }
        val username = databaseManager.userDao.getUsernameFromUserId(userId, sqlClient)!!

        debugLog("MC Link: user='$username' (id=$userId) linking MC uuid=${mcProfile.uuid}, mcUsername=${mcProfile.username}")

        // Check if this premium UUID is already linked to a different user
        val existingByUuid = databaseManager.userDao.getByMcUuid(mcProfile.uuid, sqlClient)
        if (existingByUuid != null && existingByUuid.id != userId) {
            plugin.logger.warn("MC Link: UUID ${mcProfile.uuid} already linked to user '${existingByUuid.username}' (id=${existingByUuid.id})")
            throw MinecraftAccountAlreadyLinked(statusCode = 409)
        }

        // Update the user's mcUuid
        databaseManager.userDao.setMcUuidById(userId, mcProfile.uuid, sqlClient)

        debugLog("MC Link: Successfully linked MC uuid=${mcProfile.uuid} to user '$username' (id=$userId)")

        return Successful(mapOf(
            "action" to "link",
            "mcUsername" to mcProfile.username
        ))
    }
}
