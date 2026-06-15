package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val username: String,
    initialStage: Int,
    private val scope: CoroutineScope,
    private val onGameOver: (score: Int, stage: Int) -> Unit
) {
    // Stage settings
    var currentStage by mutableStateOf(initialStage)
    var score by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)
    var isStageCleared by mutableStateOf(false)

    // Player Configuration
    var playerX by mutableStateOf(0f)
    var playerY by mutableStateOf(100f) // Centered lower screen
    val playerZ = 100f // Fixed forward projection plane
    var playerHealth by mutableStateOf(5)
    val playerMaxHealth = 5
    var shieldCharges by mutableStateOf(0)

    // Cosmic Shake Effect
    var shakeAmplitude by mutableStateOf(0f)

    // Active skills list
    var skillAurora by mutableStateOf(SkillInfo("全屏极光", "瞬间湮灭屏幕内所有敌机", 15f))
    var skillTimeWarp by mutableStateOf(SkillInfo("时空减速", "敌机与子弹速度削减80%", 20f, durationMaxSeconds = 5f))
    var skillShield by mutableStateOf(SkillInfo("量子护盾", "无敌保护罩，抵御3次伤害", 25f))

    // Lists of Entities
    val stars3D = mutableListOf<Star3D>()
    var bullets3D = mutableStateOf<List<Bullet3D>>(emptyList())
    var enemies3D = mutableStateOf<List<Enemy3D>>(emptyList())
    var particles3D = mutableStateOf<List<Particle3D>>(emptyList())

    // Spawning timers / triggers
    private var frameCount = 0
    private var bossSpawned = false
    var bossMaxHealth = 150f
    var bossHealth by mutableStateOf(0f)
    val isBossStage: Boolean get() = currentStage == 5
    var showAuroraEffect by mutableStateOf(false)
    var auroraAnimProgress by mutableStateOf(0f)

    private var gameLoopJob: Job? = null
    private var isEngineRunning = false
    private val random = Random(System.currentTimeMillis())

    init {
        initializeStars()
    }

    private fun initializeStars() {
        stars3D.clear()
        for (i in 0..150) {
            stars3D.add(
                Star3D(
                    x = random.nextFloat() * 1000f - 500f,
                    y = random.nextFloat() * 1000f - 500f,
                    z = random.nextFloat() * 1100f + 100f,
                    color = random.nextInt(3)
                )
            )
        }
    }

    fun start() {
        if (isEngineRunning) return
        isEngineRunning = true
        isGameOver = false
        isStageCleared = false
        playerHealth = playerMaxHealth
        shieldCharges = 0
        score = 0
        bossSpawned = false
        bullets3D.value = emptyList()
        enemies3D.value = emptyList()
        particles3D.value = emptyList()
        frameCount = 0
        resetSkills()

        gameLoopJob = scope.launch(Dispatchers.Default) {
            val frameTimeMs = 16L // Cap at ~60 FPS
            while (isEngineRunning) {
                val startTime = System.currentTimeMillis()
                updateGameEngine()
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = frameTimeMs - elapsed
                if (sleepTime > 0) {
                    delay(sleepTime)
                } else {
                    delay(1L) // Prevent starvation
                }
            }
        }
    }

    fun stop() {
        isEngineRunning = false
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun resetSkills() {
        skillAurora = skillAurora.copy(currentCooldownMs = 0, activeRemainingMs = 0)
        skillTimeWarp = skillTimeWarp.copy(currentCooldownMs = 0, activeRemainingMs = 0)
        skillShield = skillShield.copy(currentCooldownMs = 0, activeRemainingMs = 0)
    }

    // Handlers for skill acts
    fun triggerSkill1Aurora() {
        if (!skillAurora.isReady || isGameOver) return
        skillAurora.currentCooldownMs = (skillAurora.cooldownMaxSeconds * 1000).toLong()

        // Action: wipe all normal enemies on screen
        val list = enemies3D.value
        var pointsEarned = 0
        list.forEach { enemy ->
            if (enemy.type != EnemyType.BOSS_MOTHERSHIP) {
                pointsEarned += when (enemy.type) {
                    EnemyType.SCOUT -> 100
                    EnemyType.WEAVER -> 150
                    EnemyType.GUNSHIP -> 250
                    EnemyType.PHANTOM -> 350
                    else -> 0
                }
                spawn3DExplosion(enemy.x, enemy.y, enemy.z, 0xFFFF00FF) // Purple flash
            } else {
                // Boss takes massive damage
                enemy.health -= 50f
                bossHealth = enemy.health.coerceAtLeast(0f)
                spawn3DExplosion(enemy.x, enemy.y, enemy.z, 0xFFFFCC00)
            }
        }

        score += pointsEarned
        enemies3D.value = list.filter { it.type == EnemyType.BOSS_MOTHERSHIP && it.health > 0f }

        // Start stunning flash/screen shake
        shakeAmplitude = 35f
        showAuroraEffect = true
        auroraAnimProgress = 1.0f

        scope.launch {
            // Decay Aurora visual effect
            while (auroraAnimProgress > 0f) {
                delay(30L)
                auroraAnimProgress -= 0.05f
            }
            showAuroraEffect = false
        }
    }

    fun triggerSkill2TimeWarp() {
        if (!skillTimeWarp.isReady || isGameOver) return
        skillTimeWarp.currentCooldownMs = (skillTimeWarp.cooldownMaxSeconds * 1000).toLong()
        skillTimeWarp.activeRemainingMs = (skillTimeWarp.durationMaxSeconds * 1000).toLong()

        shakeAmplitude = 10f
        spawn3DExplosion(playerX, playerY, playerZ, 0xFF00FFFF) // Cyan wave
    }

    fun triggerSkill3QuantumShield() {
        if (!skillShield.isReady || isGameOver) return
        skillShield.currentCooldownMs = (skillShield.cooldownMaxSeconds * 1000).toLong()
        shieldCharges = 3

        shakeAmplitude = 8f
        spawn3DExplosion(playerX, playerY, playerZ, 0xFFCC00FF) // Deep violet shielding flare
    }

    // Core physics updates
    private fun updateGameEngine() {
        if (isGameOver || isStageCleared) return

        frameCount++

        // Update Skill Cooldowns & Active Times
        val tickMs = 16
        updateSkills(tickMs)

        // Slowly decay screen shake
        if (shakeAmplitude > 0f) {
            shakeAmplitude -= 0.6f
            if (shakeAmplitude < 0) shakeAmplitude = 0f
        }

        // Time slowdown multiplier (Skill 2)
        val timeWarpActive = skillTimeWarp.isActive
        val speedFactor = if (timeWarpActive) 0.2f else 1.0f

        // 1. Move Background Stars in 3D Space
        stars3D.forEach { star ->
            // Shift forward through Z plane
            val baseSpeed = when (star.color) {
                0 -> -16f
                1 -> -10f
                else -> -5f
            }
            star.z += baseSpeed * speedFactor
            if (star.z < 2f) {
                star.z = 1200f
                star.x = random.nextFloat() * 1000f - 500f
                star.y = random.nextFloat() * 1000f - 500f
            }
        }

        // 2. Spawn Enemies dynamically based on level config
        handleEnemySpawning()

        // 3. Spawns automatic player rapid-fire bullets (from the wingtips)
        if (frameCount % 8 == 0) {
            val bullets = bullets3D.value.toMutableList()
            val bulletLeftId = System.nanoTime() + random.nextLong()
            bullets.add(Bullet3D(bulletLeftId, playerX - 15f, playerY, playerZ + 15f, dx = 0f, dy = 0f, dz = 35f, owner = BulletOwner.PLAYER, damage = 1, color = 0xFF00FFCC))
            val bulletRightId = System.nanoTime() + random.nextLong()
            bullets.add(Bullet3D(bulletRightId, playerX + 15f, playerY, playerZ + 15f, dx = 0f, dy = 0f, dz = 35f, owner = BulletOwner.PLAYER, damage = 1, color = 0xFF00FFCC))
            bullets3D.value = bullets
        }

        // 4. Update bullets (Player + Enemy bullets)
        val bullets = bullets3D.value.toMutableList()
        val bulletsIterator = bullets.iterator()
        while (bulletsIterator.hasNext()) {
            val bullet = bulletsIterator.next()
            if (bullet.owner == BulletOwner.PLAYER) {
                bullet.z += bullet.dz
                if (bullet.z > 1200f) {
                    bulletsIterator.remove()
                }
            } else {
                // Enemy bullet flies towards smaller Z values
                bullet.z += bullet.dz * speedFactor
                bullet.x += bullet.dx * speedFactor
                bullet.y += bullet.dy * speedFactor
                // Let's check collision when z aligns near player boundary (Z was 100f)
                val buffer = 15f
                if (bullet.z <= playerZ + buffer && bullet.z >= playerZ - buffer) {
                    // Check horizontal distance
                    val distSq = (bullet.x - playerX) * (bullet.x - playerX) + (bullet.y - playerY) * (bullet.y - playerY)
                    if (distSq <= 625f) { // 25dp radius collision
                        bulletsIterator.remove()
                        onPlayerHit()
                        continue
                    }
                }
                if (bullet.z < 2f) {
                    bulletsIterator.remove()
                }
            }
        }
        bullets3D.value = bullets

        // 5. Update Enemies
        val enemies = enemies3D.value.toMutableList()
        val enemiesIterator = enemies.iterator()
        while (enemiesIterator.hasNext()) {
            val enemy = enemiesIterator.next()
            enemy.customAge += 0.05f * speedFactor

            // Handle specific enemy behaviors
            when (enemy.type) {
                EnemyType.WEAVER -> {
                    // Oscillates on X coordinate (Cosine swing)
                    enemy.x += (cos(enemy.customAge) * 6f) * speedFactor
                    enemy.z += enemy.dz * speedFactor
                }
                EnemyType.PHANTOM -> {
                    // Slower forwards but does quick teleports sideways and becomes semi-transparent
                    if (frameCount % 60 == 0) {
                        enemy.x += (random.nextFloat() * 150f - 75f)
                    }
                    enemy.z += enemy.dz * speedFactor
                }
                EnemyType.BOSS_MOTHERSHIP -> {
                    // Emerges to z=400, then hovers left-right
                    if (enemy.z > 400f) {
                        enemy.z -= 4f * speedFactor
                    } else {
                        enemy.x = sin(enemy.customAge * 0.5f) * 160f
                    }

                    // Boss attack patterns
                    enemy.shootCooldown--
                    if (enemy.shootCooldown <= 0) {
                        enemy.shootCooldown = 40 // shoots circular clusters
                        val newBullets = bullets3D.value.toMutableList()
                        val degrees = 8
                        for (i in 0 until degrees) {
                            val angle = (2 * Math.PI * i / degrees)
                            val bDx = (sin(angle) * 3f).toFloat()
                            val bDy = (cos(angle) * 3f).toFloat()
                            newBullets.add(Bullet3D(
                                System.nanoTime() + random.nextLong(),
                                enemy.x, enemy.y, enemy.z - 10f,
                                dx = bDx, dy = bDy, dz = -10f,
                                owner = BulletOwner.ENEMY, color = 0xFFFF3330
                            ))
                        }
                        bullets3D.value = newBullets
                    }
                }
                else -> { // SCOUT, GUNSHIP
                    enemy.z += enemy.dz * speedFactor
                    enemy.x += enemy.dx * speedFactor
                }
            }

            // Normal shooter fire logic for non-boss enemies
            if (enemy.type != EnemyType.BOSS_MOTHERSHIP) {
                enemy.shootCooldown--
                if (enemy.shootCooldown <= 0) {
                    // Aim and shoot at player depth
                    enemy.shootCooldown = when (enemy.type) {
                        EnemyType.GUNSHIP -> 60
                        EnemyType.WEAVER -> 90
                        else -> 120
                    }
                    val currentBullets = bullets3D.value.toMutableList()
                    if (enemy.type == EnemyType.GUNSHIP) {
                        // Spread shotgun fire
                        currentBullets.add(Bullet3D(System.nanoTime() + random.nextLong(), enemy.x, enemy.y, enemy.z, dx = -2f, dy = 0f, dz = -14f, owner = BulletOwner.ENEMY, color = 0xFFFF9900))
                        currentBullets.add(Bullet3D(System.nanoTime() + random.nextLong(), enemy.x, enemy.y, enemy.z, dx = 0f, dy = 0f, dz = -16f, owner = BulletOwner.ENEMY, color = 0xFFFF9900))
                        currentBullets.add(Bullet3D(System.nanoTime() + random.nextLong(), enemy.x, enemy.y, enemy.z, dx = 2f, dy = 0f, dz = -14f, owner = BulletOwner.ENEMY, color = 0xFFFF9900))
                    } else {
                        // Standard single bullet towards player orientation
                        currentBullets.add(Bullet3D(System.nanoTime() + random.nextLong(), enemy.x, enemy.y, enemy.z, dx = 0f, dy = 0f, dz = -15f, owner = BulletOwner.ENEMY, color = 0xFFFF3300))
                    }
                    bullets3D.value = currentBullets
                }
            }

            // Boundary and Player collisions
            if (enemy.z <= playerZ + 20f && enemy.z >= playerZ - 20f) {
                // If it crashes directly with the player (depth intersects)
                val distSq = (enemy.x - playerX) * (enemy.x - playerX) + (enemy.y - playerY) * (enemy.y - playerY)
                val hitDist = if (enemy.type == EnemyType.BOSS_MOTHERSHIP) 2500f else 1225f // larger radius for boss
                if (distSq <= hitDist) {
                    if (enemy.type != EnemyType.BOSS_MOTHERSHIP) {
                        enemiesIterator.remove()
                    }
                    onPlayerHit()
                    continue
                }
            }

            // Beyond screen boundary: clean up
            if (enemy.z < 10f) {
                enemiesIterator.remove()
            }
        }
        enemies3D.value = enemies

        // 6. Check Bullet vs Enemy Collisions (3D Perspective intersecting checks)
        checkBulletCollisions()

        // 7. Update Explosion Particles
        updateParticles(speedFactor)
    }

    private fun updateSkills(tickMs: Int) {
        if (skillAurora.currentCooldownMs > 0) {
            skillAurora.currentCooldownMs = (skillAurora.currentCooldownMs - tickMs).coerceAtLeast(0)
        }
        if (skillTimeWarp.currentCooldownMs > 0) {
            skillTimeWarp.currentCooldownMs = (skillTimeWarp.currentCooldownMs - tickMs).coerceAtLeast(0)
        }
        if (skillTimeWarp.activeRemainingMs > 0) {
            skillTimeWarp.activeRemainingMs = (skillTimeWarp.activeRemainingMs - tickMs).coerceAtLeast(0)
        }
        if (skillShield.currentCooldownMs > 0) {
            skillShield.currentCooldownMs = (skillShield.currentCooldownMs - tickMs).coerceAtLeast(0)
        }
    }

    private fun handleEnemySpawning() {
        if (isBossStage) {
            // Stage 5 involves purely defeating the Mothership boss
            if (!bossSpawned) {
                bossSpawned = true
                val list = enemies3D.value.toMutableList()
                val boss = Enemy3D(
                    id = System.nanoTime(),
                    type = EnemyType.BOSS_MOTHERSHIP,
                    x = 0f, y = -100f, z = 900f,
                    dx = 0f, dy = 0f, dz = -2f,
                    health = bossMaxHealth, maxHealth = bossMaxHealth,
                    shootCooldown = 40
                )
                list.add(boss)
                enemies3D.value = list
                bossHealth = bossMaxHealth
            }
            return
        }

        // Normal stages: spawn streams of enemies based on timing rules
        val spawnInterval = when (currentStage) {
            1 -> 90
            2 -> 75
            3 -> 60
            4 -> 45
            else -> 60
        }

        if (frameCount % spawnInterval == 0) {
            val list = enemies3D.value.toMutableList()
            if (list.size < 12) { // Cap max active normal enemies
                val etype = when (currentStage) {
                    1 -> EnemyType.SCOUT
                    2 -> if (random.nextFloat() < 0.35f) EnemyType.WEAVER else EnemyType.SCOUT
                    3 -> {
                        val randIdx = random.nextFloat()
                        if (randIdx < 0.25f) EnemyType.GUNSHIP else if (randIdx < 0.55f) EnemyType.WEAVER else EnemyType.SCOUT
                    }
                    4 -> {
                        val randIdx = random.nextFloat()
                        if (randIdx < 0.2f) EnemyType.PHANTOM else if (randIdx < 0.45f) EnemyType.GUNSHIP else if (randIdx < 0.7f) EnemyType.WEAVER else EnemyType.SCOUT
                    }
                    else -> EnemyType.SCOUT
                }

                val speedZ = when (etype) {
                    EnemyType.SCOUT -> -6f - currentStage
                    EnemyType.WEAVER -> -5f - currentStage
                    EnemyType.GUNSHIP -> -4f - currentStage
                    EnemyType.PHANTOM -> -7f - currentStage
                    else -> -5f
                }

                val enemyHp = when (etype) {
                    EnemyType.SCOUT -> 1f
                    EnemyType.WEAVER -> 2f
                    EnemyType.GUNSHIP -> 5f
                    EnemyType.PHANTOM -> 3f
                    else -> 1f
                }

                val newEnemy = Enemy3D(
                    id = System.nanoTime() + random.nextLong(),
                    type = etype,
                    x = random.nextFloat() * 320f - 160f, // width spread
                    y = random.nextFloat() * 200f - 150f, // centered height spread
                    z = 1050f,
                    dx = 0f, dy = 0f, dz = speedZ,
                    health = enemyHp, maxHealth = enemyHp,
                    shootCooldown = random.nextInt(40, 100)
                )
                list.add(newEnemy)
                enemies3D.value = list
            }
        }

        // Transition: Earn enough score to complete normal levels
        val scoreToNextStage = currentStage * 2000
        if (score >= scoreToNextStage) {
            triggerStageClear()
        }
    }

    private fun checkBulletCollisions() {
        val currBullets = bullets3D.value.toMutableList()
        val currEnemies = enemies3D.value.toMutableList()

        val bulletIterator = currBullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            if (bullet.owner == BulletOwner.ENEMY) continue // Enemy bullets hit player separately

            // Scan all enemies to check for depth overlaps
            val enemiesList = currEnemies.listIterator()
            var bulletExploded = false
            while (enemiesList.hasNext()) {
                val enemy = enemiesList.next()
                val depthBuffer = if (enemy.type == EnemyType.BOSS_MOTHERSHIP) 50f else 20f
                if (bullet.z >= enemy.z - depthBuffer && bullet.z <= enemy.z + depthBuffer) {
                    // Check horizontal spacing bounding
                    val enemyRadiusSq = when (enemy.type) {
                        EnemyType.BOSS_MOTHERSHIP -> 4900f // 70dp hitbox
                        EnemyType.GUNSHIP -> 900f // 30dp hitbox
                        EnemyType.WEAVER -> 400f  // 20dp hitbox
                        else -> 225f             // 15dp hitbox
                    }
                    val distX = bullet.x - enemy.x
                    val distY = bullet.y - enemy.y
                    if (distX * distX + distY * distY <= enemyRadiusSq) {
                        // Collision hit detected!
                        bulletExploded = true
                        enemy.health -= bullet.damage

                        if (enemy.type == EnemyType.BOSS_MOTHERSHIP) {
                            bossHealth = enemy.health.coerceAtLeast(0f)
                            shakeAmplitude = 12f
                            spawn3DExplosion(bullet.x, bullet.y, bullet.z, 0xFFFFCC00) // Sparks on Boss
                        } else {
                            spawn3DExplosion(enemy.x, enemy.y, enemy.z, 0xFFFF3330) // Red hot sparks
                        }

                        if (enemy.health <= 0f) {
                            // Destroyed
                            val points = when (enemy.type) {
                                EnemyType.SCOUT -> 100
                                EnemyType.WEAVER -> 150
                                EnemyType.GUNSHIP -> 250
                                EnemyType.PHANTOM -> 350
                                EnemyType.BOSS_MOTHERSHIP -> 5000
                            }
                            score += points
                            spawn3DExplosion(enemy.x, enemy.y, enemy.z, 0xFFFF7700, count = 18) // Big burst

                            if (enemy.type == EnemyType.BOSS_MOTHERSHIP) {
                                triggerStageClear()
                            }
                            enemiesList.remove()
                        } else {
                            enemiesList.set(enemy) // Update HP State
                        }
                        break // bullet explodes and exits enemy scans
                    }
                }
            }
            if (bulletExploded) {
                bulletIterator.remove()
            }
        }
        bullets3D.value = currBullets
        enemies3D.value = currEnemies
    }

    private fun onPlayerHit() {
        if (isGameOver || isStageCleared) return

        if (shieldCharges > 0) {
            shieldCharges--
            shakeAmplitude = 8f
            spawn3DExplosion(playerX, playerY, playerZ, 0xFFCC00FF, count = 10) // purple energy spark protection
            return
        }

        playerHealth--
        shakeAmplitude = 26f
        spawn3DExplosion(playerX, playerY, playerZ, 0xFFFF0000, count = 15) // Angry Red structural blast

        if (playerHealth <= 0) {
            isGameOver = true
            stop()
            onGameOver(score, currentStage)
        }
    }

    private fun triggerStageClear() {
        isStageCleared = true
        stop()
    }

    private fun spawn3DExplosion(px: Float, py: Float, pz: Float, colorHex: Long, count: Int = 8) {
        val list = particles3D.value.toMutableList()
        for (i in 0 until count) {
            val angleTheta = random.nextFloat() * 2 * Math.PI
            val anglePhi = random.nextFloat() * Math.PI
            val speed = random.nextFloat() * 8f + 3f
            val dx = (speed * sin(anglePhi) * cos(angleTheta)).toFloat()
            val dy = (speed * sin(anglePhi) * sin(angleTheta)).toFloat()
            val dz = (speed * cos(anglePhi)).toFloat()
            val life = random.nextFloat() * 20f + 10f
            list.add(Particle3D(px, py, pz, dx, dy, dz, life, life, colorHex))
        }
        particles3D.value = list
    }

    private fun updateParticles(speedFactor: Float) {
        val list = particles3D.value.toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= 1f * speedFactor
            if (p.life <= 0f) {
                iterator.remove()
            } else {
                p.x += p.dx * speedFactor
                p.y += p.dy * speedFactor
                p.z += p.dz * speedFactor
            }
        }
        particles3D.value = list
    }
}
