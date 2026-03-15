package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformRequest

/**
 * Request to query a player's premium/cracked status from Pano.
 * Event name: GET_PLAYER_PREMIUM_STATUS
 */
class GetPlayerPremiumStatusRequest(val playerName: String) : PlatformRequest()
