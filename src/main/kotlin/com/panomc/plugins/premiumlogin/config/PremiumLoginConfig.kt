package com.panomc.plugins.premiumlogin.config

import com.panomc.platform.api.config.PluginConfig

class PremiumLoginConfig(
    val defaultAccountType: AccountType = AccountType.AUTO,
    val microsoftLoginEnabled: Boolean = false,
    val microsoftClientId: String = "",
    val microsoftClientSecret: String = "",
    val microsoftRedirectUri: String = "",
    val debugLogging: Boolean = false,
    version: Int = 1
) : PluginConfig(version)
