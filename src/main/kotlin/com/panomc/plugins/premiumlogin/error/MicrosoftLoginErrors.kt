package com.panomc.plugins.premiumlogin.error

import com.panomc.platform.model.Error

/** `error.message` of the answer: the English text a caller may show; omitted when null. */
private fun messageExtras(message: String?): Map<String, Any?> =
    if (message == null) mapOf() else mapOf("message" to message)

class MicrosoftLoginNotEnabled(message: String? = null, statusCode: Int = 409) :
    Error("MICROSOFT_LOGIN_NOT_ENABLED", statusCode, extras = messageExtras(message))

class MicrosoftLoginFailed(message: String? = null, statusCode: Int = 400) :
    Error("MICROSOFT_LOGIN_FAILED", statusCode, extras = messageExtras(message))

class MinecraftAccountNotFound : Error("MINECRAFT_ACCOUNT_NOT_FOUND", 404)

class MinecraftAccountAlreadyLinked(message: String? = null, statusCode: Int = 409) :
    Error("MINECRAFT_ACCOUNT_ALREADY_LINKED", statusCode, extras = messageExtras(message))

class MinecraftAccountNotLinked : Error("MINECRAFT_ACCOUNT_NOT_LINKED", 409)

/** Staff set this player's account type in the panel; the player cannot change it themselves. */
class PremiumStatusSetByStaff : Error("PREMIUM_STATUS_SET_BY_STAFF", 403)
