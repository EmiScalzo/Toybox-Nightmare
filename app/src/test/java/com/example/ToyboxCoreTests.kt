package com.example

import com.example.audio.SoundEffectsEngine
import com.example.data.catalog.DefectCatalog
import com.example.data.catalog.GameCatalog
import com.example.data.model.BodyZone
import com.example.data.model.CharacterId
import com.example.data.model.CompanionId
import com.example.data.model.Difficulty
import com.example.data.model.EquippedBodyPart
import com.example.data.model.RoomMechanic
import com.example.data.model.RoomType
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.random.Random

class ToyboxCoreTests {

    @Test
    fun defectCatalogHas24DefectsAndRespectsIncompatibilities() {
        assertEquals(24, DefectCatalog.allDefects.size)
        val buttonEye = DefectCatalog.byId("button_eye")!!
        val oneEyeOnly = DefectCatalog.byId("one_eye_only")!!
        assertFalse(DefectCatalog.isCompatible(listOf(buttonEye), oneEyeOnly))

        val nightmareDefects = DefectCatalog.generateRandomForDifficulty(
            Difficulty.PESADILLA,
            rng = Random(42)
        )
        assertEquals(3, nightmareDefects.size)
        assertEquals(3, nightmareDefects.distinctBy { it.id }.size)
    }

