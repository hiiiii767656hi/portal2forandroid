package com.example.game.physics

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vec2(x / scalar, y / scalar) else Vec2(0f, 0f)

    fun lengthSquared(): Float = x * x + y * y
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec2 {
        val l = length()
        return if (l > 0.0001f) Vec2(x / l, y / l) else Vec2(0f, 0f)
    }

    fun dot(other: Vec2): Float = x * other.x + y * other.y
    fun cross(other: Vec2): Float = x * other.y - y * other.x

    fun distanceTo(other: Vec2): Float = (this - other).length()

    companion object {
        val ZERO = Vec2(0f, 0f)
        val UP = Vec2(0f, -1f)
        val DOWN = Vec2(0f, 1f)
        val LEFT = Vec2(-1f, 0f)
        val RIGHT = Vec2(1f, 0f)
    }
}

enum class SurfaceType {
    PORTALABLE,     // White Aperture concrete / tiles
    NON_PORTALABLE, // Dark metal / grates
    ACID,           // Toxic goo that resets the player
    EMANCIPATION_GRID // Fizzles portals and cubes
}

data class Wall(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val type: SurfaceType = SurfaceType.PORTALABLE,
    val isExitDoor: Boolean = false,
    val isEntryDoor: Boolean = false
) {
    val left: Float get() = x
    val right: Float get() = x + width
    val top: Float get() = y
    val bottom: Float get() = y + height

    fun intersects(otherX: Float, otherY: Float, otherW: Float, otherH: Float): Boolean {
        return otherX < right && otherX + otherW > left && otherY < bottom && otherY + otherH > top
    }
}

enum class PortalType {
    BLUE, ORANGE
}

data class Portal(
    val type: PortalType,
    val active: Boolean = false,
    val position: Vec2 = Vec2.ZERO,
    val normal: Vec2 = Vec2.ZERO, // Points away from wall into room
    val width: Float = 70f
) {
    // Portal segment endpoints
    val p1: Vec2 get() {
        val tangent = Vec2(-normal.y, normal.x)
        return position + tangent * (width * 0.5f)
    }
    val p2: Vec2 get() {
        val tangent = Vec2(-normal.y, normal.x)
        return position - tangent * (width * 0.5f)
    }
}

data class WeightedCube(
    val id: String = "cube_0",
    var pos: Vec2 = Vec2.ZERO,
    var vel: Vec2 = Vec2.ZERO,
    val size: Float = 36f,
    var isHeld: Boolean = false,
    var onButton: Boolean = false,
    var portalCooldown: Float = 0f
) {
    val halfSize: Float get() = size * 0.5f
}

data class HeavyDutyButton(
    val id: String = "btn_0",
    val x: Float,
    val y: Float,
    val width: Float = 60f,
    val height: Float = 16f,
    var isPressed: Boolean = false,
    val targetsDoor: Boolean = true
)

data class LaserEmitter(
    val x: Float,
    val y: Float,
    val direction: Vec2,
    var isActive: Boolean = true
)

data class LaserReceiver(
    val x: Float,
    val y: Float,
    val size: Float = 30f,
    var isPowered: Boolean = false
)

data class EnergyPellet(
    var pos: Vec2,
    var vel: Vec2,
    var lifeTime: Float = 15f
)

data class AerialFaithPlate(
    val x: Float,
    val y: Float,
    val width: Float = 50f,
    val launchVel: Vec2
)

data class RaycastHit(
    val point: Vec2,
    val normal: Vec2,
    val wall: Wall,
    val distance: Float
)
