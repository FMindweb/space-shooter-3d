package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.LeaderboardEntry
import com.example.data.LeaderboardRepository
import com.example.ui.LoginLobbyScreen
import com.example.ui.game.GameScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

// Screen states inside MainActivity
sealed interface Screen {
    object Lobby : Screen
    data class Playing(val stage: Int) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: LeaderboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Room components initialization
        database = AppDatabase.getDatabase(this)
        repository = LeaderboardRepository(database.leaderboardDao())

        setContent {
            MyApplicationTheme {
                // Shared States across composition
                var pilotName by rememberSaveable { mutableStateOf("") }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Lobby) }

                // Observe Leaderboard reactively from Room flow
                val topScores by repository.topScores.collectAsState(initial = emptyList())

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (val screen = currentScreen) {
                            is Screen.Lobby -> {
                                LoginLobbyScreen(
                                    username = pilotName,
                                    onLoginCompleted = { pilotName = it },
                                    topScores = topScores,
                                    onClearLeaderboard = {
                                        lifecycleScope.launch {
                                            repository.clearLeaderboard()
                                        }
                                    },
                                    onLaunchGame = { stage ->
                                        currentScreen = Screen.Playing(stage)
                                    }
                                )
                            }
                            is Screen.Playing -> {
                                GameScreen(
                                    username = pilotName,
                                    stage = screen.stage,
                                    scope = rememberCoroutineScope(),
                                    onBackToLobby = {
                                        currentScreen = Screen.Lobby
                                    },
                                    onSaveScore = { score, stageReached ->
                                        lifecycleScope.launch {
                                            repository.insertScore(
                                                LeaderboardEntry(
                                                    username = pilotName,
                                                    score = score,
                                                    stage = stageReached
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
