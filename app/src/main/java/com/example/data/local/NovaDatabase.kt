package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.ConnectionResult
import com.example.data.model.VpnProtocol
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
  @PrimaryKey val countryIso: String,
  val addedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_locations")
data class RecentLocationEntity(
  @PrimaryKey val countryIso: String,
  val lastConnectedEpochMs: Long,
)

@Entity(tableName = "connection_history")
data class ConnectionHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val countryIso: String,
  val countryNameEn: String,
  val countryNameAr: String,
  val cityNameEn: String,
  val cityNameAr: String,
  val serverId: String,
  val protocol: String,
  val startedAtEpochMs: Long,
  val durationSeconds: Long,
  val result: String,
) {
  fun toDomain(): ConnectionHistoryRecord = ConnectionHistoryRecord(
    id = id,
    countryIso = countryIso,
    countryNameEn = countryNameEn,
    countryNameAr = countryNameAr,
    cityNameEn = cityNameEn,
    cityNameAr = cityNameAr,
    serverId = serverId,
    protocol = runCatching { VpnProtocol.valueOf(protocol) }.getOrDefault(VpnProtocol.WIREGUARD),
    startedAtEpochMs = startedAtEpochMs,
    durationSeconds = durationSeconds,
    result = runCatching { ConnectionResult.valueOf(result) }.getOrDefault(ConnectionResult.DISCONNECTED),
  )
}

@Entity(tableName = "cached_servers")
data class CachedServerEntity(
  @PrimaryKey val id: String,
  val countryIso: String,
  val cityId: String,
  val cityNameEn: String,
  val cityNameAr: String,
  val hostname: String,
  val endpointHost: String,
  val endpointPort: Int,
  val publicKey: String,
  val supportedProtocolsCsv: String,
  val status: String,
  val loadPercent: Int?,
  val availableCapacityPercent: Int?,
  val measuredLatencyMs: Int?,
  val jitterMs: Int?,
  val bandwidthCapacityMbps: Int?,
  val isAvailable: Boolean,
  val lastHealthCheckEpochMs: Long?,
)

@Dao
interface LocationDao {
  @Query("SELECT countryIso FROM favorite_locations")
  fun observeFavoriteIsos(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addFavorite(entity: FavoriteLocationEntity)

  @Query("DELETE FROM favorite_locations WHERE countryIso = :iso")
  suspend fun removeFavorite(iso: String)

  @Query("SELECT * FROM recent_locations ORDER BY lastConnectedEpochMs DESC LIMIT 12")
  fun observeRecentLocations(): Flow<List<RecentLocationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecentLocation(entity: RecentLocationEntity)

  @Query("SELECT * FROM cached_servers")
  fun observeCachedServers(): Flow<List<CachedServerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertServers(servers: List<CachedServerEntity>)

  @Query("DELETE FROM cached_servers")
  suspend fun clearCachedServers()
}

@Dao
interface HistoryDao {
  @Query("SELECT * FROM connection_history ORDER BY startedAtEpochMs DESC LIMIT 200")
  fun observeHistory(): Flow<List<ConnectionHistoryEntity>>

  @Query("SELECT * FROM connection_history ORDER BY startedAtEpochMs DESC LIMIT 50")
  suspend fun getRecentHistorySnapshot(): List<ConnectionHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecord(record: ConnectionHistoryEntity): Long

  @Query("DELETE FROM connection_history")
  suspend fun clearHistory()
}

@Database(
  entities = [
    FavoriteLocationEntity::class,
    RecentLocationEntity::class,
    ConnectionHistoryEntity::class,
    CachedServerEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class NovaDatabase : RoomDatabase() {
  abstract fun locationDao(): LocationDao
  abstract fun historyDao(): HistoryDao

  companion object {
    @Volatile private var instance: NovaDatabase? = null

    fun getInstance(context: Context): NovaDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          NovaDatabase::class.java,
          "nova_vpn.db",
        ).build().also { instance = it }
      }
  }
}