    @Test
    fun dungeonFloorIsFullyConnectedAndContainsBoss() {
        for (floorNum in 1..6) {
            val floor = DungeonGenerator.generateFloor(floorNum, Difficulty.NORMAL, rng = Random(floorNum * 17))
            assertTrue(floor.rooms.size >= 11)
            assertEquals(RoomType.START, floor.rooms[floor.startCoord]?.type)
            assertEquals(RoomType.BOSS, floor.rooms[floor.bossCoord]?.type)

            // BFS reachability including secret doors
            val visited = mutableSetOf(floor.startCoord)
            val queue = ArrayDeque(listOf(floor.startCoord))
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                val room = floor.rooms[cur]!!
                for (dir in room.doors + room.secretDoors) {
                    val nxt = cur.neighbor(dir)
                    if (nxt in floor.rooms && nxt !in visited) {
                        visited.add(nxt)
                        queue.add(nxt)
                    }
                }
            }
            assertEquals("All rooms on floor $floorNum must be reachable", floor.rooms.size, visited.size)
        }
    }

    @Test
    fun tagBasedSynergiesActivateAutomatically() {
        val parts = mapOf(
            BodyZone.ARMS to EquippedBodyPart(GameCatalog.partById("spring_arm")),
            BodyZone.EYES to EquippedBodyPart(GameCatalog.partById("glass_eye"))
        )
        val synergies = GameCatalog.computeActiveSynergies(parts, emptyList())
        assertTrue(synergies.any { it.id == "syn_clockwork_tracker" })
    }

    @Test
    fun toyboxMemoryGeneratesLegibleTraitsAndContextualDialogue() {
        val mem = ToyboxMemoryState(
            dodgeUseCount = 9,
            explosiveUseCount = 5,
            compassionScore = 1,
            enemiesAdaptDodge = true,
            enemiesStealBombs = true,
            memoriesCollected = listOf("mem_nursery", "mem_founder")
        )
        val traits = ToyboxMemorySystem.activeTraits(mem)
        assertTrue(traits.size >= 4)
        val event = GameCatalog.events.first { it.id == "lost_toy" }
        val contextualized = ToyboxMemorySystem.contextualizeEvent(event, mem)
        assertTrue(contextualized.narrative.contains("[La juguetería recuerda]"))
    }

    @Test
    fun expandedCatalogHasNewItemsSynergiesAndRoomVariants() {
        assertEquals(20, GameCatalog.passiveItems.size)
        assertEquals(14, GameCatalog.synergies.size)

        val parts = mapOf(
            BodyZone.ARMS to EquippedBodyPart(GameCatalog.partById("scissor_arm"))
        )
        val items = listOfNotNull(
            GameCatalog.itemById("brass_thimble"),
            GameCatalog.itemById("moth_lantern"),
            GameCatalog.itemById("marionette_cross")
        )
        val synergies = GameCatalog.computeActiveSynergies(parts, items)
        assertTrue(synergies.any { it.id == "syn_crimson_tailor" })

        val floor = DungeonGenerator.generateFloor(1, Difficulty.NORMAL, rng = Random(123))
        val variants = floor.rooms.values.map { it.visualVariant }.distinct()
        assertTrue(variants.size >= 2)
    }

    @Test
    fun treasureRoomObstaclesNeverBlockPedestals() {
        val pedestalSpots = listOf(0.36f to 0.50f, 0.38f to 0.50f, 0.62f to 0.50f, 0.64f to 0.50f)
        for (seed in 1..25) {
            for (diff in Difficulty.entries) {
                val floor = DungeonGenerator.generateFloor(1 + (seed % 4), diff, rng = Random(seed * 97 + diff.ordinal))
                val treasureRooms = floor.rooms.values.filter { it.type == RoomType.TREASURE }
                assertTrue("Floor must contain a Treasure room", treasureRooms.isNotEmpty())
                treasureRooms.forEach { room ->
                    room.obstacles.forEach { obs ->
                        val minX = obs.x - 0.04f
                        val maxX = obs.x + obs.w + 0.04f
                        val minY = obs.y - 0.04f
                        val maxY = obs.y + obs.h + 0.04f
                        pedestalSpots.forEach { (px, py) ->
                            assertFalse(
                                "Obstacle $obs overlapped treasure pedestal ($px, $py) on seed $seed",
                                px in minX..maxX && py in minY..maxY
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun companionSystemCoversAllRequestedBehaviors() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val baseRun = engine.createNewRun(
            character = GameCatalog.characterById(CharacterId.PATCH),
            difficulty = Difficulty.NORMAL,
            defects = emptyList()
        )
        var nextId = 5000L

        // 1) Bunny picks up nearby button
        val bunnyState = baseRun.copy(
            activeCompanions = listOf(ActiveCompanion(CompanionId.BUNNY, x = 300f, y = 300f)),
            pickups = listOf(GroundPickup(id = 999L, kind = PickupKind.BUTTON, x = 310f, y = 300f, amount = 1)),
            buttons = 2
        )
        val afterBunny = CompanionSystem.updateCompanions(bunnyState, 0.12f, { nextId++ }, sfx)
        assertEquals(3, afterBunny.buttons)
        assertTrue(afterBunny.pickups.none { it.id == 999L })

        // 2) Forgotten Bear intercepts fatal hit leaving 1 heart & disappears
        val bearState = baseRun.copy(
            hearts = 1,
            activeCompanions = listOf(ActiveCompanion(CompanionId.FORGOTTEN_BEAR, x = 280f, y = 320f))
        )
        val (saved, afterBear) = CompanionSystem.tryBearFatalIntercept(bearState, pendingHearts = 0)
        assertTrue(saved)
        assertEquals(1, afterBear.hearts)
        assertTrue(afterBear.activeCompanions.none { it.id == CompanionId.FORGOTTEN_BEAR })

        // 3) Broken Soldier breaks from enemy bullets and repairs via buttons
        val soldierState = baseRun.copy(
            buttons = 5,
            activeCompanions = listOf(ActiveCompanion(CompanionId.BROKEN_SOLDIER, x = 300f, y = 300f, hp = 1, maxHp = 4)),
            projectiles = listOf(
                ActiveProjectile(id = 777L, x = 302f, y = 300f, vx = 0f, vy = 0f, radius = 8f, damage = 1f, fromPlayer = false, colorHex = 0xFFFF0000)
            )
        )
        val brokenSoldierState = CompanionSystem.updateCompanions(soldierState, 0.05f, { nextId++ }, sfx)
        assertTrue(brokenSoldierState.activeCompanions.first().broken)

        val dlg = CompanionSystem.buildInspectDialog(brokenSoldierState.activeCompanions.first(), brokenSoldierState)
        val repairedState = CompanionSystem.resolveCompanionDialog(
            state = brokenSoldierState.copy(pendingCompanionDialog = dlg),
            optionId = "repair_soldier_buttons",
            sfx = sfx,
            allocId = { nextId++ },
            onDiscovery = { _, _ -> },
            onGrudge = { },
            acquireItemFn = { st, _ -> st }
        )
        assertFalse(repairedState.activeCompanions.first().broken)
        assertEquals(2, repairedState.buttons)

        // 4) Rejecting Bunny records cross-run grudge
        var grudgedId: String? = null
        val bunnyShrine = CompanionSystem.buildShrineDialog(CompanionId.BUNNY, emptyList())
        CompanionSystem.resolveCompanionDialog(
            state = baseRun.copy(pendingCompanionDialog = bunnyShrine),
            optionId = "recruit_reject",
            sfx = sfx,
            allocId = { nextId++ },
            onDiscovery = { _, _ -> },
            onGrudge = { grudgedId = it },
            acquireItemFn = { st, _ -> st }
        )
        assertEquals(CompanionId.BUNNY.name, grudgedId)
    }

    @Test
    fun roomMechanicsNurseryMazeAndWeepingStatues() {
        val watched = RoomMechanicSystem.isWeepingToyWatched(
            enemyX = 500f, enemyY = 300f,
            playerX = 300f, playerY = 300f,
            facingAngle = 0f
        )
        assertTrue(watched)

        val notWatched = RoomMechanicSystem.isWeepingToyWatched(
            enemyX = 500f, enemyY = 300f,
            playerX = 300f, playerY = 300f,
            facingAngle = PI.toFloat()
        )
        assertFalse(notWatched)

        val floor = DungeonGenerator.generateFloor(2, Difficulty.PESADILLA, rng = Random(314))
        val specialRooms = floor.rooms.values.filter { it.mechanic != RoomMechanic.NONE }
        assertTrue("Hard floor should roll dynamic room mechanics", specialRooms.isNotEmpty())

        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val run = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        val vanishingRoom = run.currentRoom.copy(mechanic = RoomMechanic.VANISHING_FLOOR)
        var s = RoomMechanicSystem.initForRoom(run.copy(currentRoom = vanishingRoom), vanishingRoom)
        assertEquals(9, s.vanishingSectors.size)
        assertTrue(s.vanishingSectors.all { it.state == SectorState.STABLE })
        repeat(60) {
            s = RoomMechanicSystem.stepMechanics(s, 0.1f) { it }
        }
        assertTrue(s.vanishingSectors.any { it.state == SectorState.WARNING || it.state == SectorState.GONE })

        val mazeRoom = DungeonGenerator.generateFloor(1, Difficulty.PESADILLA, rng = Random(77))
            .rooms.values.firstOrNull { it.mechanic == RoomMechanic.NURSERY_MAZE }
        if (mazeRoom != null) {
            assertTrue(mazeRoom.obstacles.all { it.kind == 5 })
            // Keep door lanes clear
            val doorSpots = listOf(0.5f to 0.1f, 0.5f to 0.9f, 0.1f to 0.5f, 0.9f to 0.5f)
            mazeRoom.obstacles.forEach { obs ->
                doorSpots.forEach { (dx, dy) ->
                    assertFalse(dx in obs.x..(obs.x + obs.w) && dy in obs.y..(obs.y + obs.h))
                }
            }
        }
    }
}
