package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformMessageResponse

/**
 * Response from Pano confirming a premium status update.
 * Response name: UPDATE_PLAYER_PREMIUM_STATUS (strips "Message" from class name)
 */
data class UpdatePlayerPremiumStatusMessage(
    val success: Boolean = true,
    val error: String? = null
) : PlatformMessageResponse
