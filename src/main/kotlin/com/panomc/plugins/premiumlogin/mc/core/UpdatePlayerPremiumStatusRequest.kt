package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformRequest

/**
 * Request to update a player's premium/cracked status on Pano.
 * Event name: UPDATE_PLAYER_PREMIUM_STATUS
 */
class UpdatePlayerPremiumStatusRequest(
    val playerName: String,
    val playerUuid: String,
    val accountType: String,
    val source: String = "SERVER"
) : PlatformRequest()
