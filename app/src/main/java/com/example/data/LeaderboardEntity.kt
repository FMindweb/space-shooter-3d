package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val score: Int,
    val stage: Int,
    val timestamp: Long = System.currentTimeMillis()
)
