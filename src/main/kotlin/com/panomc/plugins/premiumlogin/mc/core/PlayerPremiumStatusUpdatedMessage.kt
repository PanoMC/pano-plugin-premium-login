package com.panomc.plugins.premiumlogin.mc.core

import com.panomc.plugins.pano.core.platform.PlatformMessage

/**
 * Push message data class for PLAYER_PREMIUM_STATUS_UPDATED events.
 *
 * This is sent from the Pano backend to connected MC servers when an admin
 * changes a player's premium status via the panel.
 *
 * Message name derivation:
 *   "PlayerPremiumStatusUpdatedMessage" → remove "Message" → "PlayerPremiumStatusUpdated"
 *   → convertToSnakeCase → "PLAYER_PREMIUM_STATUS_UPDATED"
 */
data class PlayerPremiumStatusUpdatedMessage(
    val playerName: String = "",
    val accountType: String = ""
) : PlatformMessage
