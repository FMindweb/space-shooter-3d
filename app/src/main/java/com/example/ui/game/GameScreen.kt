package com.example.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.*
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    username: String,
    stage: Int,
    scope: CoroutineScope,
    onBackToLobby: () -> Unit,
    onSaveScore: (Int, Int) -> Unit
) {
    // Instantiate game engine
    val engine = remember {
        GameEngine(
            username = username,
            initialStage = stage,
            scope = scope,
            onGameOver = { score, stageReached ->
                onSaveScore(score, stageReached)
            }
        )
    }

    // Connect game engine loops
    DisposableEffect(Unit) {
        engine.start()
        onDispose {
            engine.stop()
        }
    }

    // Engine States for UI
    val bullets by engine.bullets3D
    val enemies by engine.enemies3D
    val particles by engine.particles3D

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Continuous spinning angles for decoration
        val infiniteTransition = rememberInfiniteTransition(label = "deco")
        val spinningAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )

        // 1. Core 3D Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Map relative drag with bounds.
                        // Sensitivity adjusted for a smooth and immediate tactical grip
                        engine.playerX = (engine.playerX + dragAmount.x * 0.9f).coerceIn(-240f, 240f)
                        engine.playerY = (engine.playerY + dragAmount.y * 0.9f).coerceIn(-350f, 350f)
                    }
                }
                .testTag("game_canvas")
        ) {
            val screenW = size.width
            val screenH = size.height
            val centerX = screenW / 2f
            val centerY = screenH / 2f

            // Account for structural shake amplitude
            var finalCenterX = centerX
            var finalCenterY = centerY
            if (engine.shakeAmplitude > 0f) {
                finalCenterX += (Math.random() * engine.shakeAmplitude * 2 - engine.shakeAmplitude).toFloat()
                finalCenterY += (Math.random() * engine.shakeAmplitude * 2 - engine.shakeAmplitude).toFloat()
            }

            // A: PERSPECTIVE CAMERA FIELD-OF-VIEW
            val fov = 380f

            // B: DRAWEN ENDLESS ENDLESS 3D STARFIELD
            engine.stars3D.forEach { star ->
                // Math: Perspective division x_screen = centerX + x * fov / z
                val scale = fov / star.z
                val projX = finalCenterX + star.x * scale
                val projY = finalCenterY + star.y * scale

                // Render stars if they land anywhere inside screen frame
                if (projX >= 0 && projX <= screenW && projY >= 0 && projY <= screenH) {
                    val baseRadius = when (star.color) {
                        0 -> 3.5f
                        1 -> 2.2f
                        else -> 1.2f
                    }
                    val starSize = (baseRadius * scale).coerceIn(1f, 8f)
                    val colorHex = when (star.color) {
                        0 -> Color(0xFF00FFCC) // glowing cyan
                        1 -> Color(0xFFE2EAF5) // bright white
                        else -> Color(0x9E70849E) // dim starlight
                    }
                    drawCircle(color = colorHex, radius = starSize, center = Offset(projX, projY))
                }
            }

            // C: DRAW 3D PARTICLES EXPLOSION SPARK BURSTS
            particles.forEach { p ->
                val scale = fov / (p.z + 0.1f)
                val projX = finalCenterX + p.x * scale
                val projY = finalCenterY + p.y * scale

                if (projX >= 0 && projX <= screenW && projY >= 0 && projY <= screenH) {
                    val pSize = (3f * scale).coerceIn(1f, 12f)
                    // Fade color depending on remaining life span ratio
                    val alphaRatio = (p.life / p.maxLife).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(p.color).copy(alpha = alphaRatio),
                        radius = pSize,
                        center = Offset(projX, projY)
                    )
                }
            }

            // D: DRAW LASER BULLETS (3D capsules)
            bullets.forEach { bullet ->
                val scale = fov / (bullet.z + 0.1f)
                val projX = finalCenterX + bullet.x * scale
                val projY = finalCenterY + bullet.y * scale

                // Bullet is drawn with a perspective trail in 3D
                val trailZ = bullet.z + (if (bullet.owner == BulletOwner.PLAYER) -30f else 25f)
                val trailScale = fov / (trailZ + 0.1f)
                val trailProjX = finalCenterX + bullet.x * trailScale
                val trailProjY = finalCenterY + bullet.y * trailScale

                val strokeWidth = (4f * scale).coerceIn(2f, 12f)
                drawLine(
                    color = Color(bullet.color),
                    start = Offset(projX, projY),
                    end = Offset(trailProjX, trailProjY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // E: DRAW ENEMY SPACESIPS WITH VECTORS
            enemies.forEach { enemy ->
                if (enemy.z < 10f) return@forEach

                val scale = fov / enemy.z
                val projX = finalCenterX + enemy.x * scale
                val projY = finalCenterY + enemy.y * scale

                // Size of representation scales matching depth
                val baseRadius = when (enemy.type) {
                    EnemyType.BOSS_MOTHERSHIP -> 75f
                    EnemyType.GUNSHIP -> 35f
                    EnemyType.WEAVER -> 22f
                    else -> 15f
                }
                val r = baseRadius * scale

                // Render vector hulls based on enemy style
                when (enemy.type) {
                    EnemyType.SCOUT -> {
                        // Cyan/Red scouts (Triangular nose pointing towards camera)
                        val p1 = Offset(projX, projY + r)
                        val p2 = Offset(projX - r, projY - r * 0.5f)
                        val p3 = Offset(projX + r, projY - r * 0.5f)
                        drawPath(
                            path = Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                close()
                            },
                            color = LaserCrimson.copy(alpha = 0.2f)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                close()
                            },
                            color = LaserCrimson,
                            style = Stroke(width = 2f * scale.coerceIn(0.5f, 4f))
                        )
                        // Thruster sparks
                        drawCircle(Color(0xFFFF9900), radius = r * 0.3f, center = Offset(projX, projY - r * 0.5f))
                    }
                    EnemyType.WEAVER -> {
                        // Sweeping moth shape
                        val pLeftWing = Offset(projX - r, projY + r * 0.3f)
                        val pRightWing = Offset(projX + r, projY + r * 0.3f)
                        val pNose = Offset(projX, projY + r)
                        val pTail = Offset(projX, projY - r)

                        drawPath(
                            path = Path().apply {
                                moveTo(pNose.x, pNose.y)
                                lineTo(pLeftWing.x, pLeftWing.y)
                                lineTo(pTail.x, pTail.y)
                                lineTo(pRightWing.x, pRightWing.y)
                                close()
                            },
                            color = Color(0xFFFF9933).copy(alpha = 0.2f)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(pNose.x, pNose.y)
                                lineTo(pLeftWing.x, pLeftWing.y)
                                lineTo(pTail.x, pTail.y)
                                lineTo(pRightWing.x, pRightWing.y)
                                close()
                            },
                            color = Color(0xFFFF9933),
                            style = Stroke(width = 2f * scale.coerceIn(0.5f, 4f))
                        )
                    }
                    EnemyType.GUNSHIP -> {
                        // Heavy multi-deck octagonal cargo model
                        val points = mutableListOf<Offset>()
                        for (i in 0 until 8) {
                            val angle = i * Math.PI / 4f
                            points.add(
                                Offset(
                                    projX + (r * cos(angle)).toFloat(),
                                    projY + (r * sin(angle)).toFloat()
                                )
                            )
                        }
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until 8) {
                                lineTo(points[i].x, points[i].y)
                            }
                            close()
                        }
                        drawPath(path = path, color = Color(0xFFFF3300).copy(alpha = 0.25f))
                        drawPath(path = path, color = Color(0xFFFF3300), style = Stroke(width = 3.5f * scale.coerceIn(0.5f, 5f)))
                        // Core yellow engine
                        drawCircle(CyberGold, radius = r * 0.4f, center = Offset(projX, projY))
                    }
                    EnemyType.PHANTOM -> {
                        // Phantom has a flickering transparent diamond hull
                        val flickerAlpha = (0.23f + 0.45f * sin(enemy.customAge * 12)).coerceIn(0.1f, 0.9f)
                        val pTop = Offset(projX, projY - r)
                        val pBottom = Offset(projX, projY + r)
                        val pL = Offset(projX - r, projY)
                        val pR = Offset(projX + r, projY)

                        val dPath = Path().apply {
                            moveTo(pTop.x, pTop.y)
                            lineTo(pR.x, pR.y)
                            lineTo(pBottom.x, pBottom.y)
                            lineTo(pL.x, pL.y)
                            close()
                        }
                        drawPath(path = dPath, color = PlasmaPurple.copy(alpha = flickerAlpha * 0.3f))
                        drawPath(path = dPath, color = PlasmaPurple.copy(alpha = flickerAlpha), style = Stroke(width = 2f * scale.coerceIn(0.5f, 4f)))
                    }
                    EnemyType.BOSS_MOTHERSHIP -> {
                        // Mega Level 5 Carrier Dreadnought Starship
                        // Base circle outline
                        drawCircle(
                            color = LaserCrimson.copy(alpha = 0.15f),
                            radius = r,
                            center = Offset(projX, projY)
                        )
                        // Outer armored frame
                        drawCircle(
                            color = LaserCrimson,
                            radius = r,
                            center = Offset(projX, projY),
                            style = Stroke(width = 5f * scale.coerceIn(1f, 8f))
                        )
                        // Side hangar decks
                        drawRect(
                            color = GrayBorder,
                            topLeft = Offset(projX - r * 1.3f, projY - r * 0.2f),
                            size = Size(r * 2.6f, r * 0.4f),
                            style = Stroke(width = 2f)
                        )
                        // Glowing cores (HP visual link)
                        val hpPercent = enemy.health / enemy.maxHealth
                        val coreColor = if (hpPercent > 0.5f) TextGreen else if (hpPercent > 0.2f) CyberGold else LaserCrimson
                        drawCircle(coreColor, radius = r * 0.35f, center = Offset(projX - r * 0.5f, projY))
                        drawCircle(coreColor, radius = r * 0.35f, center = Offset(projX + r * 0.5f, projY))

                        // HP mini segment overlaying just above the boss
                        val barW = r * 1.5f
                        val barH = 5f * scale.coerceIn(1f, 4f)
                        drawRect(
                            color = Color(0x7F424242),
                            topLeft = Offset(projX - barW / 2f, projY - r * 1.25f),
                            size = Size(barW, barH)
                        )
                        drawRect(
                            color = TextGreen,
                            topLeft = Offset(projX - barW / 2f, projY - r * 1.25f),
                            size = Size(barW * hpPercent, barH)
                        )
                    }
                }
            }

            // F: DRAW PLAYER VEHICLE INSIDE OUR DRAGGABLE PLANE COORDINATES (z=100)
            val pScale = fov / engine.playerZ
            val pXProj = finalCenterX + engine.playerX * pScale
            val pYProj = finalCenterY + engine.playerY * pScale

            // Draw glowing engine flare
            val flameHeight = 25f + (Math.random() * 20f).toFloat()
            drawPath(
                path = Path().apply {
                    moveTo(pXProj - 10f, pYProj + 8f)
                    lineTo(pXProj, pYProj + 8f + flameHeight)
                    lineTo(pXProj + 10f, pYProj + 8f)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(ElectroCyan, Color.Transparent),
                    startY = pYProj + 8f,
                    endY = pYProj + 8f + flameHeight
                )
            )

            // Player Vector Wings (Aesthetic fighter shape)
            val pathFighter = Path().apply {
                moveTo(pXProj, pYProj - 26f) // Nosecone
                lineTo(pXProj - 28f, pYProj + 15f) // Left Wingtip
                lineTo(pXProj - 10f, pYProj + 8f) // Internal notch
                lineTo(pXProj + 10f, pYProj + 8f) // Internal notch
                lineTo(pXProj + 28f, pYProj + 15f) // Right Wingtip
                close()
            }
            drawPath(path = pathFighter, color = ElectroCyan.copy(alpha = 0.25f))
            drawPath(path = pathFighter, color = ElectroCyan, style = Stroke(width = 3.5f))

            // Cockpit glass
            drawCircle(Color.White.copy(0.7f), radius = 6f, center = Offset(pXProj, pYProj - 6f))

            // G: DRAW ACTIVE SPECIAL OVERLAYS
            // Shield ring overlay (Skill 3 Active)
            if (engine.shieldCharges > 0) {
                withTransform({
                    rotate(spinningAngle, pivot = Offset(pXProj, pYProj))
                }) {
                    for (i in 0 until engine.shieldCharges) {
                        val shieldRadius = 40f + i * 14f
                        drawCircle(
                            color = PlasmaPurple,
                            radius = shieldRadius,
                            center = Offset(pXProj, pYProj),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                        )
                    }
                }
            }

            // Aurora visual active beams (Skill 1 trigger)
            if (engine.showAuroraEffect) {
                val alphaColumn = engine.auroraAnimProgress.coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectroCyan.copy(alpha = alphaColumn * 0.45f),
                            Color.White.copy(alpha = alphaColumn * 0.9f),
                            ElectroCyan.copy(alpha = alphaColumn * 0.45f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(screenW, screenH)
                )

                // Laser grids shooting downwards
                for (step in 1..4) {
                    val beamX = screenW * (step / 5f)
                    drawLine(
                        color = Color.White.copy(alpha = alphaColumn),
                        start = Offset(beamX, 0f),
                        end = Offset(beamX, screenH),
                        strokeWidth = 6f * alphaColumn
                    )
                }
            }

            // Time Warp overlay lens (Skill 2 Active)
            if (engine.skillTimeWarp.isActive) {
                // pulsing blue grid vignette
                drawRect(
                    color = ElectroCyan.copy(alpha = 0.11f),
                    topLeft = Offset(0f, 0f),
                    size = Size(screenW, screenH)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, TechnoBlue.copy(alpha = 0.45f)),
                        center = Offset(centerX, centerY),
                        radius = screenW / 1.3f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(screenW, screenH)
                )
            }
        }

        // 2. STAGE & SCORE TOP GRAPHICS LAYER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ElectroCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "得分: ${engine.score}",
                        color = ElectroCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("score_text")
                    )
                }

                // Stage descriptor tag
                Box(
                    modifier = Modifier
                        .background(LaserCrimson, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "SECTOR 0${engine.currentStage}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player Structural Integrity HP Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(26.dp)
                    .background(TechnoBlue, RoundedCornerShape(6.dp))
                    .border(1.dp, GrayBorder, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Shield health",
                    tint = LaserCrimson,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Max health blocks
                for (i in 0 until engine.playerMaxHealth) {
                    val isActiveBlock = i < engine.playerHealth
                    val cellColor = if (isActiveBlock) {
                        if (engine.playerHealth <= 1) LaserCrimson else ElectroCyan
                    } else {
                        Color(0xFF1E2638)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor)
                    )
                }
            }

            // If Boss Stage, show a custom glowing Boss Integrity Bar at key top
            if (engine.isBossStage && engine.bossHealth > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "母舰BOSS: 耶梦加得级毁灭者 (MOTHERSHIP)",
                            color = LaserCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(engine.bossHealth / engine.bossMaxHealth * 100).toInt()}%",
                            color = LaserCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { engine.bossHealth / engine.bossMaxHealth },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LaserCrimson,
                        trackColor = Color(0xFF1E1111)
                    )
                }
            }
        }

        // 3. FLOATING COCKPIT TACTICAL SKILL BUTTONS (BOTTOM RIGHT, 48DP+)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 32.dp, end = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time slowdown details (Time Warp lens duration)
                if (engine.skillTimeWarp.isActive) {
                    Box(
                        modifier = Modifier
                            .background(ElectroCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .border(1.dp, ElectroCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "时空锁定中: ${(engine.skillTimeWarp.activeRemainingMs / 1000f).format(1)}s",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skill 1: Full-screen Aurora
                    SkillCircularButton(
                        skill = engine.skillAurora,
                        iconSeed = Icons.Default.Cyclone,
                        buttonColor = ElectroCyan,
                        testTag = "skill_1_aurora",
                        onTrigger = { engine.triggerSkill1Aurora() }
                    )

                    // Skill 2: Time slowdown
                    SkillCircularButton(
                        skill = engine.skillTimeWarp,
                        iconSeed = Icons.Default.HourglassEmpty,
                        buttonColor = ElectroCyan,
                        testTag = "skill_2_timewarp",
                        onTrigger = { engine.triggerSkill2TimeWarp() }
                    )

                    // Skill 3: Quantum Shield protect
                    SkillCircularButton(
                        skill = engine.skillShield,
                        iconSeed = Icons.Default.Shield,
                        buttonColor = PlasmaPurple,
                        testTag = "skill_3_shield",
                        extraBadgeValue = if (engine.shieldCharges > 0) "${engine.shieldCharges}" else null,
                        onTrigger = { engine.triggerSkill3QuantumShield() }
                    )
                }
            }
        }

        // Drag controller guide hint (fades away automatically or stays low-key)
        Text(
            text = "◀ 拖动屏幕任意位置即可闪避与射击 ▶",
            color = TextMuted.copy(alpha = 0.61f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )

        // 4. OVERLAY STENCILS (GAME OVER or STAGE CLEARED)
        AnimatedVisibility(
            visible = engine.isGameOver,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            OverlayPanel(
                title = "星战结束 MISSION FAILED",
                subTitle = "飞船结构已被外星机群彻底瓦解",
                titleColor = LaserCrimson,
                accentColor = LaserCrimson,
                buttonLabel = "复盘分析 & 返回大厅",
                score = engine.score,
                stage = engine.currentStage,
                onActionTriggered = onBackToLobby
            )
        }

        AnimatedVisibility(
            visible = engine.isStageCleared,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut()
        ) {
            OverlayPanel(
                title = "战役告捷 STAGE CLEARED!",
                subTitle = "星际跃迁通道已激活，区域扫荡完毕",
                titleColor = TextGreen,
                accentColor = ElectroCyan,
                buttonLabel = "荣耀晋衔 & 凯旋大厅",
                score = engine.score,
                stage = engine.currentStage,
                onActionTriggered = onBackToLobby
            )
        }
    }
}

