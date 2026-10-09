package com.panomc.plugins.premiumlogin.config.migration

import com.panomc.platform.annotation.Migration
import com.panomc.platform.api.config.PluginConfigMigration
import com.panomc.platform.route.ApiPaths
import io.vertx.core.json.JsonObject

/**
 * The Microsoft callback moved with the /api/v1 cutover (api/paths.old-new.json); the stored redirect URI follows it.
 * The same URL must also be registered on the Microsoft app: the old one answers nothing any more.
 */
@Migration
class PremiumLoginConfigMigration1to2 : PluginConfigMigration(
    1,
    2,
    "Microsoft redirect URI moves to the /api/v1 plugin path"
) {
    override fun migrate(config: JsonObject) {
        val redirectUri = config.getString("microsoftRedirectUri") ?: return

        config.put("microsoftRedirectUri", redirectUri.replace(OLD_CALLBACK, NEW_CALLBACK))
    }

    private companion object {
        // /api/premium-login/microsoft/callback -> /api/plugins/pano-plugin-premium-login/microsoft/callback
        val OLD_CALLBACK = ApiPaths.BASE + "/premium-login/microsoft/callback"
        val NEW_CALLBACK = ApiPaths.plugin("pano-plugin-premium-login", "/microsoft/callback")
    }
}
