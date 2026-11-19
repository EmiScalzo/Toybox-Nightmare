package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.GameCatalog
import com.example.data.model.*
import kotlin.math.*
import kotlin.random.Random

data class EngineInput(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val aimX: Float = 0f,
    val aimY: Float = 0f,
    val isShooting: Boolean = false
)

class GameEngine(
    private val sfx: SoundEffectsEngine,
    private val onDiscovery: (category: String, id: String) -> Unit,
    private val onAchievement: (id: String) -> Unit,
    private val onCompanionGrudge: (String) -> Unit = {},
    private val onClearCompanionGrudge: (String) -> Unit = {},
    private val onManufacturerTrust: (Boolean) -> Unit = {},
    private val onLorePuzzleSolved: (String) -> Unit = {},
    private val onUnlockBackpack: (BackpackTier) -> Unit = {},
    private val onDiscoverRecipe: (String) -> Unit = {}
) {
    private var nextEntityId = 1L
    private fun allocId(): Long = nextEntityId++

    fun createNewRun(
        character: ToyCharacterDef,
        difficulty: Difficulty,
        defects: List<DefectDef>,
        initialGrudges: List<CompanionId> = emptyList()
    ): GameRunState {
        val parts = character.startingParts.mapValues { (_, partId) ->
            EquippedBodyPart(GameCatalog.partById(partId))
        }
        val startItem = GameCatalog.itemById(character.startingItemId)
        val items = if (startItem != null) listOf(startItem) else emptyList()
        val secretBonus = items.sumOf { it.effects.secretRoomChanceBonus.toDouble() }.toFloat()
        val floor1 = DungeonGenerator.generateFloor(1, difficulty, secretBonus)
        val startRoom = floor1.rooms[floor1.startCoord]!!

        var maxHearts = character.maxHearts +
            parts.values.sumOf { it.def.effects.maxHeartsBonus } +
            items.sumOf { it.effects.maxHeartsBonus }
        if (defects.any { it.effects.fragileBody }) {
            maxHearts = (maxHearts - 1).coerceAtLeast(2)
        }
        maxHearts = maxHearts.coerceAtLeast(1)

        val initialConsumables = buildList {
            add(ConsumableType.BOMB)
            add(ConsumableType.SEWING_KIT)
            if (difficulty == Difficulty.SUAVE) add(ConsumableType.REPAIR_PATCH)
        }

        onDiscovery("character", character.id.name)
        parts.values.forEach { onDiscovery("part", it.def.id) }
        items.forEach { onDiscovery("item", it.id) }

        val synergies = GameCatalog.computeActiveSynergies(parts, items)

        val startInteractables = InteractablesSystem.generateForRoom(
            RoomType.START, 1, startRoom.obstacles, null, ::allocId, lightingMode = startRoom.lightingMode
        )
        val startLamps = LightingSystem.generateRoomLamps(startRoom, ::allocId)
        val initialContracts = ContractSystem.generateBoardContracts(1)

        return GameRunState(
            isActive = true,
            character = character,
            difficulty = difficulty,
            floorNumber = 1,
            floorTheme = GameCatalog.floors[0],
            dungeon = floor1,
            currentCoord = floor1.startCoord,
            currentRoom = startRoom,
            hearts = maxHearts,
            maxHearts = maxHearts,
            equippedParts = parts,
            passiveItems = items,
            activeSynergies = synergies,
            activeDefects = defects,
            consumables = initialConsumables,
            buttons = if (difficulty == Difficulty.SUAVE) 6 else 3,
            pendingGrudgeCompanions = initialGrudges,
            pickups = emptyList(),
            roomInteractables = startInteractables,
            roomLamps = startLamps,
            ownedLightSources = listOf(LightSourceType.CANDLE),
            lightSource = LightSourceType.CANDLE,
            lightOn = true,
            lightSourceCharge = 100f,
            availableContracts = initialContracts,
            activeBanner = RunUiBanner(
                title = "Piso 1: ${GameCatalog.floors[0].name}",
                subtitle = GameCatalog.floors[0].subtitle
            )
        )
    }

    fun step(state: GameRunState, input: EngineInput, dtRaw: Float): GameRunState {
        if (state.pendingRepairMinigame != null) {
            return RepairMinigameSystem.tickSpringMinigame(state, dtRaw.coerceIn(0.001f, 0.05f))
        }
        if (!state.isActive || state.isPaused || state.isGameOver || state.isVictory ||
            state.activeEvent != null || state.pendingPartChoice != null ||
            state.pendingCompanionDialog != null || state.manufacturerEncounter != null ||
            state.pendingArchiveDiscovery != null || state.contractBoardOpen ||
            state.shopOpen || state.repairBenchOpen
        ) {
            return state
        }

        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        var s = state.copy(
            elapsedSeconds = state.elapsedSeconds + dt,
            invulnTimer = (state.invulnTimer - dt).coerceAtLeast(0f),
            dodgeTimer = (state.dodgeTimer - dt).coerceAtLeast(0f),
            dodgeCooldownTimer = (state.dodgeCooldownTimer - dt).coerceAtLeast(0f),
            fireCooldownTimer = (state.fireCooldownTimer - dt).coerceAtLeast(0f),
            abilityCooldownTimer = (state.abilityCooldownTimer - dt).coerceAtLeast(0f),
            abilityBuffTimer = (state.abilityBuffTimer - dt).coerceAtLeast(0f),
            disorientTimer = (state.disorientTimer - dt).coerceAtLeast(0f),
            lightFlashTimer = (state.lightFlashTimer - dt).coerceAtLeast(0f),
            blurPulseTimer = (state.blurPulseTimer + dt),
            stealthTimer = (state.stealthTimer - dt).coerceAtLeast(0f),
            screenShake = (state.screenShake - dt * 28f).coerceAtLeast(0f),
            activeBanner = state.activeBanner?.let {
                val rem = it.ttlSec - dt
                if (rem <= 0f) null else it.copy(ttlSec = rem)
            }
        )

        // Check periodic torso/item sound & stun waves
        s = updatePeriodicWaves(s, dt)

        // Lighting & Emotional state updates
        s = LightingSystem.stepLighting(s, dt)
        s = EmotionSystem.stepEmotions(s, dt)

        // Player Movement & Dodge
        s = updatePlayerMovement(s, input, dt)

        // Dynamic Room Mechanics (Tilt, Vanishing Floor, Shifting Doors, Blackout, Carousel)
        s = RoomMechanicSystem.stepMechanics(s, dt, ::damagePlayer)

        // Interactive Environment Toys & Traps
        s = InteractablesSystem.stepInteractables(s, dt, ::damagePlayer, ::allocId, sfx, onDiscovery)

        // Manual Aim & Shoot
        s = updatePlayerShooting(s, input, dt)

        // Boss Arena Mechanics & 3-Phase Transitions (consumes projectiles hitting anchors/boxes/switches)
        s = BossMechanicSystem.stepBossMechanics(s, dt, ::allocId)

        // Update Bombs
        s = updateBombs(s, dt)

        // Update Enemies, AI & Enemy Relationships
        s = updateEnemies(s, dt)
        s = EnemyRelationshipSystem.stepRelationships(s, dt, ::allocId)

        // Update Alternative Room Objectives
        s = RoomObjectiveSystem.stepObjective(s, dt, ::damagePlayer, ::allocId, sfx)

        // Check Physical Secret Anomalies & Lore Room Puzzles
        val prevAnomResolved = s.roomAnomalies.count { it.resolved }
        val prevPuzzlesSolved = s.lorePuzzlesSolvedThisRun
        s = SecretDiscoverySystem.stepAnomalies(s, ::allocId, sfx, onDiscovery)
        s = PuzzleInteractionSystem.stepLoreProps(s, dt, ::damagePlayer, ::allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
        if (s.roomAnomalies.count { it.resolved } > prevAnomResolved || s.lorePuzzlesSolvedThisRun > prevPuzzlesSolved) {
            s = ContractSystem.onRelicOrClueFound(s)
            s = EmotionSystem.triggerEmotion(s, PatchEmotion.CURIOSITY, 24f, 20f)
        }

        // Update Labyrinth Progress & Conveyor Physics
        s = LabyrinthSystem.stepLabyrinth(s, dt, ::allocId)

        // Update Conditional Companions
        s = CompanionSystem.updateCompanions(s, dt, ::allocId, sfx)

        // Update Projectiles
        s = updateProjectiles(s, dt)

        // Update Pickups & Decoys & Particles
        s = updatePickupsAndFx(s, dt)

        // Update Contextual Interact Prompt
        s = s.copy(interactionPrompt = computeInteractionPrompt(s))

        // Check Room Clear & Door Transitions
        s = checkRoomStateAndDoors(s)

        return s
    }

    private fun updatePeriodicWaves(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val hasMusicalPart = s.equippedParts.values.any { !it.isDetached && it.def.effects.periodicSoundWave }
        val hasMusicItem = s.passiveItems.any { it.effects.periodicStunWave }
        val sonicAmp = s.activeSynergies.any { it.effectId == "sonic_amp" }

        if (hasMusicalPart) {
            val nextT = s.soundWaveTimer + dt
            if (nextT >= 4.5f) {
                val torsoWear = s.equippedParts[BodyZone.TORSO]?.wearState
                val waveRadius = when (torsoWear) {
                    WearState.INTACT -> 185f
                    WearState.DAMAGED -> 130f
                    else -> 95f
                }
                val waveDmg = if (sonicAmp) 14f else 6f
                val updatedEnemies = s.enemies.map { e ->
                    val d = hypot(e.x - s.playerX, e.y - s.playerY)
                    if (d <= waveRadius) {
                        e.copy(
                            hp = e.hp - waveDmg,
                            stunnedTimer = maxOf(e.stunnedTimer, 0.8f),
                            hitFlashTimer = 0.12f
                        )
                    } else e
                }
                val fx = s.particles + spawnRingParticles(s.playerX, s.playerY, waveRadius * 0.6f, 0xFFC89948)
                s = s.copy(soundWaveTimer = 0f, enemies = updatedEnemies, particles = fx)
            } else {
                s = s.copy(soundWaveTimer = nextT)
            }
        }

        if (hasMusicItem) {
            val nextT = s.stunWaveTimer + dt
            if (nextT >= 6.0f) {
                val updatedEnemies = s.enemies.map { e ->
                    if (hypot(e.x - s.playerX, e.y - s.playerY) <= 210f) {
                        e.copy(stunnedTimer = maxOf(e.stunnedTimer, 1.4f))
                    } else e
                }
                s = s.copy(stunWaveTimer = 0f, enemies = updatedEnemies)
            } else {
                s = s.copy(stunWaveTimer = nextT)
            }
        }

        if (s.decoys.isNotEmpty() && s.activeSynergies.any { it.effectId == "decoy_pulse" }) {
            val updatedEnemies = s.enemies.map { e ->
                val nearDecoy = s.decoys.any { d -> hypot(e.x - d.x, e.y - d.y) <= 135f }
                if (nearDecoy) e.copy(hp = e.hp - 12f * dt) else e
            }
            s = s.copy(enemies = updatedEnemies)
        }
        return s
    }

    private fun updatePlayerMovement(state: GameRunState, input: EngineInput, dt: Float): GameRunState {
        var speedMult = 1f
        state.equippedParts.values.forEach { part ->
            if (!part.isDetached) {
                val factor = when (part.wearState) {
                    WearState.INTACT -> 1f
                    WearState.DAMAGED -> 0.5f
                    WearState.CRITICAL -> 0.2f
                    WearState.DETACHED -> 0f
                }
                speedMult *= (1f + (part.def.effects.moveSpeedMult - 1f) * factor)
            } else if (part.def.zone == BodyZone.LEGS) {
                speedMult *= 0.72f
            }
        }
        if (state.equippedParts[BodyZone.LEGS]?.wearState == WearState.CRITICAL) {
            speedMult *= 0.86f
        }
        val emotionMods = EmotionSystem.computeModifiers(state.emotionState)
        state.passiveItems.forEach { speedMult *= it.effects.speedMult }
        state.activeDefects.forEach { speedMult *= it.effects.speedMult }
        if (state.activeSynergies.any { it.effectId == "carousel_overclock" }) speedMult *= 1.12f
        speedMult *= BackpackSystem.speedPenaltyMult(state) * emotionMods.moveSpeedMult

        val maxSpeed = state.character.baseSpeed * speedMult

        var vx: Float
        var vy: Float
        if (state.dodgeTimer > 0f) {
            var dodgeMult = 2.55f * emotionMods.dodgeLengthMult
            state.equippedParts.values.filter { !it.isDetached }.forEach {
                if (it.wearState != WearState.CRITICAL) dodgeMult *= it.def.effects.dodgeLengthMult
            }
            state.passiveItems.forEach { dodgeMult *= it.effects.dodgeLengthMult }
            state.activeDefects.forEach { dodgeMult *= it.effects.dodgeDistanceMult }
            vx = state.dodgeDirX * maxSpeed * dodgeMult
            vy = state.dodgeDirY * maxSpeed * dodgeMult
        } else {
            var mx = input.moveX
            var my = input.moveY
            if (state.disorientTimer > 0f) {
                mx = -mx
                my = -my
            }
            if (state.activeDefects.any { it.effects.irregularWalk } && (abs(mx) > 0.1f || abs(my) > 0.1f)) {
                val wobble = sin(state.elapsedSeconds * 9f) * 0.22f
                mx += wobble
            }
            val mag = hypot(mx, my)
            val normX = if (mag > 1f) mx / mag else mx
            val normY = if (mag > 1f) my / mag else my
            val targetVx = normX * maxSpeed
            val targetVy = normY * maxSpeed
            val accel = if (state.activeDefects.any { it.effects.stiffAccel }) 7.5f else 18f
            vx = state.playerVx + (targetVx - state.playerVx) * (accel * dt).coerceAtMost(1f)
            vy = state.playerVy + (targetVy - state.playerVy) * (accel * dt).coerceAtMost(1f)
        }

        val playerR = 22f
        var nx = (state.playerX + vx * dt).coerceIn(WALL_THICKNESS + playerR, ARENA_W - WALL_THICKNESS - playerR)
        var ny = (state.playerY + vy * dt).coerceIn(WALL_THICKNESS + playerR, ARENA_H - WALL_THICKNESS - playerR)

        // Obstacle collisions
        for (obs in state.currentRoom.obstacles) {
            val ox = obs.x * ARENA_W
            val oy = obs.y * ARENA_H
            val ow = obs.w * ARENA_W
            val oh = obs.h * ARENA_H
            val closestX = nx.coerceIn(ox, ox + ow)
            val closestY = ny.coerceIn(oy, oy + oh)
            val dx = nx - closestX
            val dy = ny - closestY
            val dist = hypot(dx, dy)
            if (dist < playerR) {
                if (dist > 0.001f) {
                    val push = playerR - dist
                    nx += (dx / dist) * push
                    ny += (dy / dist) * push
                } else {
                    ny = oy - playerR
                }
            }
        }

        val speedMag = hypot(vx, vy)
        val walkAdvance = if (speedMag > 15f) dt * (speedMag / 35f) else 0f
        val newFacing = if (abs(input.aimX) > 0.15f || abs(input.aimY) > 0.15f) {
            atan2(input.aimY, input.aimX)
        } else if (speedMag > 20f) {
            atan2(vy, vx)
        } else {
            state.facingAngle
        }

        return state.copy(
            playerX = nx,
            playerY = ny,
            playerVx = vx,
            playerVy = vy,
            walkCycle = state.walkCycle + walkAdvance,
            facingAngle = newFacing,
            aimX = input.aimX,
            aimY = input.aimY
        )
    }

    private fun updatePlayerShooting(state: GameRunState, input: EngineInput, dt: Float): GameRunState {
        var s = state
        val aiming = hypot(input.aimX, input.aimY) > 0.2f
        if (!input.isShooting && !aiming) {
            if (s.activeDefects.any { it.effects.impulseDelayThenBurst }) {
                s = s.copy(impulsePauseTimer = s.impulsePauseTimer + dt, impulseBurstTimer = 0f)
            }
            return s
        }

        if (s.activeDefects.any { it.effects.impulseDelayThenBurst } && s.impulsePauseTimer > 1.2f) {
            s = s.copy(impulsePauseTimer = 0f, impulseBurstTimer = 2.0f, fireCooldownTimer = 0.28f)
            return s
        }

        if (s.fireCooldownTimer > 0f) {
            if (s.impulseBurstTimer > 0f) s = s.copy(impulseBurstTimer = (s.impulseBurstTimer - dt).coerceAtLeast(0f))
            return s
        }

        val arm = s.equippedParts[BodyZone.ARMS]
        if (arm != null && arm.isDetached) {
            // Detached arm -> weak emergency throw at half fire rate
            return spawnPlayerShot(s, input, damageOverride = 4.5f, rateMult = 0.55f, style = AttackStyle.BUTTON_SHOT)
        }

        // Spring arm critical misfire jam check
        if (arm?.def?.id == "spring_arm" && arm.wearState == WearState.CRITICAL && Random.nextFloat() < 0.18f) {
            return s.copy(fireCooldownTimer = 0.45f)
        }

        return spawnPlayerShot(s, input)
    }

    private fun spawnPlayerShot(
        state: GameRunState,
        input: EngineInput,
        damageOverride: Float? = null,
        rateMult: Float = 1f,
        style: AttackStyle? = null
    ): GameRunState {
        val arm = state.equippedParts[BodyZone.ARMS]
        val attackStyle = style ?: arm?.def?.effects?.attackStyle ?: AttackStyle.BUTTON_SHOT

        var dmg = damageOverride ?: state.character.baseDamage
        var fireRate = state.character.baseFireRate * rateMult
        var spread = 0.04f

        state.equippedParts.values.filter { !it.isDetached }.forEach { part ->
            val factor = when (part.wearState) {
                WearState.INTACT -> 1f
                WearState.DAMAGED -> 0.6f
                WearState.CRITICAL -> 0.25f
                WearState.DETACHED -> 0f
            }
            dmg += part.def.effects.damageBonus * factor
            fireRate *= (1f + (part.def.effects.fireRateMult - 1f) * factor)
            spread = (spread - part.def.effects.accuracyBonus * factor).coerceAtLeast(0f)
        }

        state.passiveItems.forEach { item ->
            dmg += item.effects.damageBonus
            fireRate *= item.effects.fireRateMult
        }
        state.activeDefects.forEach { def ->
            spread += def.effects.spreadPenalty
            spread = (spread - def.effects.frontalPrecisionBonus).coerceAtLeast(0f)
            if (def.effects.weakHeavyFastLight) {
                if (attackStyle == AttackStyle.EXPLOSIVE_TOY || attackStyle == AttackStyle.SCISSOR_SLASH) {
                    dmg *= 0.82f
                } else {
                    fireRate *= 1.15f
                }
            }
        }
        val emotionMods = EmotionSystem.computeModifiers(state.emotionState)
        if (state.abilityBuffTimer > 0f) fireRate *= 2.0f
        if (state.impulseBurstTimer > 0f) fireRate *= 1.45f
        if (state.activeSynergies.any { it.effectId == "carousel_overclock" }) fireRate *= 1.22f
        dmg *= emotionMods.damageMult
        fireRate *= emotionMods.fireRateMult
        spread = (spread + emotionMods.spreadDelta).coerceAtLeast(0f)

        val isDouble = state.activeSynergies.any { it.effectId == "double_shot" }
        val isHoming = state.passiveItems.any { it.effects.homingBullets } ||
            state.activeSynergies.any { it.effectId == "homing_bounce" || it.effectId == "homing_pierce" }
        val hasPorcelainRicochet = state.activeSynergies.any { it.effectId == "porcelain_ricochet" }
        val isBounce = state.passiveItems.any { it.effects.bounceBullets } ||
            state.activeSynergies.any { it.effectId == "homing_bounce" } ||
            hasPorcelainRicochet
        val extraBounces = state.passiveItems.sumOf { it.effects.extraBounces } +
            (if (hasPorcelainRicochet) 2 else 0)
        val isPierce = state.passiveItems.any { it.effects.pierceBullets } ||
            state.activeSynergies.any { it.effectId == "homing_pierce" }
        val isHeavy = state.activeSynergies.any { it.effectId == "heavy_gear" }

        if (isHeavy) dmg += 2.5f
        if (hasPorcelainRicochet) dmg += 2.5f
        if (state.activeSynergies.any { it.effectId == "greed_power" }) {
            dmg += minOf(6f, state.buttons * 0.15f)
        }
        val totalCrit = state.passiveItems.sumOf { it.effects.critChance.toDouble() }.toFloat() +
            (if (state.activeSynergies.any { it.effectId == "oracle_crit" }) 0.20f else 0f) +
            emotionMods.critBonus
        val isCrit = totalCrit > 0f && Random.nextFloat() < totalCrit
        if (isCrit) dmg *= 1.75f

        val baseAngle = if (hypot(input.aimX, input.aimY) > 0.15f) {
            atan2(input.aimY, input.aimX)
        } else {
            state.facingAngle
        }

        val oracleRange = if (state.activeSynergies.any { it.effectId == "oracle_crit" }) 1.3f else 1f
        val lifeSec = (if (state.activeDefects.any { it.effects.shortRangeBonusDamage }) 0.85f else 1.45f) * oracleRange
        val projSpeed = when (attackStyle) {
            AttackStyle.GEAR_PROJECTILE -> 560f
            AttackStyle.SCISSOR_SLASH -> 500f
            AttackStyle.EXPLOSIVE_TOY -> 390f
            else -> 510f
        }
        val projRadius = when {
            isHeavy -> 13f
            attackStyle == AttackStyle.EXPLOSIVE_TOY -> 12f
            attackStyle == AttackStyle.SCISSOR_SLASH -> 11f
            else -> 8.5f
        }
        val colorHex = if (isCrit) 0xFFFFD15C else when (attackStyle) {
            AttackStyle.GEAR_PROJECTILE -> 0xFFC89948
            AttackStyle.SCISSOR_SLASH -> 0xFFD9E0E6
            AttackStyle.EXPLOSIVE_TOY -> 0xFFD13B42
            AttackStyle.DOLL_BOLT -> 0xFFE8DCC4
            else -> 0xFFD5C3A1
        }

        val offsets = if (isDouble) listOf(-0.09f, 0.09f) else listOf(0f)
        val spawned = offsets.map { offset ->
            val angle = baseAngle + offset + (Random.nextFloat() - 0.5f) * spread
            ActiveProjectile(
                id = allocId(),
                x = state.playerX + cos(angle) * 20f,
                y = state.playerY + sin(angle) * 20f,
                vx = cos(angle) * projSpeed,
                vy = sin(angle) * projSpeed,
                radius = projRadius,
                damage = dmg,
                fromPlayer = true,
                colorHex = colorHex,
                style = attackStyle,
                lifeSec = lifeSec,
                pierceLeft = if (isPierce) 1 else 0,
                bouncesLeft = if (isBounce) 1 + extraBounces else 0,
                homing = isHoming,
                explosive = attackStyle == AttackStyle.EXPLOSIVE_TOY || state.modifiedArmExplosiveGears
            )
        }

        sfx.play(ToySfx.SHOOT)
        val mirrorShots = RoomMechanicSystem.mirrorShotsForCopies(state, spawned, ::allocId)
        return state.copy(
            projectiles = state.projectiles + spawned + mirrorShots,
            fireCooldownTimer = (1f / fireRate.coerceAtLeast(0.8f)),
            impulsePauseTimer = 0f
        )
    }

    fun triggerDodge(state: GameRunState, moveX: Float, moveY: Float): GameRunState {
        if (state.dodgeCooldownTimer > 0f || state.dodgeTimer > 0f || !state.isActive) return state
        var dx = moveX
        var dy = moveY
        val mag = hypot(dx, dy)
        if (mag < 0.15f) {
            dx = cos(state.facingAngle)
            dy = sin(state.facingAngle)
        } else {
            dx /= mag
            dy /= mag
        }

        var cd = state.character.dodgeCooldownSec
        state.equippedParts.values.filter { !it.isDetached }.forEach {
            cd *= it.def.effects.dodgeCooldownMult
        }
        state.passiveItems.forEach { cd *= it.effects.dodgeCooldownMult }
        state.activeDefects.forEach { cd *= it.effects.dodgeCooldownMult }
        if (state.equippedParts[BodyZone.LEGS]?.wearState == WearState.CRITICAL) cd *= 1.25f

        var s = state.copy(
            dodgeTimer = 0.22f,
            dodgeCooldownTimer = cd,
            invulnTimer = maxOf(state.invulnTimer, 0.28f),
            dodgeDirX = dx,
            dodgeDirY = dy,
            stealthTimer = if (state.activeSynergies.any { it.effectId == "phase_dodge" }) 1.1f else state.stealthTimer
        )

        // Faulty spring shockwave or Jester's Leap synergy
        val hasShockwave = s.activeDefects.any { it.effects.dodgeShockwave }
        val hasJesterBomb = s.activeSynergies.any { it.effectId == "dodge_bomb" }
        if (hasShockwave || hasJesterBomb) {
            val dmg = if (hasJesterBomb) 24f else 10f
            val r = if (hasJesterBomb) 130f else 95f
            val hitEnemies = s.enemies.map { e ->
                if (hypot(e.x - s.playerX, e.y - s.playerY) <= r) {
                    e.copy(hp = e.hp - dmg, stunnedTimer = maxOf(e.stunnedTimer, 0.7f), hitFlashTimer = 0.15f)
                } else e
            }
            s = s.copy(
                enemies = hitEnemies,
                particles = s.particles + spawnRingParticles(s.playerX, s.playerY, r * 0.65f, 0xFFD13B42)
            )
        }

        // Toybox Remembers tracking
        val nextDodgeCount = s.toyboxMemory.dodgeUseCount + 1
        var mem = s.toyboxMemory.copy(dodgeUseCount = nextDodgeCount)
        var banner = s.activeBanner
        if (nextDodgeCount == 8 && !mem.enemiesAdaptDodge) {
            mem = mem.copy(enemiesAdaptDodge = true)
            banner = RunUiBanner(
                "La juguetería recuerda...",
                "Los juguetes anticipan ahora tu ritmo de esquiva.",
                0xFFD13B42
            )
            onAchievement("ach_toybox_remembers")
        }

        sfx.play(ToySfx.DODGE)
        return s.copy(toyboxMemory = mem, activeBanner = banner)
    }

    fun triggerAbility(state: GameRunState): GameRunState {
        if (state.abilityCooldownTimer > 0f || !state.isActive) return state
        val char = state.character
        var s = state.copy(abilityCooldownTimer = char.abilityCooldownSec)

        when (char.id) {
            CharacterId.PATCH -> {
                val repaired = repairAllParts(s, 25)
                sfx.play(ToySfx.REPAIR_STITCH)
                s = repaired.copy(
                    particles = repaired.particles + spawnRingParticles(s.playerX, s.playerY, 45f, 0xFF597354),
                    activeBanner = RunUiBanner("Remiendo Rápido", "Todas las piezas recuperan +25 de costura.", 0xFF597354, 2.0f)
                )
            }
            CharacterId.TINNY -> {
                sfx.play(ToySfx.SYNERGY)
                s = s.copy(
                    abilityBuffTimer = 4.0f,
                    activeBanner = RunUiBanner("Sobrecarga de Cuerda", "Cadencia de disparo duplicada 4s.", 0xFFC89948, 2.0f)
                )
            }
            CharacterId.DOLLY -> {
                val shards = (0 until 10).map { i ->
                    val ang = (i / 10f) * (2f * PI.toFloat())
                    ActiveProjectile(
                        id = allocId(),
                        x = s.playerX, y = s.playerY,
                        vx = cos(ang) * 480f, vy = sin(ang) * 480f,
                        radius = 9f, damage = 14f, fromPlayer = true,
                        colorHex = 0xFFE8DCC4, style = AttackStyle.SCISSOR_SLASH, pierceLeft = 1
                    )
                }
                sfx.play(ToySfx.SHOOT)
                s = s.copy(projectiles = s.projectiles + shards)
            }
            CharacterId.HOPS -> {
                val stunned = s.enemies.map { e ->
                    if (hypot(e.x - s.playerX, e.y - s.playerY) <= 210f) {
                        e.copy(stunnedTimer = 2.0f, hp = e.hp - 10f, hitFlashTimer = 0.15f)
                    } else e
                }
                sfx.play(ToySfx.DODGE)
                s = s.copy(
                    enemies = stunned,
                    invulnTimer = 0.5f,
                    particles = s.particles + spawnRingParticles(s.playerX, s.playerY, 90f, 0xFF597354)
                )
            }
            CharacterId.JACK -> {
                val hit = s.enemies.map { e ->
                    if (hypot(e.x - s.playerX, e.y - s.playerY) <= 220f) {
                        e.copy(hp = e.hp - 30f, stunnedTimer = 1.2f, hitFlashTimer = 0.2f)
                    } else e
                }
                sfx.play(ToySfx.EXPLOSION)
                s = s.copy(
                    enemies = hit,
                    screenShake = 14f,
                    particles = s.particles + spawnRingParticles(s.playerX, s.playerY, 120f, 0xFFD13B42)
                )
            }
            CharacterId.STITCH -> {
                val repaired = repairAllParts(s, 40)
                // Also auto-recover detached ground parts
                val groundParts = repaired.pickups.filter { it.kind == PickupKind.DETACHED_PART }
                val updatedMap = repaired.equippedParts.toMutableMap()
                groundParts.forEach { gp ->
                    gp.detachedPart?.let { dp ->
                        updatedMap[dp.def.zone] = dp.copy(isDetached = false, durability = 55)
                    }
                }
                sfx.play(ToySfx.REPAIR_STITCH)
                s = repaired.copy(
                    equippedParts = updatedMap,
                    pickups = repaired.pickups.filter { it.kind != PickupKind.DETACHED_PART },
                    particles = repaired.particles + spawnRingParticles(s.playerX, s.playerY, 60f, 0xFF597354)
                )
            }
        }
        return s
    }

    fun cycleConsumable(state: GameRunState): GameRunState {
        if (state.consumables.isEmpty()) return state
        return state.copy(selectedConsumableIdx = (state.selectedConsumableIdx + 1) % state.consumables.size)
    }

    fun useSelectedConsumable(state: GameRunState): GameRunState {
        if (state.consumables.isEmpty() || !state.isActive) return state
        val idx = state.selectedConsumableIdx.coerceIn(0, state.consumables.lastIndex)
        val item = state.consumables[idx]
        val remaining = state.consumables.toMutableList().apply { removeAt(idx) }
        var s = state.copy(
            consumables = remaining,
            selectedConsumableIdx = 0,
            usedConsumableInBoss = state.usedConsumableInBoss || (state.currentRoom.type == RoomType.BOSS)
        )

        when (item) {
            ConsumableType.BOMB, ConsumableType.EXPLOSIVE_TOY -> {
                val tx = if (item == ConsumableType.EXPLOSIVE_TOY) {
                    (s.playerX + cos(s.facingAngle) * 110f).coerceIn(WALL_THICKNESS + 20f, ARENA_W - WALL_THICKNESS - 20f)
                } else s.playerX
                val ty = if (item == ConsumableType.EXPLOSIVE_TOY) {
                    (s.playerY + sin(s.facingAngle) * 110f).coerceIn(WALL_THICKNESS + 20f, ARENA_H - WALL_THICKNESS - 20f)
                } else s.playerY
                val nextExp = s.toyboxMemory.explosiveUseCount + 1
                var mem = s.toyboxMemory.copy(explosiveUseCount = nextExp)
                var banner = s.activeBanner
                if (nextExp == 4 && !mem.enemiesStealBombs) {
                    mem = mem.copy(enemiesStealBombs = true)
                    banner = RunUiBanner(
                        "La juguetería recuerda...",
                        "¡Los duendes aprendieron a patear tus bombas!",
                        0xFFD13B42
                    )
                    onAchievement("ach_toybox_remembers")
                }
                s = s.copy(
                    bombs = s.bombs + HazardBomb(allocId(), tx, ty, fuseSec = 1.25f),
                    toyboxMemory = mem,
                    activeBanner = banner
                )
                s = ContractSystem.onBombUsed(s)
            }
            ConsumableType.SEWING_KIT -> {
                val mostDamaged = s.equippedParts.values.minByOrNull { if (it.isDetached) -1 else it.durability }
                if (mostDamaged != null) {
                    val repairMult = s.activeDefects.fold(1f) { acc, d -> acc * d.effects.repairBonusMult }
                    val gain = (45 * repairMult).toInt()
                    val map = s.equippedParts.toMutableMap()
                    map[mostDamaged.def.zone] = mostDamaged.copy(
                        isDetached = false,
                        durability = (mostDamaged.durability + gain).coerceAtMost(mostDamaged.def.maxDurability)
                    )
                    sfx.play(ToySfx.REPAIR_STITCH)
                    s = s.copy(
                        equippedParts = map,
                        usedAnyRepair = true,
                        activeBanner = RunUiBanner("Kit de Costura", "${mostDamaged.def.name} reparado (+$gain).", 0xFF597354, 2.0f)
                    )
                    s = ContractSystem.onRepairDone(s, viaBench = false)
                }
            }
            ConsumableType.REPAIR_PATCH -> {
                val repaired = repairAllParts(s, 20)
                sfx.play(ToySfx.REPAIR_STITCH)
                s = repaired.copy(
                    hearts = (repaired.hearts + 1).coerceAtMost(repaired.maxHearts),
                    usedAnyRepair = true,
                    activeBanner = RunUiBanner("Parche de Tela", "+1 Corazón y +20 durabilidad general.", 0xFF597354, 2.0f)
                )
                s = ContractSystem.onRepairDone(s, viaBench = false)
            }
            ConsumableType.WINDUP_STRING -> {
                sfx.play(ToySfx.SYNERGY)
                s = s.copy(
                    abilityBuffTimer = 5.5f,
                    activeBanner = RunUiBanner("Cuerda de Reloj", "Cadencia de disparo aumentada 5.5s.", 0xFFC89948, 2.0f)
                )
            }
            ConsumableType.SPRING_COIL -> {
                sfx.play(ToySfx.DODGE)
                s = s.copy(
                    dodgeCooldownTimer = 0f,
                    invulnTimer = 0.8f,
                    activeBanner = RunUiBanner("Muelle Reforzado", "Esquiva reiniciada y protección breve.", 0xFFC89948, 1.8f)
                )
            }
            ConsumableType.KEY -> {
                // Opens secret doors if any or grants bonus chest loot
                if (s.currentRoom.secretDoors.isNotEmpty()) {
                    val opened = s.currentRoom.copy(
                        doors = s.currentRoom.doors + s.currentRoom.secretDoors,
                        secretDoors = emptySet()
                    )
                    val rooms = s.dungeon.rooms.toMutableMap()
                    rooms[s.currentCoord] = opened
                    sfx.play(ToySfx.DOOR_OPEN)
                    s = s.copy(
                        currentRoom = opened,
                        dungeon = s.dungeon.copy(rooms = rooms),
                        activeBanner = RunUiBanner("Puerta Secreta Abierta", "Un pasadizo oculto se revela.", 0xFFC89948, 2.4f)
                    )
                } else {
                    sfx.play(ToySfx.PICKUP_BUTTON)
                    s = s.copy(
                        buttons = s.buttons + 5,
                        activeBanner = RunUiBanner("Cerradura Forzada", "+5 Botones de una caja oculta.", 0xFFC89948, 2.0f)
                    )
                }
            }
            ConsumableType.SURPRISE_BOX -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        sfx.play(ToySfx.PICKUP_ITEM)
                        s = s.copy(
                            buttons = s.buttons + 6,
                            hearts = (s.hearts + 1).coerceAtMost(s.maxHearts),
                            activeBanner = RunUiBanner("Caja Sorpresa", "¡+1 Corazón y +6 Botones!", 0xFFC89948, 2.2f)
                        )
                    }
                    1 -> {
                        val hit = s.enemies.map { it.copy(hp = it.hp - 25f, stunnedTimer = 2f) }
                        sfx.play(ToySfx.EXPLOSION)
                        s = s.copy(
                            enemies = hit,
                            activeBanner = RunUiBanner("Caja Sorpresa", "¡Estallido de confeti cortante!", 0xFFD13B42, 2.2f)
                        )
                    }
                    else -> {
                        sfx.play(ToySfx.REPAIR_STITCH)
                        s = repairAllParts(s, 30).copy(
                            activeBanner = RunUiBanner("Caja Sorpresa", "Bobinas mágicas cosen tus piezas.", 0xFF597354, 2.2f)
                        )
                    }
                }
            }
        }
        return s
    }

    private fun updateBombs(state: GameRunState, dt: Float): GameRunState {
        if (state.bombs.isEmpty()) return state
        val remaining = mutableListOf<HazardBomb>()
        var s = state
        for (b in s.bombs) {
            val nf = b.fuseSec - dt
            if (nf <= 0f) {
                sfx.play(ToySfx.EXPLOSION)
                var killsInBlast = 0
                val updatedEnemies = s.enemies.map { e ->
                    if (hypot(e.x - b.x, e.y - b.y) <= b.radius + e.radius) {
                        if (!LightingSystem.isVeiledSpecterVulnerable(s, e)) {
                            e
                        } else {
                            val elemDmg = EnemyRelationshipSystem.computeElementalDamage(
                                b.damage, DamageElement.EXPLOSIVE, e.materialType
                            )
                            val (finalDmg, afterBoss) = if (e.type.isBoss) {
                                val (bd, be, _) = BossMechanicSystem.computeIncomingDamage(s, e, elemDmg, DamageElement.EXPLOSIVE)
                                bd to be
                            } else elemDmg to e
                            val newHp = afterBoss.hp - finalDmg
                            if (newHp <= 0f && e.hp > 0f) killsInBlast++
                            afterBoss.copy(hp = newHp, hitFlashTimer = 0.2f, alertState = EnemyAlertState.COMBAT, isSleeping = false)
                        }
                    } else e
                }
                if (killsInBlast >= 2 && b.fromPlayer) onAchievement("ach_bomb_multikill")
                if (!b.fromPlayer && hypot(s.playerX - b.x, s.playerY - b.y) <= b.radius) {
                    s = damagePlayer(s)
                }
                // Open secret walls if bomb explodes near a secret door!
                var room = s.currentRoom
                if (b.fromPlayer && room.secretDoors.isNotEmpty()) {
                    room = room.copy(doors = room.doors + room.secretDoors, secretDoors = emptySet())
                    val rooms = s.dungeon.rooms.toMutableMap()
                    rooms[s.currentCoord] = room
                    s = s.copy(currentRoom = room, dungeon = s.dungeon.copy(rooms = rooms))
                }
                var disorient = s.disorientTimer
                if (s.activeDefects.any { it.effects.noiseDisorient }) {
                    disorient = 1.4f
                }
                s = s.copy(
                    enemies = updatedEnemies,
                    screenShake = 16f,
                    disorientTimer = disorient,
                    particles = s.particles + spawnRingParticles(b.x, b.y, b.radius * 0.7f, 0xFFD13B42)
                )
                s = InteractablesSystem.onExplosionInteractables(s, b.x, b.y, b.radius, ::allocId, sfx)
                s = SecretDiscoverySystem.onExplosionNearAnomaly(s, b.x, b.y, b.radius, ::allocId, sfx, onDiscovery)
                s = PuzzleInteractionSystem.onExplosionLoreProps(s, b.x, b.y, b.radius, ::allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                s = CompanionSystem.onExplosionNearCompanions(s, b.x, b.y)
            } else {
                remaining.add(b.copy(fuseSec = nf))
            }
        }
        return s.copy(bombs = remaining)
    }

    private fun updateEnemies(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val newEnemyProjs = mutableListOf<ActiveProjectile>()
        val spawnedMinions = mutableListOf<ActiveEnemy>()
        val nextEnemies = mutableListOf<ActiveEnemy>()
        val targetX = s.decoys.firstOrNull()?.x ?: s.playerX
        val targetY = s.decoys.firstOrNull()?.y ?: s.playerY

        val emotionMods = EmotionSystem.computeModifiers(s.emotionState)
        val detectionMult = (s.passiveItems.fold(1f) { acc, i -> acc * i.effects.enemyDetectionMult }) *
            (if (s.activeDefects.any { it.effects.noisySteps }) 1.35f else 1f) *
            (if (s.stealthTimer > 0f) 0.4f else 1f) *
            emotionMods.enemyDetectMult * emotionMods.noiseMult
        val bulletSlowMult = s.passiveItems.fold(1f) { acc, i -> acc * i.effects.slowEnemyBulletsMult }
        val aggroMult = if (s.passiveItems.any { it.effects.bonusDropsAndAggro }) 1.2f else 1f

        for (e in s.enemies) {
            if (e.hp <= 0f) {
                s = handleEnemyKilled(s, e)
                continue
            }

            var enemy = e.copy(
                hitFlashTimer = (e.hitFlashTimer - dt).coerceAtLeast(0f),
                stunnedTimer = (e.stunnedTimer - dt).coerceAtLeast(0f),
                telegraphTimer = (e.telegraphTimer - dt).coerceAtLeast(0f),
                stateTimer = e.stateTimer + dt
            )

            if (enemy.stunnedTimer > 0f) {
                nextEnemies.add(enemy)
                continue
            }

            if (enemy.isSleeping && enemy.alertState == EnemyAlertState.CALM) {
                nextEnemies.add(enemy)
                continue
            }

            val dx = targetX - enemy.x
            val dy = targetY - enemy.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val dirX = dx / dist
            val dirY = dy / dist
            val moveSpeed = enemy.type.speed * s.difficulty.enemySpeedMult * aggroMult

            if (enemy.alertState == EnemyAlertState.PANIC || (enemy.fleeing && enemy.role != EnemyRole.COWARD)) {
                val fx = (enemy.x - dirX * moveSpeed * 1.15f * dt).coerceIn(WALL_THICKNESS + enemy.radius, ARENA_W - WALL_THICKNESS - enemy.radius)
                val fy = (enemy.y - dirY * moveSpeed * 1.15f * dt).coerceIn(WALL_THICKNESS + enemy.radius, ARENA_H - WALL_THICKNESS - enemy.radius)
                nextEnemies.add(enemy.copy(x = fx, y = fy))
                continue
            }

            // Thief gremlin seeks dropped detached body parts OR active bombs if Toybox Memory triggered!
            if (enemy.type == EnemyType.THIEF_GREMLIN) {
                val droppedPart = s.pickups.firstOrNull { it.kind == PickupKind.DETACHED_PART }
                val activeBomb = if (s.toyboxMemory.enemiesStealBombs) s.bombs.firstOrNull() else null
                if (droppedPart != null) {
                    val pdx = droppedPart.x - enemy.x
                    val pdy = droppedPart.y - enemy.y
                    val pd = hypot(pdx, pdy).coerceAtLeast(1f)
                    enemy = enemy.copy(
                        x = (enemy.x + (pdx / pd) * moveSpeed * 1.15f * dt).coerceIn(WALL_THICKNESS + 20f, ARENA_W - WALL_THICKNESS - 20f),
                        y = (enemy.y + (pdy / pd) * moveSpeed * 1.15f * dt).coerceIn(WALL_THICKNESS + 20f, ARENA_H - WALL_THICKNESS - 20f),
                        telegraphTimer = 0.25f
                    )
                    if (pd < 24f) {
                        enemy = enemy.copy(carriedPart = droppedPart.detachedPart)
                        s = s.copy(
                            pickups = s.pickups.filterNot { it.id == droppedPart.id },
                            activeBanner = RunUiBanner("¡Pieza Robada!", "El duende robó tu ${droppedPart.detachedPart?.def?.name ?: "pieza"}. ¡Derrótalo!", 0xFFD13B42, 2.5f),
                            recentEngineEvents = (s.recentEngineEvents + EngineEventType.ENEMY_STOLE_PART.key).takeLast(12)
                        )
                    }
                    nextEnemies.add(enemy)
                    continue
                } else if (activeBomb != null) {
                    val bdx = activeBomb.x - enemy.x
                    val bdy = activeBomb.y - enemy.y
                    val bd = hypot(bdx, bdy).coerceAtLeast(1f)
                    enemy = enemy.copy(
                        x = (enemy.x + (bdx / bd) * moveSpeed * 1.1f * dt).coerceIn(WALL_THICKNESS + 20f, ARENA_W - WALL_THICKNESS - 20f),
                        y = (enemy.y + (bdy / bd) * moveSpeed * 1.1f * dt).coerceIn(WALL_THICKNESS + 20f, ARENA_H - WALL_THICKNESS - 20f),
                        telegraphTimer = 0.25f
                    )
                    if (bd < 28f) {
                        // Fair & readable: kicks bomb away from enemies instead of instantly deleting it
                        val kickAngle = atan2(s.playerY - activeBomb.y, s.playerX - activeBomb.x)
                        val kickedX = (activeBomb.x + cos(kickAngle) * 110f).coerceIn(WALL_THICKNESS + 30f, ARENA_W - WALL_THICKNESS - 30f)
                        val kickedY = (activeBomb.y + sin(kickAngle) * 110f).coerceIn(WALL_THICKNESS + 30f, ARENA_H - WALL_THICKNESS - 30f)
                        s = s.copy(
                            bombs = s.bombs.map { if (it.id == activeBomb.id) it.copy(x = kickedX, y = kickedY) else it },
                            activeBanner = RunUiBanner("¡Bomba Pateada!", "El duende desvió tu explosivo.", 0xFFD98A2B, 1.8f)
                        )
                    }
                    nextEnemies.add(enemy)
                    continue
                }
            }

            when (enemy.type) {
                EnemyType.CREEPY_DOLL, EnemyType.MIMIC_CHEST -> {
                    val wakeRadius = 260f * detectionMult
                    if (enemy.disguised && dist > wakeRadius) {
                        nextEnemies.add(enemy)
                        continue
                    }
                    enemy = enemy.copy(
                        disguised = false,
                        x = enemy.x + dirX * moveSpeed * dt,
                        y = enemy.y + dirY * moveSpeed * dt
                    )
                }

                EnemyType.TIN_SOLDIER -> {
                    val desiredDist = 260f
                    val moveFactor = if (dist > desiredDist) 1f else if (dist < 160f) -0.7f else 0.1f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * moveFactor * dt,
                        y = enemy.y + dirY * moveSpeed * moveFactor * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val leadAngle = computeAimAngle(enemy.x, enemy.y, s, 320f * bulletSlowMult)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(leadAngle) * 320f * bulletSlowMult,
                                vy = sin(leadAngle) * 320f * bulletSlowMult,
                                radius = 9f, damage = 1f, fromPlayer = false, colorHex = 0xFFD13B42
                            )
                        )
                        enemy = enemy.copy(attackTimer = 1.65f / aggroMult)
                    }
                }

                EnemyType.PLUSH_BEAR -> {
                    if (enemy.telegraphTimer > 0f) {
                        // Charging!
                        enemy = enemy.copy(
                            x = enemy.x + enemy.chargeDirX * moveSpeed * 3.1f * dt,
                            y = enemy.y + enemy.chargeDirY * moveSpeed * 3.1f * dt
                        )
                    } else {
                        enemy = enemy.copy(
                            x = enemy.x + dirX * moveSpeed * dt,
                            y = enemy.y + dirY * moveSpeed * dt,
                            attackTimer = enemy.attackTimer - dt
                        )
                        if (enemy.attackTimer <= 0f && dist < 340f) {
                            enemy = enemy.copy(
                                telegraphTimer = 0.65f,
                                chargeDirX = dirX,
                                chargeDirY = dirY,
                                attackTimer = 2.6f
                            )
                        }
                    }
                }

                EnemyType.JACK_IN_BOX -> {
                    enemy = enemy.copy(attackTimer = enemy.attackTimer - dt)
                    if (enemy.attackTimer <= 0f) {
                        for (i in 0 until 6) {
                            val a = (i / 6f) * (2f * PI.toFloat()) + enemy.stateTimer
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 260f * bulletSlowMult,
                                    vy = sin(a) * 260f * bulletSlowMult,
                                    radius = 8.5f, damage = 1f, fromPlayer = false, colorHex = 0xFFD98A2B
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 2.1f / aggroMult)
                    }
                }

                EnemyType.CLOCKWORK_SPIDER -> {
                    val fleeLight = LightingSystem.isPointLitByPlayer(s, enemy.x, enemy.y)
                    val mSign = if (fleeLight) -1.25f else 1f
                    val strafeX = -dirY * sin(enemy.stateTimer * 4f) * 0.6f
                    val strafeY = dirX * sin(enemy.stateTimer * 4f) * 0.6f
                    enemy = enemy.copy(
                        x = enemy.x + (dirX * mSign + strafeX) * moveSpeed * dt,
                        y = enemy.y + (dirY * mSign + strafeY) * moveSpeed * dt
                    )
                }

                EnemyType.MIRROR_DOLL -> {
                    // Mirrors player velocity inverted + fires occasional bolt
                    enemy = enemy.copy(
                        x = enemy.x + (-s.playerVx * 0.55f + dirX * 45f) * dt,
                        y = enemy.y + (-s.playerVy * 0.55f + dirY * 45f) * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val ang = atan2(dy, dx)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(ang) * 340f * bulletSlowMult,
                                vy = sin(ang) * 340f * bulletSlowMult,
                                radius = 9f, damage = 1f, fromPlayer = false, colorHex = 0xFFE8DCC4
                            )
                        )
                        enemy = enemy.copy(attackTimer = 1.8f)
                    }
                }

                EnemyType.THIEF_GREMLIN -> {
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * dt,
                        y = enemy.y + dirY * moveSpeed * dt
                    )
                }

                EnemyType.PORCELAIN_CLOWN -> {
                    val hopX = -dirY * cos(enemy.stateTimer * 3.5f) * 0.75f
                    val hopY = dirX * cos(enemy.stateTimer * 3.5f) * 0.75f
                    enemy = enemy.copy(
                        x = enemy.x + (dirX * 0.55f + hopX) * moveSpeed * dt,
                        y = enemy.y + (dirY * 0.55f + hopY) * moveSpeed * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val baseAng = computeAimAngle(enemy.x, enemy.y, s, 290f * bulletSlowMult)
                        for (i in -1..1) {
                            val a = baseAng + i * 0.25f
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 290f * bulletSlowMult,
                                    vy = sin(a) * 290f * bulletSlowMult,
                                    radius = 8.5f, damage = 1f, fromPlayer = false, colorHex = 0xFFE5A93B
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 2.1f / aggroMult)
                    }
                }

                EnemyType.WINDUP_CHOMPER -> {
                    if (enemy.telegraphTimer > 0f) {
                        enemy = enemy.copy(
                            x = enemy.x + enemy.chargeDirX * moveSpeed * 2.2f * dt,
                            y = enemy.y + enemy.chargeDirY * moveSpeed * 2.2f * dt
                        )
                    } else {
                        enemy = enemy.copy(
                            x = enemy.x + dirX * moveSpeed * 0.8f * dt,
                            y = enemy.y + dirY * moveSpeed * 0.8f * dt,
                            attackTimer = enemy.attackTimer - dt
                        )
                        if (enemy.attackTimer <= 0f && dist < 310f) {
                            enemy = enemy.copy(
                                telegraphTimer = 0.45f,
                                chargeDirX = dirX,
                                chargeDirY = dirY,
                                attackTimer = 1.6f
                            )
                        }
                    }
                }

                EnemyType.NEEDLE_MARIONETTE -> {
                    val factor = if (dist > 300f) 0.9f else if (dist < 180f) -0.8f else 0.15f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * factor * dt,
                        y = enemy.y + dirY * moveSpeed * factor * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val baseAng = computeAimAngle(enemy.x, enemy.y, s, 360f * bulletSlowMult)
                        listOf(-0.14f, 0.14f).forEach { off ->
                            val a = baseAng + off
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 360f * bulletSlowMult,
                                    vy = sin(a) * 360f * bulletSlowMult,
                                    radius = 7f, damage = 1f, fromPlayer = false, colorHex = 0xFFD9E0E6
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 1.75f / aggroMult)
                    }
                }

                EnemyType.SHADOW_PUPPET -> {
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * dt,
                        y = enemy.y + dirY * moveSpeed * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        if (dist > 340f) {
                            // Shadow step closer while leaving clear telegraph ring
                            val jumpX = (s.playerX - dirX * 150f).coerceIn(WALL_THICKNESS + 30f, ARENA_W - WALL_THICKNESS - 30f)
                            val jumpY = (s.playerY - dirY * 150f).coerceIn(WALL_THICKNESS + 30f, ARENA_H - WALL_THICKNESS - 30f)
                            enemy = enemy.copy(x = jumpX, y = jumpY, telegraphTimer = 0.35f)
                        } else {
                            val a = atan2(dy, dx)
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 300f * bulletSlowMult,
                                    vy = sin(a) * 300f * bulletSlowMult,
                                    radius = 9f, damage = 1f, fromPlayer = false, colorHex = 0xFF9E64B5
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 1.9f / aggroMult)
                    }
                }

                EnemyType.RUSTY_TIN_KNIGHT -> {
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * dt,
                        y = enemy.y + dirY * moveSpeed * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val a = computeAimAngle(enemy.x, enemy.y, s, 260f * bulletSlowMult)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(a) * 260f * bulletSlowMult,
                                vy = sin(a) * 260f * bulletSlowMult,
                                radius = 12f, damage = 1f, fromPlayer = false, colorHex = 0xFF9EAAB5
                            )
                        )
                        enemy = enemy.copy(attackTimer = 2.0f / aggroMult)
                    }
                }

                EnemyType.RAG_BOMBER -> {
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * dt,
                        y = enemy.y + dirY * moveSpeed * dt
                    )
                }

                EnemyType.BETRAYED_BUNNY -> {
                    if (enemy.telegraphTimer > 0f) {
                        enemy = enemy.copy(
                            x = enemy.x + enemy.chargeDirX * moveSpeed * 2.5f * dt,
                            y = enemy.y + enemy.chargeDirY * moveSpeed * 2.5f * dt
                        )
                    } else {
                        enemy = enemy.copy(
                            x = enemy.x + dirX * moveSpeed * dt,
                            y = enemy.y + dirY * moveSpeed * dt,
                            attackTimer = enemy.attackTimer - dt
                        )
                        if (enemy.attackTimer <= 0f) {
                            val a = computeAimAngle(enemy.x, enemy.y, s, 340f * bulletSlowMult)
                            for (i in -1..1) {
                                val ang = a + i * 0.2f
                                newEnemyProjs.add(
                                    ActiveProjectile(
                                        id = allocId(), x = enemy.x, y = enemy.y,
                                        vx = cos(ang) * 340f * bulletSlowMult,
                                        vy = sin(ang) * 340f * bulletSlowMult,
                                        radius = 9f, damage = 1f, fromPlayer = false, colorHex = 0xFFD13B42
                                    )
                                )
                            }
                            enemy = enemy.copy(
                                telegraphTimer = 0.45f,
                                chargeDirX = dirX,
                                chargeDirY = dirY,
                                attackTimer = 2.0f / aggroMult
                            )
                        }
                    }
                }

                EnemyType.MIRROR_COPY -> {
                    val targetMirrorX = ARENA_W - s.playerX
                    val targetMirrorY = ARENA_H - s.playerY
                    val mdx = targetMirrorX - enemy.x
                    val mdy = targetMirrorY - enemy.y
                    val md = hypot(mdx, mdy).coerceAtLeast(1f)
                    enemy = enemy.copy(
                        x = enemy.x + (-s.playerVx * 0.72f + (mdx / md) * 55f) * dt,
                        y = enemy.y + (-s.playerVy * 0.72f + (mdy / md) * 55f) * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val a = atan2(dy, dx)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(a) * 310f * bulletSlowMult,
                                vy = sin(a) * 310f * bulletSlowMult,
                                radius = 8.5f, damage = 1f, fromPlayer = false, colorHex = 0xFFE8DCC4
                            )
                        )
                        enemy = enemy.copy(attackTimer = 2.1f / aggroMult)
                    }
                }

                EnemyType.WEEPING_TOY -> {
                    val watched = RoomMechanicSystem.isWeepingToyWatched(
                        enemy.x, enemy.y, s.playerX, s.playerY, s.facingAngle
                    ) || LightingSystem.isPointIlluminated(s, enemy.x, enemy.y)
                    if (watched) {
                        enemy = enemy.copy(disguised = true, vx = 0f, vy = 0f)
                    } else {
                        enemy = enemy.copy(
                            disguised = false,
                            x = enemy.x + dirX * moveSpeed * 1.18f * dt,
                            y = enemy.y + dirY * moveSpeed * 1.18f * dt
                        )
                    }
                }

                EnemyType.TIN_CAPTAIN -> {
                    val moveFactor = if (dist > 250f) 0.9f else if (dist < 150f) -0.5f else 0.1f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * moveFactor * dt,
                        y = enemy.y + dirY * moveSpeed * moveFactor * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val baseAng = computeAimAngle(enemy.x, enemy.y, s, 325f * bulletSlowMult)
                        for (i in -1..1) {
                            val a = baseAng + i * 0.16f
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 325f * bulletSlowMult,
                                    vy = sin(a) * 325f * bulletSlowMult,
                                    radius = 9.5f, damage = 1f, fromPlayer = false, colorHex = 0xFFE5A93B
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 1.95f / aggroMult)
                    }
                }

                EnemyType.BODYGUARD_BEAR -> {
                    val spdMult = if (enemy.enraged) 1.45f else 1.0f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * spdMult * dt,
                        y = enemy.y + dirY * moveSpeed * spdMult * dt
                    )
                }

                EnemyType.CLOCKWORK_COURIER -> {
                    val litFlee = LightingSystem.isPointLitByPlayer(s, enemy.x, enemy.y)
                    val awayX = if (s.alarmBellPos == null || litFlee) -dirX else 0f
                    val awayY = if (s.alarmBellPos == null || litFlee) -dirY else 0f
                    enemy = enemy.copy(
                        x = enemy.x + awayX * moveSpeed * (if (litFlee) 1.2f else 1f) * dt,
                        y = enemy.y + awayY * moveSpeed * (if (litFlee) 1.2f else 1f) * dt
                    )
                }

                EnemyType.STITCH_MECHANIC -> {
                    val factor = if (dist > 240f) 0.7f else -0.6f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * factor * dt,
                        y = enemy.y + dirY * moveSpeed * factor * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f && s.enemies.size == 1) {
                        val a = computeAimAngle(enemy.x, enemy.y, s, 310f * bulletSlowMult)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(a) * 310f * bulletSlowMult,
                                vy = sin(a) * 310f * bulletSlowMult,
                                radius = 7.5f, damage = 1f, fromPlayer = false, colorHex = 0xFF597354
                            )
                        )
                        enemy = enemy.copy(attackTimer = 2.0f)
                    }
                }

                EnemyType.VEILED_SPECTER -> {
                    val hoverX = -dirY * sin(enemy.stateTimer * 2.5f) * 0.45f
                    val hoverY = dirX * sin(enemy.stateTimer * 2.5f) * 0.45f
                    enemy = enemy.copy(
                        x = enemy.x + (dirX * 0.75f + hoverX) * moveSpeed * dt,
                        y = enemy.y + (dirY * 0.75f + hoverY) * moveSpeed * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val a = computeAimAngle(enemy.x, enemy.y, s, 295f * bulletSlowMult)
                        newEnemyProjs.add(
                            ActiveProjectile(
                                id = allocId(), x = enemy.x, y = enemy.y,
                                vx = cos(a) * 295f * bulletSlowMult,
                                vy = sin(a) * 295f * bulletSlowMult,
                                radius = 10f, damage = 1f, fromPlayer = false, colorHex = 0xFF9E64B5
                            )
                        )
                        enemy = enemy.copy(attackTimer = 2.1f / aggroMult)
                    }
                }

                EnemyType.LAMP_SNUFFER -> {
                    val anyLitLamp = s.roomLamps.any { it.lit }
                    if (!anyLitLamp) {
                        enemy = enemy.copy(
                            x = enemy.x + dirX * moveSpeed * dt,
                            y = enemy.y + dirY * moveSpeed * dt
                        )
                    }
                }

                EnemyType.CLOCKWORK_SENTINEL -> {
                    val moveFactor = if (dist > 220f) 0.9f else if (dist < 130f) -0.4f else 0.15f
                    enemy = enemy.copy(
                        x = enemy.x + dirX * moveSpeed * moveFactor * dt,
                        y = enemy.y + dirY * moveSpeed * moveFactor * dt,
                        attackTimer = enemy.attackTimer - dt
                    )
                    if (enemy.attackTimer <= 0f) {
                        val baseAng = computeAimAngle(enemy.x, enemy.y, s, 290f * bulletSlowMult)
                        for (i in -1..1) {
                            val a = baseAng + i * 0.22f
                            newEnemyProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = enemy.x, y = enemy.y,
                                    vx = cos(a) * 290f * bulletSlowMult,
                                    vy = sin(a) * 290f * bulletSlowMult,
                                    radius = 11f, damage = 1f, fromPlayer = false, colorHex = 0xFFC89948
                                )
                            )
                        }
                        enemy = enemy.copy(attackTimer = 2.0f / aggroMult)
                    }
                }

                // BOSSES
                else -> {
                    enemy = updateBossBehavior(enemy, dirX, dirY, dist, moveSpeed, bulletSlowMult, dt, newEnemyProjs, spawnedMinions, s)
                }
            }

            // Clamp enemy inside arena & obstacles (except crawling/shadow/boss enemies)
            var cx = enemy.x.coerceIn(WALL_THICKNESS + enemy.radius, ARENA_W - WALL_THICKNESS - enemy.radius)
            var cy = enemy.y.coerceIn(WALL_THICKNESS + enemy.radius, ARENA_H - WALL_THICKNESS - enemy.radius)
            if (!enemy.type.isBoss &&
                enemy.type != EnemyType.CLOCKWORK_SPIDER &&
                enemy.type != EnemyType.SHADOW_PUPPET &&
                s.currentRoom.obstacles.isNotEmpty()
            ) {
                val (ox, oy) = RoomMechanicSystem.clampToObstacles(cx, cy, enemy.radius, s.currentRoom.obstacles)
                cx = ox
                cy = oy
            }
            enemy = enemy.copy(x = cx, y = cy)

            // Contact damage check (Weeping Toy doesn't deal contact damage while frozen under gaze)
            val canDealContact = !(enemy.type == EnemyType.WEEPING_TOY && enemy.disguised) &&
                (!enemy.disguised || enemy.type == EnemyType.BETRAYED_BUNNY)
            if (canDealContact && hypot(enemy.x - s.playerX, enemy.y - s.playerY) < enemy.radius + 20f) {
                s = damagePlayer(s)
            }

            nextEnemies.add(enemy)
        }

        return s.copy(
            enemies = nextEnemies + spawnedMinions,
            projectiles = s.projectiles + newEnemyProjs
        )
    }

    private fun updateBossBehavior(
        boss: ActiveEnemy,
        dirX: Float,
        dirY: Float,
        dist: Float,
        moveSpeed: Float,
        bulletSlowMult: Float,
        dt: Float,
        outProjs: MutableList<ActiveProjectile>,
        outMinions: MutableList<ActiveEnemy>,
        state: GameRunState
    ): ActiveEnemy {
        val strafe = if (boss.type == EnemyType.BOSS_TITIRITERO || boss.type == EnemyType.BOSS_BOTONERO) {
            cos(boss.stateTimer * 1.8f) * 0.45f
        } else 0f
        var b = boss.copy(
            x = boss.x + (dirX * 0.85f - dirY * strafe) * moveSpeed * dt,
            y = boss.y + (dirY * 0.85f + dirX * strafe) * moveSpeed * dt,
            attackTimer = boss.attackTimer - dt
        )
        if (b.attackTimer <= 0f) {
            val phase = (b.phase + 1) % 3
            val enraged = b.bossCurrentPhase >= 3 || b.hp < b.maxHp * 0.4f
            val pMult = when (b.bossCurrentPhase) {
                3 -> 2
                2 -> 1
                else -> 0
            }
            val baseAngle = computeAimAngle(b.x, b.y, state, 310f * bulletSlowMult)
            when (b.type) {
                EnemyType.BOSS_BOTONERO -> when (phase) {
                    0 -> {
                        val dirs = (if (enraged) 8 else 4) + pMult * 2
                        for (i in 0 until dirs) {
                            val a = (i / dirs.toFloat()) * (2f * PI.toFloat()) + (if (enraged) b.stateTimer * 0.3f else 0f)
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 310f * bulletSlowMult, sin(a) * 310f * bulletSlowMult, 11f, 1f, false, 0xFFE5A93B))
                        }
                    }
                    1 -> {
                        for (i in -(2 + pMult / 2)..(2 + pMult / 2)) {
                            val a = baseAngle + i * 0.18f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 335f * bulletSlowMult, sin(a) * 335f * bulletSlowMult, 9.5f, 1f, false, 0xFFD13B42))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        outMinions.add(ActiveEnemy(id = allocId(), type = EnemyType.WINDUP_CHOMPER, x = b.x, y = (b.y + 55f).coerceAtMost(ARENA_H - 120f), hp = 18f, maxHp = 18f, radius = 19f))
                    }
                }
                EnemyType.BOSS_TITIRITERO -> when (phase) {
                    0 -> {
                        val dirs = 4 + pMult * 2
                        for (i in 0 until dirs) {
                            val a = (PI.toFloat() / 4f) + i * ((2f * PI.toFloat()) / dirs)
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 320f * bulletSlowMult, sin(a) * 320f * bulletSlowMult, 10f, 1f, false, 0xFFD9E0E6))
                        }
                    }
                    1 -> {
                        for (i in -1 - (pMult / 2)..1 + (pMult / 2)) {
                            val a = baseAngle + i * 0.22f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 350f * bulletSlowMult, sin(a) * 350f * bulletSlowMult, 9f, 1f, false, 0xFF9E64B5))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        outMinions.add(ActiveEnemy(id = allocId(), type = EnemyType.NEEDLE_MARIONETTE, x = (b.x + 50f).coerceAtMost(ARENA_W - 120f), y = b.y, hp = 24f, maxHp = 24f, radius = 19f))
                    }
                }
                EnemyType.BOSS_RELOJERO -> when (phase) {
                    0 -> {
                        val count = (if (enraged) 12 else 9) + pMult
                        for (i in 0 until count) {
                            val a = (i / count.toFloat()) * (2f * PI.toFloat()) + b.stateTimer * 1.4f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 265f * bulletSlowMult, sin(a) * 265f * bulletSlowMult, 10f, 1f, false, 0xFFC89948))
                        }
                    }
                    1 -> {
                        for (i in -2..2) {
                            val a = baseAngle + i * 0.16f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 340f * bulletSlowMult, sin(a) * 340f * bulletSlowMult, 9.5f, 1f, false, 0xFF7D8C96))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        outMinions.add(ActiveEnemy(id = allocId(), type = EnemyType.CLOCKWORK_SPIDER, x = b.x, y = (b.y + 60f).coerceAtMost(ARENA_H - 120f), hp = 18f, maxHp = 18f, radius = 19f))
                    }
                }
                EnemyType.BOSS_BUHONERO -> when (phase) {
                    0 -> {
                        val count = (if (enraged) 6 else 4) + pMult
                        for (i in 0 until count) {
                            val a = baseAngle + (i - count / 2f) * 0.25f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 300f * bulletSlowMult, sin(a) * 300f * bulletSlowMult, 11.5f, 1f, false, 0xFFD13B42))
                        }
                    }
                    1 -> {
                        val count = (if (enraged) 10 else 8) + pMult
                        for (i in 0 until count) {
                            val a = (i / count.toFloat()) * (2f * PI.toFloat()) + b.stateTimer
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 260f * bulletSlowMult, sin(a) * 260f * bulletSlowMult, 10f, 1f, false, 0xFF9E64B5))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        val minion = if (Random.nextBoolean()) EnemyType.SHADOW_PUPPET else EnemyType.PORCELAIN_CLOWN
                        outMinions.add(ActiveEnemy(id = allocId(), type = minion, x = (b.x - 40f).coerceAtLeast(120f), y = b.y, hp = minion.maxHp * 0.65f, maxHp = minion.maxHp * 0.65f, radius = 19f))
                    }
                }
                EnemyType.BOSS_SUBSUELO -> when (phase) {
                    0 -> {
                        val count = 8 + pMult * 2
                        for (i in 0 until count) {
                            val a = (i / count.toFloat()) * (2f * PI.toFloat()) + b.stateTimer * 0.7f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 275f * bulletSlowMult, sin(a) * 275f * bulletSlowMult, 11f, 1f, false, 0xFF4C8EA8))
                        }
                    }
                    1 -> {
                        for (i in -2..2) {
                            val a = baseAngle + i * 0.20f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 340f * bulletSlowMult, sin(a) * 340f * bulletSlowMult, 10f, 1f, false, 0xFFE5A93B))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        val mType = if (b.bossCurrentPhase >= 2) EnemyType.LAMP_SNUFFER else EnemyType.CLOCKWORK_SPIDER
                        outMinions.add(ActiveEnemy(id = allocId(), type = mType, x = b.x, y = (b.y + 55f).coerceAtMost(ARENA_H - 120f), hp = mType.maxHp * 0.65f, maxHp = mType.maxHp * 0.65f, radius = 19f))
                    }
                }
                EnemyType.BOSS_FABRICANTE -> when (phase) {
                    0 -> {
                        val count = 10 + pMult * 2
                        for (i in 0 until count) {
                            val a = (i / count.toFloat()) * (2f * PI.toFloat()) + b.stateTimer * 1.1f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 290f * bulletSlowMult, sin(a) * 290f * bulletSlowMult, 10.5f, 1f, false, 0xFFC89948))
                        }
                    }
                    1 -> {
                        for (i in -(2 + pMult / 2)..(2 + pMult / 2)) {
                            val a = baseAngle + i * 0.16f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 355f * bulletSlowMult, sin(a) * 355f * bulletSlowMult, 10f, 1f, false, 0xFFD13B42))
                        }
                    }
                    else -> if (state.enemies.size < 3) {
                        val mType = if (b.bossCurrentPhase >= 2) EnemyType.VEILED_SPECTER else EnemyType.CLOCKWORK_SENTINEL
                        outMinions.add(ActiveEnemy(id = allocId(), type = mType, x = (b.x - 45f).coerceAtLeast(120f), y = b.y, hp = mType.maxHp * 0.55f, maxHp = mType.maxHp * 0.55f, radius = 21f))
                    }
                }
                else -> when (phase) {
                    0 -> {
                        // Fan volley
                        val spreadCount = (if (enraged) 5 else 3) + (if (b.bossCurrentPhase >= 2) 2 else 0)
                        for (i in -(spreadCount / 2)..(spreadCount / 2)) {
                            val a = baseAngle + i * 0.24f
                            outProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = b.x, y = b.y,
                                    vx = cos(a) * 320f * bulletSlowMult,
                                    vy = sin(a) * 320f * bulletSlowMult,
                                    radius = 11f, damage = 1f, fromPlayer = false, colorHex = 0xFFD13B42
                                )
                            )
                        }
                    }
                    1 -> {
                        // Radial ring
                        val count = (if (enraged) 10 else 8) + pMult * 2
                        for (i in 0 until count) {
                            val a = (i / count.toFloat()) * (2f * PI.toFloat()) + b.stateTimer
                            outProjs.add(
                                ActiveProjectile(
                                    id = allocId(), x = b.x, y = b.y,
                                    vx = cos(a) * 250f * bulletSlowMult,
                                    vy = sin(a) * 250f * bulletSlowMult,
                                    radius = 10f, damage = 1f, fromPlayer = false, colorHex = 0xFFC89948
                                )
                            )
                        }
                    }
                    else -> {
                        // Summon helper toy if fewer than 3 enemies
                        if (state.enemies.size < 3) {
                            val minionType = when (b.type) {
                                EnemyType.BOSS_COSTURERA -> EnemyType.CLOCKWORK_SPIDER
                                EnemyType.BOSS_NINERA -> EnemyType.CREEPY_DOLL
                                EnemyType.BOSS_MAESTRO -> EnemyType.PORCELAIN_CLOWN
                                EnemyType.BOSS_COLECCIONISTA -> EnemyType.RUSTY_TIN_KNIGHT
                                EnemyType.BOSS_MADRE -> EnemyType.SHADOW_PUPPET
                                else -> EnemyType.TIN_SOLDIER
                            }
                            outMinions.add(
                                ActiveEnemy(
                                    id = allocId(),
                                    type = minionType,
                                    x = (b.x + Random.nextFloat() * 100f - 50f).coerceIn(120f, ARENA_W - 120f),
                                    y = (b.y + 60f).coerceIn(120f, ARENA_H - 120f),
                                    hp = minionType.maxHp * 0.7f,
                                    maxHp = minionType.maxHp * 0.7f,
                                    radius = 19f
                                )
                            )
                        }
                    }
                }
            }
            val nextCd = when (b.bossCurrentPhase) {
                3 -> 1.08f
                2 -> 1.30f
                else -> if (enraged) 1.2f else 1.55f
            }
            b = b.copy(phase = phase, attackTimer = nextCd)
        }
        return b
    }

    private fun computeAimAngle(ex: Float, ey: Float, state: GameRunState, projSpeed: Float): Float {
        var tx = state.playerX
        var ty = state.playerY
        if (state.toyboxMemory.enemiesAdaptDodge) {
            val t = hypot(tx - ex, ty - ey) / projSpeed.coerceAtLeast(100f)
            tx += state.playerVx * t * 0.45f
            ty += state.playerVy * t * 0.45f
        }
        return atan2(ty - ey, tx - ex)
    }

    private fun attackStyleToDamageElement(style: AttackStyle, damage: Float): DamageElement = when (style) {
        AttackStyle.SCISSOR_SLASH, AttackStyle.RAPID_NEEDLES -> DamageElement.SHARP
        AttackStyle.EXPLOSIVE_TOY -> DamageElement.EXPLOSIVE
        AttackStyle.DOLL_BOLT -> DamageElement.ELECTRIC
        AttackStyle.GEAR_PROJECTILE, AttackStyle.BUTTON_SHOT ->
            if (damage >= 18f) DamageElement.BLUNT_HEAVY else DamageElement.PHYSICAL_LIGHT
    }

    private fun updateProjectiles(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val remaining = mutableListOf<ActiveProjectile>()
        val emotionMods = EmotionSystem.computeModifiers(s.emotionState)

        for (p0 in s.projectiles) {
            var p = p0.copy(lifeSec = p0.lifeSec - dt)
            if (p.lifeSec <= 0f) continue

            // Homing curve
            if (p.fromPlayer && p.homing && s.enemies.isNotEmpty()) {
                val target = s.enemies.minByOrNull { hypot(it.x - p.x, it.y - p.y) }
                if (target != null) {
                    val speed = hypot(p.vx, p.vy)
                    val desired = atan2(target.y - p.y, target.x - p.x)
                    val cur = atan2(p.vy, p.vx)
                    var diff = desired - cur
                    while (diff > PI) diff -= (2 * PI).toFloat()
                    while (diff < -PI) diff += (2 * PI).toFloat()
                    val newAng = cur + diff.coerceIn(-4.5f * dt, 4.5f * dt)
                    p = p.copy(vx = cos(newAng) * speed, vy = sin(newAng) * speed)
                }
            }

            var nx = p.x + p.vx * dt
            var ny = p.y + p.vy * dt
            var vx = p.vx
            var vy = p.vy
            var bouncesLeft = p.bouncesLeft
            var hitWall = false

            if (nx < WALL_THICKNESS + p.radius || nx > ARENA_W - WALL_THICKNESS - p.radius) {
                if (bouncesLeft > 0) {
                    vx = -vx
                    bouncesLeft--
                    nx = nx.coerceIn(WALL_THICKNESS + p.radius, ARENA_W - WALL_THICKNESS - p.radius)
                } else hitWall = true
            }
            if (ny < WALL_THICKNESS + p.radius || ny > ARENA_H - WALL_THICKNESS - p.radius) {
                if (bouncesLeft > 0) {
                    vy = -vy
                    bouncesLeft--
                    ny = ny.coerceIn(WALL_THICKNESS + p.radius, ARENA_H - WALL_THICKNESS - p.radius)
                } else hitWall = true
            }

            // Obstacle hit
            if (!hitWall) {
                for (obs in s.currentRoom.obstacles) {
                    val ox = obs.x * ARENA_W
                    val oy = obs.y * ARENA_H
                    val ow = obs.w * ARENA_W
                    val oh = obs.h * ARENA_H
                    if (nx + p.radius > ox && nx - p.radius < ox + ow &&
                        ny + p.radius > oy && ny - p.radius < oy + oh
                    ) {
                        hitWall = true
                        break
                    }
                }
            }
            if (hitWall) {
                if (p.explosive) {
                    s = triggerMiniExplosion(s, nx, ny, p.damage * 0.9f)
                }
                continue
            }

            p = p.copy(x = nx, y = ny, vx = vx, vy = vy, bouncesLeft = bouncesLeft)

            val crateBoostProj = if (p.fromPlayer && emotionMods.breakCratesOnShot) p.copy(damage = p.damage * 2.2f) else p
            val (hitInteractable, nextInteractableState) = InteractablesSystem.applyProjectileToInteractables(s, crateBoostProj, ::allocId, sfx, onDiscovery)
            s = nextInteractableState
            if (hitInteractable) {
                if (p.explosive) s = triggerMiniExplosion(s, p.x, p.y, p.damage * 0.7f)
                continue
            }

            val (hitLoreProp, nextLoreState) = PuzzleInteractionSystem.applyProjectileToLoreProps(s, p, ::allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
            s = nextLoreState
            if (hitLoreProp) {
                continue
            }

            if (p.fromPlayer) {
                var consumed = false
                val shortRangeBonus = if (s.activeDefects.any { it.effects.shortRangeBonusDamage }) 1.22f else 1f
                var blockedBanner: RunUiBanner? = null
                val updatedEnemies = s.enemies.map { e ->
                    if (!consumed && e.id !in p.hitEnemyIds &&
                        hypot(e.x - p.x, e.y - p.y) <= e.radius + p.radius
                    ) {
                        if (!LightingSystem.isVeiledSpecterVulnerable(s, e)) {
                            sfx.play(ToySfx.DODGE)
                            consumed = true
                            if (s.activeBanner == null) {
                                blockedBanner = RunUiBanner(
                                    "Espectro Inmune a la Luz",
                                    "Apaga tu luz o aléjalo de la lámpara para herirlo.",
                                    0xFF9E64B5,
                                    2.0f
                                )
                            }
                            e
                        } else {
                            sfx.play(ToySfx.HIT_ENEMY)
                            val baseDmg = p.damage * shortRangeBonus
                            val elem = attackStyleToDamageElement(p.style, baseDmg)
                            var finalDmg = EnemyRelationshipSystem.computeElementalDamage(baseDmg, elem, e.materialType)
                            if (emotionMods.bleedOnHit) finalDmg += 2.5f

                            val targetAfterArmor = if (e.type.isBoss) {
                                val (bd, be, armorBlocked) = BossMechanicSystem.computeIncomingDamage(s, e, finalDmg, elem)
                                finalDmg = bd
                                if (armorBlocked && s.activeBanner == null) {
                                    blockedBanner = RunUiBanner("¡Armadura Activa!", e.type.bossMechanicKind().hint, 0xFFE5A93B, 2.2f)
                                }
                                be
                            } else e

                            if (p.explosive) {
                                s = triggerMiniExplosion(s, p.x, p.y, baseDmg * 0.7f)
                            }
                            if (p.pierceLeft > 0) {
                                p = p.copy(pierceLeft = p.pierceLeft - 1, hitEnemyIds = p.hitEnemyIds + e.id)
                            } else {
                                consumed = true
                            }
                            targetAfterArmor.copy(
                                hp = targetAfterArmor.hp - finalDmg,
                                disguised = false,
                                hitFlashTimer = 0.12f,
                                alertState = EnemyAlertState.COMBAT,
                                isSleeping = false
                            )
                        }
                    } else e
                }
                s = s.copy(
                    enemies = updatedEnemies,
                    activeBanner = blockedBanner ?: s.activeBanner,
                    particles = if (consumed) s.particles + spawnHitParticles(p.x, p.y, p.colorHex) else s.particles
                )
                if (!consumed) remaining.add(p)
            } else {
                // Enemy bullet vs player
                if (hypot(s.playerX - p.x, s.playerY - p.y) <= 18f + p.radius) {
                    s = damagePlayer(s)
                } else {
                    remaining.add(p)
                }
            }
        }
        return s.copy(projectiles = remaining)
    }

    private fun triggerMiniExplosion(state: GameRunState, x: Float, y: Float, dmg: Float): GameRunState {
        sfx.play(ToySfx.EXPLOSION)
        val enemies = state.enemies.map { e ->
            if (hypot(e.x - x, e.y - y) <= 85f) {
                e.copy(hp = e.hp - dmg, hitFlashTimer = 0.15f)
            } else e
        }
        val explodedState = state.copy(
            enemies = enemies,
            screenShake = maxOf(state.screenShake, 7f),
            particles = state.particles + spawnRingParticles(x, y, 48f, 0xFFD13B42)
        )
        return CompanionSystem.onExplosionNearCompanions(explodedState, x, y)
    }

    private fun damagePlayer(state: GameRunState): GameRunState {
        if (state.invulnTimer > 0f || state.dodgeTimer > 0f) return state
        sfx.play(ToySfx.PLAYER_HURT)

        val dmgHearts = state.difficulty.damageTakenHearts
        val newHearts = (state.hearts - dmgHearts).coerceAtLeast(0)

        // Forgotten Bear blocks a single fatal blow once!
        val (savedByBear, bearState) = CompanionSystem.tryBearFatalIntercept(state, newHearts)
        if (savedByBear) {
            return bearState.copy(
                damagedInCurrentRoom = true,
                damageTakenInCurrentBoss = state.damageTakenInCurrentBoss || (state.currentRoom.type == RoomType.BOSS)
            )
        }

        // Check armorChance from equipped parts + passive items
        val armorChance = (state.equippedParts.values
            .filter { !it.isDetached && it.wearState != WearState.CRITICAL }
            .sumOf { it.def.effects.armorChance.toDouble() }.toFloat() +
            state.passiveItems.sumOf { it.effects.armorWearReduction.toDouble() }.toFloat())
            .coerceIn(0f, 0.85f)

        var updatedParts = state.equippedParts
        var newPickups = state.pickups
        var banner = state.activeBanner

        if (Random.nextFloat() >= armorChance) {
            val candidates = updatedParts.values.filter { !it.isDetached }
            if (candidates.isNotEmpty()) {
                val target = candidates.random()
                val reinforced = (state.partReinforcedRooms[target.def.zone] ?: 0) > 0
                val wearMult = state.activeDefects.fold(1f) { acc, d -> acc * d.effects.wearRateMult } * (if (reinforced) 0.45f else 1f)
                val wearDamage = (Random.nextInt(16, 28) * wearMult).toInt()
                if (!reinforced && target.wearState == WearState.CRITICAL && Random.nextFloat() < 0.65f) {
                    // DETACH PART onto floor!
                    val detached = target.copy(isDetached = true, durability = 10)
                    val mut = updatedParts.toMutableMap()
                    mut[target.def.zone] = detached
                    updatedParts = mut
                    val (dropX, dropY) = safePickupPos(
                        (state.playerX + Random.nextFloat() * 90f - 45f),
                        (state.playerY + Random.nextFloat() * 90f - 45f),
                        state.currentRoom.obstacles
                    )
                    newPickups = newPickups + GroundPickup(
                        id = allocId(),
                        kind = PickupKind.DETACHED_PART,
                        x = dropX,
                        y = dropY,
                        valueStr = target.def.name,
                        expireSec = 13f,
                        detachedPart = detached
                    )
                    sfx.play(ToySfx.PART_DETACH)
                    banner = RunUiBanner(
                        "¡${target.def.name} desprendida!",
                        "Recógela del suelo antes de que desaparezca.",
                        0xFFD13B42,
                        2.8f
                    )
                } else {
                    val mut = updatedParts.toMutableMap()
                    mut[target.def.zone] = target.copy(
                        durability = (target.durability - wearDamage).coerceAtLeast(5)
                    )
                    updatedParts = mut
                }
            }
        }

        // Decoy spawn item
        val decoys = if (state.passiveItems.any { it.effects.spawnDecoyOnHit }) {
            state.decoys + DecoyPuppet(state.playerX, state.playerY, 4.5f)
        } else state.decoys

        val isDead = newHearts <= 0
        var damagedState = state.copy(
            hearts = newHearts,
            invulnTimer = 1.05f,
            screenShake = 14f,
            equippedParts = updatedParts,
            pickups = newPickups,
            decoys = decoys,
            activeBanner = banner,
            damagedInCurrentRoom = true,
            damageTakenInCurrentBoss = state.damageTakenInCurrentBoss || (state.currentRoom.type == RoomType.BOSS),
            isGameOver = isDead,
            isActive = !isDead,
            particles = state.particles + spawnHitParticles(state.playerX, state.playerY, 0xFFF2EDE4)
        )
        damagedState = ContractSystem.onPlayerDamaged(damagedState)
        if (!isDead) {
            val prevBanner = damagedState.activeBanner
            damagedState = EmotionSystem.triggerEmotion(
                damagedState,
                if (newHearts <= 1) PatchEmotion.FEAR else PatchEmotion.ANGER,
                24f,
                20f
            )
            if (prevBanner != null && prevBanner != state.activeBanner) {
                damagedState = damagedState.copy(activeBanner = prevBanner)
            }
        }
        return damagedState
    }

    private fun handleEnemyKilled(state: GameRunState, enemy: ActiveEnemy): GameRunState {
        onDiscovery("enemy", enemy.type.name)
        var s = state.copy(
            enemiesDefeatedCount = state.enemiesDefeatedCount + 1,
            particles = state.particles + spawnRingParticles(enemy.x, enemy.y, enemy.radius * 1.4f, 0xFFE8DCC4)
        )
        s = ContractSystem.onEnemyKilled(s, enemy)
        s = CompanionSystem.grantCompanionXp(s, if (enemy.type.isBoss) 4 else 1)

        if (s.activeSynergies.any { it.effectId == "kill_repair" }) {
            s = repairAllParts(s, 4)
        }
        if (enemy.type == EnemyType.RAG_BOMBER) {
            s = s.copy(
                bombs = s.bombs + HazardBomb(allocId(), enemy.x, enemy.y, fuseSec = 0.65f, fromPlayer = false, radius = 110f, damage = 22f)
            )
        }
        if (enemy.type == EnemyType.BETRAYED_BUNNY) {
            onClearCompanionGrudge(CompanionId.BUNNY.name)
            s = s.copy(
                activeBanner = RunUiBanner("Conejito Traicionado Derrotado", "La deuda de tu abandono quedó saldada.", 0xFFC89948, 3.0f)
            )
        }

        val btnMult = if (s.passiveItems.any { it.effects.bonusDropsAndAggro }) 2 else 1
        val pickups = s.pickups.toMutableList()
        if (enemy.type.isBoss) {
            onDiscovery("boss", enemy.type.name)
            s = s.copy(bossesDefeatedCount = s.bossesDefeatedCount + 1)
            if (!s.damageTakenInCurrentBoss) onAchievement("ach_flawless_boss")
            if (!s.usedConsumableInBoss) onAchievement("ach_boss_no_consumable")
            if (s.activeDefects.size >= 3) onAchievement("ach_three_defects")
            if (s.floorNumber == 1) onAchievement("ach_first_floor")

            repeat(5 * btnMult) {
                val (bx, by) = safePickupPos(
                    enemy.x + Random.nextFloat() * 70f - 35f,
                    enemy.y + Random.nextFloat() * 70f - 35f,
                    s.currentRoom.obstacles
                )
                pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, bx, by))
            }
            val newPart = GameCatalog.bodyParts.filter { it.rarity.ordinal >= 1 }.random()
            val (px1, py1) = safePickupPos(ARENA_W * 0.36f, ARENA_H * 0.45f, s.currentRoom.obstacles)
            pickups.add(GroundPickup(allocId(), PickupKind.NEW_PART_PEDESTAL, px1, py1, valueStr = newPart.id))
            val newItem = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
            if (newItem != null) {
                val (px2, py2) = safePickupPos(ARENA_W * 0.64f, ARENA_H * 0.45f, s.currentRoom.obstacles)
                pickups.add(GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, px2, py2, valueStr = newItem.id))
            }
            s = s.copy(exitTrapdoorOpen = true)
        } else {
            val (dx, dy) = safePickupPos(enemy.x, enemy.y, s.currentRoom.obstacles)
            val dropRoll = Random.nextFloat()
            if (enemy.type == EnemyType.BETRAYED_BUNNY) {
                repeat(3) { pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, dx + it * 14f - 14f, dy)) }
            } else if (dropRoll < 0.65f * s.difficulty.resourceMult) {
                pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, dx, dy, amount = btnMult))
            } else if (dropRoll < 0.78f * s.difficulty.resourceMult) {
                pickups.add(GroundPickup(allocId(), PickupKind.HEART, dx, dy))
            } else if (dropRoll < 0.88f * s.difficulty.resourceMult) {
                val cons = ConsumableType.entries.random()
                pickups.add(GroundPickup(allocId(), PickupKind.CONSUMABLE, dx, dy, valueStr = cons.name))
            }
        }
        s = EnemyRelationshipSystem.onEnemyKilledRelationships(s, enemy)
        var npc = s.manufacturerNpc
        if (enemy.type.isBoss && Random.nextFloat() < 0.42f) {
            npc = ManufacturerNpcState(x = ARENA_W * 0.78f, y = ARENA_H * 0.28f, present = true)
        }
        return s.copy(pickups = pickups, manufacturerNpc = npc)
    }

    private fun updatePickupsAndFx(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val remainingPickups = mutableListOf<GroundPickup>()

        for (p in s.pickups) {
            val remExp = p.expireSec?.let { it - dt }
            if (remExp != null && remExp <= 0f) continue
            var updated = p.copy(expireSec = remExp)

            // Smooth magnetic pull so drops near sewing table / obstacle corners are effortlessly collectible
            val canMagnet = updated.kind in setOf(
                PickupKind.BUTTON,
                PickupKind.GOLD_BUTTON,
                PickupKind.MATERIAL,
                PickupKind.GEAR_TOKEN,
                PickupKind.CONSUMABLE
            ) || (updated.kind == PickupKind.BACKPACK_ITEM && s.backpackTier != null && BackpackSystem.remainingSlots(s) > 0)
            val distToPlayer = hypot(updated.x - s.playerX, updated.y - s.playerY)
            if (canMagnet && distToPlayer in 1f..75f) {
                val ux = (s.playerX - updated.x) / distToPlayer
                val uy = (s.playerY - updated.y) / distToPlayer
                val pull = 280f * dt
                updated = updated.copy(x = updated.x + ux * pull, y = updated.y + uy * pull)
            }

            val pickupRadius = when (updated.kind) {
                PickupKind.NEW_PART_PEDESTAL, PickupKind.PASSIVE_ITEM_PEDESTAL, PickupKind.COMPANION_SHRINE, PickupKind.BACKPACK_TIER -> 48f
                else -> 42f
            }
            if (hypot(updated.x - s.playerX, updated.y - s.playerY) <= pickupRadius) {
                when (updated.kind) {
                    PickupKind.BUTTON -> {
                        sfx.play(ToySfx.PICKUP_BUTTON)
                        val newBtns = s.buttons + updated.amount
                        if (newBtns >= 25) onAchievement("ach_rich_buttons")
                        s = s.copy(buttons = newBtns)
                    }
                    PickupKind.GOLD_BUTTON -> {
                        sfx.play(ToySfx.PICKUP_BUTTON)
                        s = s.copy(goldenButtons = s.goldenButtons + 1, buttons = s.buttons + 5)
                    }
                    PickupKind.HEART -> {
                        if (s.hearts < s.maxHearts) {
                            sfx.play(ToySfx.PICKUP_ITEM)
                            s = s.copy(hearts = (s.hearts + 1).coerceAtMost(s.maxHearts))
                            s = ContractSystem.onHeartPicked(s)
                        } else {
                            remainingPickups.add(updated)
                        }
                    }
                    PickupKind.CONSUMABLE -> {
                        val c = runCatching { ConsumableType.valueOf(updated.valueStr) }.getOrNull()
                        if (c != null) {
                            sfx.play(ToySfx.PICKUP_ITEM)
                            s = s.copy(
                                consumables = s.consumables + c,
                                activeBanner = RunUiBanner("Consumible", c.displayName, 0xFFC89948, 1.8f)
                            )
                        }
                    }
                    PickupKind.DETACHED_PART -> {
                        updated.detachedPart?.let { dp ->
                            val map = s.equippedParts.toMutableMap()
                            map[dp.def.zone] = dp.copy(isDetached = false, durability = maxOf(dp.durability, 25))
                            sfx.play(ToySfx.REPAIR_STITCH)
                            onAchievement("ach_recover_part")
                            s = s.copy(
                                equippedParts = map,
                                activeBanner = RunUiBanner("Pieza Recuperada", "${dp.def.name} cosida de nuevo.", 0xFF597354, 2.0f)
                            )
                        }
                    }
                    PickupKind.NEW_PART_PEDESTAL -> {
                        val def = GameCatalog.partById(updated.valueStr)
                        val current = s.equippedParts[def.zone]
                        val testParts = s.equippedParts.toMutableMap().apply {
                            this[def.zone] = EquippedBodyPart(def)
                        }
                        val nextSyns = GameCatalog.computeActiveSynergies(testParts, s.passiveItems)
                        s = s.copy(
                            pendingPartChoice = PendingPartChoice(def, current, nextSyns)
                        )
                    }
                    PickupKind.PASSIVE_ITEM_PEDESTAL -> {
                        val item = GameCatalog.itemById(updated.valueStr)
                        if (item != null) {
                            s = acquirePassiveItem(s, item)
                        }
                    }
                    PickupKind.COMPANION_SHRINE -> {
                        val cid = runCatching { CompanionId.valueOf(updated.valueStr) }.getOrNull()
                        if (cid != null) {
                            s = s.copy(
                                pendingCompanionDialog = CompanionSystem.buildShrineDialog(cid, s.activeCompanions)
                            )
                        }
                    }
                    PickupKind.MATERIAL -> {
                        val mat = runCatching { RepairMaterial.valueOf(updated.valueStr) }.getOrNull()
                        if (mat != null) {
                            sfx.play(ToySfx.PICKUP_ITEM)
                            onDiscovery("material", mat.name)
                            val matMap = s.materials.toMutableMap()
                            matMap[mat] = (matMap[mat] ?: 0) + updated.amount
                            s = s.copy(
                                materials = matMap,
                                activeBanner = RunUiBanner("Material de Taller", "+${updated.amount} ${mat.displayName}", 0xFF597354, 1.8f)
                            )
                            s = ContractSystem.onMaterialOrItemCollected(s, updated.amount)
                        }
                    }
                    PickupKind.GEAR_TOKEN -> {
                        sfx.play(ToySfx.PICKUP_BUTTON)
                        s = ContractSystem.onMaterialOrItemCollected(s, 1)
                        val obj = s.roomObjective
                        if (obj != null && obj.type == ObjectiveType.RECOVER_GEARS && !obj.completed) {
                            val nextCount = obj.currentCount + 1
                            val updatedObj = obj.copy(currentCount = nextCount)
                            s = if (nextCount >= updatedObj.targetCount) {
                                RoomObjectiveSystem.completeObjective(s, updatedObj, ::allocId, sfx)
                            } else {
                                s.copy(
                                    roomObjective = updatedObj,
                                    activeBanner = RunUiBanner("Engranaje Recogido", "$nextCount/${updatedObj.targetCount}", 0xFFC89948, 1.6f)
                                )
                            }
                        }
                    }
                    PickupKind.BACKPACK_ITEM -> {
                        val bKind = runCatching { BackpackItemKind.valueOf(updated.valueStr) }.getOrNull()
                        if (bKind != null) {
                            val (added, nextState) = BackpackSystem.addItem(s, bKind, allocId = ::allocId, sfx = sfx)
                            s = nextState
                            if (!added) {
                                remainingPickups.add(updated)
                            }
                        }
                    }
                    PickupKind.BACKPACK_TIER -> {
                        val tier = runCatching { BackpackTier.valueOf(updated.valueStr) }.getOrNull()
                        if (tier != null) {
                            val currentCap = s.backpackTier?.capacity ?: 0
                            if (s.backpackTier != null && currentCap >= tier.capacity) {
                                sfx.play(ToySfx.PICKUP_BUTTON)
                                s = s.copy(
                                    buttons = s.buttons + 3,
                                    activeBanner = RunUiBanner("Mochila Reciclada", "Ya llevas una mejor: +3 Botones.", 0xFFC89948, 2.0f)
                                )
                            } else {
                                s = BackpackSystem.equipTier(s, tier, ::allocId, sfx, onUnlockBackpack)
                            }
                        }
                    }
                    PickupKind.LIGHT_SOURCE -> {
                        val ls = runCatching { LightSourceType.valueOf(updated.valueStr) }.getOrNull()
                        if (ls != null) {
                            sfx.play(ToySfx.PICKUP_ITEM)
                            s = LightingSystem.grantLightSource(s, ls)
                        }
                    }
                    PickupKind.LOST_TOY_TOKEN -> {
                        sfx.play(ToySfx.PICKUP_ITEM)
                        s = LabyrinthSystem.onLostToyCollected(s)
                    }
                }
            } else {
                remainingPickups.add(updated)
            }
        }

        val fx = s.particles.mapNotNull { pt ->
            val nl = pt.life - dt
            if (nl <= 0f) null
            else pt.copy(x = pt.x + pt.vx * dt, y = pt.y + pt.vy * dt, life = nl)
        }
        val decoys = s.decoys.mapNotNull { d ->
            val nl = d.lifeSec - dt
            if (nl <= 0f) null else d.copy(lifeSec = nl)
        }

        return s.copy(pickups = remainingPickups, particles = fx, decoys = decoys)
    }

    fun resolvePartChoice(state: GameRunState, equipNew: Boolean): GameRunState {
        val choice = state.pendingPartChoice ?: return state
        if (!equipNew) {
            sfx.play(ToySfx.PICKUP_BUTTON)
            return state.copy(
                pendingPartChoice = null,
                buttons = state.buttons + 3,
                activeBanner = RunUiBanner("Pieza Desguazada", "+3 Botones obtenidos.", 0xFFC89948, 1.8f)
            )
        }
        onDiscovery("part", choice.newPart.id)
        val map = state.equippedParts.toMutableMap()
        map[choice.newPart.zone] = EquippedBodyPart(choice.newPart)
        val prevSyns = state.activeSynergies
        val newSyns = GameCatalog.computeActiveSynergies(map, state.passiveItems)
        val newlyUnlocked = newSyns.firstOrNull { it !in prevSyns }

        var maxH = state.character.maxHearts +
            map.values.sumOf { it.def.effects.maxHeartsBonus } +
            state.passiveItems.sumOf { it.effects.maxHeartsBonus }
        if (state.activeDefects.any { it.effects.fragileBody }) maxH = (maxH - 1).coerceAtLeast(2)

        if (newlyUnlocked != null) {
            onDiscovery("synergy", newlyUnlocked.id)
            onAchievement("ach_first_synergy")
            sfx.play(ToySfx.SYNERGY)
        } else {
            sfx.play(ToySfx.PICKUP_ITEM)
        }

        return state.copy(
            equippedParts = map,
            activeSynergies = newSyns,
            maxHearts = maxH,
            hearts = state.hearts.coerceAtMost(maxH),
            pendingPartChoice = null,
            activeBanner = if (newlyUnlocked != null) {
                RunUiBanner("SINERGIA: ${newlyUnlocked.name}", newlyUnlocked.description, 0xFFC89948, 3.6f)
            } else {
                RunUiBanner("Pieza Equipada", choice.newPart.name, 0xFF597354, 2.2f)
            }
        )
    }

    private fun acquirePassiveItem(state: GameRunState, item: PassiveItemDef): GameRunState {
        onDiscovery("item", item.id)
        val nextItems = state.passiveItems + item
        val prevSyns = state.activeSynergies
        val nextSyns = GameCatalog.computeActiveSynergies(state.equippedParts, nextItems)
        val newlyUnlocked = nextSyns.firstOrNull { it !in prevSyns }

        var mem = state.toyboxMemory
        if (item.memoryFragmentId != null && item.memoryFragmentId !in mem.memoriesCollected) {
            val list = mem.memoriesCollected + item.memoryFragmentId
            mem = mem.copy(memoriesCollected = list)
            onDiscovery("memory", item.memoryFragmentId)
            if (list.size >= 2) onAchievement("ach_memory_2")
        }

        val maxH = (state.maxHearts + item.effects.maxHeartsBonus).coerceAtLeast(1)
        val h = (state.hearts + item.effects.maxHeartsBonus).coerceIn(1, maxH)

        if (newlyUnlocked != null) {
            onDiscovery("synergy", newlyUnlocked.id)
            onAchievement("ach_first_synergy")
            sfx.play(ToySfx.SYNERGY)
        } else {
            sfx.play(ToySfx.PICKUP_ITEM)
        }

        return state.copy(
            passiveItems = nextItems,
            activeSynergies = nextSyns,
            maxHearts = maxH,
            hearts = h,
            toyboxMemory = mem,
            activeBanner = if (newlyUnlocked != null) {
                RunUiBanner("SINERGIA: ${newlyUnlocked.name}", newlyUnlocked.description, 0xFFC89948, 3.6f)
            } else {
                RunUiBanner("Objeto Obtenido", "${item.name}: ${item.description}", 0xFFC89948, 3.2f)
            }
        )
    }

    private fun checkRoomStateAndDoors(state: GameRunState): GameRunState {
        var s = state
        // Check room clear
        if (!s.currentRoom.cleared && s.enemies.isEmpty()) {
            val clearedRoom = s.currentRoom.copy(cleared = true)
            val rooms = s.dungeon.rooms.toMutableMap()
            rooms[s.currentCoord] = clearedRoom
            val newRoomsCount = s.roomsClearedCount + 1
            if (newRoomsCount >= 10) onAchievement("ach_10_rooms")

            // Red Thread / Immortal Stitch auto-recovery on room clear
            var parts = s.equippedParts
            if (s.passiveItems.any { it.effects.autoRecoverPartPerRoom }) {
                parts = parts.mapValues { (_, ep) -> if (ep.isDetached) ep.copy(isDetached = false, durability = 35) else ep }
            }

            // Room clear drop (safely outside any obstacles)
            val drops = s.pickups.toMutableList()
            if (!clearedRoom.rewardSpawned && clearedRoom.type != RoomType.BOSS) {
                val (bx, by) = safePickupPos(ARENA_W * 0.5f, ARENA_H * 0.5f, clearedRoom.obstacles)
                drops.add(GroundPickup(allocId(), PickupKind.BUTTON, bx, by, amount = 2))
                if (clearedRoom.type == RoomType.CHALLENGE || clearedRoom.type == RoomType.CURSE) {
                    val part = GameCatalog.bodyParts.random()
                    val (px, py) = safePickupPos(ARENA_W * 0.5f, ARENA_H * 0.36f, clearedRoom.obstacles)
                    drops.add(GroundPickup(allocId(), PickupKind.NEW_PART_PEDESTAL, px, py, valueStr = part.id))
                }
                rooms[s.currentCoord] = clearedRoom.copy(rewardSpawned = true)
            }
            // Track Curse/Challenge clears for Toybox Memory
            var mem = s.toyboxMemory
            if (clearedRoom.type == RoomType.CURSE || clearedRoom.type == RoomType.CHALLENGE) {
                val nextCurseCount = mem.curseOrChallengeCleared + 1
                mem = mem.copy(
                    curseOrChallengeCleared = nextCurseCount,
                    secretCluesRevealed = mem.secretCluesRevealed || nextCurseCount >= 2
                )
            }
            sfx.play(ToySfx.DOOR_OPEN)
            s = s.copy(
                currentRoom = rooms[s.currentCoord]!!,
                dungeon = s.dungeon.copy(rooms = rooms),
                roomsClearedCount = newRoomsCount,
                equippedParts = parts,
                pickups = drops,
                toyboxMemory = mem,
                doorMapping = Direction.entries.associateWith { it }
            )
            s = ContractSystem.onRoomCleared(s, s.damagedInCurrentRoom)
        }

        // Check Trapdoor to Next Floor or Victory!
        if (s.exitTrapdoorOpen && hypot(s.playerX - ARENA_W * 0.5f, s.playerY - ARENA_H * 0.24f) < 36f) {
            return advanceToNextFloorOrWin(s)
        }

        // Door transitions when room is cleared (honors SHIFTING_DOORS mapping!)
        if (s.currentRoom.cleared) {
            val doorMargin = WALL_THICKNESS + 28f
            val midX = ARENA_W * 0.5f
            val midY = ARENA_H * 0.5f
            val physDir = when {
                s.playerY <= doorMargin && abs(s.playerX - midX) < 70f -> Direction.NORTH
                s.playerY >= ARENA_H - doorMargin && abs(s.playerX - midX) < 70f -> Direction.SOUTH
                s.playerX <= doorMargin && abs(s.playerY - midY) < 70f -> Direction.WEST
                s.playerX >= ARENA_W - doorMargin && abs(s.playerY - midY) < 70f -> Direction.EAST
                else -> null
            }
            if (physDir != null) {
                val logicalDir = if (s.currentRoom.mechanic == RoomMechanic.SHIFTING_DOORS) {
                    s.doorMapping[physDir]
                } else physDir
                if (logicalDir != null && logicalDir in s.currentRoom.doors) {
                    return enterRoom(s, s.currentCoord.neighbor(logicalDir), fromDir = logicalDir)
                }
            }
        }
        return s
    }

    private fun oppositeDirection(dir: Direction): Direction = when (dir) {
        Direction.NORTH -> Direction.SOUTH
        Direction.SOUTH -> Direction.NORTH
        Direction.EAST -> Direction.WEST
        Direction.WEST -> Direction.EAST
    }

    private fun enterRoom(state: GameRunState, targetCoord: GridCoord, fromDir: Direction): GameRunState {
        val targetRoom = state.dungeon.rooms[targetCoord] ?: return state
        val wasVisited = targetRoom.visited
        val entrySide = oppositeDirection(fromDir)
        val updatedRoom = if (entrySide in targetRoom.secretDoors) {
            targetRoom.copy(
                visited = true,
                doors = targetRoom.doors + entrySide,
                secretDoors = targetRoom.secretDoors - entrySide
            )
        } else {
            targetRoom.copy(visited = true)
        }
        val rooms = state.dungeon.rooms.toMutableMap()
        rooms[targetCoord] = updatedRoom

        if (updatedRoom.type == RoomType.SECRET) onAchievement("ach_secret_room")

        // Spawn position opposite to entry door
        val (px, py) = when (fromDir) {
            Direction.NORTH -> ARENA_W * 0.5f to ARENA_H - WALL_THICKNESS - 58f
            Direction.SOUTH -> ARENA_W * 0.5f to WALL_THICKNESS + 58f
            Direction.WEST -> ARENA_W - WALL_THICKNESS - 58f to ARENA_H * 0.5f
            Direction.EAST -> WALL_THICKNESS + 58f to ARENA_H * 0.5f
        }

        // Persist departing room's dynamic state + Lore room before leaving so backtracking keeps rooms identical
        val leavingSnapshot = SavedRoomState(
            roomInteractables = state.roomInteractables.map {
                it.copy(vx = 0f, vy = 0f, active = false, activeTimer = 0f, cooldownTimer = 0f)
            },
            pickups = state.pickups,
            roomAnomalies = state.roomAnomalies,
            roomObjective = state.roomObjective,
            manufacturerNpc = state.manufacturerNpc,
            alarmBellPos = state.alarmBellPos,
            roomLamps = state.roomLamps,
            labyrinthState = state.labyrinthState
        )
        var s = state.copy(
            savedRoomsByCoord = state.savedRoomsByCoord + (state.currentCoord to leavingSnapshot)
        )
        if (s.loreRoom != null) {
            s = s.copy(loreRoomsByCoord = s.loreRoomsByCoord + (s.currentCoord to s.loreRoom!!))
        }
        s = BackpackSystem.degradeStoredSparesOnRoomAdvance(s)
        if (s.activeSynergies.any { it.effectId == "room_repair" }) {
            s = repairAllParts(s, 8)
        }

        // Spawn enemies if room is not cleared
        val spawnedEnemies = if (!updatedRoom.cleared) {
            val baseList = updatedRoom.initialEnemies.map { sp ->
                val hp = sp.type.maxHp * s.difficulty.enemyHpMult * (1f + (s.floorNumber - 1) * 0.16f)
                ActiveEnemy(
                    id = allocId(),
                    type = sp.type,
                    x = sp.nx * ARENA_W,
                    y = sp.ny * ARENA_H,
                    hp = hp,
                    maxHp = hp,
                    radius = if (sp.type.isBoss) 38f else 21f,
                    disguised = (sp.type == EnemyType.CREEPY_DOLL || sp.type == EnemyType.MIMIC_CHEST || sp.type == EnemyType.WEEPING_TOY),
                    attackTimer = 1.0f + Random.nextFloat() * 0.8f
                )
            }.toMutableList()
            if (ToyboxMemorySystem.shouldInjectBombScavenger(updatedRoom.type, s.toyboxMemory) &&
                baseList.none { it.type == EnemyType.THIEF_GREMLIN } && Random.nextFloat() < 0.6f
            ) {
                baseList.add(
                    ActiveEnemy(
                        id = allocId(),
                        type = EnemyType.THIEF_GREMLIN,
                        x = ARENA_W * 0.8f,
                        y = ARENA_H * 0.25f,
                        hp = EnemyType.THIEF_GREMLIN.maxHp * s.difficulty.enemyHpMult,
                        maxHp = EnemyType.THIEF_GREMLIN.maxHp * s.difficulty.enemyHpMult,
                        radius = 20f
                    )
                )
            }
            baseList
        } else emptyList()

        // Restore saved room pickups on revisit, or initialize on first visit
        val savedTarget = if (wasVisited) s.savedRoomsByCoord[targetCoord] else null
        val pickups = if (savedTarget != null) savedTarget.pickups.toMutableList() else mutableListOf()
        var activeEvent: RoomEventDef? = null
        var shopOpen = false
        var repairOpen = false

        if (!wasVisited) {
            when (updatedRoom.type) {
                RoomType.TREASURE -> {
                    val part = GameCatalog.bodyParts.filter { it.rarity.ordinal >= 1 }.random()
                    val item = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
                    val (tx1, ty1) = safePickupPos(ARENA_W * 0.36f, ARENA_H * 0.5f, updatedRoom.obstacles)
                    pickups.add(GroundPickup(allocId(), PickupKind.NEW_PART_PEDESTAL, tx1, ty1, valueStr = part.id))
                    if (item != null) {
                        val (tx2, ty2) = safePickupPos(ARENA_W * 0.64f, ARENA_H * 0.5f, updatedRoom.obstacles)
                        pickups.add(GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, tx2, ty2, valueStr = item.id))
                    }
                    if (s.backpackTier == null && Random.nextFloat() < 0.45f) {
                        val (bx, by) = safePickupPos(ARENA_W * 0.5f, ARENA_H * 0.68f, updatedRoom.obstacles)
                        pickups.add(GroundPickup(allocId(), PickupKind.BACKPACK_TIER, bx, by, valueStr = BackpackTier.STITCHED_POUCH.name))
                    }
                }
                RoomType.SECRET -> {
                    val (sx1, sy1) = safePickupPos(ARENA_W * 0.4f, ARENA_H * 0.45f, updatedRoom.obstacles)
                    pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, sx1, sy1))
                    val item = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
                    if (item != null) {
                        val (sx2, sy2) = safePickupPos(ARENA_W * 0.6f, ARENA_H * 0.5f, updatedRoom.obstacles)
                        pickups.add(GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, sx2, sy2, valueStr = item.id))
                    }
                    if (s.backpackTier == null && Random.nextFloat() < 0.50f) {
                        val (bx, by) = safePickupPos(ARENA_W * 0.5f, ARENA_H * 0.70f, updatedRoom.obstacles)
                        pickups.add(GroundPickup(allocId(), PickupKind.BACKPACK_TIER, bx, by, valueStr = BackpackTier.STITCHED_POUCH.name))
                    }
                }
                RoomType.EVENT -> {
                    activeEvent = updatedRoom.eventId?.let { eid ->
                        GameCatalog.events.find { it.id == eid }?.let { ev ->
                            ToyboxMemorySystem.contextualizeEvent(ev, s.toyboxMemory)
                        }
                    }
                }
                RoomType.SHOP -> {
                    val delta = when {
                        s.toyboxMemory.compassionScore >= 1 -> -1
                        s.toyboxMemory.compassionScore <= -1 -> 1
                        else -> 0
                    }
                    if (delta != 0) {
                        val adjustedStock = updatedRoom.shopStock.map { offer ->
                            offer.copy(price = (offer.price + delta).coerceAtLeast(2))
                        }
                        rooms[targetCoord] = updatedRoom.copy(shopStock = adjustedStock)
                    }
                    shopOpen = true
                }
                RoomType.REPAIR -> repairOpen = true
                else -> Unit
            }
        }

        if (updatedRoom.type == RoomType.BOSS && !updatedRoom.cleared) {
            sfx.play(ToySfx.BOSS_ROAR)
        } else {
            sfx.play(ToySfx.DOOR_OPEN)
        }

        val lightFlash = if (s.activeDefects.any { it.effects.lightSensitivity }) 0.55f else 0f
        val roomDecoys = if (!updatedRoom.cleared && s.passiveItems.any { it.effects.spawnDecoyOnRoomEnter }) {
            listOf(DecoyPuppet((px + 34f).coerceAtMost(ARENA_W - WALL_THICKNESS - 25f), py, 6.5f))
        } else emptyList()
        val activeBossType = spawnedEnemies.firstOrNull { it.type.isBoss }?.type ?: s.floorTheme.bossType
        val curRoomResolved = rooms[targetCoord] ?: updatedRoom

        val mechanicBanner = if (!wasVisited && curRoomResolved.mechanic != RoomMechanic.NONE && updatedRoom.type != RoomType.BOSS) {
            RunUiBanner(curRoomResolved.mechanic.displayName, curRoomResolved.mechanic.hintText, 0xFFC89948, 2.8f)
        } else null

        var nextState = s.copy(
            currentCoord = targetCoord,
            currentRoom = curRoomResolved,
            dungeon = s.dungeon.copy(rooms = rooms),
            playerX = px,
            playerY = py,
            playerVx = 0f,
            playerVy = 0f,
            enemies = spawnedEnemies,
            projectiles = emptyList(),
            pickups = pickups,
            bombs = emptyList(),
            decoys = roomDecoys,
            exitTrapdoorOpen = updatedRoom.type == RoomType.BOSS && updatedRoom.cleared,
            activeEvent = activeEvent,
            shopOpen = shopOpen,
            repairBenchOpen = repairOpen,
            pendingRepairMinigame = null,
            manufacturerEncounter = null,
            interactionPrompt = null,
            partReinforcedRooms = s.partReinforcedRooms.mapValues { (_, rem) -> (rem - 1).coerceAtLeast(0) }.filterValues { it > 0 },
            lightFlashTimer = lightFlash,
            damagedInPrevRoom = s.damagedInCurrentRoom,
            damagedInCurrentRoom = false,
            damageTakenInCurrentBoss = false,
            usedConsumableInBoss = false,
            activeBanner = when (updatedRoom.type) {
                RoomType.BOSS -> RunUiBanner("JEFE DEL PISO", ToyboxMemorySystem.bossDialogueFor(activeBossType, s.toyboxMemory), 0xFFD13B42, 3.4f)
                RoomType.TREASURE -> RunUiBanner("Sala de Tesoro", "Piezas y reliquias olvidadas", 0xFFC89948, 2.4f)
                RoomType.SECRET -> RunUiBanner(
                    updatedRoom.secretFlavor?.displayName ?: "Sala Secreta",
                    updatedRoom.secretFlavor?.subtitle ?: "Un escondite detrás de los estantes",
                    0xFFC89948,
                    2.6f
                )
                RoomType.REPAIR -> RunUiBanner("Mesa de Costura", "Hilo tibio y taller interactivo", 0xFF597354, 2.2f)
                RoomType.MANUFACTURER -> RunUiBanner("Sala del Fabricante", "Acércate a la figura enmascarada para negociar.", 0xFFC89948, 2.8f)
                RoomType.FORBIDDEN -> RunUiBanner("Sala Prohibida", "Peligro alto y recompensas oscuras", 0xFFD13B42, 2.6f)
                else -> mechanicBanner ?: s.activeBanner
            }
        )

        // Expanded Mechanics room setup (restore saved room state on revisits instead of regenerating!)
        val objective: RoomObjectiveState?
        val objPickups: List<GroundPickup>
        var finalInteractables: List<RoomInteractable>
        val anomalies: List<RoomAnomaly>
        var finalEnemies: List<ActiveEnemy>
        val bellPos: Pair<Float, Float>?
        val npcState: ManufacturerNpcState?
        val roomLamps: List<RoomLampProp>
        var bossArenaProps: List<BossArenaProp> = emptyList()
        var cageBanner = nextState.activeBanner

        if (savedTarget != null) {
            objective = savedTarget.roomObjective
            objPickups = emptyList()
            finalInteractables = savedTarget.roomInteractables
            anomalies = savedTarget.roomAnomalies
            finalEnemies = nextState.enemies
            bellPos = savedTarget.alarmBellPos
            npcState = savedTarget.manufacturerNpc
            roomLamps = savedTarget.roomLamps.ifEmpty { LightingSystem.generateRoomLamps(curRoomResolved, ::allocId) }
        } else {
            objective = if (!wasVisited && !curRoomResolved.cleared) {
                RoomObjectiveSystem.maybeGenerateObjective(curRoomResolved.type, curRoomResolved.mechanic, nextState.floorNumber)
            } else null
            objPickups = RoomObjectiveSystem.initPickupsForObjective(objective, curRoomResolved.obstacles, ::allocId)
            finalInteractables = if (!wasVisited) {
                InteractablesSystem.generateForRoom(
                    curRoomResolved.type, nextState.floorNumber, curRoomResolved.obstacles, objective, ::allocId, lightingMode = curRoomResolved.lightingMode
                )
            } else {
                emptyList()
            }
            anomalies = if (!wasVisited) SecretDiscoverySystem.generateAnomaliesForRoom(curRoomResolved, nextState.dungeon, ::allocId) else emptyList()
            val (relEnemies, generatedBellPos) = EnemyRelationshipSystem.initializeRoomRelationships(
                nextState.enemies,
                hasBell = !wasVisited && curRoomResolved.type == RoomType.COMBAT && Random.nextFloat() < 0.25f,
                defeatedCaptainsCount = nextState.defeatedCaptainsCount,
                scenario = curRoomResolved.emergentScenario
            )
            finalEnemies = relEnemies
            bellPos = generatedBellPos
            roomLamps = LightingSystem.generateRoomLamps(curRoomResolved, ::allocId)
            npcState = if (ManufacturerSystem.shouldSpawnNpcInRoom(curRoomResolved, nextState)) {
                ManufacturerNpcState(x = ARENA_W * 0.5f, y = ARENA_H * 0.36f, present = true)
            } else null

            if (curRoomResolved.type == RoomType.SECRET && !wasVisited && curRoomResolved.secretFlavor == SecretRoomFlavor.FORBIDDEN_CHAMBER) {
                val forbiddenItem = GameCatalog.passiveItems.filter { it.rarity == Rarity.CURSED || it.rarity == Rarity.RARE }.randomOrNull()
                if (forbiddenItem != null) {
                    nextState = nextState.copy(
                        pickups = nextState.pickups + GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, ARENA_W * 0.5f, ARENA_H * 0.62f, valueStr = forbiddenItem.id)
                    )
                }
            }

            // Random Companion Cage guarded by protector enemies (guaranteed when TRAPPED_ALLY scenario)
            val baseCageChance = if (nextState.activeCompanions.isEmpty() && nextState.roomsClearedCount >= 1) 0.42f else 0.26f
            val forceTrappedAlly = curRoomResolved.emergentScenario == EmergentRoomScenario.TRAPPED_ALLY
            if (!wasVisited && !curRoomResolved.cleared &&
                curRoomResolved.type in setOf(RoomType.COMBAT, RoomType.CHALLENGE, RoomType.CURSE, RoomType.FORBIDDEN) &&
                finalEnemies.isNotEmpty() && nextState.activeCompanions.size < 2 &&
                finalInteractables.none { it.type == InteractableType.COMPANION_CAGE } &&
                (forceTrappedAlly || Random.nextFloat() < baseCageChance)
            ) {
                val compPool = CompanionId.entries.filter { id ->
                    nextState.activeCompanions.none { it.id == id } && id !in nextState.pendingGrudgeCompanions
                }
                val chosenComp = compPool.randomOrNull()
                if (chosenComp != null) {
                    val cageTargetX = if (px < ARENA_W * 0.5f) ARENA_W * 0.72f else ARENA_W * 0.28f
                    val cageTargetY = if (py < ARENA_H * 0.5f) ARENA_H * 0.68f else ARENA_H * 0.32f
                    val cage = InteractablesSystem.placeSafe(
                        type = InteractableType.COMPANION_CAGE,
                        x = cageTargetX,
                        y = cageTargetY,
                        obstacles = curRoomResolved.obstacles,
                        allocId = ::allocId,
                        variant = chosenComp.ordinal
                    )
                    finalInteractables = finalInteractables + cage

                    // Assign 1-2 enemies in this room to actively guard & protect the cage
                    val guardCount = if (finalEnemies.size >= 3) 2 else 1
                    val orderedIndices = finalEnemies.indices.sortedByDescending { idx ->
                        when (finalEnemies[idx].type) {
                            EnemyType.BODYGUARD_BEAR, EnemyType.RUSTY_TIN_KNIGHT -> 3
                            EnemyType.PLUSH_BEAR, EnemyType.TIN_SOLDIER -> 2
                            else -> 1
                        }
                    }.take(guardCount).toSet()

                    finalEnemies = finalEnemies.mapIndexed { idx, enemy ->
                        if (idx in orderedIndices) {
                            val offsetSign = if (idx == orderedIndices.first()) -1f else 1f
                            val gx = (cage.x + offsetSign * 42f).coerceIn(WALL_THICKNESS + 32f, ARENA_W - WALL_THICKNESS - 32f)
                            val gy = (cage.y + 44f).coerceIn(WALL_THICKNESS + 32f, ARENA_H - WALL_THICKNESS - 32f)
                            enemy.copy(
                                x = gx,
                                y = gy,
                                role = EnemyRole.PROTECTOR,
                                protectedAllyId = null,
                                protectedCageId = cage.id
                            )
                        } else enemy
                    }
                    if (cageBanner == null || cageBanner == s.activeBanner) {
                        cageBanner = RunUiBanner(
                            "¡Mascota Enjaulada!",
                            "Enemigos custodian la jaula de ${chosenComp.displayName}. Destrúyela con bolitas o bomba.",
                            0xFFC89948,
                            3.2f
                        )
                    }
                }
            }
        }

        // Initialize Boss Arena Mechanics & 3-Phase Boss Properties when boss is present
        val bossIdx = finalEnemies.indexOfFirst { it.type.isBoss && it.hp > 0f }
        if (bossIdx != -1) {
            val (initBoss, props) = BossMechanicSystem.initBossArena(finalEnemies[bossIdx], ::allocId)
            val mutEnemies = finalEnemies.toMutableList()
            mutEnemies[bossIdx] = initBoss
            finalEnemies = mutEnemies
            bossArenaProps = props
        }

        val nextLoreRoom = if (curRoomResolved.type == RoomType.LORE) {
            nextState.loreRoomsByCoord[targetCoord]
                ?: LoreRoomSystem.generateForRoom(nextState.floorNumber, targetCoord, curRoomResolved.obstacles, ::allocId)
        } else null

        nextState = nextState.copy(
            enemies = finalEnemies,
            alarmBellPos = bellPos,
            roomObjective = objective,
            roomInteractables = finalInteractables,
            roomAnomalies = anomalies,
            roomLamps = roomLamps,
            bossArenaProps = bossArenaProps,
            pickups = nextState.pickups + objPickups,
            manufacturerNpc = npcState,
            loreRoom = nextLoreRoom,
            loreRoomsByCoord = if (nextLoreRoom != null) nextState.loreRoomsByCoord + (targetCoord to nextLoreRoom) else nextState.loreRoomsByCoord,
            activeBanner = if (nextLoreRoom != null && !wasVisited) {
                RunUiBanner("${nextLoreRoom.loreRoomType.displayName}: ${nextLoreRoom.title}", nextLoreRoom.hintSummary, nextLoreRoom.loreRoomType.badgeColorHex, 3.6f)
            } else if (!wasVisited && curRoomResolved.emergentScenario != EmergentRoomScenario.STANDARD && curRoomResolved.emergentScenario.hint.isNotEmpty() && cageBanner == s.activeBanner) {
                RunUiBanner(curRoomResolved.emergentScenario.displayName, curRoomResolved.emergentScenario.hint, 0xFFC89948, 2.8f)
            } else cageBanner
        )
        nextState = LabyrinthSystem.initLabyrinthRoom(nextState, curRoomResolved, ::allocId)
        nextState = ManufacturerSystem.tickClosedMysteryBoxOnRoomEnter(nextState, ::allocId, sfx)
        nextState = RoomMechanicSystem.initForRoom(nextState, curRoomResolved)
        nextState = CompanionSystem.onEnterRoomCompanions(nextState, curRoomResolved, ::allocId, sfx, wasVisited = wasVisited)
        return nextState
    }

    private fun safePickupPos(x: Float, y: Float, obstacles: List<ObstacleRect>): Pair<Float, Float> {
        val cx = x.coerceIn(WALL_THICKNESS + 44f, ARENA_W - WALL_THICKNESS - 44f)
        val cy = y.coerceIn(WALL_THICKNESS + 44f, ARENA_H - WALL_THICKNESS - 44f)
        if (obstacles.isEmpty()) return cx to cy
        val (rx, ry) = RoomMechanicSystem.clampToObstacles(cx, cy, 52f, obstacles)
        return rx.coerceIn(WALL_THICKNESS + 44f, ARENA_W - WALL_THICKNESS - 44f) to
            ry.coerceIn(WALL_THICKNESS + 44f, ARENA_H - WALL_THICKNESS - 44f)
    }

    fun openCompanionInspect(state: GameRunState, companionId: CompanionId): GameRunState {
        val comp = state.activeCompanions.find { it.id == companionId } ?: return state
        return state.copy(pendingCompanionDialog = CompanionSystem.buildInspectDialog(comp, state))
    }

    fun resolveCompanionDialog(state: GameRunState, optionId: String): GameRunState {
        return CompanionSystem.resolveCompanionDialog(
            state = state,
            optionId = optionId,
            sfx = sfx,
            allocId = ::allocId,
            onDiscovery = onDiscovery,
            onGrudge = onCompanionGrudge,
            acquireItemFn = ::acquirePassiveItem
        )
    }

    private fun advanceToNextFloorOrWin(state: GameRunState): GameRunState {
        if (!state.usedAnyRepair) onAchievement("ach_no_repair")
        if (state.activeDefects.size >= 2) onAchievement("ach_two_defects")
        if (state.activeDefects.any { it.severity >= 3 }) onAchievement("ach_severe_defect")
        if (state.difficulty == Difficulty.PESADILLA) onAchievement("ach_nightmare_floor")

        if (state.floorNumber >= 8) {
            onAchievement("ach_win_run")
            if (state.character.id == CharacterId.PATCH) onAchievement("ach_patch_win")
            return state.copy(isVictory = true, isActive = false)
        }

        val nextFloorNum = state.floorNumber + 1
        val secretBonus = state.passiveItems.sumOf { it.effects.secretRoomChanceBonus.toDouble() }.toFloat() +
            (if (state.toyboxMemory.secretCluesRevealed) 0.25f else 0f) +
            (if (state.toyboxMemory.compassionScore >= 1) 0.15f else 0f)
        val nextDungeon = DungeonGenerator.generateFloor(
            floorNumber = nextFloorNum,
            difficulty = state.difficulty,
            secretBonusChance = secretBonus,
            forceManufacturerRoom = (state.nextFloorBias == "manufacturer")
        )
        val theme = GameCatalog.floors[(nextFloorNum - 1).coerceIn(0, GameCatalog.floors.lastIndex)]
        val startRoom = nextDungeon.rooms[nextDungeon.startCoord]!!
        sfx.play(ToySfx.SYNERGY)

        val nextStartInteractables = InteractablesSystem.generateForRoom(
            RoomType.START, nextFloorNum, startRoom.obstacles, null, ::allocId, lightingMode = startRoom.lightingMode
        )
        val nextStartLamps = LightingSystem.generateRoomLamps(startRoom, ::allocId)
        val nextContracts = ContractSystem.generateBoardContracts(nextFloorNum)

        val nextFloorState = state.copy(
            floorNumber = nextFloorNum,
            floorTheme = theme,
            dungeon = nextDungeon,
            currentCoord = nextDungeon.startCoord,
            currentRoom = startRoom,
            playerX = ARENA_W * 0.5f,
            playerY = ARENA_H * 0.5f,
            enemies = emptyList(),
            projectiles = emptyList(),
            pickups = emptyList(),
            bombs = emptyList(),
            roomInteractables = nextStartInteractables,
            roomLamps = nextStartLamps,
            labyrinthState = null,
            bossArenaProps = emptyList(),
            availableContracts = nextContracts,
            contractBoardOpen = false,
            roomObjective = null,
            roomAnomalies = emptyList(),
            manufacturerNpc = null,
            loreRoom = null,
            loreRoomsByCoord = emptyMap(),
            savedRoomsByCoord = emptyMap(),
            interactionPrompt = null,
            nextFloorBias = null,
            exitTrapdoorOpen = false,
            damagedInPrevRoom = false,
            damagedInCurrentRoom = false,
            activeBanner = RunUiBanner(
                "Piso $nextFloorNum: ${theme.name}",
                theme.subtitle,
                0xFFC89948,
                3.5f
            )
        )
        return RoomMechanicSystem.initForRoom(nextFloorState, startRoom)
    }

    fun resolveShopPurchase(state: GameRunState, offerId: String): GameRunState {
        val offer = state.currentRoom.shopStock.find { it.id == offerId } ?: return state
        if (offer.purchased || state.buttons < offer.price) return state

        var s = state.copy(buttons = state.buttons - offer.price)
        val updatedStock = s.currentRoom.shopStock.map { if (it.id == offerId) it.copy(purchased = true) else it }
        val room = s.currentRoom.copy(shopStock = updatedStock)
        val rooms = s.dungeon.rooms.toMutableMap().apply { this[s.currentCoord] = room }
        s = s.copy(currentRoom = room, dungeon = s.dungeon.copy(rooms = rooms))

        when {
            offer.isHeal -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                s = s.copy(hearts = (s.hearts + 1).coerceAtMost(s.maxHearts))
            }
            offer.isConsumable -> {
                val c = runCatching { ConsumableType.valueOf(offer.itemId ?: "") }.getOrNull()
                if (c != null) {
                    sfx.play(ToySfx.PICKUP_ITEM)
                    s = s.copy(consumables = s.consumables + c)
                }
            }
            offer.isBodyPart && offer.itemId != null -> {
                val part = GameCatalog.partById(offer.itemId)
                s = s.copy(
                    shopOpen = false,
                    pendingPartChoice = PendingPartChoice(
                        part,
                        s.equippedParts[part.zone],
                        GameCatalog.computeActiveSynergies(
                            s.equippedParts + (part.zone to EquippedBodyPart(part)),
                            s.passiveItems
                        )
                    )
                )
            }
            offer.itemId != null && offer.itemId.startsWith("backpack_item:") -> {
                val bKind = runCatching { BackpackItemKind.valueOf(offer.itemId.removePrefix("backpack_item:")) }.getOrNull()
                if (bKind != null) {
                    val (added, ns) = BackpackSystem.addItem(s, bKind, allocId = ::allocId, sfx = sfx)
                    s = ns
                    if (!added) {
                        s = s.copy(
                            pickups = s.pickups + GroundPickup(allocId(), PickupKind.BACKPACK_ITEM, s.playerX, (s.playerY + 42f).coerceAtMost(ARENA_H - WALL_THICKNESS - 40f), valueStr = bKind.name)
                        )
                    }
                }
            }
            offer.itemId != null && offer.itemId.startsWith("backpack_tier:") -> {
                val tier = runCatching { BackpackTier.valueOf(offer.itemId.removePrefix("backpack_tier:")) }.getOrNull()
                if (tier != null) {
                    s = BackpackSystem.equipTier(s, tier, ::allocId, sfx, onUnlockBackpack)
                }
            }
            offer.itemId != null && offer.itemId.startsWith("light:") -> {
                val ls = runCatching { LightSourceType.valueOf(offer.itemId.removePrefix("light:")) }.getOrNull()
                if (ls != null) {
                    sfx.play(ToySfx.PICKUP_ITEM)
                    s = LightingSystem.grantLightSource(s, ls)
                }
            }
            offer.itemId != null -> {
                GameCatalog.itemById(offer.itemId)?.let { item ->
                    s = acquirePassiveItem(s, item)
                }
            }
        }
        return s
    }

    fun resolveRepairBench(state: GameRunState, zone: BodyZone?, repairAllCost: Int = 5): GameRunState {
        if (zone == null) {
            if (state.buttons < repairAllCost) return state
            sfx.play(ToySfx.REPAIR_STITCH)
            val next = repairAllParts(state, 50).copy(
                buttons = state.buttons - repairAllCost,
                usedAnyRepair = true,
                repairBenchOpen = false,
                activeBanner = RunUiBanner("Mesa de Costura", "Todas tus piezas han sido cosidas (+50).", 0xFF597354, 2.2f)
            )
            return ContractSystem.onRepairDone(next, viaBench = true)
        } else {
            val part = state.equippedParts[zone] ?: return state
            val cost = 2
            if (state.buttons < cost) return state
            val map = state.equippedParts.toMutableMap()
            map[zone] = part.copy(isDetached = false, durability = part.def.maxDurability)
            sfx.play(ToySfx.REPAIR_STITCH)
            val next = state.copy(
                buttons = state.buttons - cost,
                equippedParts = map,
                usedAnyRepair = true,
                activeBanner = RunUiBanner("Pieza Restaurada", "${part.def.name} al 100%.", 0xFF597354, 2.0f)
            )
            return ContractSystem.onRepairDone(next, viaBench = true)
        }
    }

    fun resolveEventOption(state: GameRunState, optionId: String): GameRunState {
        val ev = state.activeEvent ?: return state
        val opt = ev.options.find { it.id == optionId } ?: return state.copy(activeEvent = null)
        if (state.buttons < opt.buttonCost) return state

        var s = state.copy(
            buttons = state.buttons - opt.buttonCost,
            activeEvent = null
        )
        val room = s.currentRoom.copy(eventResolved = true)
        val rooms = s.dungeon.rooms.toMutableMap().apply { this[s.currentCoord] = room }
        s = s.copy(currentRoom = room, dungeon = s.dungeon.copy(rooms = rooms))

        when (optionId) {
            "insert_coin", "open_box" -> {
                val roll = Random.nextFloat()
                if (roll < 0.55f) {
                    val item = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
                    if (item != null) s = acquirePassiveItem(s, item)
                } else if (roll < 0.85f) {
                    sfx.play(ToySfx.PICKUP_BUTTON)
                    s = s.copy(
                        buttons = s.buttons + 6,
                        consumables = s.consumables + ConsumableType.entries.random(),
                        activeBanner = RunUiBanner("Premio", "+6 Botones y un consumible.", 0xFFC89948, 2.4f)
                    )
                } else {
                    s = s.copy(
                        enemies = listOf(ActiveEnemy(id = allocId(), type = EnemyType.MIMIC_CHEST, x = ARENA_W * 0.5f, y = ARENA_H * 0.35f, hp = 44f, maxHp = 44f, radius = 22f)),
                        activeBanner = RunUiBanner("¡Emboscada!", "La caja tenía dientes.", 0xFFD13B42, 2.4f)
                    )
                }
            }
            "kick_machine" -> {
                val part = GameCatalog.bodyParts.random()
                val arm = s.equippedParts[BodyZone.ARMS]
                if (arm != null) {
                    val map = s.equippedParts.toMutableMap()
                    map[BodyZone.ARMS] = arm.copy(durability = (arm.durability - 25).coerceAtLeast(10))
                    s = s.copy(equippedParts = map)
                }
                s = s.copy(
                    pendingPartChoice = PendingPartChoice(
                        part, s.equippedParts[part.zone],
                        GameCatalog.computeActiveSynergies(s.equippedParts + (part.zone to EquippedBodyPart(part)), s.passiveItems)
                    )
                )
            }
            "trade_part" -> {
                val worst = s.equippedParts.values.minByOrNull { it.durability }
                val pool = GameCatalog.bodyParts.filter { it.zone == worst?.def?.zone && it.id != worst.def.id }
                val newPart = pool.randomOrNull() ?: GameCatalog.bodyParts.random()
                s = s.copy(
                    pendingPartChoice = PendingPartChoice(
                        newPart, s.equippedParts[newPart.zone],
                        GameCatalog.computeActiveSynergies(s.equippedParts + (newPart.zone to EquippedBodyPart(newPart)), s.passiveItems)
                    )
                )
            }
            "repair_trade" -> {
                sfx.play(ToySfx.REPAIR_STITCH)
                s = repairAllParts(s, 35).copy(usedAnyRepair = true)
            }
            "help_toy" -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                s = s.copy(
                    toyboxMemory = s.toyboxMemory.copy(compassionScore = s.toyboxMemory.compassionScore + 1),
                    consumables = s.consumables + ConsumableType.REPAIR_PATCH,
                    activeBanner = RunUiBanner("Compasión Recordada", "El juguete agradece y entrega un Parche de Tela.", 0xFF597354, 2.5f)
                )
            }
            "salvage_toy" -> {
                val part = GameCatalog.bodyParts.random()
                s = s.copy(
                    toyboxMemory = s.toyboxMemory.copy(compassionScore = s.toyboxMemory.compassionScore - 1),
                    pendingPartChoice = PendingPartChoice(
                        part, s.equippedParts[part.zone],
                        GameCatalog.computeActiveSynergies(s.equippedParts + (part.zone to EquippedBodyPart(part)), s.passiveItems)
                    )
                )
            }
            "offer_button" -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                s = s.copy(
                    consumables = s.consumables + ConsumableType.KEY,
                    activeBanner = RunUiBanner("Susurro Infantil", "Alguien dejó caer una Llave de Latón.", 0xFFC89948, 2.4f)
                )
            }
            "touch_mirror" -> {
                val item = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
                if (item != null) s = acquirePassiveItem(s, item)
                s = damagePlayer(s)
            }
        }
        return s
    }

    private fun repairAllParts(state: GameRunState, amount: Int): GameRunState {
        val mult = state.activeDefects.fold(1f) { acc, d -> acc * d.effects.repairBonusMult }
        val gain = (amount * mult).toInt()
        val map = state.equippedParts.mapValues { (_, ep) ->
            ep.copy(
                isDetached = false,
                durability = (ep.durability + gain).coerceAtMost(ep.def.maxDurability)
            )
        }
        return state.copy(equippedParts = map)
    }

    private fun spawnHitParticles(x: Float, y: Float, colorHex: Long): List<ParticleFx> {
        return (0 until 6).map {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 40f + Random.nextFloat() * 90f
            ParticleFx(x, y, cos(a) * spd, sin(a) * spd, 3.5f, colorHex, 0.32f, 0.32f)
        }
    }

    private fun spawnRingParticles(x: Float, y: Float, speed: Float, colorHex: Long): List<ParticleFx> {
        return (0 until 12).map { i ->
            val a = (i / 12f) * 2f * PI.toFloat()
            ParticleFx(x, y, cos(a) * speed, sin(a) * speed, 4.5f, colorHex, 0.4f, 0.4f)
        }
    }

    private fun computeInteractionPrompt(state: GameRunState): String? {
        val npc = state.manufacturerNpc
        if (npc != null && npc.present && !npc.interacted && hypot(npc.x - state.playerX, npc.y - state.playerY) <= 68f) {
            return "Hablar con El Fabricante"
        }
        PuzzleInteractionSystem.findNearbyLorePrompt(state)?.let { return it.second }
        SecretDiscoverySystem.findNearbyAnomalyPrompt(state)?.let { return it.second }
        RoomObjectiveSystem.findNearbyObjectivePrompt(state)?.let { return it }
        InteractablesSystem.findNearbyInteractablePrompt(state)?.let { return it.second }
        return null
    }

    fun interactNearby(state: GameRunState): GameRunState {
        val npc = state.manufacturerNpc
        if (npc != null && npc.present && !npc.interacted && hypot(npc.x - state.playerX, npc.y - state.playerY) <= 68f) {
            return state.copy(manufacturerEncounter = ManufacturerSystem.buildEncounter(state))
        }
        PuzzleInteractionSystem.findNearbyLorePrompt(state)?.let { (prop, _) ->
            val after = PuzzleInteractionSystem.interactLoreProp(
                state = state,
                propId = prop.id,
                allocId = ::allocId,
                sfx = sfx,
                onDiscovery = onDiscovery,
                onAchievement = onAchievement,
                onLorePuzzleSolved = onLorePuzzleSolved
            )
            return if (after.lorePuzzlesSolvedThisRun > state.lorePuzzlesSolvedThisRun || after.pendingArchiveDiscovery != null) {
                EmotionSystem.triggerEmotion(ContractSystem.onRelicOrClueFound(after), PatchEmotion.CURIOSITY, 24f, 20f)
            } else after
        }
        SecretDiscoverySystem.findNearbyAnomalyPrompt(state)?.let { (anom, _) ->
            val after = SecretDiscoverySystem.interactWithAnomaly(state, anom.id, ::allocId, sfx, onDiscovery)
            return if (after.roomAnomalies.count { it.resolved } > state.roomAnomalies.count { it.resolved }) {
                EmotionSystem.triggerEmotion(ContractSystem.onRelicOrClueFound(after), PatchEmotion.CURIOSITY, 24f, 20f)
            } else after
        }
        val (handledObj, objState) = RoomObjectiveSystem.tryInteractObjective(state, ::allocId, sfx)
        if (handledObj) return objState
        InteractablesSystem.findNearbyInteractablePrompt(state)?.let { (inter, _) ->
            return InteractablesSystem.activateInteractable(state, inter.id, ::allocId, sfx, onDiscovery)
        }
        return state
    }

    fun abandonRoomObjective(state: GameRunState): GameRunState {
        return RoomObjectiveSystem.abandonObjective(state)
    }

    fun openRepairMinigame(state: GameRunState, zone: BodyZone, mode: RepairActionMode): GameRunState {
        return RepairMinigameSystem.startMinigame(state, zone, mode)
    }

    fun handleRepairMinigameAction(state: GameRunState, action: String): GameRunState {
        return RepairMinigameSystem.handleAction(state, action, sfx)
    }

    fun resolveManufacturerOffer(state: GameRunState, offerId: String?): GameRunState {
        return ManufacturerSystem.resolveOffer(
            state = state,
            offerId = offerId,
            allocId = ::allocId,
            sfx = sfx,
            onDiscovery = onDiscovery,
            onAchievement = onAchievement,
            onUnlockBackpack = onUnlockBackpack,
            acquireItemFn = ::acquirePassiveItem,
            onRecordTrust = onManufacturerTrust
        )
    }

    fun useBackpackItem(state: GameRunState, uid: Long): GameRunState {
        return BackpackSystem.useItem(state, uid, ::allocId, sfx, onDiscovery, onAchievement)
    }

    fun discardBackpackItem(state: GameRunState, uid: Long): GameRunState {
        return BackpackSystem.discardItem(state, uid)
    }

    fun craftBackpack(state: GameRunState, recipe: BackpackRecipe): GameRunState {
        return BackpackSystem.craftBackpack(
            state = state,
            recipe = recipe,
            allocId = ::allocId,
            sfx = sfx,
            onDiscovery = onDiscovery,
            onAchievement = onAchievement,
            onUnlockTier = onUnlockBackpack,
            onDiscoverRecipe = onDiscoverRecipe
        )
    }

    fun dismissArchiveDiscovery(state: GameRunState): GameRunState {
        return state.copy(pendingArchiveDiscovery = null)
    }

    fun toggleLight(state: GameRunState): GameRunState = LightingSystem.toggleLight(state)

    fun cycleLightSource(state: GameRunState): GameRunState = LightingSystem.cycleLightSource(state)

    fun openContractBoard(state: GameRunState): GameRunState = state.copy(contractBoardOpen = true)

    fun closeContractBoard(state: GameRunState): GameRunState = state.copy(contractBoardOpen = false)

    fun acceptContract(state: GameRunState, def: ContractDef): GameRunState = ContractSystem.acceptContract(state, def)

    fun abandonContract(state: GameRunState, contractId: String): GameRunState = ContractSystem.abandonContract(state, contractId)
}
