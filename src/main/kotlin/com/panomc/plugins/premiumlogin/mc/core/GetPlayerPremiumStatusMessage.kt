package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformMessageResponse

/**
 * Response from Pano containing a player's premium status.
 * Response name: GET_PLAYER_PREMIUM_STATUS (strips "Message" from class name)
 */
data class GetPlayerPremiumStatusMessage(
    val playerName: String = "",
    val playerUuid: String = "",
    val accountType: String = "AUTO",
    val source: String = "",
    val lastUpdated: Long = 0L
) : PlatformMessageResponse
