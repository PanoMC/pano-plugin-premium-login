package com.panomc.plugins.premiumlogin.log

import com.panomc.platform.db.model.PluginActivityLog
import io.vertx.core.json.JsonObject

class UpdatedPlayerPremiumStatusLog(
    userId: Long,
    username: String,
    targetUsername: String,
    newAccountType: String,
    pluginId: String
) : PluginActivityLog(
    userId = userId,
    pluginId = pluginId,
    details = JsonObject()
        .put("username", username)
        .put("targetUsername", targetUsername)
        .put("newAccountType", newAccountType)
)
