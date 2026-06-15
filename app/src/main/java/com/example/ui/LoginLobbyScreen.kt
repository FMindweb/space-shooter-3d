package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaderboardEntry
import com.example.ui.theme.*

@Composable
fun LoginLobbyScreen(
    username: String,
    onLoginCompleted: (String) -> Unit,
    topScores: List<LeaderboardEntry>,
    onClearLeaderboard: () -> Unit,
    onLaunchGame: (Int) -> Unit
) {
    val isLoginNeeded = username.trim().isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .drawBehind {
                // Precision grid drawing to evoke a high-fidelity neon orbital matrix background
                val gridSize = 45.dp.toPx()
                val lineAlpha = 0.05f
                val gridColor = VibrantBlueLight

                // Draw vertical grid lines
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                        alpha = lineAlpha
                    )
                    x += gridSize
                }

                // Draw horizontal grid lines
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        alpha = lineAlpha
                    )
                    y += gridSize
                }
            }
    ) {
        if (isLoginNeeded) {
            LoginPanel(onLoginCompleted = onLoginCompleted)
        } else {
            LobbyPanel(
                username = username,
                topScores = topScores,
                onClearLeaderboard = onClearLeaderboard,
                onLaunchGame = onLaunchGame
            )
        }
    }
}

@Composable
fun LoginPanel(onLoginCompleted: (String) -> Unit) {
    var rawInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .background(TechnoBlue.copy(alpha = 0.95f))
                .border(1.dp, BlueBorderAccent, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cyber logo avatar box
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(VibrantBlue, PlasmaPurple)),
                        RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = "Fighter Terminal",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "星 战 战 基",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "SPACE SHOOTER 3D",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VibrantBlueLight,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tech instruction console log
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(0.6f))
                    .border(1.dp, SlateEightHundred, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(ElectroCyan).clip(RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYS_STATUS: PILOT_LINK_Awaiting",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ElectroCyan
                    )
                }
                Text(
                    text = "Awaiting authorized pilot callsign registration signals...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Entry
            OutlinedTextField(
                value = rawInput,
                onValueChange = { if (it.length <= 16) rawInput = it },
                label = { Text("输入绝密飞行员呼号 / CALLSIGN", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectroCyan,
                    unfocusedBorderColor = GrayBorder,
                    focusedLabelColor = ElectroCyan,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    cursorColor = ElectroCyan
                ),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ElectroCyan)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Trigger
            Button(
                onClick = {
                    val trimmed = rawInput.trim()
                    if (trimmed.isNotEmpty()) {
                        focusManager.clearFocus()
                        onLoginCompleted(trimmed)
                    }
                },
                enabled = rawInput.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantBlue,
                    contentColor = Color.White,
                    disabledContainerColor = GrayBorder,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "认证进入大厅 LINK TERMINAL",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LobbyPanel(
    username: String,
    topScores: List<LeaderboardEntry>,
    onClearLeaderboard: () -> Unit,
    onLaunchGame: (Int) -> Unit
) {
    var selectedStage by remember { mutableIntStateOf(1) }

    // Retrieve high score dynamically from topScores if available
    val activeHighScore = if (topScores.isNotEmpty()) topScores.maxOf { it.score } else 0

    // Pulse Launch Animation
    val transitionPulse = rememberInfiniteTransition(label = "pulse_launch_lobby")
    val scalePulse by transitionPulse.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_btn"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System / Orbit Status headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM: ONLINE",
                    color = VibrantBlueLight.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ORBIT: SECTOR 0${selectedStage}",
                    color = VibrantBlueLight.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }

        // Leaderboard Matrix
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechnoBlue.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, BlueBorderAccent, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(listOf(VibrantBlue, PlasmaPurple)),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🚀", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "COMMANDER",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = username.uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "HIGH SCORE",
                        color = VibrantBlueLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (activeHighScore > 0) String.format("%,d", activeHighScore) else "000,000",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Stage Selection Section: Grid style matching the Design HTML
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechnoBlue.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(1.dp, GrayBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "STAGE SELECTION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = VibrantBlue,
                        letterSpacing = 3.sp
                    )
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .background(VibrantBlue.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Col 5 Grid Select button elements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (stageNum in 1..5) {
                        val isSelected = selectedStage == stageNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) VibrantBlue else TechnoBlue)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) VibrantBlueLight else GrayBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedStage = stageNum }
                                .testTag("stage_item_$stageNum"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$stageNum",
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail display for current selection
                val activeConfig = when (selectedStage) {
                    1 -> StageConfig(1, "星域侦察 (Scout Sector)", "迎击轻量级巡逻机组, 升级战区需要 Score > 2,000")
                    2 -> StageConfig(2, "星云织网 (Nebula Weavers)", "避开左右穿梭钟摆型怪异机群, 升级战区需要 Score > 4,000")
                    3 -> StageConfig(3, "流星火炮 (Heavy Gunships)", "散弹猛击对决，高火力压制防御, 升级战区需要 Score > 6,000")
                    4 -> StageConfig(4, "量子幽灵 (Quantum Phantoms)", "隐形闪烁干扰，战术轨迹诡谲，升级战区需要 Score > 8,000")
                    5 -> StageConfig(5, "幽灵母舰 (The Final Leviathan)", "终极战列虚空战，歼灭全副武装的移动毁灭级Boss巨兽！")
                    else -> StageConfig(1, "未知星域", "探索星尘深空")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "0${activeConfig.number} - ${activeConfig.title}",
                        color = VibrantBlueLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = activeConfig.desc,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Tactical Loadout skills layout based on Design preview section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechnoBlue.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(1.dp, GrayBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "TACTICAL LOADOUT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberGold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Skill A
                    TacticalSkillRow(
                        letter = "A",
                        name = "AURORA BURST 全屏极光",
                        desc = "Instant full-screen normal enemy clearance energy blast",
                        statusLabel = "READY",
                        badgeColor = ElectroCyan
                    )

                    // Skill S
                    TacticalSkillRow(
                        letter = "S",
                        name = "TIME DILATION 时空减速",
                        desc = "Reduce enemy and bullet speed speeds by 80% (5s)",
                        statusLabel = "COOLDOWN",
                        badgeColor = CyberGold
                    )

                    // Skill Q
                    TacticalSkillRow(
                        letter = "Q",
                        name = "QUANTUM SHIELD 量子护盾",
                        desc = "Deploy invincible forcefield to withstand 3 heavy strikes",
                        statusLabel = "READY",
                        badgeColor = TextGreen
                    )
                }
            }
        }

        // Leaderboard ranking board
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechnoBlue.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(1.dp, GrayBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RANKS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = LaserCrimson,
                            letterSpacing = 3.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyberGold, CircleShape)
                        )
                    }

                    if (topScores.isNotEmpty()) {
                        Text(
                            text = "RESET LEADERBOARD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = LaserCrimson,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onClearLeaderboard() }
                                .padding(4.dp)
                                .testTag("clear_leaderboard_button")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (topScores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "--- NO COMBAT LOGS DETECTED ---",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        topScores.take(5).forEachIndexed { index, entry ->
                            val rank = index + 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
                                    .border(1.dp, SlateEightHundred, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val rankColor = when (rank) {
                                        1 -> CyberGold
                                        2 -> Color(0xFF94A3B8) // silver
                                        3 -> Color(0xFFB45309) // bronze
                                        else -> TextMuted
                                    }
                                    Text(
                                        text = "0$rank",
                                        color = rankColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = entry.username.uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${entry.score} PTS",
                                        color = ElectroCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Trigger Launch Button
        item {
            Button(
                onClick = { onLaunchGame(selectedStage) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, VibrantBlueLight.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .testTag("launch_mission_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH MISSION",
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Mock Navigation Bar exactly matching the Design HTML bottom navigation
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MockBottomIcon(emoji = "📊", label = "STATS", isActive = false)
                MockBottomIcon(emoji = "⚔️", label = "COMBAT", isActive = true)
                MockBottomIcon(emoji = "🏆", label = "RANKS", isActive = false)
                MockBottomIcon(emoji = "⚙️", label = "SETUP", isActive = false)
            }
        }
    }
}

@Composable
fun TacticalSkillRow(
    letter: String,
    name: String,
    desc: String,
    statusLabel: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.4f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.12f))
                .border(1.dp, badgeColor.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = desc,
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = statusLabel,
            color = badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun MockBottomIcon(
    emoji: String,
    label: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp,
                modifier = Modifier.alpha(if (isActive) 1.0f else 0.4f)
            )
        }
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) VibrantBlueLight else TextMuted,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

data class StageConfig(
    val number: Int,
    val title: String,
    val desc: String
)

