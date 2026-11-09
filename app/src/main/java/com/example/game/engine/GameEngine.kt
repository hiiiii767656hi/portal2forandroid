package com.example.game.engine

import com.example.data.model.GameSave
import com.example.game.level.ChamberCatalog
import com.example.game.level.TestChamber
import com.example.game.physics.HeavyDutyButton
import com.example.game.physics.Portal
import com.example.game.physics.PortalPhysicsEngine
import com.example.game.physics.PortalType
import com.example.game.physics.SurfaceType
import com.example.game.physics.Vec2
import com.example.game.physics.WeightedCube
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class GameState(
    val chamber: TestChamber,
    val playerPos: Vec2,
    val playerVel: Vec2,
    val isGrounded: Boolean = false,
    val isFacingRight: Boolean = true,
    val bluePortal: Portal = Portal(PortalType.BLUE),
    val orangePortal: Portal = Portal(PortalType.ORANGE),
    val cubes: List<WeightedCube> = emptyList(),
    val buttons: List<HeavyDutyButton> = emptyList(),
    val isDoorOpen: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val isDead: Boolean = false,
    val deathReason: String = "",
    val portalsFired: Long = 0L,
    val elapsedSeconds: Double = 0.0,
    val heldCubeId: String? = null,
    val aimingRayStart: Vec2? = null,
    val aimingRayEnd: Vec2? = null,
    val aimingColor: PortalType? = null
)

class GameEngine(initialChamberIndex: Int = 0) {

    private val physics = PortalPhysicsEngine()
    private val allChambers = ChamberCatalog.chambers
    var currentChamberIndex = initialChamberIndex
        private set

    private var currentChamber = allChambers[currentChamberIndex]

    private val _state = MutableStateFlow(createInitialState(currentChamber))
    val state = _state.asStateFlow()

    // Portal cooldown to prevent instant flip-flop re-teleportation
    private var playerTeleportCooldown = 0f

    private fun createInitialState(chamber: TestChamber): GameState {
        return GameState(
            chamber = chamber,
            playerPos = chamber.playerStart,
            playerVel = Vec2.ZERO,
            isGrounded = false,
            bluePortal = Portal(PortalType.BLUE),
            orangePortal = Portal(PortalType.ORANGE),
            cubes = chamber.cubes.map { it.copy() },
            buttons = chamber.buttons.map { it.copy() },
            isDoorOpen = chamber.buttons.isEmpty(),
            isLevelCompleted = false,
            isDead = false,
            deathReason = "",
            portalsFired = 0L,
            elapsedSeconds = 0.0,
            heldCubeId = null
        )
    }

    fun loadChamber(index: Int) {
        currentChamberIndex = index.coerceIn(0, allChambers.size - 1)
        currentChamber = allChambers[currentChamberIndex]
        playerTeleportCooldown = 0f
        _state.value = createInitialState(currentChamber)
    }

    fun restartChamber() {
        loadChamber(currentChamberIndex)
    }

    fun nextChamber(): Boolean {
        if (currentChamberIndex < allChambers.size - 1) {
            loadChamber(currentChamberIndex + 1)
            return true
        }
        return false
    }

    // Restore from Cloud Save
    fun restoreFromSave(save: GameSave) {
        val chamber = allChambers.find { it.id == save.chamberId } ?: allChambers[0]
        currentChamber = chamber
        currentChamberIndex = allChambers.indexOf(chamber).coerceAtLeast(0)

        val blue = Portal(
            type = PortalType.BLUE,
            active = save.bluePortalActive,
            position = Vec2(save.bluePortalX.toFloat(), save.bluePortalY.toFloat()),
            normal = Vec2(save.bluePortalNormalX.toFloat(), save.bluePortalNormalY.toFloat())
        )
        val orange = Portal(
            type = PortalType.ORANGE,
            active = save.orangePortalActive,
            position = Vec2(save.orangePortalX.toFloat(), save.orangePortalY.toFloat()),
            normal = Vec2(save.orangePortalNormalX.toFloat(), save.orangePortalNormalY.toFloat())
        )

        _state.value = GameState(
            chamber = chamber,
            playerPos = Vec2(save.playerPosX.toFloat(), save.playerPosY.toFloat()),
            playerVel = Vec2(save.playerVelX.toFloat(), save.playerVelY.toFloat()),
            isGrounded = false,
            bluePortal = blue,
            orangePortal = orange,
            cubes = chamber.cubes.map { it.copy() },
            buttons = chamber.buttons.map { it.copy() },
            isDoorOpen = chamber.buttons.isEmpty(),
            isLevelCompleted = save.isCompleted,
            isDead = false,
            deathReason = "",
            portalsFired = save.portalsFired,
            elapsedSeconds = save.elapsedSeconds,
            heldCubeId = null
        )
    }

