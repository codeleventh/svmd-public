package ru.eleventh.svmd.model.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object InvitesTable : Table("invites") {
    val id = long("id").autoIncrement()
    val code = uuid("code").uniqueIndex()
    val issuedAt = timestamp("issued_at")
    val usedAt = timestamp("used_at").nullable()
    val isUsed = bool("is_used").default(false).index()
    val usedBy = reference("used_by", UsersTable.id).nullable()

    override val primaryKey = PrimaryKey(id)
}