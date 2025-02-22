package de.seemoo.at_tracking_detection.database.daos

import androidx.room.*
import de.seemoo.at_tracking_detection.database.models.Beacon
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface BeaconDao {
    @Query("SELECT * FROM beacon ORDER BY receivedAt DESC")
    fun getAllBeacons(): List<Beacon>

    // @Query("SELECT mfg FROM beacon WHERE mfg LIKE :Key LIMIT 1")
    @Query("SELECT * FROM beacon WHERE mfg LIKE :serviceData")
    fun getBeaconsWithDataLike(serviceData: String): List<Beacon>

    @Query("SELECT * FROM beacon WHERE receivedAt >= :since")
    fun getLatestBeacons(since: LocalDateTime): List<Beacon>

    @Query("SELECT COUNT(DISTINCT(deviceAddress)) FROM beacon WHERE receivedAt >= :since")
    fun getLatestBeaconCount(since: LocalDateTime): Flow<Int>

    @Query("SELECT COUNT(*) FROM beacon WHERE receivedAt >= :since")
    fun getTotalCountChange(since: LocalDateTime): Flow<Int>

    @Query("SELECT * FROM beacon WHERE receivedAt >= :since")
    fun getBeaconsSince(since: LocalDateTime): Flow<List<Beacon>>

    @Query("SELECT COUNT(*) FROM beacon")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM beacon WHERE deviceAddress LIKE :deviceAddress")
    fun getDeviceBeaconsCount(deviceAddress: String): Int

    @Query("SELECT * FROM beacon WHERE deviceAddress LIKE :deviceAddress ORDER BY receivedAt DESC")
    fun getDeviceBeacons(deviceAddress: String): List<Beacon>

    @Query("SELECT * FROM beacon WHERE deviceAddress LIKE :deviceAddress AND receivedAt >= :since ORDER BY receivedAt DESC")
    fun getDeviceBeaconsSince(deviceAddress: String, since: LocalDateTime): List<Beacon>

    @Query("SELECT * FROM (SELECT * FROM beacon ORDER BY receivedAt DESC, deviceAddress ASC) GROUP BY deviceAddress")
    fun getLatestBeaconPerDevice(): Flow<List<Beacon>>

    @Query("SELECT COUNT(*) FROM beacon, location WHERE location.locationId = beacon.locationId AND latitude IS NOT NULL AND longitude IS NOT NULL")
    fun getTotalLocationCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT(deviceAddress)) FROM beacon, location WHERE location.locationId = beacon.locationId AND latitude IS NOT NULL AND longitude IS NOT NULL AND receivedAt >= :since")
    fun getLatestLocationsCount(since: LocalDateTime): Flow<Int>

    @Query("SELECT COUNT(*) FROM beacon WHERE deviceAddress LIKE :deviceAddress AND receivedAt >= :since ORDER BY receivedAt DESC")
    fun getNumberOfBeaconsAddress(deviceAddress: String, since: LocalDateTime): Int

    @Query("SELECT COUNT(*) FROM beacon WHERE deviceAddress LIKE :deviceAddress AND locationId == :locationId AND receivedAt >= :since ORDER BY receivedAt DESC")
    fun getNumberOfBeaconsAddressAndLocation(deviceAddress: String, locationId: Int, since: LocalDateTime): Int

    @Query("""
    SELECT beacon.* FROM beacon
    JOIN device ON beacon.deviceAddress = device.address
    WHERE device.deviceType = :deviceType
    AND device.payloadData = :payload
    AND beacon.connectionState = :connectionState
    AND device.lastSeen BETWEEN :since AND :until
    AND beacon.receivedAt BETWEEN :since AND :until
    LIMIT 1 
    """)
    fun getRecentBeaconForDevice(deviceType: String, connectionState: Int, payload: Byte?, since: LocalDateTime, until: LocalDateTime): Beacon?


    /*
    @Query("SELECT COUNT(*) FROM beacon WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    fun getTotalLocationCount(): Flow<Int> // old version

    @Query("SELECT COUNT(DISTINCT(deviceAddress)) FROM beacon WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND receivedAt >= :since")
    fun getLatestLocationsCount(since: LocalDateTime): Flow<Int> // old version
     */

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(beacon: Beacon): Long

    @Update
    suspend fun update(beacon: Beacon)

    @Delete
    suspend fun delete(beacon: Beacon)

    @Delete
    suspend fun deleteBeacons(beacons: List<Beacon>)

    @Query("SELECT * FROM beacon LEFT JOIN notification ON beacon.deviceAddress = notification.deviceAddress WHERE receivedAt < :deleteEverythingBefore AND notification.deviceAddress IS NULL AND beacon.deviceAddress IS NOT NULL AND beacon.deviceAddress <> ''")
    fun getBeaconsOlderThanWithoutNotifications(deleteEverythingBefore: LocalDateTime): List<Beacon>
}