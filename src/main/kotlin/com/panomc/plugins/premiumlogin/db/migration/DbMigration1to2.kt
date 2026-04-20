package com.panomc.plugins.premiumlogin.db.migration

import com.panomc.platform.annotation.Migration
import com.panomc.platform.db.DatabaseMigration
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlClient

@Migration
class DbMigration1to2 : DatabaseMigration(
    1,
    2,
    "Rename activity log types"
) {
    override val handlers: List<suspend (SqlClient) -> Unit> = listOf(
        renameActivityLogTypes()
    )

    private fun renameActivityLogTypes(): suspend (sqlClient: SqlClient) -> Unit =
        { sqlClient: SqlClient ->
            val prefix = getTablePrefix()
            val types = mapOf(
                "UPDATED_PREMIUMIN_SETTINGS" to "UPDATED_PREMIUM_LOGIN_SETTINGS"
            )

            types.forEach { (oldType, newType) ->
                val query = "UPDATE `${prefix}panel_activity_log` SET `type` = '$newType' WHERE `type` = '$oldType'"
                sqlClient.query(query).execute().coAwait()
            }
        }
}
