package com.panomc.plugins.premiumlogin.service

import com.panomc.plugins.premiumlogin.config.AccountType
import com.panomc.plugins.premiumlogin.config.StatusSource
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.sqlclient.SqlClient

/** What unlinking a Microsoft account does to the premium status row of the Minecraft name that was linked. */
object PremiumUnlink {

    /**
     * The row after an unlink: no longer linked, and a PREMIUM type that only came from the Microsoft link
     * goes back to [defaultAccountType]. A type staff or the player chose themselves stays.
     */
    fun unlinkedStatus(existing: PlayerPremiumStatus, defaultAccountType: AccountType, now: Long): PlayerPremiumStatus {
        val fromLink = existing.source == StatusSource.MICROSOFT.name

        return PlayerPremiumStatus(
            playerUuid = existing.playerUuid,
            playerName = existing.playerName,
            accountType = if (fromLink) defaultAccountType.name else existing.accountType,
            source = existing.source,
            premiumLinked = false,
            lastUpdated = now
        )
    }

    /**
     * Clears the link on the status row. Link and sign-in store that row under the Minecraft profile, so it is
     * found by the premium UUID that was linked ([linkedMcUuid], read before it is reset), and by [panoUsername]
     * only when no row has that UUID.
     */
    suspend fun clearLink(
        dao: PlayerPremiumStatusDao,
        linkedMcUuid: String?,
        panoUsername: String,
        defaultAccountType: AccountType,
        sqlClient: SqlClient
    ) {
        val existing = linkedMcUuid?.takeIf { it.isNotBlank() }?.let { dao.getByPlayerUuid(it, sqlClient) }
            ?: dao.getByPlayerName(panoUsername, sqlClient)
            ?: return

        dao.upsertByPlayerName(unlinkedStatus(existing, defaultAccountType, System.currentTimeMillis()), sqlClient)
    }
}
