package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.server.ServerEventResponse

/**
 * Response sent back to the MC plugin.
 * Response name derived from class: "GetPlayerPremiumStatus" → GET_PLAYER_PREMIUM_STATUS
 */
data class GetPlayerPremiumStatusEventResponse(
    val playerName: String,
    val playerUuid: String,
    val accountType: String,
    val source: String,
    val lastUpdated: Long
) : ServerEventResponse()
