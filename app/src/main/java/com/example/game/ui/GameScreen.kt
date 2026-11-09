package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fitbit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameSave
import com.example.game.engine.GameEngine
import com.example.game.engine.GameState
import com.example.game.physics.Portal
import com.example.game.physics.PortalPhysicsEngine
import com.example.game.physics.PortalType
import com.example.game.physics.SurfaceType
import com.example.game.physics.Vec2
import com.example.game.physics.Wall
import com.example.game.physics.WeightedCube
import com.example.ui.theme.AcidGreen
import com.example.ui.theme.ApertureBorder
import com.example.ui.theme.ApertureDarkBackground
import com.example.ui.theme.ApertureSurface
import com.example.ui.theme.ApertureSurfaceVariant
import com.example.ui.theme.ApertureTextMuted
import com.example.ui.theme.ApertureTextPrimary
import com.example.ui.theme.ApertureTextSecondary
import com.example.ui.theme.ApertureYellow
import com.example.ui.theme.CompanionHeartPink
import com.example.ui.theme.EmancipationFizzlerCyan
import com.example.ui.theme.LaserRed
import com.example.ui.theme.PortalBlue
import com.example.ui.theme.PortalBlueGlow
import com.example.ui.theme.PortalOrange
import com.example.ui.theme.PortalOrangeGlow
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameScreen(
    gameEngine: GameEngine,
    onSaveToCloud: (GameSave) -> Unit,
    onRecordCompletion: (chamberId: String, time: Double, portals: Long) -> Unit,
    onOpenCloudManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by gameEngine.state.collectAsState()

    // Touch control state
    var moveJoystickX by remember { mutableFloatStateOf(0f) }
    var isJumpHeld by remember { mutableStateOf(false) }

    // Aiming joystick for Portal Gun
    var aimJoystickX by remember { mutableFloatStateOf(1f) }
    var aimJoystickY by remember { mutableFloatStateOf(0f) }

    var saveToastMessage by remember { mutableStateOf<String?>(null) }

    // 60FPS Game Loop with Delta Time
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastTime != 0L) {
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                    gameEngine.update(
                        dt = dt,
                        moveInputX = moveJoystickX,
                        jumpRequested = isJumpHeld
                    )
                }
                lastTime = now
            }
        }
    }

    // Report victory when chamber is completed
    LaunchedEffect(state.isLevelCompleted) {
        if (state.isLevelCompleted) {
            onRecordCompletion(
                state.chamber.id,
                state.elapsedSeconds,
                state.portalsFired
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ApertureDarkBackground)
    ) {
        val screenW = maxWidth.value
        val screenH = maxHeight.value

        // Level world dimensions are 1000 x 600
        val scaleX = screenW / 1000f
        val scaleY = screenH / 600f
        val scale = minOf(scaleX, scaleY)

        val offsetX = (screenW - 1000f * scale) * 0.5f
        val offsetY = (screenH - 600f * scale) * 0.5f

        // Render Canvas (Walls, Portals, Laser, Cubes, Player, Particle effects)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Tap anywhere in the chamber to direct fire portals
                    detectTapGestures(
                        onTap = { offset ->
                            val worldX = (offset.x - offsetX) / scale
                            val worldY = (offset.y - offsetY) / scale
                            val aimDir = Vec2(worldX - state.playerPos.x, worldY - state.playerPos.y)
                            gameEngine.firePortal(PortalType.BLUE, aimDir)
                        },
                        onLongPress = { offset ->
                            val worldX = (offset.x - offsetX) / scale
                            val worldY = (offset.y - offsetY) / scale
                            val aimDir = Vec2(worldX - state.playerPos.x, worldY - state.playerPos.y)
                            gameEngine.firePortal(PortalType.ORANGE, aimDir)
                        }
                    )
                }
        ) {
            drawChamber(
                state = state,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }

        // Top HUD Overlay
        TopChamberHud(
            state = state,
            onQuickSave = {
                val currentSave = gameEngine.exportSave()
                onSaveToCloud(currentSave)
                saveToastMessage = "Progress Saved to Aperture Cloud"
            },
            onRestart = { gameEngine.restartChamber() },
            onOpenCloud = onOpenCloudManager,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Save Toast Banner
        AnimatedVisibility(
            visible = saveToastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Surface(
                color = ApertureSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PortalBlue),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Saved",
                        tint = PortalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = saveToastMessage ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = ApertureTextPrimary
                    )
                }
            }
        }
        LaunchedEffect(saveToastMessage) {
            if (saveToastMessage != null) {
                kotlinx.coroutines.delay(2200)
                saveToastMessage = null
            }
        }

        // On-screen Touch Controls (Dual Virtual Joysticks & Action Buttons)
        TouchControlsOverlay(
            onMoveChange = { moveJoystickX = it },
            onJumpChange = { isJumpHeld = it },
            onAimChange = { x, y ->
                aimJoystickX = x
                aimJoystickY = y
            },
            onFireBlue = {
                gameEngine.firePortal(PortalType.BLUE, Vec2(aimJoystickX, aimJoystickY))
            },
            onFireOrange = {
                gameEngine.firePortal(PortalType.ORANGE, Vec2(aimJoystickX, aimJoystickY))
            },
            onInteractCube = {
                gameEngine.toggleCubeInteract()
            },
            heldCube = state.heldCubeId != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )

        // Level Complete Overlay Dialog
        if (state.isLevelCompleted) {
            ChamberSuccessDialog(
                state = state,
                onNextChamber = {
                    val hasNext = gameEngine.nextChamber()
                    if (!hasNext) {
                        gameEngine.loadChamber(0)
                    }
                },
                onRestart = { gameEngine.restartChamber() }
            )
        }

        // Death / Reset Dialog
        if (state.isDead) {
            ChamberFailedDialog(
                reason = state.deathReason,
                onRetry = { gameEngine.restartChamber() }
            )
        }
    }
}

