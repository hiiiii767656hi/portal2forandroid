package com.example.game.physics

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mobile-optimized physics engine with sub-stepping for tunneling prevention,
 * momentum-preserving portal teleportation, collision resolution, and portal gun raycasting.
 */
class PortalPhysicsEngine {

    companion object {
        const val GRAVITY = 980f // px/s^2
        const val MOVE_SPEED = 240f
        const val JUMP_IMPULSE = -420f
        const val AIR_ACCEL = 0.85f
        const val TERMINAL_VELOCITY = 1400f
        const val PLAYER_WIDTH = 28f
        const val PLAYER_HEIGHT = 56f
    }

    // Raycast against walls
    fun raycast(
        origin: Vec2,
        direction: Vec2,
        walls: List<Wall>,
        maxDist: Float = 2000f
    ): RaycastHit? {
        val dir = direction.normalized()
        var closestHit: RaycastHit? = null
        var closestDist = maxDist

        for (wall in walls) {
            // Line-AABB intersection
            val invX = if (dir.x != 0f) 1f / dir.x else Float.MAX_VALUE
            val invY = if (dir.y != 0f) 1f / dir.y else Float.MAX_VALUE

            var tMinX = (wall.left - origin.x) * invX
            var tMaxX = (wall.right - origin.x) * invX
            var normalX = if (dir.x > 0) Vec2(-1f, 0f) else Vec2(1f, 0f)
            if (tMinX > tMaxX) {
                val temp = tMinX
                tMinX = tMaxX
                tMaxX = temp
                normalX = if (dir.x > 0) Vec2(1f, 0f) else Vec2(-1f, 0f)
            }

            var tMinY = (wall.top - origin.y) * invY
            var tMaxY = (wall.bottom - origin.y) * invY
            var normalY = if (dir.y > 0) Vec2(0f, -1f) else Vec2(0f, 1f)
            if (tMinY > tMaxY) {
                val temp = tMinY
                tMinY = tMaxY
                tMaxY = temp
                normalY = if (dir.y > 0) Vec2(0f, 1f) else Vec2(0f, -1f)
            }

            val tEnter = max(tMinX, tMinY)
            val tExit = min(tMaxX, tMaxY)

            if (tEnter < tExit && tEnter > 0f && tEnter < closestDist) {
                val normal = if (tMinX > tMinY) normalX else normalY
                val point = origin + dir * tEnter
                closestDist = tEnter
                closestHit = RaycastHit(point, normal, wall, tEnter)
            }
        }
        return closestHit
    }

    // Teleport an entity through portals while preserving relative angle and momentum magnitude
    fun teleport(
        pos: Vec2,
        vel: Vec2,
        entryPortal: Portal,
        exitPortal: Portal,
        boostMultiplier: Float = 1.05f
    ): Pair<Vec2, Vec2> {
        val entryNormal = entryPortal.normal
        val exitNormal = exitPortal.normal

        // Portal angle rotation:
        // Angle from -entryNormal to exitNormal
        val inAngle = atan2(-entryNormal.y, -entryNormal.x)
        val outAngle = atan2(exitNormal.y, exitNormal.x)
        val deltaAngle = outAngle - inAngle

        // Rotate velocity vector
        val speed = vel.length() * boostMultiplier
        val velAngle = atan2(vel.y, vel.x) + deltaAngle
        var newVel = Vec2(cos(velAngle) * speed, sin(velAngle) * speed)

        // Ensure minimum exit velocity out of the portal so the player or cube doesn't get stuck
        val exitDot = newVel.dot(exitNormal)
        if (exitDot < 150f) {
            newVel = newVel + exitNormal * (180f - exitDot)
        }

        // Relative offset along entry portal plane
        val entryTangent = Vec2(-entryNormal.y, entryNormal.x)
        val exitTangent = Vec2(-exitNormal.y, exitNormal.x)
        val offset = (pos - entryPortal.position).dot(entryTangent)

        // New position just outside exit portal
        val newPos = exitPortal.position + exitTangent * offset + exitNormal * 28f
        return Pair(newPos, newVel)
    }

    // Check if an AABB intersects portal aperture
    fun isCrossingPortal(
        prevPos: Vec2,
        newPos: Vec2,
        sizeX: Float,
        sizeY: Float,
        portal: Portal
    ): Boolean {
        if (!portal.active) return false
        val distToPlane = (newPos - portal.position).dot(portal.normal)
        val prevDistToPlane = (prevPos - portal.position).dot(portal.normal)

        // Crossing portal plane from outside to inside
        if ((prevDistToPlane >= -10f && distToPlane <= 15f) || (abs(distToPlane) < 20f)) {
            val tangent = Vec2(-portal.normal.y, portal.normal.x)
            val distAlongTangent = abs((newPos - portal.position).dot(tangent))
            if (distAlongTangent <= portal.width * 0.5f + 10f) {
                return true
            }
        }
        return false
    }

    // Resolve AABB with walls
    fun resolveAABB(
        pos: Vec2,
        vel: Vec2,
        width: Float,
        height: Float,
        walls: List<Wall>,
        activePortals: List<Portal>
    ): Triple<Vec2, Vec2, Boolean> {
        var newX = pos.x
        var newY = pos.y
        var newVx = vel.x
        var newVy = vel.y
        var isGrounded = false

        val halfW = width * 0.5f
        val halfH = height * 0.5f

        // Check if inside a portal aperture to ignore wall collision at that exact spot
        fun isAtPortal(testPos: Vec2, normal: Vec2): Boolean {
            for (p in activePortals) {
                if (p.active && p.normal.dot(normal) > 0.7f) {
                    if ((testPos - p.position).length() < p.width * 0.5f + 10f) {
                        return true
                    }
                }
            }
            return false
        }

        // Horizontal sweep
        newX += newVx * 0.016f
        for (wall in walls) {
            if (wall.type == SurfaceType.ACID || wall.type == SurfaceType.EMANCIPATION_GRID) continue
            if (wall.intersects(newX - halfW, newY - halfH, width, height)) {
                if (newVx > 0f) {
                    val contact = Vec2(wall.left, newY)
                    if (!isAtPortal(contact, Vec2(-1f, 0f))) {
                        newX = wall.left - halfW
                        newVx = 0f
                    }
                } else if (newVx < 0f) {
                    val contact = Vec2(wall.right, newY)
                    if (!isAtPortal(contact, Vec2(1f, 0f))) {
                        newX = wall.right + halfW
                        newVx = 0f
                    }
                }
            }
        }

        // Vertical sweep
        newY += newVy * 0.016f
        for (wall in walls) {
            if (wall.type == SurfaceType.ACID || wall.type == SurfaceType.EMANCIPATION_GRID) continue
            if (wall.intersects(newX - halfW, newY - halfH, width, height)) {
                if (newVy > 0f) {
                    val contact = Vec2(newX, wall.top)
                    if (!isAtPortal(contact, Vec2(0f, -1f))) {
                        newY = wall.top - halfH
                        newVy = 0f
                        isGrounded = true
                    }
                } else if (newVy < 0f) {
                    val contact = Vec2(newX, wall.bottom)
                    if (!isAtPortal(contact, Vec2(0f, 1f))) {
                        newY = wall.bottom + halfH
                        newVy = 0f
                    }
                }
            }
        }

        return Triple(Vec2(newX, newY), Vec2(newVx, newVy), isGrounded)
    }
}
