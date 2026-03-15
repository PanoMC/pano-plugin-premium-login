package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.Server
import com.panomc.platform.server.ServerEvent
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus

/**
 * Handles UPDATE_PLAYER_PREMIUM_STATUS WebSocket events from MC plugins.
 * Upserts the player's premium status in the database.
 *
 * Event name: UPDATE_PLAYER_PREMIUM_STATUS
 */
class UpdatePlayerPremiumStatusEvent(
    private val databaseManager: DatabaseManager,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : ServerEvent<UpdatePlayerPremiumStatusEventRequest, UpdatePlayerPremiumStatusEventResponse>() {

    override suspend fun handle(
        request: UpdatePlayerPremiumStatusEventRequest,
        server: Server
    ): UpdatePlayerPremiumStatusEventResponse {
        return try {
            val accountType = try {
                AccountType.valueOf(request.accountType)
            } catch (_: Exception) {
                return UpdatePlayerPremiumStatusEventResponse(
                    success = false,
                    error = "Invalid account type: ${request.accountType}"
                )
            }

            val source = try {
                StatusSource.valueOf(request.source)
            } catch (_: Exception) {
                StatusSource.FASTLOGIN
            }

            val sqlClient = databaseManager.getSqlClient()

            val status = PlayerPremiumStatus(
                playerUuid = request.playerUuid,
                playerName = request.playerName,
                accountType = accountType.name,
                source = source.name,
                lastUpdated = System.currentTimeMillis()
            )

            playerPremiumStatusDao.upsertByPlayerName(status, sqlClient)

            UpdatePlayerPremiumStatusEventResponse(success = true)
        } catch (e: Exception) {
            UpdatePlayerPremiumStatusEventResponse(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
