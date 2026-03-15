package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.server.ServerEventResponse

/**
 * Response sent back to the MC plugin confirming update.
 * Response name: UPDATE_PLAYER_PREMIUM_STATUS
 */
data class UpdatePlayerPremiumStatusEventResponse(
    val success: Boolean,
    val error: String? = null
) : ServerEventResponse()
