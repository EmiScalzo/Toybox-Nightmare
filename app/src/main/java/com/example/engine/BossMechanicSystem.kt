package com.example.engine

import com.example.data.model.BossArenaProp
import com.example.data.model.BossMechanicKind
import com.example.data.model.DamageElement
import com.example.data.model.EnemyType
import com.example.data.model.bossMechanicKind
import kotlin.math.hypot
import kotlin.random.Random

object BossMechanicSystem {

    fun initBossArena(boss: ActiveEnemy, allocId: () -> Long): Pair<ActiveEnemy, List<BossArenaProp>> {
        val kind = boss.type.bossMechanicKind()
        val props = when (kind) {
            BossMechanicKind.PUPPET_STRINGS -> listOf(
                BossArenaProp(allocId(), "puppet_anchor", ARENA_W * 0.22f, ARENA_H * 0.28f, hp = 32f, active = true),
                BossArenaProp(allocId(), "puppet_anchor", ARENA_W * 0.78f, ARENA_H * 0.28f, hp = 32f, active = true),
                BossArenaProp(allocId(), "puppet_anchor", ARENA_W * 0.50f, ARENA_H * 0.78f, hp = 32f, active = true)
            )
            BossMechanicKind.SURPRISE_BOXES -> {
                val realIdx = Random.nextInt(3)
                listOf(
                    BossArenaProp(allocId(), "surprise_box", ARENA_W * 0.25f, ARENA_H * 0.34f, isReal = realIdx == 0, hp = 28f, active = true),
                    BossArenaProp(allocId(), "surprise_box", ARENA_W * 0.50f, ARENA_H * 0.26f, isReal = realIdx == 1, hp = 28f, active = true),
                    BossArenaProp(allocId(), "surprise_box", ARENA_W * 0.75f, ARENA_H * 0.34f, isReal = realIdx == 2, hp = 28f, active = true)
                )
            }
            BossMechanicKind.RAIL_TRAIN -> listOf(
                BossArenaProp(allocId(), "rail_switch", ARENA_W * 0.16f, ARENA_H * 0.50f, hp = 20f, active = true),
                BossArenaProp(allocId(), "rail_switch", ARENA_W * 0.84f, ARENA_H * 0.50f, hp = 20f, active = true)
            )
            BossMechanicKind.WINDUP_SHELL -> listOf(
                BossArenaProp(allocId(), "overheat_vent", ARENA_W * 0.30f, ARENA_H * 0.50f, hp = 35f, active = true),
                BossArenaProp(allocId(), "overheat_vent", ARENA_W * 0.70f, ARENA_H * 0.50f, hp = 35f, active = true)
            )
            BossMechanicKind.PORCELAIN_ARMOR -> emptyList()
        }
        val armoredOnStart = kind in setOf(
            BossMechanicKind.PUPPET_STRINGS,
            BossMechanicKind.PORCELAIN_ARMOR,
            BossMechanicKind.WINDUP_SHELL,
            BossMechanicKind.RAIL_TRAIN
        )
        return boss.copy(
            bossCurrentPhase = 1,
            bossArmorActive = armoredOnStart,
            bossVulnerabilityTimer = if (armoredOnStart) 0f else 999f,
            bossOverheatMeter = 0f
        ) to props
    }