@Composable
fun TopChamberHud(
    state: GameState,
    onQuickSave: () -> Unit,
    onRestart: () -> Unit,
    onOpenCloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ApertureSurface.copy(alpha = 0.90f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ApertureBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chamber info & GLaDOS quote
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHAMBER ${state.chamber.number.toString().padStart(2, '0')}: ${state.chamber.title.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ApertureTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = state.chamber.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = ApertureTextSecondary
                )
            }

            // Stats: Portals Fired & Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PORTALS: ${state.portalsFired}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PortalBlue
                    )
                    Text(
                        text = String.format("%.1fs", state.elapsedSeconds),
                        style = MaterialTheme.typography.labelLarge,
                        color = ApertureYellow
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onQuickSave,
                    modifier = Modifier.size(36.dp).testTag("quick_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Save to Cloud",
                        tint = PortalBlue
                    )
                }

                IconButton(
                    onClick = onOpenCloud,
                    modifier = Modifier.size(36.dp).testTag("cloud_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Cloud Saves",
                        tint = PortalOrange
                    )
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(36.dp).testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Chamber",
                        tint = ApertureTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TouchControlsOverlay(
    onMoveChange: (Float) -> Unit,
    onJumpChange: (Boolean) -> Unit,
    onAimChange: (Float, Float) -> Unit,
    onFireBlue: () -> Unit,
    onFireOrange: () -> Unit,
    onInteractCube: () -> Unit,
    heldCube: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Virtual Movement Pad
        VirtualMovePad(
            onMoveChange = onMoveChange,
            onJump = { onJumpChange(true) },
            onJumpRelease = { onJumpChange(false) }
        )

        // Center Quick Cube Action
        Button(
            onClick = onInteractCube,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (heldCube) CompanionHeartPink else ApertureSurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ApertureBorder),
            modifier = Modifier
                .height(48.dp)
                .testTag("cube_interact_button")
        ) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = "Cube Interact",
                tint = if (heldCube) Color.White else ApertureTextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (heldCube) "DROP CUBE" else "GRAB CUBE",
                style = MaterialTheme.typography.labelSmall,
                color = if (heldCube) Color.White else ApertureTextPrimary
            )
        }

        // Right Portal Gun Aim & Trigger Controls
        PortalGunTriggerPad(
            onAimChange = onAimChange,
            onFireBlue = onFireBlue,
            onFireOrange = onFireOrange
        )
    }
}

