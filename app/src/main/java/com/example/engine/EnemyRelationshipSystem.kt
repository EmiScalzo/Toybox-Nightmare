package com.example.engine

import com.example.data.model.DamageElement
import com.example.data.model.EmergentRoomScenario
import com.example.data.model.EnemyAlertState
import com.example.data.model.EnemyFaction
import com.example.data.model.EnemyHpTier
import com.example.data.model.EnemyMaterialType
import com.example.data.model.EnemyRole
import com.example.data.model.EnemyType
import com.example.data.model.EngineEventType
import com.example.data.model.InteractableType
import com.example.data.model.darknessTrait
import com.example.data.model.defaultFaction
import com.example.data.model.defaultRole
import com.example.data.model.hpTier
import com.example.data.model.materialType
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object EnemyRelationshipSystem {

    fun computeElementalDamage(
        rawDamage: Float,
        element: DamageElement,
        material: EnemyMaterialType
    ): Float {
        val mult = when (material) {
            EnemyMaterialType.PORCELAIN -> when (element) {
                DamageElement.BLUNT_HEAVY, DamageElement.EXPLOSIVE -> 1.45f
                DamageElement.PHYSICAL_LIGHT -> 0.72f
                else -> 1.0f
            }
            EnemyMaterialType.CLOTH -> when (element) {
                DamageElement.FIRE, DamageElement.SHARP -> 1.40f
                DamageElement.BLUNT_HEAVY -> 0.75f
                else -> 1.0f
            }
            EnemyMaterialType.METAL -> when (element) {
                DamageElement.ELECTRIC, DamageElement.EXPLOSIVE -> 1.45f
                DamageElement.SHARP -> 0.65f
                DamageElement.PHYSICAL_LIGHT -> 0.75f
                else -> 1.0f
            }
            EnemyMaterialType.WOOD -> when (element) {
                DamageElement.FIRE, DamageElement.EXPLOSIVE -> 1.38f
                DamageElement.ELECTRIC -> 0.72f
                else -> 1.0f
            }
            EnemyMaterialType.RUBBER -> when (element) {
                DamageElement.ELECTRIC -> 0.40f
                DamageElement.BLUNT_HEAVY -> 0.75f
                DamageElement.SHARP -> 1.38f
                else -> 1.0f
            }
            EnemyMaterialType.MECHANISM -> when (element) {
                DamageElement.ELECTRIC -> 1.50f
                DamageElement.BLUNT_HEAVY -> 1.25f
                DamageElement.FIRE -> 0.80f
                else -> 1.0f
            }
        }
        return rawDamage * mult
    }

    fun initializeRoomRelationships(
        enemies: List<ActiveEnemy>,
        hasBell: Boolean,
        defeatedCaptainsCount: Int,
        scenario: EmergentRoomScenario = EmergentRoomScenario.STANDARD
    ): Pair<List<ActiveEnemy>, Pair<Float, Float>?> {
        val bellPos = if (hasBell || enemies.any { it.type == EnemyType.CLOCKWORK_COURIER }) {
            ARENA_W * 0.84f to ARENA_H * 0.22f
        } else null

        val fragileTarget = enemies.firstOrNull {
            it.type == EnemyType.TIN_SOLDIER || it.type == EnemyType.CREEPY_DOLL || it.type == EnemyType.STITCH_MECHANIC
        }

        val withRoles = enemies.mapIndexed { idx, e ->
            val role = e.type.defaultRole()
            val faction = e.type.defaultFaction()
            val scaredFromPast = defeatedCaptainsCount >= 2 && e.type == EnemyType.TIN_SOLDIER && Random.nextFloat() < 0.25f
            val startsSleeping = scenario == EmergentRoomScenario.SLEEPING_DEN && !e.type.isBoss && idx > 0
            val initAlert = when {
                scaredFromPast -> EnemyAlertState.PANIC
                startsSleeping -> EnemyAlertState.CALM
                scenario == EmergentRoomScenario.GUARDING_VALUABLE && !e.type.isBoss -> EnemyAlertState.SUSPICIOUS
                else -> EnemyAlertState.COMBAT
            }
            e.copy(
                role = role,
                faction = faction,
                materialType = e.type.materialType(),
                darknessTrait = e.type.darknessTrait(),
                alertState = initAlert,
                isSleeping = startsSleeping,
                protectedAllyId = if (role == EnemyRole.PROTECTOR) fragileTarget?.id else null,
                fleeing = scaredFromPast
            )
        }
        return withRoles to bellPos
    }

    fun stepRelationships(
        state: GameRunState,
        dt: Float,
        allocId: () -> Long
    ): GameRunState {
        if (state.enemies.isEmpty()) return state
        var s = state
        val enemies = s.enemies.toMutableList()
        val extraEnemies = mutableListOf<ActiveEnemy>()
        var events = s.recentEngineEvents
        var banner = s.activeBanner
        var escapedCowards = s.escapedCowardsCount

        val mods = EmotionSystem.computeModifiers(s.emotionState)
        val hasRechargeStation = s.roomInteractables.firstOrNull { it.type == InteractableType.RECHARGE_STATION && !it.destroyed }
        val clowns = enemies.filter { it.type == EnemyType.PORCELAIN_CLOWN && it.hp > 0f }

        // Check if Protector shields or guards fragile ally, update 5 Alert States & Factions
        for (i in enemies.indices) {
            var e = enemies[i]
            if (e.hp <= 0f) continue

            // Update 5-level alert state (CALM / SUSPICIOUS / ALERT / COMBAT / PANIC)
            val distToPlayer = hypot(s.playerX - e.x, s.playerY - e.y)
            val detectRange = 220f * mods.noiseMult * mods.enemyDetectMult
            if (e.alertState != EnemyAlertState.COMBAT && e.alertState != EnemyAlertState.PANIC) {
                when {
                    distToPlayer < detectRange * 0.68f || e.hp < e.maxHp -> {
                        e = e.copy(alertState = EnemyAlertState.COMBAT, isSleeping = false, suspicion = 1f)
                    }
                    distToPlayer < detectRange -> {
                        val nextSus = (e.suspicion + dt * 0.8f).coerceAtMost(1f)
                        val nextAlert = if (nextSus >= 0.7f) EnemyAlertState.ALERT else EnemyAlertState.SUSPICIOUS
                        e = e.copy(alertState = nextAlert, suspicion = nextSus, isSleeping = false)
                    }
                }
            }

            // Porcelain Clown laugh occasionally scares VERY_WEAK / WEAK nearby allies into PANIC
            if (clowns.isNotEmpty() && e.type != EnemyType.PORCELAIN_CLOWN && !e.type.isBoss &&
                e.type.hpTier() in setOf(EnemyHpTier.VERY_WEAK, EnemyHpTier.WEAK) &&
                e.alertState != EnemyAlertState.PANIC &&
                clowns.any { hypot(it.x - e.x, it.y - e.y) < 160f } && Random.nextFloat() < dt * 0.35f
            ) {
                e = e.copy(alertState = EnemyAlertState.PANIC, fleeing = true, scareTimer = 3.2f)
            }
            if (e.scareTimer > 0f) {
                val rem = (e.scareTimer - dt).coerceAtLeast(0f)
                e = e.copy(
                    scareTimer = rem,
                    alertState = if (rem <= 0f) EnemyAlertState.COMBAT else EnemyAlertState.PANIC,
                    fleeing = rem > 0f
                )
            }

            // Injured mechanical toys seek Recharge Stations to recover HP
            if (hasRechargeStation != null &&
                e.materialType in setOf(EnemyMaterialType.MECHANISM, EnemyMaterialType.METAL) &&
                e.hp < e.maxHp * 0.60f && !e.type.isBoss
            ) {
                val dx = hasRechargeStation.x - e.x
                val dy = hasRechargeStation.y - e.y
                val d = hypot(dx, dy).coerceAtLeast(1f)
                if (d < 46f) {
                    e = e.copy(hp = (e.hp + 8f * dt).coerceAtMost(e.maxHp))
                } else {
                    e = e.copy(
                        x = e.x + (dx / d) * e.type.speed * 0.9f * dt,
                        y = e.y + (dy / d) * e.type.speed * 0.9f * dt
                    )
                }
            }

            // Faction rivalry: Tin Army vs Rag Plush real skirmish when rival is close
            if (e.faction == EnemyFaction.TIN_ARMY && !e.type.isBoss && Random.nextFloat() < 0.025f) {
                val rivalIdx = enemies.indexOfFirst { it.id != e.id && it.faction == EnemyFaction.RAG_PLUSH && hypot(it.x - e.x, it.y - e.y) < 130f }
                if (rivalIdx != -1 && enemies[rivalIdx].hp > 0f) {
                    enemies[rivalIdx] = enemies[rivalIdx].copy(hp = enemies[rivalIdx].hp - 5f, hitFlashTimer = 0.12f)
                }
            }

            when (e.role) {
                EnemyRole.LEADER -> {
                    // Buff nearby Tin Army allies with faster attack cadence
                    for (j in enemies.indices) {
                        if (j != i && enemies[j].faction == e.faction && hypot(enemies[j].x - e.x, enemies[j].y - e.y) < 250f) {
                            enemies[j] = enemies[j].copy(attackTimer = (enemies[j].attackTimer - dt * 0.25f).coerceAtLeast(0.05f))
                        }
                    }
                }

                EnemyRole.PROTECTOR -> {
                    val cage = e.protectedCageId?.let { cid ->
                        s.roomInteractables.firstOrNull { it.id == cid && !it.destroyed }
                    }
                    if (cage != null) {
                        // Position tightly between player and the caged pet to shield the cage
                        val pdx = s.playerX - cage.x
                        val pdy = s.playerY - cage.y
                        val pd = hypot(pdx, pdy).coerceAtLeast(1f)
                        val guardX = (cage.x + (pdx / pd) * 54f).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius)
                        val guardY = (cage.y + (pdy / pd) * 54f).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
                        val dx = guardX - e.x
                        val dy = guardY - e.y
                        val d = hypot(dx, dy).coerceAtLeast(1f)
                        if (d > 10f) {
                            e = e.copy(
                                x = (e.x + (dx / d) * e.type.speed * 1.15f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius),
                                y = (e.y + (dy / d) * e.type.speed * 1.15f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
                            )
                        }
                    } else if (e.protectedCageId != null && !e.enraged) {
                        e = e.copy(enraged = true, protectedCageId = null)
                        events = (events + EngineEventType.ENEMY_PROTECTED_ALLY.key).takeLast(12)
                        banner = RunUiBanner("${e.type.displayName} Furioso", "Destruiste la jaula que custodiaba.", 0xFFD13B42, 2.2f)
                    } else {
                        val ally = enemies.firstOrNull { it.id == e.protectedAllyId && it.hp > 0f }
                        if (ally != null) {
                            // Position between player and protected ally
                            val midX = (ally.x + s.playerX) * 0.45f
                            val midY = (ally.y + s.playerY) * 0.45f
                            val dx = midX - e.x
                            val dy = midY - e.y
                            val d = hypot(dx, dy).coerceAtLeast(1f)
                            if (d > 14f) {
                                e = e.copy(
                                    x = (e.x + (dx / d) * e.type.speed * 1.15f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius),
                                    y = (e.y + (dy / d) * e.type.speed * 1.15f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
                                )
                            }
                        } else if (e.protectedAllyId != null && !e.enraged) {
                            e = e.copy(enraged = true, protectedAllyId = null)
                            events = (events + EngineEventType.ENEMY_PROTECTED_ALLY.key).takeLast(12)
                            banner = RunUiBanner("${e.type.displayName} Enfurecido", "Su protegido cayó; ataca sin control.", 0xFFD13B42, 2.2f)
                        }
                    }
                }

                EnemyRole.COWARD -> {
                    val bell = s.alarmBellPos
                    if (bell != null) {
                        val bdx = bell.first - e.x
                        val bdy = bell.second - e.y
                        val bd = hypot(bdx, bdy).coerceAtLeast(1f)
                        e = e.copy(
                            x = (e.x + (bdx / bd) * e.type.speed * dt).coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f),
                            y = (e.y + (bdy / bd) * e.type.speed * dt).coerceIn(WALL_THICKNESS + 24f, ARENA_H - WALL_THICKNESS - 24f),
                            fleeing = true
                        )
                        if (bd < 28f && enemies.size + extraEnemies.size < 6) {
                            escapedCowards++
                            val backup = EnemyType.TIN_SOLDIER
                            extraEnemies.add(
                                ActiveEnemy(
                                    id = allocId(),
                                    type = backup,
                                    x = ARENA_W * 0.5f,
                                    y = ARENA_H * 0.25f,
                                    hp = backup.maxHp * s.difficulty.enemyHpMult,
                                    maxHp = backup.maxHp * s.difficulty.enemyHpMult,
                                    radius = 21f,
                                    role = backup.defaultRole(),
                                    faction = backup.defaultFaction(),
                                    materialType = backup.materialType(),
                                    darknessTrait = backup.darknessTrait()
                                )
                            )
                            e = e.copy(hp = 0f) // Coward alerts & flees room
                            banner = RunUiBanner("¡Alarma Activada!", "Un Conejo de Cuerda llamó a un refuerzo y escapó.", 0xFFD98A2B, 2.4f)
                        }
                    }
                }

                EnemyRole.REPAIRER -> {
                    if (e.actionReady(dt)) {
                        val injuredIdx = enemies.indexOfFirst { it.id != e.id && it.hp > 0f && it.hp < it.maxHp * 0.85f }
                        if (injuredIdx != -1) {
                            val injured = enemies[injuredIdx]
                            val d = hypot(injured.x - e.x, injured.y - e.y).coerceAtLeast(1f)
                            if (d > 58f) {
                                e = e.copy(
                                    x = e.x + ((injured.x - e.x) / d) * e.type.speed * dt,
                                    y = e.y + ((injured.y - e.y) / d) * e.type.speed * dt
                                )
                            } else {
                                enemies[injuredIdx] = injured.copy(hp = (injured.hp + 10f).coerceAtMost(injured.maxHp))
                                e = e.copy(attackTimer = 2.6f)
                            }
                        }
                    } else {
                        e = e.copy(attackTimer = e.attackTimer - dt)
                    }
                }

                else -> {
                    if (e.disorganized) {
                        val wobbleAngle = e.stateTimer * 3.5f + e.id
                        e = e.copy(
                            x = (e.x + cos(wobbleAngle) * 42f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius),
                            y = (e.y + sin(wobbleAngle) * 42f * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
                        )
                    }
                }
            }
            enemies[i] = e
        }

        // Protector intercepts player projectiles near protected fragile ally!
        var projs = s.projectiles
        val protectors = enemies.filter { it.role == EnemyRole.PROTECTOR && it.hp > 0f }
        if (protectors.isNotEmpty()) {
            val intercepted = mutableSetOf<Long>()
            projs.forEach { p ->
                if (p.fromPlayer) {
                    val blocker = protectors.firstOrNull { hypot(it.x - p.x, it.y - p.y) <= it.radius + p.radius + 14f }
                    if (blocker != null) {
                        intercepted.add(p.id)
                        val bIdx = enemies.indexOfFirst { it.id == blocker.id }
                        if (bIdx != -1) {
                            enemies[bIdx] = enemies[bIdx].copy(hp = enemies[bIdx].hp - p.damage * 0.75f, hitFlashTimer = 0.1f)
                        }
                    }
                }
            }
            if (intercepted.isNotEmpty()) {
                projs = projs.filterNot { it.id in intercepted }
            }
        }

        return s.copy(
            enemies = enemies + extraEnemies,
            projectiles = projs,
            recentEngineEvents = events,
            activeBanner = banner,
            escapedCowardsCount = escapedCowards
        )
    }

    private fun ActiveEnemy.actionReady(dt: Float): Boolean = (attackTimer - dt) <= 0f

    fun onEnemyKilledRelationships(state: GameRunState, killed: ActiveEnemy): GameRunState {
        var s = state
        var enemies = s.enemies
        var pickups = s.pickups
        var banner = s.activeBanner
        var defeatedCaptains = s.defeatedCaptainsCount

        if (killed.carriedPart != null) {
            pickups = pickups + GroundPickup(
                id = killed.id + 90000L,
                kind = PickupKind.DETACHED_PART,
                x = killed.x,
                y = killed.y,
                valueStr = killed.carriedPart.def.name,
                detachedPart = killed.carriedPart
            )
            banner = RunUiBanner("¡Pieza Recuperada!", "El ladrón soltó tu ${killed.carriedPart.def.name}.", 0xFF597354, 2.2f)
        }

        if (killed.role == EnemyRole.LEADER) {
            defeatedCaptains++
            enemies = enemies.map {
                if (it.id != killed.id && it.faction == killed.faction) it.copy(disorganized = true, stunnedTimer = maxOf(it.stunnedTimer, 0.8f))
                else it
            }
            banner = RunUiBanner("¡Líder Caído!", "El escuadrón de hojalata perdió el orden.", 0xFFC89948, 2.4f)
        }

        return s.copy(
            enemies = enemies,
            pickups = pickups,
            activeBanner = banner,
            defeatedCaptainsCount = defeatedCaptains
        )
    }
}