    fun stepBossMechanics(
        state: GameRunState,
        dt: Float,
        allocId: () -> Long
    ): GameRunState {
        val bossIdx = state.enemies.indexOfFirst { it.type.isBoss && it.hp > 0f }
        if (bossIdx == -1) return state

        var boss = state.enemies[bossIdx]
        val kind = boss.type.bossMechanicKind()
        var props = state.bossArenaProps.toMutableList()
        var banner = state.activeBanner
        var enemies = state.enemies.toMutableList()
        var projectiles = state.projectiles
        var projsHitProp = false

        val hpRatio = (boss.hp / boss.maxHp).coerceIn(0f, 1f)
        val targetPhase = when {
            hpRatio <= 0.33f -> 3
            hpRatio <= 0.66f -> 2
            else -> 1
        }
        if (targetPhase > boss.bossCurrentPhase) {
            boss = boss.copy(bossCurrentPhase = targetPhase, enraged = targetPhase == 3)
            val phaseLabel = if (targetPhase == 2) "Fase II" else "Fase III — ¡Desesperación!"
            banner = RunUiBanner(
                "${boss.type.displayName}: $phaseLabel",
                kind.hint,
                if (targetPhase == 3) 0xFFD13B42 else 0xFFE5A93B,
                3.0f
            )
            // Phase transition refreshes interactive anchors/boxes for dynamic boss fights
            if (kind == BossMechanicKind.PUPPET_STRINGS) {
                props = props.map { it.copy(active = true, hp = 34f) }.toMutableList()
                boss = boss.copy(bossArmorActive = true, bossVulnerabilityTimer = 0f)
            } else if (kind == BossMechanicKind.SURPRISE_BOXES) {
                val realIdx = Random.nextInt(props.size.coerceAtLeast(1))
                props = props.mapIndexed { idx, p -> p.copy(active = true, hp = 28f, isReal = idx == realIdx) }.toMutableList()
            } else if (kind == BossMechanicKind.PORCELAIN_ARMOR || kind == BossMechanicKind.WINDUP_SHELL) {
                boss = boss.copy(bossArmorActive = true, bossVulnerabilityTimer = 0f, bossOverheatMeter = 0f)
            }
        }

        // Tick vulnerability window
        if (boss.bossVulnerabilityTimer > 0f) {
            val rem = (boss.bossVulnerabilityTimer - dt).coerceAtLeast(0f)
            boss = boss.copy(
                bossVulnerabilityTimer = rem,
                bossArmorActive = if (rem <= 0f && kind != BossMechanicKind.PUPPET_STRINGS) true else boss.bossArmorActive
            )
        }

        // Handle player shots hitting boss arena props (anchors, surprise boxes, rail switches, vents)
        if (props.isNotEmpty() && projectiles.any { it.fromPlayer }) {
            val consumedProjIds = mutableSetOf<Long>()
            for (proj in projectiles) {
                if (!proj.fromPlayer) continue
                val hitIdx = props.indexOfFirst { it.active && hypot(it.x - proj.x, it.y - proj.y) < 34f }
                if (hitIdx != -1) {
                    consumedProjIds.add(proj.id)
                    projsHitProp = true
                    var prop = props[hitIdx]
                    when (prop.kind) {
                        "puppet_anchor" -> {
                            val nextHp = prop.hp - proj.damage
                            if (nextHp <= 0f) {
                                prop = prop.copy(hp = 0f, active = false)
                                props[hitIdx] = prop
                                if (props.none { it.kind == "puppet_anchor" && it.active }) {
                                    boss = boss.copy(bossArmorActive = false, bossVulnerabilityTimer = 7.5f)
                                    banner = RunUiBanner("¡Hilos Cortados!", "${boss.type.displayName} quedó expuesto.", 0xFF597354, 2.2f)
                                }
                            } else {
                                props[hitIdx] = prop.copy(hp = nextHp)
                            }
                        }
                        "surprise_box" -> {
                            val nextHp = prop.hp - proj.damage
                            if (nextHp <= 0f) {
                                prop = prop.copy(hp = 0f, active = false)
                                props[hitIdx] = prop
                                if (prop.isReal) {
                                    boss = boss.copy(
                                        x = prop.x,
                                        y = prop.y,
                                        hp = (boss.hp - 36f).coerceAtLeast(1f),
                                        stunnedTimer = 1.8f,
                                        bossArmorActive = false,
                                        bossVulnerabilityTimer = 5.0f
                                    )
                                    banner = RunUiBanner("¡Caja Verdadera!", "Encontraste al jefe oculto y quedó aturdido.", 0xFF597354, 2.0f)
                                } else {
                                    // Decoy trap: fires burst toward player!
                                    val dx = state.playerX - prop.x
                                    val dy = state.playerY - prop.y
                                    val d = hypot(dx, dy).coerceAtLeast(1f)
                                    projectiles = projectiles + ActiveProjectile(
                                        id = allocId(), x = prop.x, y = prop.y,
                                        vx = (dx / d) * 280f, vy = (dy / d) * 280f,
                                        radius = 10f, damage = 1f, fromPlayer = false, colorHex = 0xFFD13B42
                                    )
                                    banner = RunUiBanner("¡Caja Trampa!", "Era un señuelo explosivo.", 0xFFD13B42, 1.8f)
                                }
                            } else {
                                props[hitIdx] = prop.copy(hp = nextHp)
                            }
                        }
                        "rail_switch" -> {
                            prop = prop.copy(active = false)
                            props[hitIdx] = prop
                            boss = boss.copy(bossArmorActive = false, bossVulnerabilityTimer = 6.5f, stunnedTimer = 2.0f)
                            banner = RunUiBanner("¡Freno Accionado!", "La locomotora descarriló y perdió su blindaje.", 0xFFE5A93B, 2.4f)
                        }
                        "overheat_vent" -> {
                            val nextHeat = (boss.bossOverheatMeter + 38f).coerceAtMost(100f)
                            val exposed = nextHeat >= 100f
                            boss = boss.copy(
                                bossOverheatMeter = if (exposed) 0f else nextHeat,
                                bossArmorActive = !exposed,
                                bossVulnerabilityTimer = if (exposed) 6.0f else boss.bossVulnerabilityTimer,
                                stunnedTimer = if (exposed) 1.5f else boss.stunnedTimer
                            )
                            if (exposed) {
                                banner = RunUiBanner("¡Núcleo Sobrecalentado!", "Su coraza se abrió temporalmente.", 0xFFE5A93B, 2.2f)
                            }
                        }
                    }
                }
            }
            if (projsHitProp) {
                projectiles = projectiles.filterNot { it.id in consumedProjIds }
            }
        }

        // Windup Shell naturally gains overheat during telegraph/charge
        if (kind == BossMechanicKind.WINDUP_SHELL && boss.bossArmorActive) {
            val heatGain = if (boss.telegraphTimer > 0f) 24f * dt else 4.5f * dt
            val nextHeat = boss.bossOverheatMeter + heatGain
            if (nextHeat >= 100f) {
                boss = boss.copy(
                    bossOverheatMeter = 0f,
                    bossArmorActive = false,
                    bossVulnerabilityTimer = 5.5f
                )
                banner = RunUiBanner("¡Cuerda Sobrecalentada!", "${boss.type.displayName} abrió su núcleo.", 0xFFE5A93B, 2.0f)
            } else {
                boss = boss.copy(bossOverheatMeter = nextHeat)
            }
        }

        // Update surprise box subtle shake animation phase
        for (i in props.indices) {
            if (props[i].kind == "surprise_box" && props[i].isReal && props[i].active) {
                props[i] = props[i].copy(shakePhase = props[i].shakePhase + dt * 10f)
            }
        }

        enemies[bossIdx] = boss
        return state.copy(
            enemies = enemies,
            bossArenaProps = props,
            projectiles = projectiles,
            activeBanner = banner
        )
    }

