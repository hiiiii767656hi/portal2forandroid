package com.example.game.level

import com.example.game.physics.AerialFaithPlate
import com.example.game.physics.HeavyDutyButton
import com.example.game.physics.LaserEmitter
import com.example.game.physics.LaserReceiver
import com.example.game.physics.SurfaceType
import com.example.game.physics.Vec2
import com.example.game.physics.Wall
import com.example.game.physics.WeightedCube

data class TestChamber(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val gladosQuote: String,
    val playerStart: Vec2,
    val exitDoorPos: Vec2,
    val walls: List<Wall>,
    val cubes: List<WeightedCube> = emptyList(),
    val buttons: List<HeavyDutyButton> = emptyList(),
    val faithPlates: List<AerialFaithPlate> = emptyList(),
    val laserEmitters: List<LaserEmitter> = emptyList(),
    val laserReceivers: List<LaserReceiver> = emptyList()
)

object ChamberCatalog {
    val chambers = listOf(
        // Chamber 00: Orientation & First Portals
        TestChamber(
            id = "chamber_00",
            number = 0,
            title = "Relaxation Vault",
            subtitle = "Momentum & Quantum Tunneling",
            gladosQuote = "Hello and, again, welcome to the Aperture Science Computer-Aided Enrichment Center.",
            playerStart = Vec2(120f, 520f),
            exitDoorPos = Vec2(880f, 480f),
            walls = listOf(
                // Boundary walls
                Wall(0f, 0f, 1000f, 40f, SurfaceType.NON_PORTALABLE),
                Wall(0f, 560f, 1000f, 40f, SurfaceType.PORTALABLE), // Floor
                Wall(0f, 0f, 40f, 600f, SurfaceType.PORTALABLE),   // Left wall
                Wall(960f, 0f, 40f, 600f, SurfaceType.PORTALABLE),  // Right wall
                // Partition obstacle
                Wall(480f, 260f, 40f, 300f, SurfaceType.NON_PORTALABLE),
                // High test platform
                Wall(680f, 380f, 280f, 30f, SurfaceType.PORTALABLE),
                // Exit door indicator
                Wall(880f, 480f, 40f, 80f, SurfaceType.NON_PORTALABLE, isExitDoor = true)
            ),
            buttons = listOf(
                HeavyDutyButton("btn_00", 300f, 544f, 60f, 16f, targetsDoor = true)
            ),
            cubes = listOf(
                WeightedCube("cube_00", Vec2(220f, 520f))
            )
        ),

        // Chamber 01: Momentum Fling
        TestChamber(
            id = "chamber_01",
            number = 1,
            title = "Flings and Momentum",
            subtitle = "Speedy Thing Goes In, Speedy Thing Comes Out",
            gladosQuote = "In layman's terms: speedy thing goes in, speedy thing comes out.",
            playerStart = Vec2(100f, 160f),
            exitDoorPos = Vec2(880f, 220f),
            walls = listOf(
                // Boundaries
                Wall(0f, 0f, 1000f, 30f, SurfaceType.NON_PORTALABLE), // Ceiling
                Wall(0f, 570f, 1000f, 30f, SurfaceType.PORTALABLE),   // Deep floor pit
                Wall(0f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                Wall(970f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                // Starting ledge
                Wall(30f, 220f, 200f, 30f, SurfaceType.NON_PORTALABLE),
                // High goal platform
                Wall(750f, 280f, 220f, 30f, SurfaceType.PORTALABLE),
                // Angled wall / vertical fling wall
                Wall(500f, 150f, 30f, 220f, SurfaceType.PORTALABLE),
                // Acid pool in the center bottom
                Wall(350f, 580f, 300f, 20f, SurfaceType.ACID),
                // Exit Door
                Wall(880f, 200f, 40f, 80f, SurfaceType.NON_PORTALABLE, isExitDoor = true)
            ),
            buttons = listOf(
                HeavyDutyButton("btn_01", 120f, 204f, 60f, 16f, targetsDoor = true)
            ),
            cubes = listOf(
                WeightedCube("cube_01", Vec2(160f, 180f))
            )
        ),

        // Chamber 02: Aerial Faith Plates & Lasers
        TestChamber(
            id = "chamber_02",
            number = 2,
            title = "Propulsion & Laser Redirection",
            subtitle = "Thermal Discouragement Beam",
            gladosQuote = "Please note that any appearance of danger is merely a device to enhance your testing experience.",
            playerStart = Vec2(80f, 500f),
            exitDoorPos = Vec2(900f, 180f),
            walls = listOf(
                Wall(0f, 0f, 1000f, 30f, SurfaceType.PORTALABLE),
                Wall(0f, 570f, 1000f, 30f, SurfaceType.PORTALABLE),
                Wall(0f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                Wall(970f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                // Central tower
                Wall(450f, 200f, 50f, 370f, SurfaceType.NON_PORTALABLE),
                // Upper chamber deck
                Wall(720f, 250f, 250f, 30f, SurfaceType.PORTALABLE),
                // Fizzler barrier
                Wall(500f, 200f, 15f, 370f, SurfaceType.EMANCIPATION_GRID),
                // Exit Door
                Wall(900f, 170f, 40f, 80f, SurfaceType.NON_PORTALABLE, isExitDoor = true)
            ),
            faithPlates = listOf(
                AerialFaithPlate(250f, 560f, 60f, Vec2(320f, -680f))
            ),
            buttons = listOf(
                HeavyDutyButton("btn_02", 760f, 234f, 60f, 16f, targetsDoor = true)
            ),
            cubes = listOf(
                WeightedCube("cube_02", Vec2(800f, 200f))
            )
        ),

        // Chamber 03: The Gauntlet (Speed & Timing)
        TestChamber(
            id = "chamber_03",
            number = 3,
            title = "Terminal Velocity",
            subtitle = "Infinite Falling Chamber",
            gladosQuote = "The Enrichment Center promises to always provide a safe testing environment. In dangerous testing environments, a promise will not be made.",
            playerStart = Vec2(100f, 510f),
            exitDoorPos = Vec2(860f, 130f),
            walls = listOf(
                Wall(0f, 0f, 1000f, 30f, SurfaceType.PORTALABLE),
                Wall(0f, 570f, 1000f, 30f, SurfaceType.PORTALABLE),
                Wall(0f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                Wall(970f, 0f, 30f, 600f, SurfaceType.PORTALABLE),
                // Vertical pit dividers
                Wall(300f, 0f, 40f, 460f, SurfaceType.PORTALABLE),
                Wall(650f, 150f, 40f, 420f, SurfaceType.NON_PORTALABLE),
                // High destination shelf
                Wall(750f, 200f, 220f, 30f, SurfaceType.PORTALABLE),
                // Toxic acid pit below
                Wall(340f, 580f, 310f, 20f, SurfaceType.ACID),
                // Exit door
                Wall(860f, 120f, 40f, 80f, SurfaceType.NON_PORTALABLE, isExitDoor = true)
            ),
            buttons = listOf(
                HeavyDutyButton("btn_03", 800f, 184f, 60f, 16f, targetsDoor = true)
            ),
            cubes = listOf(
                WeightedCube("cube_03", Vec2(180f, 520f))
            )
        )
    )
}
