package com.panomc.plugins.premiumlogin.mail

import com.panomc.platform.db.model.Translation.Companion.TranslationType
import com.panomc.platform.i18n.I18nManager
import com.panomc.platform.mail.Mail
import com.panomc.platform.mail.MailManager
import com.panomc.platform.mail.MailParameters

class UsernameChangeMail(
    private val oldUsername: String,
    private val newMcUsername: String,
    private val setUsernameLink: String
) : Mail {
    override val templatePath = "mail/username-change.hbs"
    override val subject = "mail.username-change.subject"

    override fun getTemplateContent(): String {
        // Use the plugin's classloader to find the template
        val resourceStream = UsernameChangeMail::class.java.classLoader.getResourceAsStream(templatePath)
            ?: throw IllegalStateException("Template not found: $templatePath")
        return resourceStream.bufferedReader().use { it.readText() }
    }

    override suspend fun generateParameters(
        systemParameters: MailManager.Companion.SystemParameters,
        i18nManager: I18nManager,
        locale: String
    ): MailParameters {
        val translations = getTranslations(i18nManager, locale, "mail.username-change")

        return UsernameChangeMailParameters(
            oldUsername = oldUsername,
            newMcUsername = newMcUsername,
            setUsernameLink = setUsernameLink,
            translation = translations
        )
    }

    override suspend fun getTranslations(
        i18nManager: I18nManager,
        locale: String,
        groupPrefix: String,
        variables: Map<String, Map<String, Any?>>
    ): Map<String, String> {
        // Use PLUGIN translation type for plugin-specific mail translations
        val allTranslations = i18nManager.getTranslations(TranslationType.PLUGIN, locale)

        return allTranslations.keys
            .filter { it.startsWith("$groupPrefix.") }
            .associate { fullKey ->
                val resultKey = fullKey.removePrefix("$groupPrefix.")
                val keyVariables = variables[resultKey] ?: emptyMap()
                val translation = i18nManager.translate(TranslationType.PLUGIN, locale, fullKey, keyVariables)!!
                resultKey to translation
            }
    }

    companion object {
        data class UsernameChangeMailParameters(
            val oldUsername: String,
            val newMcUsername: String,
            val setUsernameLink: String,
            val translation: Map<String, Any?>
        ) : MailParameters
    }
}
