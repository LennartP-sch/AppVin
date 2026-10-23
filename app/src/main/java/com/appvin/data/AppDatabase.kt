package com.appvin.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CellarDao {
    @Query(
        """SELECT Wine.*, COUNT(Bottle.id) AS bottleCount FROM Wine
           JOIN Bottle ON Bottle.wineId = Wine.id AND Bottle.removedAt IS NULL
           GROUP BY Wine.id ORDER BY Wine.producer, Wine.name, Wine.vintage"""
    )
    fun winesInCellar(): Flow<List<WineWithCount>>

    @Query("SELECT * FROM Wine WHERE id = :id")
    fun wine(id: Long): Flow<Wine?>

    @Query("SELECT * FROM Bottle WHERE removedAt IS NULL")
    fun activeBottles(): Flow<List<Bottle>>

    @Query("SELECT * FROM Bottle WHERE wineId = :wineId AND removedAt IS NULL ORDER BY addedAt")
    fun activeBottlesForWine(wineId: Long): Flow<List<Bottle>>

    @Transaction
    @Query("SELECT * FROM Bottle WHERE removedAt IS NOT NULL ORDER BY removedAt DESC")
    fun pastBottles(): Flow<List<BottleWithWine>>

    @Transaction
    @Query("SELECT * FROM Bottle ORDER BY MAX(addedAt, IFNULL(removedAt, 0)) DESC LIMIT 15")
    fun recentActivity(): Flow<List<BottleWithWine>>

    @Transaction
    @Query("SELECT * FROM Bottle")
    suspend fun allBottlesWithWine(): List<BottleWithWine>

    @Query(
        """SELECT StorageLocation.*, COUNT(Bottle.id) AS bottleCount FROM StorageLocation
           LEFT JOIN Bottle ON Bottle.locationId = StorageLocation.id AND Bottle.removedAt IS NULL
           GROUP BY StorageLocation.id ORDER BY StorageLocation.name"""
    )
    fun locations(): Flow<List<LocationWithCount>>

    @Insert suspend fun insertWine(wine: Wine): Long
    @Update suspend fun updateWine(wine: Wine)
    @Delete suspend fun deleteWine(wine: Wine)

    @Insert suspend fun insertBottles(bottles: List<Bottle>)
    @Update suspend fun updateBottle(bottle: Bottle)

    @Insert suspend fun insertLocation(location: StorageLocation): Long
    @Update suspend fun updateLocation(location: StorageLocation)
    @Delete suspend fun deleteLocation(location: StorageLocation)

    @Query("SELECT * FROM ChatMessage ORDER BY createdAt")
    fun chatMessages(): Flow<List<ChatMessage>>

    @Insert suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM ChatMessage")
    suspend fun clearChat()
}

@Database(entities = [Wine::class, Bottle::class, StorageLocation::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cellarDao(): CellarDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "appvin.db").build()
    }
}
