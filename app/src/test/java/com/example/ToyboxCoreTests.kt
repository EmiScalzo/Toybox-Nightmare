package com.example

import com.example.audio.SoundEffectsEngine
import com.example.data.catalog.AmpliacionCatalog
import com.example.data.catalog.DefectCatalog
import com.example.data.catalog.GameCatalog
import com.example.data.model.ArchiveCategory
import com.example.data.model.BackpackItemKind
import com.example.data.model.BackpackSlotItem
import com.example.data.model.BackpackTier
import com.example.data.model.BodyZone
import com.example.data.model.CharacterId
import com.example.data.model.CompanionId
import com.example.data.model.Difficulty
import com.example.data.model.EnemyFaction
import com.example.data.model.EnemyRole
import com.example.data.model.EnemyType
import com.example.data.model.EquippedBodyPart
import com.example.data.model.InteractableCategory
import com.example.data.model.InteractableType
import com.example.data.model.LorePropKind
import com.example.data.model.LorePuzzleKind
import com.example.data.model.ManufacturerDealCategory
import com.example.data.model.ManufacturerOffer
import com.example.data.model.ObjectiveType
import com.example.data.model.RepairActionMode
import com.example.data.model.RepairMaterial
import com.example.data.model.RepairMinigameType
import com.example.data.model.RoomMechanic
import com.example.data.model.RoomType
import com.example.data.model.SecretClueType
import com.example.data.model.SecretRoomFlavor
import com.example.data.model.Direction
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

    @Test
    fun expandedSixMechanicsInteractablesObjectivesRelationshipsSecretsRepairsAndManufacturerWork() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val base = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        var nid = 9000L

        // 1) Interactables: activate electric train & verify active state + nearby prompt
        val train = RoomInteractable(id = 101L, type = InteractableType.ELECTRIC_TRAIN, x = 500f, y = 500f)
        val trainState = base.copy(playerX = 510f, playerY = 510f, roomInteractables = listOf(train))
        assertNotNull(InteractablesSystem.findNearbyInteractablePrompt(trainState))
        val activatedTrain = InteractablesSystem.activateInteractable(trainState, 101L, { nid++ }, sfx) { _, _ -> }
        assertTrue(activatedTrain.roomInteractables.first().active)

        // 2) Alternative Room Objective: SURVIVE_TIME completes when timer reaches 0, WIND_THREE_MECHANISMS winds 3
        val surviveObj = RoomObjectiveState(type = ObjectiveType.SURVIVE_TIME, timerSec = 0.05f, maxTimerSec = 15f)
        val afterSurvive = RoomObjectiveSystem.stepObjective(base.copy(roomObjective = surviveObj), 0.1f, { it }, { nid++ }, sfx)
        assertTrue(afterSurvive.roomObjective?.completed == true)

        val windObj = RoomObjectiveState(
            type = ObjectiveType.WIND_THREE_MECHANISMS,
            targetCount = 3,
            windupPositions = listOf(500f to 500f, 300f to 300f, 700f to 300f)
        )
        val (handledWind, woundOnce) = RoomObjectiveSystem.tryInteractObjective(
            base.copy(playerX = 505f, playerY = 505f, roomObjective = windObj),
            { nid++ },
            sfx
        )
        assertTrue(handledWind)
        assertEquals(1, woundOnce.roomObjective?.currentCount)

        // 3) Enemy Relationships: Captain death disorganizes Tin Soldiers
        val captain = ActiveEnemy(id = 1L, type = EnemyType.TIN_CAPTAIN, x = 400f, y = 400f, hp = 50f, maxHp = 50f, radius = 22f, role = EnemyRole.LEADER, faction = EnemyFaction.TIN_ARMY)
        val soldier = ActiveEnemy(id = 2L, type = EnemyType.TIN_SOLDIER, x = 450f, y = 400f, hp = 28f, maxHp = 28f, radius = 20f, role = EnemyRole.STANDARD, faction = EnemyFaction.TIN_ARMY)
        val (relInited, _) = EnemyRelationshipSystem.initializeRoomRelationships(listOf(captain, soldier), hasBell = false, defeatedCaptainsCount = 0)
        assertEquals(EnemyRole.LEADER, relInited[0].role)
        val afterCaptainDeath = EnemyRelationshipSystem.onEnemyKilledRelationships(base.copy(enemies = relInited), relInited[0])
        assertTrue(afterCaptainDeath.enemies.first { it.id == 2L }.disorganized)
        assertEquals(1, afterCaptainDeath.defeatedCaptainsCount)

        // 4) Physical Secret Discovery: Crooked Frame reveals secret doors, Wrong Shadow resolves on movable push
        val secretDir = Direction.NORTH
        val roomWithSecret = base.currentRoom.copy(secretDoors = setOf(secretDir))
        val anom = RoomAnomaly(id = 50L, clueType = SecretClueType.CROOKED_FRAME, x = 500f, y = 120f, targetFlavor = SecretRoomFlavor.HIDDEN_TREASURE)
        val revealed = SecretDiscoverySystem.interactWithAnomaly(
            base.copy(currentRoom = roomWithSecret, roomAnomalies = listOf(anom), playerX = 500f, playerY = 140f),
            50L, { nid++ }, sfx
        ) { _, _ -> }
        assertTrue(secretDir in revealed.currentRoom.doors)
        assertTrue(revealed.currentRoom.secretDoors.isEmpty())

        val shadowAnom = RoomAnomaly(id = 51L, clueType = SecretClueType.WRONG_SHADOW_PROP, x = 300f, y = 300f, targetFlavor = SecretRoomFlavor.MEMORY_VAULT)
        val blockOnShadow = RoomInteractable(id = 52L, type = InteractableType.WOOD_BLOCK, x = 305f, y = 305f)
        val shadowResolved = SecretDiscoverySystem.stepAnomalies(
            base.copy(roomAnomalies = listOf(shadowAnom), roomInteractables = listOf(blockOnShadow)),
            { nid++ }, sfx
        ) { _, _ -> }
        assertTrue(shadowResolved.roomAnomalies.first().resolved)

        // 5) Repair Minigame: SEWING sequence + REINFORCE grants 3 reinforced rooms
        val damagedPart = EquippedBodyPart(GameCatalog.partById("button_torso"), durability = 40)
        val repStart = RepairMinigameSystem.startMinigame(
            base.copy(
                buttons = 5,
                equippedParts = base.equippedParts + (BodyZone.TORSO to damagedPart)
            ),
            BodyZone.TORSO,
            RepairActionMode.REINFORCE
        )
        assertEquals(RepairMinigameType.SEWING, repStart.pendingRepairMinigame?.minigameType)
        var mgState = repStart
        for (step in 1..4) {
            mgState = RepairMinigameSystem.handleAction(mgState, "sew_tap:$step", sfx)
        }
        assertNull(mgState.pendingRepairMinigame)
        assertTrue((mgState.equippedParts[BodyZone.TORSO]?.durability ?: 0) > 40)
        assertEquals(3, mgState.partReinforcedRooms[BodyZone.TORSO])

        // 6) El Fabricante & La Caja Cerrada across 3 rooms
        val boxOffer = ManufacturerOffer(
            id = "offer_closed_box",
            title = "La Caja Cerrada",
            category = ManufacturerDealCategory.WORLD_ALTER,
            costSummary = "2",
            rewardSummary = "Misterio",
            riskHint = "Riesgo",
            worldEffectId = "closed_mystery_box",
            buttonsCost = 2
        )
        val enc = ManufacturerSystem.resolveOffer(
            state = base.copy(
                buttons = 5,
                manufacturerEncounter = com.example.data.model.ManufacturerEncounter("El Fabricante", "Hola", false, listOf(boxOffer))
            ),
            offerId = "offer_closed_box",
            allocId = { nid++ },
            sfx = sfx,
            onDiscovery = { _, _ -> },
            onAchievement = {},
            onUnlockBackpack = {},
            acquireItemFn = { st, _ -> st },
            onRecordTrust = {}
        )
        assertEquals(3, enc.closedMysteryBoxTimer)
        var boxTick = enc
        repeat(3) {
            boxTick = ManufacturerSystem.tickClosedMysteryBoxOnRoomEnter(boxTick, { nid++ }, sfx)
        }
        assertEquals(0, boxTick.closedMysteryBoxTimer)
        assertTrue(boxTick.pickups.size > enc.pickups.size || boxTick.enemies.size > enc.enemies.size)
    }

    @Test
    fun pickupMagnetismLootCrateDirectInteractAndSingleUsePrizeMachineWork() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val base = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        var nid = 8000L

        // 1) Brass button 60px away gets pulled & collected smoothly
        val btnPickup = GroundPickup(id = 1L, kind = PickupKind.BUTTON, x = 560f, y = 500f, amount = 2)
        var s = base.copy(playerX = 500f, playerY = 500f, buttons = 0, pickups = listOf(btnPickup))
        repeat(8) { s = engine.step(s, EngineInput(), 0.05f) }
        assertTrue("Magnetic pull should collect nearby button", s.buttons >= 2 && s.pickups.isEmpty())

        // 2) LOOT_CRATE has NO interaction prompt and opens when destroyed by projectiles/bombs
        val crate = RoomInteractable(id = 2L, type = InteractableType.LOOT_CRATE, x = 515f, y = 515f, hp = 10f, maxHp = 22f)
        val crateState = base.copy(playerX = 500f, playerY = 500f, roomInteractables = listOf(crate))
        val prompt = InteractablesSystem.findNearbyInteractablePrompt(crateState)
        assertNull("LOOT_CRATE must not show direct interact button", prompt)
        val shotProj = ActiveProjectile(id = 99L, x = 515f, y = 515f, vx = 100f, vy = 0f, radius = 8f, damage = 15f, fromPlayer = true, colorHex = 0xFFFFFFFF)
        val (hitCrate, openedCrate) = InteractablesSystem.applyProjectileToInteractables(crateState, shotProj, { nid++ }, sfx) { _, _ -> }
        assertTrue(hitCrate)
        assertTrue(openedCrate.roomInteractables.first().destroyed)
        assertTrue(openedCrate.pickups.isNotEmpty() || openedCrate.enemies.isNotEmpty())

        // 3) Triggered PRIZE_MACHINE no longer shows interaction prompt
        val usedMachine = RoomInteractable(id = 3L, type = InteractableType.PRIZE_MACHINE, x = 510f, y = 510f, triggered = true)
        assertNull(InteractablesSystem.findNearbyInteractablePrompt(base.copy(playerX = 500f, playerY = 500f, roomInteractables = listOf(usedMachine))))
    }

    @Test
    fun allTenLorePuzzlesAndFragilePhotoBurnWork() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val base = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        var nid = 7000L

        assertEquals(10, AmpliacionCatalog.loreTemplates.size)

        // Check every puzzle kind initializes valid props cleanly
        AmpliacionCatalog.loreTemplates.forEachIndexed { idx, tpl ->
            val loreState = LoreRoomSystem.generateForRoom(1, GridCoord(idx, 2), emptyList(), { nid++ }, Random(idx), forcedTemplate = tpl)
            assertTrue("Template ${tpl.id} must have props", loreState.props.isNotEmpty())
            assertEquals(tpl.puzzleKind, loreState.puzzleKind)
        }

        // CROOKED_PAINTING solve unlocks archive discovery
        val paintingTpl = AmpliacionCatalog.loreTemplates.first { it.puzzleKind == LorePuzzleKind.CROOKED_PAINTING }
        val paintingRoom = base.currentRoom.copy(type = RoomType.LORE, cleared = true)
        val initPaint = base.copy(
            currentRoom = paintingRoom,
            loreRoom = LoreRoomSystem.generateForRoom(1, GridCoord(0, 0), emptyList(), { nid++ }, forcedTemplate = paintingTpl)
        )
        val paintingProp = initPaint.loreRoom!!.props.first { it.kind == LorePropKind.ROTATABLE_PAINTING }
        var pState = initPaint
        repeat(4) {
            if (!pState.loreRoom!!.solved) {
                pState = PuzzleInteractionSystem.interactLoreProp(pState, paintingProp.id, { nid++ }, sfx, { _, _ -> }, {}, {})
            }
        }
        assertTrue(pState.loreRoom!!.solved)
        assertNotNull(pState.pendingArchiveDiscovery)

        // Bomb explosion next to DESTRUCTIBLE_CRATE with fragilePhotoInside burns the photo and prevents archive reveal
        val shelfTpl = AmpliacionCatalog.loreTemplates.first { it.puzzleKind == LorePuzzleKind.STUCK_BOOKSHELF }
        val initShelf = base.copy(
            currentRoom = paintingRoom,
            loreRoom = LoreRoomSystem.generateForRoom(1, GridCoord(1, 0), emptyList(), { nid++ }, forcedTemplate = shelfTpl)
        )
        val crate = initShelf.loreRoom!!.props.first { it.kind == LorePropKind.DESTRUCTIBLE_CRATE && it.fragilePhotoInside }
        val burned = PuzzleInteractionSystem.onExplosionLoreProps(
            initShelf, crate.x, crate.y, 120f, { nid++ }, sfx, { _, _ -> }, {}, {}
        )
        assertTrue(burned.loreRoom!!.fragilePhotoBurned)
    }

    @Test
    fun backpackTiersCapacitySpeedPenaltyAndArchiveSystemsWork() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        var s = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        var nid = 6000L

        // Start run has NO backpack, NO backpack items, and NO guaranteed pet shrine in start room
        assertNull(s.backpackTier)
        assertTrue(s.backpackItems.isEmpty())
        assertEquals(0, BackpackSystem.capacity(s))
        assertTrue(s.pickups.none { it.kind == PickupKind.COMPANION_SHRINE })
        val (blockedNoPack, _) = BackpackSystem.addItem(s, BackpackItemKind.EXTRA_HEART, allocId = { nid++ }, sfx = sfx)
        assertFalse("Cannot store items without equipping a backpack first", blockedNoPack)

        // Equip STITCHED_POUCH (capacity 2)
        s = BackpackSystem.equipTier(s, BackpackTier.STITCHED_POUCH, { nid++ }, sfx) {}
        assertEquals(2, BackpackSystem.capacity(s))
        val (added1, s1) = BackpackSystem.addItem(s, BackpackItemKind.EXTRA_HEART, allocId = { nid++ }, sfx = sfx)
        assertTrue(added1)
        // 3-slot LARGE_RELIC_BOX does not fit in 2-slot pouch
        val (addedTooBig, _) = BackpackSystem.addItem(s1, BackpackItemKind.LARGE_RELIC_BOX, allocId = { nid++ }, sfx = sfx)
        assertFalse(addedTooBig)

        // Combat lock blocks CRAFTING_BUNDLE during active enemies
        val combatState = s1.copy(
            enemies = listOf(ActiveEnemy(1L, EnemyType.PLUSH_BEAR, 200f, 200f, hp = 20f, maxHp = 20f, radius = 20f)),
            currentRoom = s1.currentRoom.copy(cleared = false),
            backpackTier = BackpackTier.EXPLORER_PACK,
            backpackItems = listOf(BackpackSlotItem(uid = 50L, kind = BackpackItemKind.CRAFTING_BUNDLE))
        )
        val blockedAttempt = BackpackSystem.useItem(combatState, 50L, { nid++ }, sfx, { _, _ -> }, {})
        assertEquals(1, blockedAttempt.backpackItems.size)

        // MANUFACTURER_PACK prevents stored part degradation across rooms
        val manuPackState = s1.copy(
            backpackTier = BackpackTier.MANUFACTURER_PACK,
            backpackItems = listOf(BackpackSlotItem(uid = 60L, kind = BackpackItemKind.SMALL_PART_SPARE, storedDurability = 100))
        )
        val afterMove = BackpackSystem.degradeStoredSparesOnRoomAdvance(manuPackState)
        assertEquals(100, afterMove.backpackItems.first().storedDurability)

        // BOTTOMLESS_PACK with 6+ slots reduces speed by 10%
        val heavyBottomless = s1.copy(
            backpackTier = BackpackTier.BOTTOMLESS_PACK,
            backpackItems = listOf(
                BackpackSlotItem(uid = 70L, kind = BackpackItemKind.LARGE_RELIC_BOX),
                BackpackSlotItem(uid = 71L, kind = BackpackItemKind.LARGE_RELIC_BOX)
            )
        )
        assertEquals(0.90f, BackpackSystem.speedPenaltyMult(heavyBottomless), 0.001f)

        // Crafting KIDS_BACKPACK consumes materials + buttons and unlocks tier
        val craftReady = s1.copy(
            buttons = 5,
            materials = mapOf(RepairMaterial.THREAD to 3, RepairMaterial.NEEDLE to 2, RepairMaterial.SCREW to 2)
        )
        val kidsRecipe = AmpliacionCatalog.backpackRecipes.first { it.tier == BackpackTier.KIDS_BACKPACK }
        val crafted = BackpackSystem.craftBackpack(
            craftReady, kidsRecipe, { nid++ }, sfx, { _, _ -> }, {}, {}, {}
        )
        assertEquals(BackpackTier.KIDS_BACKPACK, crafted.backpackTier)

        // Wonderland Archive & Credits verification
        assertEquals(9, ArchiveCategory.entries.size)
        assertTrue(AmpliacionCatalog.archiveEntries.size >= 17)
        val shopArch = AmpliacionCatalog.archiveById("arch_first_shop")!!
        assertTrue(WonderlandArchiveSystem.isUnlocked(shopArch, setOf("archive:arch_first_shop")))
        assertFalse(WonderlandArchiveSystem.isExtraFragmentUnlocked(shopArch, setOf("archive:arch_first_shop")))
        assertTrue(WonderlandArchiveSystem.isExtraFragmentUnlocked(shopArch, setOf("archive:arch_first_shop", "archive:arch_workshop_photo")))

        assertEquals("1.2.0.0", GameCreditsSystem.VERSION)
        assertEquals("16/11/2026", GameCreditsSystem.LAST_UPDATED)
        assertEquals("EmIA Scalzo", GameCreditsSystem.AUTHOR_PREFIX + GameCreditsSystem.AUTHOR_HIGHLIGHT + GameCreditsSystem.AUTHOR_SUFFIX)
    }

    @Test
    fun companionCageBreaksWithProjectilesOrBombAndGuardsReact() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        val base = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())
        var nid = 9500L

        val cage = RoomInteractable(
            id = 500L,
            type = InteractableType.COMPANION_CAGE,
            x = 700f,
            y = 400f,
            radius = 30f,
            hp = 20f,
            maxHp = 28f,
            variant = CompanionId.FORGOTTEN_BEAR.ordinal
        )
        val guard = ActiveEnemy(
            id = 501L,
            type = EnemyType.BODYGUARD_BEAR,
            x = 640f,
            y = 400f,
            hp = 50f,
            maxHp = 50f,
            radius = 22f,
            role = EnemyRole.PROTECTOR,
            protectedCageId = cage.id
        )
        val cageState = base.copy(
            playerX = 400f,
            playerY = 400f,
            roomInteractables = listOf(cage),
            enemies = listOf(guard)
        )

        // Cage has NO interact prompt
        assertNull(InteractablesSystem.findNearbyInteractablePrompt(cageState.copy(playerX = 685f, playerY = 400f)))

        // Bomb destroys cage, frees companion shrine, and enrages guardian
        val afterBomb = InteractablesSystem.onExplosionInteractables(cageState, 710f, 405f, 120f, { nid++ }, sfx)
        assertTrue(afterBomb.roomInteractables.first().destroyed)
        assertTrue(afterBomb.pickups.any { it.kind == PickupKind.COMPANION_SHRINE && it.valueStr == CompanionId.FORGOTTEN_BEAR.name })
        assertTrue(afterBomb.enemies.first().enraged)
    }

    @Test
    fun backtrackingPreservesRoomInteractablesPickupsAnomaliesAndSecretReturnDoor() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        var s = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())

        // Pick a normal connected neighbor from START room that isn't a peaceful special room with empty interactables
        val startDoors = s.currentRoom.doors.toList()
        assertTrue(startDoors.isNotEmpty())
        val dirToRoomA = startDoors.first()
        val coordA = s.currentCoord.neighbor(dirToRoomA)

        // Ensure roomA is a cleared-ready combat room without Shifting Doors so doorway transition is deterministic
        val origRoomA = s.dungeon.rooms[coordA]!!
        val fixedRoomA = origRoomA.copy(
            type = RoomType.COMBAT,
            mechanic = RoomMechanic.NONE,
            initialEnemies = emptyList(),
            cleared = false,
            visited = false
        )
        val roomsWithA = s.dungeon.rooms.toMutableMap().apply { put(coordA, fixedRoomA) }
        s = s.copy(dungeon = s.dungeon.copy(rooms = roomsWithA))

        fun doorPosition(dir: Direction): Pair<Float, Float> = when (dir) {
            Direction.NORTH -> ARENA_W * 0.5f to WALL_THICKNESS + 10f
            Direction.SOUTH -> ARENA_W * 0.5f to ARENA_H - WALL_THICKNESS - 10f
            Direction.WEST -> WALL_THICKNESS + 10f to ARENA_H * 0.5f
            Direction.EAST -> ARENA_W - WALL_THICKNESS - 10f to ARENA_H * 0.5f
        }
        fun opposite(dir: Direction): Direction = when (dir) {
            Direction.NORTH -> Direction.SOUTH
            Direction.SOUTH -> Direction.NORTH
            Direction.EAST -> Direction.WEST
            Direction.WEST -> Direction.EAST
        }

        // 1) Step into roomA through the doorway from START
        val (enterX, enterY) = doorPosition(dirToRoomA)
        s = engine.step(s.copy(playerX = enterX, playerY = enterY), EngineInput(), 0.016f)
        assertEquals(coordA, s.currentCoord)

        // Step once inside roomA so room clears (0 enemies) and note generated interactables + leave an uncollected pedestal
        s = engine.step(s.copy(playerX = ARENA_W * 0.5f, playerY = ARENA_H * 0.5f), EngineInput(), 0.016f)
        assertTrue(s.currentRoom.cleared)
        assertTrue(s.roomInteractables.isNotEmpty())
        val initialInteractables = s.roomInteractables.mapIndexed { idx, it ->
            if (idx == 0) it.copy(x = it.x + 17f, destroyed = true) else it
        }
        val leftBehindPickup = GroundPickup(
            id = 98765L,
            kind = PickupKind.PASSIVE_ITEM_PEDESTAL,
            x = ARENA_W * 0.65f,
            y = ARENA_H * 0.65f,
            valueStr = GameCatalog.passiveItems.first().id
        )
        s = s.copy(
            roomInteractables = initialInteractables,
            pickups = listOf(leftBehindPickup)
        )

        // 2) Walk back to START room (0, 0)
        val backDir = opposite(dirToRoomA)
        val (backX, backY) = doorPosition(backDir)
        s = engine.step(s.copy(playerX = backX, playerY = backY), EngineInput(), 0.016f)
        assertEquals(GridCoord(0, 0), s.currentCoord)

        // 3) Re-enter roomA again and assert interactables + pickups stayed IDENTICAL (not regenerated!)
        s = engine.step(s.copy(playerX = enterX, playerY = enterY), EngineInput(), 0.016f)
        assertEquals(coordA, s.currentCoord)
        assertEquals(initialInteractables.size, s.roomInteractables.size)
        assertEquals(initialInteractables.map { it.id to it.type }, s.roomInteractables.map { it.id to it.type })
        assertEquals(initialInteractables.first().x, s.roomInteractables.first().x, 0.01f)
        assertTrue(s.roomInteractables.first().destroyed)
        assertTrue(s.pickups.any { it.id == leftBehindPickup.id })

        // 4) Secret room entry opens its return door so player can never be trapped inside
        val secretCoord = GridCoord(9, 9)
        val fromRoomCoord = GridCoord(9, 10)
        val secretRoom = RoomBlueprint(
            coord = secretCoord,
            type = RoomType.SECRET,
            doors = emptySet(),
            secretDoors = setOf(Direction.SOUTH),
            obstacles = emptyList(),
            initialEnemies = emptyList(),
            cleared = true,
            visited = false,
            secretFlavor = SecretRoomFlavor.HIDDEN_TREASURE
        )
        val entryHost = RoomBlueprint(
            coord = fromRoomCoord,
            type = RoomType.COMBAT,
            doors = setOf(Direction.NORTH),
            secretDoors = emptySet(),
            obstacles = emptyList(),
            initialEnemies = emptyList(),
            cleared = true,
            visited = true
        )
        val secretMap = s.dungeon.rooms.toMutableMap().apply {
            put(secretCoord, secretRoom)
            put(fromRoomCoord, entryHost)
        }
        val (nX, nY) = doorPosition(Direction.NORTH)
        val inSecret = engine.step(
            s.copy(
                currentCoord = fromRoomCoord,
                currentRoom = entryHost,
                dungeon = s.dungeon.copy(rooms = secretMap),
                playerX = nX,
                playerY = nY
            ),
            EngineInput(),
            0.016f
        )
        assertEquals(secretCoord, inSecret.currentCoord)
        assertTrue("Return door SOUTH must be open after entering secret room", Direction.SOUTH in inSecret.currentRoom.doors)
    }

    @Test
    fun treasureRoomBacktrackingPreservesAllItemsEvenNextToShiftingDoors() {
        val sfx = SoundEffectsEngine().apply { enabled = false }
        val engine = GameEngine(sfx = sfx, onDiscovery = { _, _ -> }, onAchievement = { })
        var s = engine.createNewRun(GameCatalog.characterById(CharacterId.PATCH), Difficulty.NORMAL, emptyList())

        val hubCoord = GridCoord(0, 1)
        val treasureCoord = GridCoord(1, 1)
        val emptyCoord = GridCoord(0, 2)

        val startRoom = s.currentRoom.copy(doors = setOf(Direction.SOUTH))
        val hubRoom = RoomBlueprint(
            coord = hubCoord,
            type = RoomType.COMBAT,
            doors = setOf(Direction.NORTH, Direction.EAST, Direction.SOUTH),
            obstacles = emptyList(),
            initialEnemies = emptyList(),
            cleared = true,
            visited = true,
            mechanic = RoomMechanic.SHIFTING_DOORS
        )
        val treasureRoom = RoomBlueprint(
            coord = treasureCoord,
            type = RoomType.TREASURE,
            doors = setOf(Direction.WEST),
            obstacles = emptyList(),
            initialEnemies = emptyList(),
            cleared = true,
            visited = false
        )
        val emptyRoom = RoomBlueprint(
            coord = emptyCoord,
            type = RoomType.COMBAT,
            doors = setOf(Direction.NORTH),
            obstacles = emptyList(),
            initialEnemies = emptyList(),
            cleared = true,
            visited = true
        )
        val map = mapOf(
            GridCoord(0, 0) to startRoom,
            hubCoord to hubRoom,
            treasureCoord to treasureRoom,
            emptyCoord to emptyRoom
        )

        // Start in cleared SHIFTING_DOORS hub room and simulate >6 seconds passing
        s = s.copy(
            currentCoord = hubCoord,
            currentRoom = hubRoom,
            dungeon = s.dungeon.copy(rooms = map),
            playerX = ARENA_W * 0.5f,
            playerY = ARENA_H * 0.5f
        )
        repeat(150) { s = engine.step(s, EngineInput(), 0.05f) }

        // Walk EAST into TREASURE room (must go to treasureCoord, NOT shifted to emptyCoord!)
        s = engine.step(s.copy(playerX = ARENA_W - WALL_THICKNESS - 10f, playerY = ARENA_H * 0.5f), EngineInput(), 0.016f)
        assertEquals(treasureCoord, s.currentCoord)
        assertEquals(RoomType.TREASURE, s.currentRoom.type)
        val treasurePickups = s.pickups.toList()
        assertTrue("Treasure room must spawn pickups on first entry", treasurePickups.size >= 2)

        // Leave TREASURE room without grabbing anything -> back to hubRoom
        s = engine.step(s.copy(playerX = WALL_THICKNESS + 10f, playerY = ARENA_H * 0.5f), EngineInput(), 0.016f)
        assertEquals(hubCoord, s.currentCoord)

        // Wait another >6s in cleared hubRoom, then go EAST back into TREASURE room
        repeat(140) { s = engine.step(s, EngineInput(), 0.05f) }
        s = engine.step(s.copy(playerX = ARENA_W - WALL_THICKNESS - 10f, playerY = ARENA_H * 0.5f), EngineInput(), 0.016f)

        assertEquals(treasureCoord, s.currentCoord)
        assertEquals(RoomType.TREASURE, s.currentRoom.type)
        assertEquals(
            "Treasure room must still contain all uncollected items upon returning",
            treasurePickups.map { it.id to it.kind },
            s.pickups.map { it.id to it.kind }
        )
    }
}
