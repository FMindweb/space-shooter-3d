package com.example.game

// Representing 3D coordinates in space (Perspective projection will handle drawing them onto the 2D Canvas)
data class Star3D(
    var x: Float,
    var y: Float,
    var z: Float,
    val color: Int // Indicator of star type or color index
)

enum class BulletOwner {
    PLAYER, ENEMY
}

data class Bullet3D(
    val id: Long,
    var x: Float,
    var y: Float,
    var z: Float,
    var dx: Float = 0f,
    var dy: Float = 0f,
    var dz: Float,
    val owner: BulletOwner,
    val damage: Int = 1,
    val color: Long = 0xFF00FFFF // Cyan for player, Orange/Red for enemy
)

enum class EnemyType {
    SCOUT,     // Basic drone running straight at player
    WEAVER,    // Sways side-to-side in a cosine wave
    GUNSHIP,   // Heavy vessel that shoots multiple bullets
    PHANTOM,   // Teleports or fades in and out
    BOSS_MOTHERSHIP // Huge stage 5 boss
}

data class Enemy3D(
    val id: Long,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var z: Float,
    var dx: Float,
    var dy: Float,
    var dz: Float,
    var health: Float,
    val maxHealth: Float,
    var shootCooldown: Int = 0,
    var customAge: Float = 0f // Used for pattern calculation (e.g. swaying)
)

data class Particle3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var dx: Float,
    var dy: Float,
    var dz: Float,
    var life: Float, // Age, decreases to 0
    val maxLife: Float,
    val color: Long
)

// Skills Status representation
data class SkillInfo(
    val name: String,
    val description: String,
    var cooldownMaxSeconds: Float,
    var currentCooldownMs: Long = 0,
    var durationMaxSeconds: Float = 0f,
    var activeRemainingMs: Long = 0
) {
    val isReady: Boolean get() = currentCooldownMs <= 0
    val isActive: Boolean get() = activeRemainingMs > 0
    val progress: Float get() = if (cooldownMaxSeconds > 0) (currentCooldownMs / (cooldownMaxSeconds * 1000f)).coerceIn(0f, 1f) else 0f
    val activeProgress: Float get() = if (durationMaxSeconds > 0) (activeRemainingMs / (durationMaxSeconds * 1000f)).coerceIn(0f, 1f) else 0f
}
