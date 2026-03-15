package com.panomc.plugins.premiumlogin.event.server

import com.panomc.platform.server.ServerEventRequest

data class GetPlayerPremiumStatusEventRequest(
    val playerName: String = ""
) : ServerEventRequest()
