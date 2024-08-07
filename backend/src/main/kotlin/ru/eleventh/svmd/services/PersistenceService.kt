package ru.eleventh.svmd.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.sql.*
import ru.eleventh.svmd.Config
import ru.eleventh.svmd.DatabaseConnection.dbQuery
import ru.eleventh.svmd.model.db.*
import ru.eleventh.svmd.model.enums.Lang
import ru.eleventh.svmd.model.enums.Theme
import ru.eleventh.svmd.model.enums.TileProvider
import java.time.Instant

val dao = PersistenceService()

class PersistenceService {

    private val mapper = jacksonObjectMapper()

    private fun toMap(row: ResultRow): MapMeta = MapMeta(
        identifier = row[MapsTable.identifier],
        center = row[MapsTable.center]?.let { mapper.readValue(it) },
        bounds = row[MapsTable.bounds]?.let { mapper.readValue(it) },
        title = row[MapsTable.title],
        createdAt = row[MapsTable.createdAt],
        lang = Lang.values().find { it.name == row[MapsTable.lang] },
        logo = row[MapsTable.logo],
        link = row[MapsTable.link],
        defaultColor = row[MapsTable.defaultColor],
        theme = Theme.values().find { it.name == row[MapsTable.theme] },
        owner = row[MapsTable.owner],
        tileProvider = TileProvider.values().find { it.name == row[MapsTable.tileProvider] },
    )

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UsersTable.id],
            email = row[UsersTable.email],
            password = row[UsersTable.password]
        )
    }

    fun createMap(userId: Long, newIdentifier: String, newMap: NewMap): String {
        val insertStatement = dbQuery {
            MapsTable.insert {
                it[identifier] = newIdentifier
                it[spreadsheetId] = newMap.spreadsheetId
                it[createdAt] = Instant.now()
                it[accessed] = 0
                it[svmdVersion] = Config.version
                it[owner] = userId
            }
        }
        return insertStatement.resultedValues?.single()!!.let(this::toMap).identifier
    }

    fun getMapsByUser(userId: Long): List<MapMeta> = dbQuery {
        MapsTable.select { MapsTable.owner eq userId }.map(this::toMap)
    }

    fun getMap(identifier: String): MapMeta? = dbQuery {
        val result = MapsTable
            .select { MapsTable.identifier eq identifier }
            .singleOrNull()

        if (result != null) {
            MapsTable.update({ MapsTable.identifier eq result[MapsTable.identifier] }) {
                it[accessed] = result[accessed] + 1
                it[accessedAt] = Instant.now()
            }
        }

        result?.let { toMap(it) }
    }

    fun getSpreadsheetId(identifier: String): String? = dbQuery {
        MapsTable
            .select { MapsTable.identifier eq identifier }
            .map { row -> row[MapsTable.spreadsheetId] }
            .singleOrNull()
    }

    fun updateMap(map: MapMeta): Int = dbQuery {
        MapsTable.update({ MapsTable.identifier eq map.identifier }) {
            it[title] = map.title
            it[center] = mapper.writeValueAsString(map.center)
            it[bounds] = mapper.writeValueAsString(map.bounds)
            it[lang] = map.lang?.name
            it[logo] = map.logo
            it[link] = map.link
            it[defaultColor] = map.defaultColor
            it[theme] = map.theme?.name
            it[tileProvider] = map.tileProvider?.name
        }
    }

    fun createUser(newUser: NewUser): Long? {
        val insertStatement = dbQuery {
            UsersTable.insert { it[email] = newUser.email }
        }
        return insertStatement.resultedValues?.singleOrNull()?.let(this::toUser)?.id
    }

    fun getUsers(): List<User> = dbQuery {
        UsersTable.selectAll().map(this::toUser)
    }

    fun getUser(id: Long): User? = dbQuery {
        UsersTable
            .select { UsersTable.id eq id }
            .map(this::toUser)
            .singleOrNull()
    }

    fun getUserByEmail(email: String): User? = dbQuery {
        UsersTable
            .select { UsersTable.email eq email }
            .map(this::toUser)
            .singleOrNull()
    }

    fun updateUser(user: User): Int = dbQuery {
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[email] = user.email
        }
    }
}