@Composable
fun SkillCircularButton(
    skill: SkillInfo,
    iconSeed: androidx.compose.ui.graphics.vector.ImageVector,
    buttonColor: Color,
    testTag: String,
    extraBadgeValue: String? = null,
    onTrigger: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(TechnoBlue.copy(alpha = 0.85f))
            .border(2.dp, if (skill.isReady) buttonColor else GrayBorder, CircleShape)
            .clickable(enabled = skill.isReady) { onTrigger() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Cooldown sweep circle overlay
        if (!skill.isReady) {
            val sweepRatio = skill.progress
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.Black.copy(alpha = 0.65f),
                    startAngle = -90f,
                    sweepAngle = 360f * sweepRatio,
                    useCenter = true,
                    size = size
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconSeed,
                contentDescription = skill.name,
                tint = if (skill.isReady) buttonColor else TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = skill.name.take(2),
                color = if (skill.isReady) Color.White else TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // Numeric countdown indicator
        if (!skill.isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.40f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(skill.currentCooldownMs / 1000f).format(1)}s",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Active Quantities overlay tag (e.g. Shield levels left)
        if (extraBadgeValue != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(LaserCrimson, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = extraBadgeValue,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun OverlayPanel(
    title: String,
    subTitle: String,
    titleColor: Color,
    accentColor: Color,
    buttonLabel: String,
    score: Int,
    stage: Int,
    onActionTriggered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .background(TechnoBlue, RoundedCornerShape(16.dp))
                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big display label
            Text(
                text = title,
                color = titleColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Text(
                text = subTitle,
                color = TextLight,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Mission debrief details panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceBlack)
                    .border(1.dp, GrayBorder, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "作 战 星 域", color = TextMuted, fontSize = 13.sp)
                    Text(text = "SECTOR 0$stage", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "最 终 战 绩", color = TextMuted, fontSize = 13.sp)
                    Text(text = "$score PTS", color = ElectroCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Action Button
            Button(
                onClick = onActionTriggered,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = SpaceBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dismiss_overlay_button")
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// Float formatting extension for neat displays
fun Float.format(digits: Int): String = String.format("%.${digits}f", this)
