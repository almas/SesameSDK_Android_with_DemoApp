package co.candyhouse.sesame.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import co.candyhouse.sesame.ble.os2.CHError
import co.candyhouse.sesame.db.model.CHDevice
import co.candyhouse.sesame.db.model.CHDeviceHistory
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.HttpResponseCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Database(entities = [CHDevice::class, CHDeviceHistory::class], version = 30, exportSchema = false)
abstract class CHDB : RoomDatabase() {

    abstract fun deviceDao(): ChDeviceDao
    abstract fun historyDao(): ChHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CHDB? = null

        fun getDatabase(): CHDB {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    CHBleManager.appContext,
                    CHDB::class.java,
                    "word_database"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }

    object CHHistoryModel {
        private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val dao get() = getDatabase().historyDao()

        fun insert(deviceUUID: String, historyData: String) {
            dbScope.launch {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val recordDate = dateFormat.format(Date(timestamp))
                
                val history = CHDeviceHistory(
                    deviceUUID = deviceUUID,
                    historyData = historyData,
                    timestamp = timestamp,
                    recordDate = recordDate
                )
                dao.insert(history)
            }
        }

        fun getHistory(deviceUUID: String, onResponse: CHResult<List<CHDeviceHistory>>) {
            dbScope.launch {
                try {
                    val history = dao.getByDeviceUUID(deviceUUID)
                    onResponse(Result.success(CHResultState.CHResultStateNetworks(history)))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }
    }

    object CHSS2Model {
        private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val dao get() = getDatabase().deviceDao()

        fun getAllDB(onResponse: HttpResponseCallback<List<CHDevice>>) {
            dbScope.launch {
                try {
                    val devices = dao.getAll()
                    onResponse(Result.success(devices))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun getDevice(deviceID: String, onResponse: HttpResponseCallback<CHDevice>) {
            dbScope.launch {
                try {
                    val device = dao.getByUUID(deviceID)
                    if (device != null) {
                        onResponse(Result.success(device))
                    } else {
                        onResponse(Result.failure(CHError.NotfoundError.value))
                    }
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun insert(device: CHDevice, onResponse: HttpResponseCallback<String>) {
            dbScope.launch {
                try {
                    if (isValidUUID(device.deviceUUID)) {
                        dao.insert(device)
                        onResponse(Result.success(""))
                    } else {
                        onResponse(Result.failure(IllegalArgumentException("Invalid UUID")))
                    }
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun delete(device: CHDevice, onResponse: HttpResponseCallback<String>) {
            dbScope.launch {
                try {
                    dao.delete(device)
                    onResponse(Result.success(""))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun deleteByDeviceId(deviceId: String, onResponse: HttpResponseCallback<Int>) {
            dbScope.launch {
                try {
                    val deletedCount = dao.deleteByUUID(deviceId)
                    onResponse(Result.success(deletedCount))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun deleteByDeviceIds(deviceIds: List<String>, onResponse: HttpResponseCallback<Int>) {
            dbScope.launch {
                try {
                    val deletedCount = dao.deleteByUUIDs(deviceIds)
                    onResponse(Result.success(deletedCount))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        fun replaceAll(devices: List<CHDevice>, onResponse: HttpResponseCallback<Unit>) {
            dbScope.launch {
                try {
                    dao.replaceAll(devices)
                    onResponse(Result.success(Unit))
                } catch (e: Exception) {
                    onResponse(Result.failure(e))
                }
            }
        }

        private fun isValidUUID(uuid: String): Boolean {
            return try {
                UUID.fromString(uuid)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }
    }
}

@Dao
interface ChDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: CHDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<CHDevice>)

    @Delete
    suspend fun delete(device: CHDevice)

    @Update
    suspend fun update(device: CHDevice)

    @Query("SELECT * FROM CHDevice")
    suspend fun getAll(): List<CHDevice>

    @Query("SELECT * FROM CHDevice WHERE deviceUUID = :uuid LIMIT 1")
    suspend fun getByUUID(uuid: String): CHDevice?

    @Query("DELETE FROM CHDevice WHERE deviceUUID = :uuid")
    suspend fun deleteByUUID(uuid: String): Int

    @Query("DELETE FROM CHDevice WHERE deviceUUID IN (:deviceIds)")
    suspend fun deleteByUUIDs(deviceIds: List<String>): Int

    @Query("DELETE FROM CHDevice")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(devices: List<CHDevice>) {
        deleteAll()
        insertAll(devices)
    }
}

@Dao
interface ChHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: CHDeviceHistory)

    @Query("SELECT * FROM CHDeviceHistory WHERE deviceUUID = :uuid ORDER BY timestamp DESC")
    suspend fun getByDeviceUUID(uuid: String): List<CHDeviceHistory>

    @Query("DELETE FROM CHDeviceHistory WHERE deviceUUID = :uuid")
    suspend fun deleteByDeviceUUID(uuid: String): Int

    @Query("DELETE FROM CHDeviceHistory")
    suspend fun deleteAll()
}