    // Export state for Cloud Save
    fun exportSave(): GameSave {
        val s = _state.value
        return GameSave(
            chamberId = s.chamber.id,
            chamberName = s.chamber.title,
            playerPosX = s.playerPos.x.toDouble(),
            playerPosY = s.playerPos.y.toDouble(),
            playerVelX = s.playerVel.x.toDouble(),
            playerVelY = s.playerVel.y.toDouble(),
            bluePortalActive = s.bluePortal.active,
            orangePortalActive = s.orangePortal.active,
            bluePortalX = s.bluePortal.position.x.toDouble(),
            bluePortalY = s.bluePortal.position.y.toDouble(),
            bluePortalNormalX = s.bluePortal.normal.x.toDouble(),
            bluePortalNormalY = s.bluePortal.normal.y.toDouble(),
            orangePortalX = s.orangePortal.position.x.toDouble(),
            orangePortalY = s.orangePortal.position.y.toDouble(),
            orangePortalNormalX = s.orangePortal.normal.x.toDouble(),
            orangePortalNormalY = s.orangePortal.normal.y.toDouble(),
            elapsedSeconds = s.elapsedSeconds,
            portalsFired = s.portalsFired,
            isCompleted = s.isLevelCompleted
        )
    }

    // Fire Portal Gun
    fun firePortal(type: PortalType, targetDirection: Vec2) {
        val s = _state.value
        if (s.isDead || s.isLevelCompleted) return
        val origin = s.playerPos + Vec2(0f, -PortalPhysicsEngine.PLAYER_HEIGHT * 0.25f)
        val dir = targetDirection.normalized()
        if (dir.lengthSquared() < 0.01f) return

        val hit = physics.raycast(origin, dir, s.chamber.walls)
        if (hit != null && hit.wall.type == SurfaceType.PORTALABLE) {
            val portalPos = hit.point + hit.normal * 2f
            val newPortal = Portal(
                type = type,
                active = true,
                position = portalPos,
                normal = hit.normal
            )
            _state.value = if (type == PortalType.BLUE) {
                s.copy(
                    bluePortal = newPortal,
                    portalsFired = s.portalsFired + 1,
                    aimingRayStart = origin,
                    aimingRayEnd = hit.point,
                    aimingColor = PortalType.BLUE
                )
            } else {
                s.copy(
                    orangePortal = newPortal,
                    portalsFired = s.portalsFired + 1,
                    aimingRayStart = origin,
                    aimingRayEnd = hit.point,
                    aimingColor = PortalType.ORANGE
                )
            }
        } else {
            // Fired at non-portalable wall (fizzle visual)
            _state.value = s.copy(
                portalsFired = s.portalsFired + 1,
                aimingRayStart = origin,
                aimingRayEnd = hit?.point ?: (origin + dir * 1000f),
                aimingColor = type
            )
        }
    }

    // Pick up or drop weighted companion cube
    fun toggleCubeInteract() {
        val s = _state.value
        if (s.heldCubeId != null) {
            // Drop cube
            val updatedCubes = s.cubes.map {
                if (it.id == s.heldCubeId) {
                    it.copy(
                        isHeld = false,
                        vel = Vec2(if (s.isFacingRight) 160f else -160f, -80f)
                    )
                } else it
            }
            _state.value = s.copy(cubes = updatedCubes, heldCubeId = null)
        } else {
            // Find closest cube within reach (65px)
            val closest = s.cubes.find { cube ->
                (cube.pos - s.playerPos).length() < 70f
            }
            if (closest != null) {
                val updatedCubes = s.cubes.map {
                    if (it.id == closest.id) it.copy(isHeld = true, vel = Vec2.ZERO) else it
                }
                _state.value = s.copy(cubes = updatedCubes, heldCubeId = closest.id)
            }
        }
    }

