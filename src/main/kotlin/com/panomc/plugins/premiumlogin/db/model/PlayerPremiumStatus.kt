package com.panomc.plugins.premiumlogin.db.model

import com.panomc.platform.db.DBEntity

open class PlayerPremiumStatus(
    var id: Long = -1,
    val playerUuid: String = "",
    val playerName: String = "",
    val accountType: String = "AUTO",
    val source: String = "PANEL",
    val premiumLinked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) : DBEntity()
