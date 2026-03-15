package com.panomc.plugins.premiumlogin.db.dao

import com.panomc.platform.db.Dao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.sqlclient.SqlClient

abstract class PlayerPremiumStatusDao : Dao<PlayerPremiumStatus>(PlayerPremiumStatus::class.java) {
    abstract suspend fun getByPlayerName(playerName: String, sqlClient: SqlClient): PlayerPremiumStatus?
    abstract suspend fun getByPlayerUuid(playerUuid: String, sqlClient: SqlClient): PlayerPremiumStatus?
    abstract suspend fun upsertByPlayerName(status: PlayerPremiumStatus, sqlClient: SqlClient)
    abstract suspend fun deleteByPlayerName(playerName: String, sqlClient: SqlClient)
    abstract suspend fun count(sqlClient: SqlClient): Long
    abstract suspend fun getAll(
        page: Long,
        pageSize: Long,
        sqlClient: SqlClient
    ): List<PlayerPremiumStatus>

    abstract suspend fun search(
        query: String,
        page: Long,
        pageSize: Long,
        sqlClient: SqlClient
    ): List<PlayerPremiumStatus>

    abstract suspend fun searchCount(query: String, sqlClient: SqlClient): Long
}