    // Step physics update (fixed dt for deterministic mobile performance)
    fun update(
        dt: Float,
        moveInputX: Float, // -1f .. +1f joystick input
        jumpRequested: Boolean
    ) {
        val s = _state.value
        if (s.isDead || s.isLevelCompleted) return

        if (playerTeleportCooldown > 0f) {
            playerTeleportCooldown -= dt
        }

        // Sub-step physics (2 passes per frame) to prevent fast momentum tunneling
        val subDt = dt / 2f
        var currentPos = s.playerPos
        var currentVel = s.playerVel
        var isGrounded = s.isGrounded
        var bluePortal = s.bluePortal
        var orangePortal = s.orangePortal
        val activePortals = listOfNotNull(
            if (bluePortal.active) bluePortal else null,
            if (orangePortal.active) orangePortal else null
        )

        var newDead = false
        var deathReason = ""

        repeat(2) {
            // 1. Horizontal acceleration
            if (isGrounded) {
                currentVel = Vec2(moveInputX * PortalPhysicsEngine.MOVE_SPEED, currentVel.y)
            } else {
                val targetVx = moveInputX * PortalPhysicsEngine.MOVE_SPEED
                val airBlend = 0.05f
                currentVel = Vec2(currentVel.x * (1f - airBlend) + targetVx * airBlend, currentVel.y)
            }

            // 2. Jump Impulse
            if (jumpRequested && isGrounded) {
                currentVel = Vec2(currentVel.x, PortalPhysicsEngine.JUMP_IMPULSE)
                isGrounded = false
            }

            // 3. Gravity
            currentVel = Vec2(
                currentVel.x,
                min(currentVel.y + PortalPhysicsEngine.GRAVITY * subDt, PortalPhysicsEngine.TERMINAL_VELOCITY)
            )

            // 4. Faith Plates check
            for (fp in s.chamber.faithPlates) {
                if (abs(currentPos.x - (fp.x + fp.width * 0.5f)) < fp.width * 0.5f &&
                    abs(currentPos.y - fp.y) < 25f
                ) {
                    currentVel = fp.launchVel
                    isGrounded = false
                }
            }

            // 5. Portal Teleportation Check (Player)
            val prevPos = currentPos
            val proposedPos = currentPos + currentVel * subDt

            if (playerTeleportCooldown <= 0f && bluePortal.active && orangePortal.active) {
                if (physics.isCrossingPortal(prevPos, proposedPos, PortalPhysicsEngine.PLAYER_WIDTH, PortalPhysicsEngine.PLAYER_HEIGHT, bluePortal)) {
                    val (telePos, teleVel) = physics.teleport(currentPos, currentVel, bluePortal, orangePortal)
                    currentPos = telePos
                    currentVel = teleVel
                    playerTeleportCooldown = 0.25f // Cooldown
                } else if (physics.isCrossingPortal(prevPos, proposedPos, PortalPhysicsEngine.PLAYER_WIDTH, PortalPhysicsEngine.PLAYER_HEIGHT, orangePortal)) {
                    val (telePos, teleVel) = physics.teleport(currentPos, currentVel, orangePortal, bluePortal)
                    currentPos = telePos
                    currentVel = teleVel
                    playerTeleportCooldown = 0.25f
                }
            }

            // 6. Wall & Obstacle Collision Resolution
            val (resolvedPos, resolvedVel, grounded) = physics.resolveAABB(
                currentPos,
                currentVel,
                PortalPhysicsEngine.PLAYER_WIDTH,
                PortalPhysicsEngine.PLAYER_HEIGHT,
                s.chamber.walls,
                activePortals
            )
            currentPos = resolvedPos
            currentVel = resolvedVel
            isGrounded = grounded

            // 7. Acid / Hazard check
            for (wall in s.chamber.walls) {
                if (wall.type == SurfaceType.ACID) {
                    if (wall.intersects(
                            currentPos.x - PortalPhysicsEngine.PLAYER_WIDTH * 0.5f,
                            currentPos.y - PortalPhysicsEngine.PLAYER_HEIGHT * 0.5f,
                            PortalPhysicsEngine.PLAYER_WIDTH,
                            PortalPhysicsEngine.PLAYER_HEIGHT
                        )
                    ) {
                        newDead = true
                        deathReason = "Subject submerged in lethal Aperture toxic goo."
                    }
                } else if (wall.type == SurfaceType.EMANCIPATION_GRID) {
                    if (wall.intersects(
                            currentPos.x - PortalPhysicsEngine.PLAYER_WIDTH * 0.5f,
                            currentPos.y - PortalPhysicsEngine.PLAYER_HEIGHT * 0.5f,
                            PortalPhysicsEngine.PLAYER_WIDTH,
                            PortalPhysicsEngine.PLAYER_HEIGHT
                        )
                    ) {
                        // Fizzle portals
                        bluePortal = bluePortal.copy(active = false)
                        orangePortal = orangePortal.copy(active = false)
                    }
                }
            }
        }

        // 8. Update Cubes & Buttons
        val updatedCubes = s.cubes.map { cube ->
            var cubePos = cube.pos
            var cubeVel = cube.vel
            var cubeCooldown = max(0f, cube.portalCooldown - dt)

            if (cube.isHeld) {
                // Held in front of player
                val holdOffsetX = if (s.isFacingRight) 32f else -32f
                cubePos = Vec2(currentPos.x + holdOffsetX, currentPos.y - 10f)
                cubeVel = currentVel
            } else {
                // Gravity on cube
                cubeVel = Vec2(cubeVel.x * 0.98f, min(cubeVel.y + PortalPhysicsEngine.GRAVITY * dt, PortalPhysicsEngine.TERMINAL_VELOCITY))

                // Cube portal teleportation
                if (cubeCooldown <= 0f && bluePortal.active && orangePortal.active) {
                    val prevCubePos = cubePos
                    val nextCubePos = cubePos + cubeVel * dt
                    if (physics.isCrossingPortal(prevCubePos, nextCubePos, cube.size, cube.size, bluePortal)) {
                        val (telePos, teleVel) = physics.teleport(cubePos, cubeVel, bluePortal, orangePortal)
                        cubePos = telePos
                        cubeVel = teleVel
                        cubeCooldown = 0.3f
                    } else if (physics.isCrossingPortal(prevCubePos, nextCubePos, cube.size, cube.size, orangePortal)) {
                        val (telePos, teleVel) = physics.teleport(cubePos, cubeVel, orangePortal, bluePortal)
                        cubePos = telePos
                        cubeVel = teleVel
                        cubeCooldown = 0.3f
                    }
                }

                // Resolve cube collisions with walls
                val (resPos, resVel, _) = physics.resolveAABB(
                    cubePos,
                    cubeVel,
                    cube.size,
                    cube.size,
                    s.chamber.walls,
                    activePortals
                )
                cubePos = resPos
                cubeVel = resVel
            }
            cube.copy(pos = cubePos, vel = cubeVel, portalCooldown = cubeCooldown)
        }

        // Update button press status (by player or cube)
        var allButtonsPressed = true
        val updatedButtons = s.buttons.map { btn ->
            val playerOnButton = (abs(currentPos.x - (btn.x + btn.width * 0.5f)) < btn.width * 0.5f + 10f) &&
                    (abs(currentPos.y - btn.y) < 32f)
            val cubeOnButton = updatedCubes.any { c ->
                (abs(c.pos.x - (btn.x + btn.width * 0.5f)) < btn.width * 0.5f + 12f) &&
                        (abs(c.pos.y - btn.y) < 32f)
            }
            val pressed = playerOnButton || cubeOnButton
            if (!pressed) allButtonsPressed = false
            btn.copy(isPressed = pressed)
        }

        val doorOpen = s.buttons.isEmpty() || allButtonsPressed

        // 9. Exit door reach check
        val distToExit = (currentPos - s.chamber.exitDoorPos).length()
        val levelCompleted = doorOpen && distToExit < 45f

        val facingRight = if (moveInputX > 0.05f) true else if (moveInputX < -0.05f) false else s.isFacingRight

        _state.value = s.copy(
            playerPos = currentPos,
            playerVel = currentVel,
            isGrounded = isGrounded,
            isFacingRight = facingRight,
            bluePortal = bluePortal,
            orangePortal = orangePortal,
            cubes = updatedCubes,
            buttons = updatedButtons,
            isDoorOpen = doorOpen,
            isLevelCompleted = levelCompleted,
            isDead = newDead,
            deathReason = deathReason,
            elapsedSeconds = s.elapsedSeconds + dt
        )
    }
}
