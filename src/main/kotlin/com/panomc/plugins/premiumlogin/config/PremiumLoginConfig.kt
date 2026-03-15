package com.panomc.plugins.premiumlogin.config

import com.panomc.platform.api.config.PluginConfig

class PremiumLoginConfig(
    val defaultAccountType: AccountType = AccountType.AUTO,
    version: Int = 1
) : PluginConfig(version)
