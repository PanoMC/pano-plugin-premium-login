package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.server.ServerEventRequest

data class UpdatePlayerPremiumStatusEventRequest(
    val playerName: String = "",
    val playerUuid: String = "",
    val accountType: String = "",
    val source: String = "SERVER"
) : ServerEventRequest()
