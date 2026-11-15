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
    private val onClearCompanionGrudge: (String) -> Unit = {}
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
        val startCompanionPick = listOf(
            CompanionId.BUNNY,
            CompanionId.BROKEN_SOLDIER,
            CompanionId.FORGOTTEN_BEAR,
            CompanionId.FACELESS_DOLL,
            CompanionId.CLOCKWORK_CAT,
            CompanionId.PAPER_MOTH,
            CompanionId.WHISPER_PUPPET,
            CompanionId.RUSTY_BALLERINA
        ).random()

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
            pickups = listOf(
                GroundPickup(
                    id = allocId(),
                    kind = PickupKind.COMPANION_SHRINE,
                    x = ARENA_W * 0.24f,
                    y = ARENA_H * 0.26f,
                    valueStr = startCompanionPick.name
                )
            ),
            activeBanner = RunUiBanner(
                title = "Piso 1: ${GameCatalog.floors[0].name}",
                subtitle = GameCatalog.floors[0].subtitle
            )
        )
    }

    fun step(state: GameRunState, input: EngineInput, dtRaw: Float): GameRunState {
        if (!state.isActive || state.isPaused || state.isGameOver || state.isVictory ||
            state.activeEvent != null || state.pendingPartChoice != null ||
            state.pendingCompanionDialog != null ||
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

        // Player Movement & Dodge
        s = updatePlayerMovement(s, input, dt)

        // Dynamic Room Mechanics (Tilt, Vanishing Floor, Shifting Doors, Blackout, Carousel)
        s = RoomMechanicSystem.stepMechanics(s, dt, ::damagePlayer)

        // Manual Aim & Shoot
        s = updatePlayerShooting(s, input, dt)

        // Update Bombs
        s = updateBombs(s, dt)

        // Update Enemies & AI
        s = updateEnemies(s, dt)

        // Update Conditional Companions
        s = CompanionSystem.updateCompanions(s, dt, ::allocId, sfx)

        // Update Projectiles
        s = updateProjectiles(s, dt)

        // Update Pickups & Decoys & Particles
        s = updatePickupsAndFx(s, dt)

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
        state.passiveItems.forEach { speedMult *= it.effects.speedMult }
        state.activeDefects.forEach { speedMult *= it.effects.speedMult }
        if (state.activeSynergies.any { it.effectId == "carousel_overclock" }) speedMult *= 1.12f

        val maxSpeed = state.character.baseSpeed * speedMult

        var vx: Float
        var vy: Float
        if (state.dodgeTimer > 0f) {
            var dodgeMult = 2.55f
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
        if (state.abilityBuffTimer > 0f) fireRate *= 2.0f
        if (state.impulseBurstTimer > 0f) fireRate *= 1.45f
        if (state.activeSynergies.any { it.effectId == "carousel_overclock" }) fireRate *= 1.22f

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
            (if (state.activeSynergies.any { it.effectId == "oracle_crit" }) 0.20f else 0f)
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
                explosive = attackStyle == AttackStyle.EXPLOSIVE_TOY
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
                        val newHp = e.hp - b.damage
                        if (newHp <= 0f && e.hp > 0f) killsInBlast++
                        e.copy(hp = newHp, hitFlashTimer = 0.2f)
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

        val detectionMult = (s.passiveItems.fold(1f) { acc, i -> acc * i.effects.enemyDetectionMult }) *
            (if (s.activeDefects.any { it.effects.noisySteps }) 1.35f else 1f) *
            (if (s.stealthTimer > 0f) 0.4f else 1f)
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

            val dx = targetX - enemy.x
            val dy = targetY - enemy.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val dirX = dx / dist
            val dirY = dy / dist
            val moveSpeed = enemy.type.speed * s.difficulty.enemySpeedMult * aggroMult

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
                        s = s.copy(
                            pickups = s.pickups.filterNot { it.id == droppedPart.id },
                            activeBanner = RunUiBanner("¡Pieza Robada!", "Un duende huyó con tu pieza caída.", 0xFFD13B42, 2.2f)
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
                    val strafeX = -dirY * sin(enemy.stateTimer * 4f) * 0.6f
                    val strafeY = dirX * sin(enemy.stateTimer * 4f) * 0.6f
                    enemy = enemy.copy(
                        x = enemy.x + (dirX + strafeX) * moveSpeed * dt,
                        y = enemy.y + (dirY + strafeY) * moveSpeed * dt
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
                    )
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
            val enraged = b.hp < b.maxHp * 0.5f
            val baseAngle = computeAimAngle(b.x, b.y, state, 310f * bulletSlowMult)
            when (b.type) {
                EnemyType.BOSS_BOTONERO -> when (phase) {
                    0 -> {
                        val dirs = if (enraged) 8 else 4
                        for (i in 0 until dirs) {
                            val a = (i / dirs.toFloat()) * (2f * PI.toFloat()) + (if (enraged) b.stateTimer * 0.3f else 0f)
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 310f * bulletSlowMult, sin(a) * 310f * bulletSlowMult, 11f, 1f, false, 0xFFE5A93B))
                        }
                    }
                    1 -> {
                        for (i in -2..2) {
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
                        for (i in 0 until 4) {
                            val a = (PI.toFloat() / 4f) + i * (PI.toFloat() / 2f)
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 320f * bulletSlowMult, sin(a) * 320f * bulletSlowMult, 10f, 1f, false, 0xFFD9E0E6))
                        }
                    }
                    1 -> {
                        for (i in -1..1) {
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
                        val count = if (enraged) 12 else 9
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
                        val count = if (enraged) 6 else 4
                        for (i in 0 until count) {
                            val a = baseAngle + (i - count / 2f) * 0.25f
                            outProjs.add(ActiveProjectile(allocId(), b.x, b.y, cos(a) * 300f * bulletSlowMult, sin(a) * 300f * bulletSlowMult, 11.5f, 1f, false, 0xFFD13B42))
                        }
                    }
                    1 -> {
                        val count = if (enraged) 10 else 8
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
                else -> when (phase) {
                    0 -> {
                        // Fan volley
                        val spreadCount = if (enraged) 5 else 3
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
                        val count = if (enraged) 10 else 8
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
            b = b.copy(phase = phase, attackTimer = if (enraged) 1.2f else 1.55f)
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

    private fun updateProjectiles(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val remaining = mutableListOf<ActiveProjectile>()

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

            if (p.fromPlayer) {
                var consumed = false
                val shortRangeBonus = if (s.activeDefects.any { it.effects.shortRangeBonusDamage }) 1.22f else 1f
                val updatedEnemies = s.enemies.map { e ->
                    if (!consumed && e.id !in p.hitEnemyIds &&
                        hypot(e.x - p.x, e.y - p.y) <= e.radius + p.radius
                    ) {
                        sfx.play(ToySfx.HIT_ENEMY)
                        val dmg = p.damage * shortRangeBonus
                        if (p.explosive) {
                            s = triggerMiniExplosion(s, p.x, p.y, dmg * 0.7f)
                        }
                        if (p.pierceLeft > 0) {
                            p = p.copy(pierceLeft = p.pierceLeft - 1, hitEnemyIds = p.hitEnemyIds + e.id)
                        } else {
                            consumed = true
                        }
                        e.copy(hp = e.hp - dmg, disguised = false, hitFlashTimer = 0.12f)
                    } else e
                }
                s = s.copy(
                    enemies = updatedEnemies,
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
                val wearMult = state.activeDefects.fold(1f) { acc, d -> acc * d.effects.wearRateMult }
                val wearDamage = (Random.nextInt(16, 28) * wearMult).toInt()
                if (target.wearState == WearState.CRITICAL && Random.nextFloat() < 0.65f) {
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
        return state.copy(
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
    }

    private fun handleEnemyKilled(state: GameRunState, enemy: ActiveEnemy): GameRunState {
        onDiscovery("enemy", enemy.type.name)
        var s = state.copy(
            enemiesDefeatedCount = state.enemiesDefeatedCount + 1,
            particles = state.particles + spawnRingParticles(enemy.x, enemy.y, enemy.radius * 1.4f, 0xFFE8DCC4)
        )

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
        return s.copy(pickups = pickups)
    }

    private fun updatePickupsAndFx(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val remainingPickups = mutableListOf<GroundPickup>()

        for (p in s.pickups) {
            val remExp = p.expireSec?.let { it - dt }
            if (remExp != null && remExp <= 0f) continue
            val updated = p.copy(expireSec = remExp)

            val pickupRadius = when (updated.kind) {
                PickupKind.NEW_PART_PEDESTAL, PickupKind.PASSIVE_ITEM_PEDESTAL, PickupKind.COMPANION_SHRINE -> 42f
                else -> 32f
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
                toyboxMemory = mem
            )
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

    private fun enterRoom(state: GameRunState, targetCoord: GridCoord, fromDir: Direction): GameRunState {
        val targetRoom = state.dungeon.rooms[targetCoord] ?: return state
        val wasVisited = targetRoom.visited
        val updatedRoom = targetRoom.copy(visited = true)
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

        // Costura Eterna synergy heals parts between rooms
        var s = state
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

        // Spawn special room contents on first visit (guaranteed obstacle-free!)
        val pickups = mutableListOf<GroundPickup>()
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
                }
                RoomType.SECRET -> {
                    val (sx1, sy1) = safePickupPos(ARENA_W * 0.4f, ARENA_H * 0.45f, updatedRoom.obstacles)
                    pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, sx1, sy1))
                    val item = GameCatalog.passiveItems.filter { it !in s.passiveItems }.randomOrNull()
                    if (item != null) {
                        val (sx2, sy2) = safePickupPos(ARENA_W * 0.6f, ARENA_H * 0.5f, updatedRoom.obstacles)
                        pickups.add(GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, sx2, sy2, valueStr = item.id))
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
            lightFlashTimer = lightFlash,
            damagedInPrevRoom = s.damagedInCurrentRoom,
            damagedInCurrentRoom = false,
            damageTakenInCurrentBoss = false,
            usedConsumableInBoss = false,
            activeBanner = when (updatedRoom.type) {
                RoomType.BOSS -> RunUiBanner("JEFE DEL PISO", ToyboxMemorySystem.bossDialogueFor(activeBossType, s.toyboxMemory), 0xFFD13B42, 3.4f)
                RoomType.TREASURE -> RunUiBanner("Sala de Tesoro", "Piezas y reliquias olvidadas", 0xFFC89948, 2.4f)
                RoomType.SECRET -> RunUiBanner("Sala Secreta", "Un escondite detrás de los estantes", 0xFFC89948, 2.6f)
                RoomType.REPAIR -> RunUiBanner("Mesa de Costura", "Hilo tibio y lámpara de taller", 0xFF597354, 2.2f)
                else -> mechanicBanner ?: s.activeBanner
            }
        )
        nextState = RoomMechanicSystem.initForRoom(nextState, curRoomResolved)
        nextState = CompanionSystem.onEnterRoomCompanions(nextState, curRoomResolved, ::allocId, sfx)
        return nextState
    }

    private fun safePickupPos(x: Float, y: Float, obstacles: List<ObstacleRect>): Pair<Float, Float> {
        val cx = x.coerceIn(WALL_THICKNESS + 36f, ARENA_W - WALL_THICKNESS - 36f)
        val cy = y.coerceIn(WALL_THICKNESS + 36f, ARENA_H - WALL_THICKNESS - 36f)
        if (obstacles.isEmpty()) return cx to cy
        val (rx, ry) = RoomMechanicSystem.clampToObstacles(cx, cy, 42f, obstacles)
        return rx.coerceIn(WALL_THICKNESS + 36f, ARENA_W - WALL_THICKNESS - 36f) to
            ry.coerceIn(WALL_THICKNESS + 36f, ARENA_H - WALL_THICKNESS - 36f)
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

        if (state.floorNumber >= 6) {
            onAchievement("ach_win_run")
            if (state.character.id == CharacterId.PATCH) onAchievement("ach_patch_win")
            return state.copy(isVictory = true, isActive = false)
        }

        val nextFloorNum = state.floorNumber + 1
        val secretBonus = state.passiveItems.sumOf { it.effects.secretRoomChanceBonus.toDouble() }.toFloat() +
            (if (state.toyboxMemory.secretCluesRevealed) 0.25f else 0f) +
            (if (state.toyboxMemory.compassionScore >= 1) 0.15f else 0f)
        val nextDungeon = DungeonGenerator.generateFloor(nextFloorNum, state.difficulty, secretBonus)
        val theme = GameCatalog.floors[(nextFloorNum - 1).coerceIn(0, GameCatalog.floors.lastIndex)]
        val startRoom = nextDungeon.rooms[nextDungeon.startCoord]!!
        sfx.play(ToySfx.SYNERGY)

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
            return repairAllParts(state, 50).copy(
                buttons = state.buttons - repairAllCost,
                usedAnyRepair = true,
                repairBenchOpen = false,
                activeBanner = RunUiBanner("Mesa de Costura", "Todas tus piezas han sido cosidas (+50).", 0xFF597354, 2.2f)
            )
        } else {
            val part = state.equippedParts[zone] ?: return state
            val cost = 2
            if (state.buttons < cost) return state
            val map = state.equippedParts.toMutableMap()
            map[zone] = part.copy(isDetached = false, durability = part.def.maxDurability)
            sfx.play(ToySfx.REPAIR_STITCH)
            return state.copy(
                buttons = state.buttons - cost,
                equippedParts = map,
                usedAnyRepair = true,
                activeBanner = RunUiBanner("Pieza Restaurada", "${part.def.name} al 100%.", 0xFF597354, 2.0f)
            )
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
}
