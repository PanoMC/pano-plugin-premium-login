package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.Server
import com.panomc.platform.server.ServerEvent
import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao

/**
 * Handles GET_PLAYER_PREMIUM_STATUS WebSocket events from MC plugins.
 * Queries the database for the player's premium status and returns it.
 *
 * Event name: GET_PLAYER_PREMIUM_STATUS (derived from "GetPlayerPremiumStatus" + "Event" → strips "Event")
 */
class GetPlayerPremiumStatusEvent(
    private val databaseManager: DatabaseManager,
    private val playerPremiumStatusDao: PlayerPremiumStatusDao
) : ServerEvent<GetPlayerPremiumStatusEventRequest, GetPlayerPremiumStatusEventResponse>() {

    override suspend fun handle(
        request: GetPlayerPremiumStatusEventRequest,
        server: Server
    ): GetPlayerPremiumStatusEventResponse {
        val sqlClient = databaseManager.getSqlClient()

        val status = playerPremiumStatusDao.getByPlayerName(request.playerName, sqlClient)

        return if (status != null) {
            GetPlayerPremiumStatusEventResponse(
                playerName = status.playerName,
                playerUuid = status.playerUuid,
                accountType = status.accountType,
                source = status.source,
                lastUpdated = status.lastUpdated
            )
        } else {
            GetPlayerPremiumStatusEventResponse(
                playerName = request.playerName,
                playerUuid = "",
                accountType = AccountType.AUTO.name,
                source = "",
                lastUpdated = 0L
            )
        }
    }
}
