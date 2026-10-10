package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Cassette
import com.example.model.TrackMarker
import kotlinx.coroutines.flow.Flow

@Dao
interface CassetteDao {
    @Query("SELECT * FROM cassettes ORDER BY createdAt DESC")
    fun getAllCassettes(): Flow<List<Cassette>>

    @Query("SELECT * FROM cassettes WHERE id = :id LIMIT 1")
    suspend fun getCassetteById(id: String): Cassette?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCassette(cassette: Cassette)

    @Update
    suspend fun updateCassette(cassette: Cassette)

    @Delete
    suspend fun deleteCassette(cassette: Cassette)

    @Query("SELECT * FROM track_markers WHERE cassetteId = :cassetteId ORDER BY startOffsetSec ASC")
    fun getTrackMarkersForCassette(cassetteId: String): Flow<List<TrackMarker>>

    @Query("SELECT * FROM track_markers ORDER BY startOffsetSec ASC")
    fun getAllTrackMarkers(): Flow<List<TrackMarker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackMarker(marker: TrackMarker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackMarkers(markers: List<TrackMarker>)
}