    fun computeIncomingDamage(
        state: GameRunState,
        boss: ActiveEnemy,
        rawDamage: Float,
        element: DamageElement
    ): Triple<Float, ActiveEnemy, Boolean> {
        val kind = boss.type.bossMechanicKind()
        if (!boss.bossArmorActive) {
            return Triple(rawDamage, boss, false)
        }
        return when (kind) {
            BossMechanicKind.PUPPET_STRINGS -> {
                val anyAnchorActive = state.bossArenaProps.any { it.kind == "puppet_anchor" && it.active }
                if (anyAnchorActive) Triple(0f, boss, true) else Triple(rawDamage, boss.copy(bossArmorActive = false), false)
            }
            BossMechanicKind.PORCELAIN_ARMOR -> {
                if (element in setOf(DamageElement.EXPLOSIVE, DamageElement.BLUNT_HEAVY)) {
                    Triple(rawDamage * 1.45f, boss.copy(bossArmorActive = false, bossVulnerabilityTimer = 6.0f), false)
                } else {
                    Triple(rawDamage * 0.50f, boss, false)
                }
            }
            BossMechanicKind.WINDUP_SHELL -> {
                val addedHeat = if (element == DamageElement.EXPLOSIVE || element == DamageElement.ELECTRIC) 35f else 12f
                val nextHeat = boss.bossOverheatMeter + addedHeat
                if (nextHeat >= 100f) {
                    Triple(rawDamage, boss.copy(bossOverheatMeter = 0f, bossArmorActive = false, bossVulnerabilityTimer = 5.5f), false)
                } else {
                    Triple(rawDamage * 0.55f, boss.copy(bossOverheatMeter = nextHeat), false)
                }
            }
            BossMechanicKind.RAIL_TRAIN -> {
                if (element == DamageElement.EXPLOSIVE || element == DamageElement.ELECTRIC) {
                    Triple(rawDamage * 1.25f, boss.copy(bossArmorActive = false, bossVulnerabilityTimer = 5.0f), false)
                } else {
                    Triple(rawDamage * 0.45f, boss, false)
                }
            }
            BossMechanicKind.SURPRISE_BOXES -> Triple(rawDamage * 0.85f, boss, false)
        }
    }
}
