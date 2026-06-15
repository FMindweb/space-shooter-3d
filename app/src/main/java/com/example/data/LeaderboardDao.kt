package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 20")
    fun getTopScores(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
