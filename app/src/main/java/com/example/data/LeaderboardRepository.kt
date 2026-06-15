package com.example.data

import kotlinx.coroutines.flow.Flow

class LeaderboardRepository(private val leaderboardDao: LeaderboardDao) {
    val topScores: Flow<List<LeaderboardEntry>> = leaderboardDao.getTopScores()

    suspend fun insertScore(entry: LeaderboardEntry) {
        leaderboardDao.insertScore(entry)
    }

    suspend fun clearLeaderboard() {
        leaderboardDao.clearLeaderboard()
    }
}
