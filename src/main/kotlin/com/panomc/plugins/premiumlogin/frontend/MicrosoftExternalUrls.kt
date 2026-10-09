package com.panomc.plugins.premiumlogin.frontend

import com.panomc.platform.api.ExternalUrl
import com.panomc.platform.api.ExternalUrlProvider
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.config.ConfigManager
import com.panomc.platform.route.ApiPaths
import com.panomc.plugins.premiumlogin.PremiumLoginPlugin
import com.panomc.plugins.premiumlogin.config.PremiumLoginConfig
import org.springframework.stereotype.Component

/**
 * The address the admin has registered at Microsoft (the redirect URL of the Azure app). It moved with the
 * `/api/v1` cutover and the old one answers nothing, so the compatibility page of the panel lists it
 * (doc 04 section 7, decision 46).
 */
@Component
class MicrosoftExternalUrls(
    private val plugin: PremiumLoginPlugin
) : ExternalUrlProvider {
    override fun urls(): List<ExternalUrl> {
        @Suppress("UNCHECKED_CAST")
        val config = (plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<PremiumLoginConfig>).config

        return urlsOf(config, plugin.pluginId) { plugin.applicationContext.getBean(ConfigManager::class.java).config.websiteUrl }
    }

    internal companion object {
        const val LABEL = "Microsoft redirect URL"

        /** [websiteUrl] is only asked for when the config names no redirect URI. */
        fun urlsOf(config: PremiumLoginConfig, pluginId: String, websiteUrl: () -> String): List<ExternalUrl> {
            // Nothing is registered at Microsoft while the sign-in is off and has no app.
            if (!config.microsoftLoginEnabled && config.microsoftClientId.isBlank()) {
                return emptyList()
            }

            // What the plugin sends as `redirect_uri` is what Microsoft compares with; without one, the default of the settings page.
            val url = config.microsoftRedirectUri.ifBlank {
                websiteUrl().trimEnd('/') + ApiPaths.plugin(pluginId, "/microsoft/callback")
            }

            return listOf(ExternalUrl(LABEL, url))
        }
    }
}
