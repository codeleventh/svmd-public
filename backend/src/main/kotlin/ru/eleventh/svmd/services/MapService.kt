package ru.eleventh.svmd.services

import ru.eleventh.svmd.model.db.MapMeta
import ru.eleventh.svmd.model.db.NewMapMeta
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.random.Random

object MapService {

    private const val UPPER_BOUND = 1099511627776 // (2^4)^8
    private const val ZEROS_STR = "00000000"

    private val rand = Random(Instant.now().epochSecond)

    /***
     * Map identifier is an alphanumeric string that consists of 8 characters
     * It generated randomly and there are no collision check for two reasons:
     * 1. Given the planned amount of maps it's nearly impossible
     * 2. It will violate `UNIQUE` constraint anyway — user will just make another try
     */
    private fun newIdentifier(): String {
        val number = (rand.nextLong() % UPPER_BOUND).absoluteValue
        val string = number.toString(32).uppercase()
        return ZEROS_STR.take(8 - string.length) + string
    }

    // TODO: error handlers + ids comparing + spreadsheetId validation

    suspend fun createMap(newMap: NewMapMeta): String? = dao.createMap(newIdentifier(), newMap)

    suspend fun getMaps(): List<MapMeta> = dao.getMaps()

    suspend fun getMapsByUser(): Nothing = TODO()

    suspend fun getMap(identifier: String): MapMeta? = dao.getMap(identifier)

    suspend fun updateMap(identifier: String, map: MapMeta): Unit = dao.updateMap(map)

    suspend fun convertMap(identifier: String): String {
        val map = getMap(identifier)!!
        val spreadsheet = CacheService.getSpreadsheet(map.spreadsheetId)
        return TransformService.transform(spreadsheet)
    }

}