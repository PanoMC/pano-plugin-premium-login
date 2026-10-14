package com.panomc.plugins.premiumlogin.mail

import com.panomc.platform.mail.MailManager
import com.panomc.platform.mail.TemplateMail

/**
 * Tells a user that a Minecraft premium account claimed their username: template `username-change` of this plugin
 * (`mail-templates/username-change/default/<locale>.hbs`).
 */
class UsernameChangeMail(
    private val oldUsername: String,
    private val newMcUsername: String,
    private val setUsernameLink: String,
    private val expireMinutes: Long = 15
) : TemplateMail(OWNER, KEY) {
    override suspend fun variables(locale: String, system: MailManager.Companion.SystemParameters): Map<String, Any?> =
        mapOf(
            "oldUsername" to oldUsername,
            "newMcUsername" to newMcUsername,
            "setUsernameLink" to setUsernameLink,
            "expireMinutes" to expireMinutes
        )

    companion object {
        const val OWNER = "pano-plugin-premium-login"
        const val KEY = "username-change"
    }
}