@Composable
fun VirtualMovePad(
    onMoveChange: (Float) -> Unit,
    onJump: () -> Unit,
    onJumpRelease: () -> Unit
) {
    var knobOffset by remember { mutableFloatStateOf(0f) }
    val maxDrag = 50f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Horizontal Move Stick
        Box(
            modifier = Modifier
                .size(100.dp, 56.dp)
                .background(ApertureSurface.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                .border(1.5.dp, ApertureBorder, RoundedCornerShape(28.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            knobOffset = 0f
                            onMoveChange(0f)
                        },
                        onDragCancel = {
                            knobOffset = 0f
                            onMoveChange(0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (knobOffset + dragAmount.x).coerceIn(-maxDrag, maxDrag)
                            knobOffset = newOffset
                            onMoveChange(newOffset / maxDrag)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "< MOVE >",
                style = MaterialTheme.typography.labelSmall,
                color = ApertureTextMuted
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(knobOffset.roundToInt(), 0) }
                    .size(46.dp)
                    .background(PortalBlue.copy(alpha = 0.9f), CircleShape)
                    .border(2.dp, PortalBlueGlow, CircleShape)
            )
        }

        // Jump Button
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(ApertureSurface.copy(alpha = 0.85f), CircleShape)
                .border(1.5.dp, ApertureBorder, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onJump()
                            tryAwaitRelease()
                            onJumpRelease()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "JUMP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ApertureTextPrimary
            )
        }
    }
}

