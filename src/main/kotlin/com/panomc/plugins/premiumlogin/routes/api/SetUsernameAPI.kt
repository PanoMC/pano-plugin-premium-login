package com.panomc.plugins.premiumlogin.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.*
import com.panomc.platform.model.*
import com.panomc.platform.token.TokenProvider
import com.panomc.platform.token.TokenType
import com.panomc.platform.util.Regexes
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas

@Endpoint
class SetUsernameAPI(
    private val plugin: PremiumLoginPlugin
) : Api() {
    override val paths = listOf(Path("/api/premium-login/set-username", RouteType.POST))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val tokenProvider by lazy {
        plugin.applicationContext.getBean(TokenProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    Schemas.objectSchema()
                        .requiredProperty("token", Schemas.stringSchema())
                        .requiredProperty("newUsername", Schemas.stringSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val parameters = getParameters(context)
        val data = parameters.body().jsonObject

        val token = data.getString("token")
        val newUsername = data.getString("newUsername")

        val sqlClient = getSqlClient()

        // Validate the SET_USERNAME token
        if (!tokenProvider.isTokenValid(token, TokenType.SET_USERNAME, sqlClient)) {
            throw InvalidToken()
        }

        val jwt = tokenProvider.parseToken(token)
        val userId = jwt.subject.toLong()

        val user = databaseManager.userDao.getById(userId, sqlClient) ?: throw InvalidToken()

        // Validate new username
        if (newUsername.isEmpty() || newUsername.length < 3) throw RegisterUsernameTooShort()
        if (newUsername.length > 16) throw RegisterUsernameTooLong()
        if (!newUsername.matches(Regex(Regexes.USERNAME))) throw RegisterInvalidUsername()

        val isUsernameExists = databaseManager.userDao.existsByUsername(newUsername, sqlClient)
        if (isUsernameExists) throw RegisterUsernameNotAvailable()

        // Set the new username
        databaseManager.userDao.setUsernameById(userId, newUsername, sqlClient)

        // Invalidate the token (one-time use)
        tokenProvider.invalidateToken(token, sqlClient)

        return Successful()
    }
}
