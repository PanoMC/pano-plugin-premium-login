package com.panomc.plugins.premiumlogin.db.impl

import com.panomc.platform.annotation.Dao
import com.panomc.plugins.premiumlogin.db.dao.PlayerPremiumStatusDao
import com.panomc.plugins.premiumlogin.db.model.PlayerPremiumStatus
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope

@Dao
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PlayerPremiumStatusDaoImpl : PlayerPremiumStatusDao() {

    override suspend fun init(sqlClient: SqlClient) {
        sqlClient
            .query(
                """
                    CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                      `id` bigint NOT NULL AUTO_INCREMENT,
                      `playerUuid` VARCHAR(36) NOT NULL DEFAULT '',
                      `playerName` VARCHAR(64) NOT NULL,
                      `accountType` VARCHAR(16) NOT NULL DEFAULT 'AUTO',
                      `source` VARCHAR(16) NOT NULL DEFAULT 'PANEL',
                      `lastUpdated` BIGINT(20) NOT NULL,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `unique_player_name` (`playerName`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Player premium/cracked authentication status.';
                """
            )
            .execute()
            .coAwait()
    }

    override suspend fun getByPlayerName(playerName: String, sqlClient: SqlClient): PlayerPremiumStatus? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `playerName` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(playerName)).coAwait()
        return rows.toEntities().getOrNull(0)
    }

    override suspend fun getByPlayerUuid(playerUuid: String, sqlClient: SqlClient): PlayerPremiumStatus? {
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `playerUuid` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(playerUuid)).coAwait()
        return rows.toEntities().getOrNull(0)
    }

    override suspend fun upsertByPlayerName(status: PlayerPremiumStatus, sqlClient: SqlClient) {
        val query = """
            INSERT INTO `${getTablePrefix() + tableName}` (`playerUuid`, `playerName`, `accountType`, `source`, `lastUpdated`)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                `playerUuid` = VALUES(`playerUuid`),
                `accountType` = VALUES(`accountType`),
                `source` = VALUES(`source`),
                `lastUpdated` = VALUES(`lastUpdated`)
        """.trimIndent()
        sqlClient.preparedQuery(query)
            .execute(
                Tuple.of(
                    status.playerUuid,
                    status.playerName,
                    status.accountType,
                    status.source,
                    status.lastUpdated
                )
            )
            .coAwait()
    }

    override suspend fun deleteByPlayerName(playerName: String, sqlClient: SqlClient) {
        val query = "DELETE FROM `${getTablePrefix() + tableName}` WHERE `playerName` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(playerName)).coAwait()
    }

    override suspend fun count(sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(*) as `count` FROM `${getTablePrefix() + tableName}`"
        val rows = sqlClient.query(query).execute().coAwait()
        return rows.first().getLong("count")
    }

    override suspend fun getAll(page: Long, pageSize: Long, sqlClient: SqlClient): List<PlayerPremiumStatus> {
        val offset = (page - 1) * pageSize
        val query =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` ORDER BY `lastUpdated` DESC LIMIT ? OFFSET ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(pageSize, offset)).coAwait()
        return rows.toEntities()
    }

    override suspend fun search(
        query: String,
        page: Long,
        pageSize: Long,
        sqlClient: SqlClient
    ): List<PlayerPremiumStatus> {
        val offset = (page - 1) * pageSize
        val sql =
            "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `playerName` LIKE ? ORDER BY `lastUpdated` DESC LIMIT ? OFFSET ?"
        val rows = sqlClient.preparedQuery(sql).execute(Tuple.of("%$query%", pageSize, offset)).coAwait()
        return rows.toEntities()
    }

    override suspend fun searchCount(query: String, sqlClient: SqlClient): Long {
        val sql =
            "SELECT COUNT(*) as `count` FROM `${getTablePrefix() + tableName}` WHERE `playerName` LIKE ?"
        val rows = sqlClient.preparedQuery(sql).execute(Tuple.of("%$query%")).coAwait()
        return rows.first().getLong("count")
    }

    override suspend fun uninstall(sqlClient: SqlClient) {
        sqlClient.query("DROP TABLE IF EXISTS `${getTablePrefix() + tableName}`").execute().coAwait()
    }
}