@Composable
fun PortalGunTriggerPad(
    onAimChange: (Float, Float) -> Unit,
    onFireBlue: () -> Unit,
    onFireOrange: () -> Unit
) {
    var aimOffset by remember { mutableStateOf(Offset(35f, 0f)) }
    val maxRadius = 45f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Blue Portal Trigger Button
        Button(
            onClick = onFireBlue,
            colors = ButtonDefaults.buttonColors(containerColor = PortalBlue),
            shape = CircleShape,
            modifier = Modifier
                .size(58.dp)
                .testTag("fire_blue_button")
        ) {
            Text(
                text = "BLUE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Orange Portal Trigger Button
        Button(
            onClick = onFireOrange,
            colors = ButtonDefaults.buttonColors(containerColor = PortalOrange),
            shape = CircleShape,
            modifier = Modifier
                .size(58.dp)
                .testTag("fire_orange_button")
        ) {
            Text(
                text = "ORNG",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 360 Aim Joystick
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(ApertureSurface.copy(alpha = 0.85f), CircleShape)
                .border(1.5.dp, ApertureBorder, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val next = aimOffset + dragAmount
                            val dist = next.getDistance()
                            val clamped = if (dist > maxRadius) next * (maxRadius / dist) else next
                            aimOffset = clamped
                            val norm = if (dist > 0.001f) next / dist else Offset(1f, 0f)
                            onAimChange(norm.x, norm.y)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AIM",
                style = MaterialTheme.typography.labelSmall,
                color = ApertureTextMuted
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(aimOffset.x.roundToInt(), aimOffset.y.roundToInt()) }
                    .size(34.dp)
                    .background(Color(0xFFE2E8F0), CircleShape)
                    .border(2.dp, PortalBlue, CircleShape)
            )
        }
    }
}

// Custom High-Performance Canvas Rendering for Portal Lab
fun DrawScope.drawChamber(
    state: GameState,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    fun toScreen(v: Vec2): Offset = Offset(offsetX + v.x * scale, offsetY + v.y * scale)
    fun toScreenX(x: Float): Float = offsetX + x * scale
    fun toScreenY(y: Float): Float = offsetY + y * scale

    // 1. Draw Walls & Hazards
    for (wall in state.chamber.walls) {
        val left = toScreenX(wall.left)
        val top = toScreenY(wall.top)
        val width = wall.width * scale
        val height = wall.height * scale

        when (wall.type) {
            SurfaceType.PORTALABLE -> {
                // White concrete testing panel
                drawRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 1.5f)
                )
            }
            SurfaceType.NON_PORTALABLE -> {
                if (wall.isExitDoor) {
                    // Chamber Exit Airlock Door
                    val doorColor = if (state.isDoorOpen) AcidGreen else LaserRed
                    drawRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                    drawRect(
                        color = doorColor,
                        topLeft = Offset(left + 2f, top + 2f),
                        size = Size(width - 4f, height - 4f),
                        style = Stroke(width = 3f)
                    )
                } else {
                    // Dark steel / mesh
                    drawRect(
                        color = Color(0xFF1E2634),
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )
                    drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 1.5f)
                    )
                }
            }
            SurfaceType.ACID -> {
                // Glowing toxic goo
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF10B981), Color(0xFF064E3B)),
                        startY = top,
                        endY = top + height
                    ),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
            }
            SurfaceType.EMANCIPATION_GRID -> {
                // Glowing cyan particle fizzler barrier
                drawRect(
                    color = EmancipationFizzlerCyan.copy(alpha = 0.45f),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawLine(
                    color = EmancipationFizzlerCyan,
                    start = Offset(left + width * 0.5f, top),
                    end = Offset(left + width * 0.5f, top + height),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }
    }

    // 2. Draw Heavy Duty Buttons
    for (btn in state.buttons) {
        val bx = toScreenX(btn.x)
        val by = toScreenY(btn.y)
        val bw = btn.width * scale
        val bh = btn.height * scale

        // Button base
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(bx, by),
            size = Size(bw, bh)
        )
        // Red / Yellow pressure pad
        val padHeight = if (btn.isPressed) bh * 0.4f else bh * 0.8f
        val padColor = if (btn.isPressed) AcidGreen else LaserRed
        drawRoundRect(
            color = padColor,
            topLeft = Offset(bx + 4f, by + (bh - padHeight)),
            size = Size(bw - 8f, padHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // 3. Draw Aerial Faith Plates
    for (fp in state.chamber.faithPlates) {
        val fx = toScreenX(fp.x)
        val fy = toScreenY(fp.y)
        val fw = fp.width * scale
        drawRoundRect(
            color = ApertureYellow,
            topLeft = Offset(fx, fy),
            size = Size(fw, 14f * scale),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Arrow pointing launch direction
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(fx + fw * 0.5f, fy + 12f * scale),
            end = Offset(fx + fw * 0.5f + fp.launchVel.x * 0.03f * scale, fy - 20f * scale),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // 4. Draw Aiming Tracer Line
    if (state.aimingRayStart != null && state.aimingRayEnd != null && state.aimingColor != null) {
        val tracerColor = if (state.aimingColor == PortalType.BLUE) PortalBlueGlow else PortalOrangeGlow
        drawLine(
            color = tracerColor.copy(alpha = 0.75f),
            start = toScreen(state.aimingRayStart),
            end = toScreen(state.aimingRayEnd),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }

    // 5. Draw Active Portals (Blue & Orange)
    fun drawPortalOval(portal: Portal, mainColor: Color, glowColor: Color) {
        if (!portal.active) return
        val center = toScreen(portal.position)
        val tangent = Vec2(-portal.normal.y, portal.normal.x)
        val p1 = toScreen(portal.position + tangent * (portal.width * 0.5f))
        val p2 = toScreen(portal.position - tangent * (portal.width * 0.5f))

        // Outer glow oval
        drawLine(
            color = glowColor.copy(alpha = 0.5f),
            start = p1,
            end = p2,
            strokeWidth = 14f * scale,
            cap = StrokeCap.Round
        )
        // Core energy ring
        drawLine(
            color = mainColor,
            start = p1,
            end = p2,
            strokeWidth = 6f * scale,
            cap = StrokeCap.Round
        )
        // Inner portal void
        drawLine(
            color = Color(0xFF05070B),
            start = p1,
            end = p2,
            strokeWidth = 3f * scale,
            cap = StrokeCap.Round
        )
    }
    drawPortalOval(state.bluePortal, PortalBlue, PortalBlueGlow)
    drawPortalOval(state.orangePortal, PortalOrange, PortalOrangeGlow)

    // 6. Draw Weighted Companion Cubes
    for (cube in state.cubes) {
        val cx = toScreenX(cube.pos.x - cube.halfSize)
        val cy = toScreenY(cube.pos.y - cube.halfSize)
        val cSize = cube.size * scale

        // Cube body
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(cx, cy),
            size = Size(cSize, cSize),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Light corner brackets
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(cx + 3f, cy + 3f),
            size = Size(cSize - 6f, cSize - 6f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 2f)
        )
        // Pink heart in center
        drawCircle(
            color = CompanionHeartPink,
            radius = 5f * scale,
            center = Offset(cx + cSize * 0.5f, cy + cSize * 0.5f)
        )
    }

    // 7. Draw Player (Aperture Test Subject Chell Silhouette)
    val px = toScreenX(state.playerPos.x)
    val py = toScreenY(state.playerPos.y)
    val pw = PortalPhysicsEngine.PLAYER_WIDTH * scale
    val ph = PortalPhysicsEngine.PLAYER_HEIGHT * scale

    // Test jumpsuit / body
    drawRoundRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(px - pw * 0.5f, py - ph * 0.5f),
        size = Size(pw, ph),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Head / helmet
    drawCircle(
        color = Color(0xFF0F172A),
        radius = 7f * scale,
        center = Offset(px, py - ph * 0.5f + 8f * scale)
    )
    // Visor glowing blue
    val visorOffsetX = if (state.isFacingRight) 4f * scale else -4f * scale
    drawCircle(
        color = PortalBlue,
        radius = 2.5f * scale,
        center = Offset(px + visorOffsetX, py - ph * 0.5f + 8f * scale)
    )
    // Handheld Portal Device (Gun)
    val gunStartX = px
    val gunStartY = py - 4f * scale
    val gunEndX = if (state.isFacingRight) px + 18f * scale else px - 18f * scale
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(gunStartX, gunStartY),
        end = Offset(gunEndX, gunStartY),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    // Gun emitter tip glow
    drawCircle(
        color = PortalBlueGlow,
        radius = 3f * scale,
        center = Offset(gunEndX, gunStartY)
    )
}

@Composable
fun ChamberSuccessDialog(
    state: GameState,
    onNextChamber: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ApertureSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AcidGreen),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TEST COMPLETED",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AcidGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.chamber.gladosQuote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApertureTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TIME", style = MaterialTheme.typography.labelSmall, color = ApertureTextMuted)
                        Text(
                            text = String.format("%.2fs", state.elapsedSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = ApertureYellow
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "PORTALS", style = MaterialTheme.typography.labelSmall, color = ApertureTextMuted)
                        Text(
                            text = "${state.portalsFired}",
                            style = MaterialTheme.typography.titleLarge,
                            color = PortalBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f).testTag("retry_chamber_button")
                    ) {
                        Text("RETRY")
                    }
                    Button(
                        onClick = onNextChamber,
                        colors = ButtonDefaults.buttonColors(containerColor = PortalBlue),
                        modifier = Modifier.weight(1f).testTag("next_chamber_button")
                    ) {
                        Text("NEXT TRACK")
                    }
                }
            }
        }
    }
}

@Composable
fun ChamberFailedDialog(
    reason: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ApertureSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, LaserRed),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TEST ABORTED",
                    style = MaterialTheme.typography.headlineLarge,
                    color = LaserRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApertureTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = LaserRed),
                    modifier = Modifier.fillMaxWidth().testTag("retry_after_death_button")
                ) {
                    Text("RESPAWN TEST SUBJECT")
                }
            }
        }
    }
}
