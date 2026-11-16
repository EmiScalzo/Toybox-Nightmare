package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.model.*
import kotlin.math.*
import kotlin.random.Random

object InteractablesSystem {

    fun generateForRoom(
        roomType: RoomType,
        floorNumber: Int,
        obstacles: List<ObstacleRect>,
        objective: RoomObjectiveState?,
        allocId: () -> Long,
        rng: Random = Random.Default
    ): List<RoomInteractable> {
        if (roomType in setOf(RoomType.START, RoomType.TREASURE, RoomType.SHOP, RoomType.REPAIR, RoomType.MANUFACTURER)) {
            return emptyList()
        }
        if (roomType == RoomType.BOSS) {
            return listOf(
                placeSafe(InteractableType.WALL_LEVER, ARENA_W * 0.16f, ARENA_H * 0.5f, obstacles, allocId),
                placeSafe(InteractableType.HANGING_SHELF, ARENA_W * 0.5f, ARENA_H * 0.36f, obstacles, allocId)
            )
        }

        val list = mutableListOf<RoomInteractable>()

        if (objective?.type == ObjectiveType.DEFEAT_WITH_ENVIRONMENT) {
            list.add(placeSafe(InteractableType.ELECTRIC_TRAIN, ARENA_W * 0.5f, ARENA_H * 0.5f, obstacles, allocId, variant = rng.nextInt(3)))
            list.add(placeSafe(InteractableType.HANGING_SHELF, ARENA_W * 0.34f, ARENA_H * 0.38f, obstacles, allocId))
            list.add(placeSafe(InteractableType.PARTS_BARREL, ARENA_W * 0.68f, ARENA_H * 0.62f, obstacles, allocId))
            list.add(placeSafe(InteractableType.LIVE_WIRE, ARENA_W * 0.64f, ARENA_H * 0.34f, obstacles, allocId))
            return list
        }

        // 2-4 varied interactive toys/mechanisms per combat/challenge/curse/secret room
        val count = when (roomType) {
            RoomType.CHALLENGE, RoomType.CURSE, RoomType.FORBIDDEN -> 4
            RoomType.EVENT, RoomType.SECRET -> 2
            else -> 2 + rng.nextInt(2)
        }

        val spots = listOf(
            ARENA_W * 0.25f to ARENA_H * 0.28f,
            ARENA_W * 0.75f to ARENA_H * 0.28f,
            ARENA_W * 0.25f to ARENA_H * 0.72f,
            ARENA_W * 0.75f to ARENA_H * 0.72f,
            ARENA_W * 0.5f to ARENA_H * 0.30f,
            ARENA_W * 0.5f to ARENA_H * 0.70f
        ).shuffled(rng)

        val pools = listOf(
            listOf(InteractableType.WOOD_BLOCK, InteractableType.TOY_CART, InteractableType.PARTS_BARREL, InteractableType.CRATE_COVER, InteractableType.GIANT_DOLL_PROP, InteractableType.BATTERY_CUBE),
            listOf(InteractableType.ELECTRIC_TRAIN, InteractableType.TOY_CAROUSEL, InteractableType.WINDUP_JACK_TRAP, InteractableType.SPRING_LAUNCHER, InteractableType.CHRONO_CLOCK, InteractableType.PRIZE_MACHINE),
            listOf(InteractableType.HANGING_SHELF, InteractableType.GLASS_LAMP, InteractableType.GLASS_CASE, InteractableType.LOOT_CRATE, InteractableType.CRACKED_WALL),
            listOf(InteractableType.OIL_PUDDLE, InteractableType.LIVE_WIRE, InteractableType.STEAM_VENT, InteractableType.PRESSURE_TILE, InteractableType.FAN_BLOWER)
        )

        var usedTrainOrCarousel = false
        for (i in 0 until minOf(count, spots.size)) {
            val catPool = pools[(i + floorNumber) % pools.size]
            var picked = catPool.random(rng)
            if ((picked == InteractableType.ELECTRIC_TRAIN || picked == InteractableType.TOY_CAROUSEL) && usedTrainOrCarousel) {
                picked = InteractableType.WINDUP_JACK_TRAP
            }
            if (picked == InteractableType.ELECTRIC_TRAIN || picked == InteractableType.TOY_CAROUSEL) {
                usedTrainOrCarousel = true
            }
            val (sx, sy) = if (picked == InteractableType.ELECTRIC_TRAIN || picked == InteractableType.TOY_CAROUSEL) {
                ARENA_W * 0.5f to ARENA_H * 0.5f
            } else {
                spots[i]
            }
            val variant = when (picked) {
                InteractableType.ELECTRIC_TRAIN -> rng.nextInt(3)
                InteractableType.TOY_CAROUSEL -> rng.nextInt(2)
                InteractableType.PRIZE_MACHINE -> if (rng.nextFloat() < 0.7f) 0 else 1
                else -> 0
            }
            list.add(placeSafe(picked, sx, sy, obstacles, allocId, variant))
        }

        // Ensure any lever/wire/shelf gets a matching partner when needed
        if (list.any { it.type == InteractableType.LIVE_WIRE } && list.none { it.type == InteractableType.WALL_LEVER }) {
            list.add(placeSafe(InteractableType.WALL_LEVER, ARENA_W * 0.16f, ARENA_H * 0.5f, obstacles, allocId))
        }
        return list
    }

    fun placeSafe(
        type: InteractableType,
        x: Float,
        y: Float,
        obstacles: List<ObstacleRect>,
        allocId: () -> Long,
        variant: Int = 0
    ): RoomInteractable {
        val (rx, ry) = RoomMechanicSystem.clampToObstacles(
            x.coerceIn(WALL_THICKNESS + 44f, ARENA_W - WALL_THICKNESS - 44f),
            y.coerceIn(WALL_THICKNESS + 44f, ARENA_H - WALL_THICKNESS - 44f),
            34f,
            obstacles
        )
        val radius = when (type) {
            InteractableType.TOY_CAROUSEL -> 52f
            InteractableType.ELECTRIC_TRAIN -> 28f
            InteractableType.OIL_PUDDLE, InteractableType.STEAM_VENT -> 44f
            InteractableType.PRESSURE_TILE, InteractableType.COMPANION_CAGE -> 30f
            else -> 26f
        }
        return RoomInteractable(
            id = allocId(),
            type = type,
            x = rx,
            y = ry,
            radius = radius,
            variant = variant,
            hp = type.maxHp,
            maxHp = type.maxHp
        )
    }

    fun stepInteractables(
        state: GameRunState,
        dt: Float,
        damagePlayerFn: (GameRunState) -> GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        if (state.roomInteractables.isEmpty()) return state
        var s = state
        var enemies = s.enemies.toMutableList()
        var projectiles = s.projectiles.toMutableList()
        val pickups = s.pickups.toMutableList()
        val particles = s.particles.toMutableList()
        val updated = mutableListOf<RoomInteractable>()
        var banner = s.activeBanner
        var envKills = 0
        var stealth = s.stealthTimer
        var blackout = s.blackoutAlpha

        fun hurtEnemy(idx: Int, dmg: Float, stun: Float = 0f) {
            val e = enemies[idx]
            val wasAlive = e.hp > 0f
            val nhp = e.hp - dmg
            if (wasAlive && nhp <= 0f) envKills++
            enemies[idx] = e.copy(
                hp = nhp,
                hitFlashTimer = 0.16f,
                stunnedTimer = maxOf(e.stunnedTimer, stun),
                disguised = false
            )
        }

        for (item0 in s.roomInteractables) {
            var item = item0.copy(
                activeTimer = (item0.activeTimer - dt).coerceAtLeast(0f),
                cooldownTimer = (item0.cooldownTimer - dt).coerceAtLeast(0f),
                electrifiedTimer = (item0.electrifiedTimer - dt).coerceAtLeast(0f)
            )

            // Move sliding/rolling movables
            if (hypot(item.vx, item.vy) > 4f) {
                var nx = item.x + item.vx * dt
                var ny = item.y + item.vy * dt
                var vx = item.vx * (1f - 1.6f * dt).coerceAtLeast(0f)
                var vy = item.vy * (1f - 1.6f * dt).coerceAtLeast(0f)

                if (nx < WALL_THICKNESS + item.radius || nx > ARENA_W - WALL_THICKNESS - item.radius) {
                    vx = -vx * 0.6f
                    nx = nx.coerceIn(WALL_THICKNESS + item.radius, ARENA_W - WALL_THICKNESS - item.radius)
                }
                if (ny < WALL_THICKNESS + item.radius || ny > ARENA_H - WALL_THICKNESS - item.radius) {
                    vy = -vy * 0.6f
                    ny = ny.coerceIn(WALL_THICKNESS + item.radius, ARENA_H - WALL_THICKNESS - item.radius)
                }
                val (cx, cy) = RoomMechanicSystem.clampToObstacles(nx, ny, item.radius, s.currentRoom.obstacles)
                item = item.copy(x = cx, y = cy, vx = vx, vy = vy)

                // Moving TOY_CART or PARTS_BARREL hits enemies!
                if (item.type == InteractableType.TOY_CART || item.type == InteractableType.PARTS_BARREL) {
                    for (i in enemies.indices) {
                        val e = enemies[i]
                        if (hypot(e.x - item.x, e.y - item.y) <= e.radius + item.radius) {
                            hurtEnemy(i, item.type.damage, stun = 0.7f)
                            item = item.copy(vx = -item.vx * 0.35f, vy = -item.vy * 0.35f)
                            sfx.play(ToySfx.HIT_ENEMY)
                            onDiscovery("interactable", item.type.name)
                        }
                    }
                }
            }

            // Player pushing movable items
            if (item.type.category == InteractableCategory.MOVABLE && !item.destroyed) {
                val dx = item.x - s.playerX
                val dy = item.y - s.playerY
                val d = hypot(dx, dy).coerceAtLeast(1f)
                val minSep = item.radius + 20f
                if (d < minSep) {
                    onDiscovery("interactable", item.type.name)
                    val ux = dx / d
                    val uy = dy / d
                    val pushSpeed = when (item.type) {
                        InteractableType.TOY_CART -> 460f
                        InteractableType.PARTS_BARREL -> 340f
                        InteractableType.GIANT_DOLL_PROP -> 90f
                        else -> 160f
                    }
                    item = item.copy(
                        x = (item.x + ux * (minSep - d)).coerceIn(WALL_THICKNESS + item.radius, ARENA_W - WALL_THICKNESS - item.radius),
                        y = (item.y + uy * (minSep - d)).coerceIn(WALL_THICKNESS + item.radius, ARENA_H - WALL_THICKNESS - item.radius),
                        vx = ux * pushSpeed,
                        vy = uy * pushSpeed
                    )
                }
            }

            // Cover blocking enemy bullets (Wood Block, Crate Cover, Giant Doll Prop, Fallen Shelf)
            if ((item.type in setOf(
                    InteractableType.WOOD_BLOCK, InteractableType.CRATE_COVER,
                    InteractableType.GIANT_DOLL_PROP, InteractableType.HANGING_SHELF
                ) && !item.destroyed) || (item.type == InteractableType.HANGING_SHELF && item.triggered)
            ) {
                projectiles = projectiles.filter { p ->
                    val hit = !p.fromPlayer && hypot(p.x - item.x, p.y - item.y) <= item.radius + p.radius
                    if (hit && item.type == InteractableType.CRATE_COVER) {
                        item = item.copy(hp = item.hp - 6f)
                        if (item.hp <= 0f) item = item.copy(destroyed = true)
                    }
                    !hit
                }.toMutableList()
            }

            when (item.type) {
                InteractableType.ELECTRIC_TRAIN -> {
                    if (item.active && item.activeTimer > 0f) {
                        val speed = if (item.variant == 1) 3.1f else 1.85f
                        val newAng = item.trainAngle + speed * dt
                        val trackRx = 235f
                        val trackRy = 210f
                        val tx = ARENA_W * 0.5f + cos(newAng) * trackRx
                        val ty = ARENA_H * 0.5f + sin(newAng) * trackRy
                        item = item.copy(trainAngle = newAng, x = tx, y = ty)
                        if (item.cooldownTimer <= 0f) {
                            for (i in enemies.indices) {
                                val e = enemies[i]
                                if (hypot(e.x - tx, e.y - ty) <= e.radius + 28f) {
                                    hurtEnemy(i, item.type.damage, stun = 0.6f)
                                    sfx.play(ToySfx.HIT_ENEMY)
                                }
                            }
                            if (hypot(s.playerX - tx, s.playerY - ty) <= 44f) {
                                s = damagePlayerFn(s)
                            }
                            if (item.variant == 2 && item.activeTimer < 0.2f) {
                                // Explosive train variant final burst
                                s = s.copy(bombs = s.bombs + HazardBomb(allocId(), tx, ty, fuseSec = 0.05f, fromPlayer = true, radius = 140f, damage = 32f))
                            }
                            item = item.copy(cooldownTimer = 0.25f)
                        }
                    } else if (item.active && item.activeTimer <= 0f) {
                        item = item.copy(active = false)
                    }
                }

                InteractableType.TOY_CAROUSEL -> {
                    if (item.active && item.activeTimer > 0f) {
                        val reach = 135f
                        if (item.cooldownTimer <= 0f) {
                            for (i in enemies.indices) {
                                val e = enemies[i]
                                val d = hypot(e.x - item.x, e.y - item.y).coerceAtLeast(1f)
                                if (d <= reach) {
                                    val pushSign = if (item.variant == 1) -1f else 1f
                                    hurtEnemy(i, item.type.damage)
                                    enemies[i] = enemies[i].copy(
                                        x = (e.x + pushSign * (e.x - item.x) / d * 24f).coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f),
                                        y = (e.y + pushSign * (e.y - item.y) / d * 24f).coerceIn(WALL_THICKNESS + 24f, ARENA_H - WALL_THICKNESS - 24f)
                                    )
                                }
                            }
                            if (hypot(s.playerX - item.x, s.playerY - item.y) <= reach * 0.72f) {
                                s = damagePlayerFn(s)
                            }
                            item = item.copy(cooldownTimer = 0.45f)
                        }
                    } else if (item.active) {
                        item = item.copy(active = false)
                    }
                }

                InteractableType.CHRONO_CLOCK -> {
                    if (item.active && item.activeTimer > 0f) {
                        for (i in enemies.indices) {
                            val e = enemies[i]
                            if (!e.type.isBoss && e.stunnedTimer < 0.1f) {
                                enemies[i] = e.copy(
                                    x = e.x - (e.vx * 0.5f * dt),
                                    y = e.y - (e.vy * 0.5f * dt),
                                    attackTimer = e.attackTimer + dt * 0.5f
                                )
                            }
                        }
                    } else if (item.active) {
                        item = item.copy(active = false)
                    }
                }

                InteractableType.GLASS_CASE -> {
                    if (item.destroyed && item.triggered && item.cooldownTimer <= 0f) {
                        for (i in enemies.indices) {
                            if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= 44f) {
                                hurtEnemy(i, 10f)
                            }
                        }
                        if (hypot(s.playerX - item.x, s.playerY - item.y) <= 34f && s.dodgeTimer <= 0f) {
                            s = damagePlayerFn(s)
                        }
                        item = item.copy(cooldownTimer = 0.75f)
                    }
                }

                InteractableType.OIL_PUDDLE -> {
                    if (hypot(s.playerX - item.x, s.playerY - item.y) <= item.radius) {
                        s = s.copy(
                            playerX = (s.playerX + s.playerVx * 0.35f * dt).coerceIn(WALL_THICKNESS + 22f, ARENA_W - WALL_THICKNESS - 22f),
                            playerY = (s.playerY + s.playerVy * 0.35f * dt).coerceIn(WALL_THICKNESS + 22f, ARENA_H - WALL_THICKNESS - 22f)
                        )
                    }
                    // Electrified oil if live wire active nearby or battery cube nearby
                    val nearLiveWire = s.roomInteractables.any {
                        it.id != item.id && (it.type == InteractableType.LIVE_WIRE && (it.active || it.electrifiedTimer > 0f) ||
                            (it.type == InteractableType.BATTERY_CUBE && hypot(it.x - item.x, it.y - item.y) <= 130f))
                    }
                    if (nearLiveWire && item.cooldownTimer <= 0f) {
                        item = item.copy(electrifiedTimer = 0.4f, cooldownTimer = 0.65f)
                        for (i in enemies.indices) {
                            if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= item.radius + 20f) {
                                hurtEnemy(i, 14f, stun = 0.5f)
                            }
                        }
                        if (hypot(s.playerX - item.x, s.playerY - item.y) <= item.radius) {
                            s = damagePlayerFn(s)
                        }
                    }
                }

                InteractableType.LIVE_WIRE -> {
                    val nearBattery = s.roomInteractables.any { it.type == InteractableType.BATTERY_CUBE && hypot(it.x - item.x, it.y - item.y) <= 135f }
                    val reach = if (nearBattery) 125f else 72f
                    if ((item.active && item.activeTimer > 0f || nearBattery) && item.cooldownTimer <= 0f) {
                        for (i in enemies.indices) {
                            if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= reach) {
                                hurtEnemy(i, item.type.damage, stun = 0.8f)
                            }
                        }
                        if (hypot(s.playerX - item.x, s.playerY - item.y) <= reach * 0.78f) {
                            s = damagePlayerFn(s)
                        }
                        item = item.copy(cooldownTimer = 0.7f)
                    } else if (item.active && item.activeTimer <= 0f) {
                        item = item.copy(active = false)
                    }
                }

                InteractableType.STEAM_VENT -> {
                    if (item.cooldownTimer <= 0f) {
                        item = item.copy(active = !item.active, activeTimer = 3.2f, cooldownTimer = 4.5f)
                    }
                    if (item.active && hypot(s.playerX - item.x, s.playerY - item.y) <= item.radius + 12f) {
                        stealth = maxOf(stealth, 1.4f)
                    }
                }

                InteractableType.PRESSURE_TILE -> {
                    val pressedByPlayer = hypot(s.playerX - item.x, s.playerY - item.y) <= item.radius
                    val pressedByBox = s.roomInteractables.any {
                        it.id != item.id && it.type.category == InteractableCategory.MOVABLE && hypot(it.x - item.x, it.y - item.y) <= item.radius + 10f
                    }
                    if ((pressedByPlayer || pressedByBox) && !item.triggered) {
                        item = item.copy(triggered = true, active = true)
                        onDiscovery("interactable", item.type.name)
                        sfx.play(ToySfx.DOOR_OPEN)
                        banner = RunUiBanner("Baldosa Activada", "Una trampa oculta del taller responde al peso.", 0xFFC89948, 2.0f)
                        for (i in enemies.indices) {
                            if (hypot(enemies[i].x - s.playerX, enemies[i].y - s.playerY) <= 220f) {
                                hurtEnemy(i, 16f, stun = 1.0f)
                            }
                        }
                    }
                }

                InteractableType.FAN_BLOWER -> {
                    // Gently deflects projectiles passing nearby
                    projectiles = projectiles.map { p ->
                        if (hypot(p.x - item.x, p.y - item.y) <= 130f) {
                            p.copy(vx = p.vx + 160f * dt, vy = p.vy - 95f * dt)
                        } else p
                    }.toMutableList()
                }

                InteractableType.GLASS_LAMP -> {
                    if (item.destroyed && item.activeTimer > 0f) {
                        blackout = maxOf(blackout, 0.35f)
                    }
                }

                else -> Unit
            }

            updated.add(item)
        }

        var objective = s.roomObjective
        var events = s.recentEngineEvents
        if (envKills > 0) {
            if (objective != null && objective.type == ObjectiveType.DEFEAT_WITH_ENVIRONMENT && !objective.completed) {
                val nc = objective.currentCount + envKills
                objective = objective.copy(currentCount = nc)
                if (nc >= objective.targetCount) {
                    objective = objective.copy(completed = true)
                    events = (events + EngineEventType.ROOM_OBJECTIVE_COMPLETED.key).takeLast(12)
                    sfx.play(ToySfx.SYNERGY)
                    banner = RunUiBanner("¡Ingenio del Taller!", "Objetivo ambiental completado.", 0xFF597354, 2.5f)
                    pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, s.playerX, (s.playerY - 40f).coerceAtLeast(WALL_THICKNESS + 40f)))
                }
            }
        }

        return s.copy(
            roomInteractables = updated,
            enemies = enemies,
            projectiles = projectiles,
            pickups = pickups,
            particles = particles,
            stealthTimer = stealth,
            blackoutAlpha = blackout,
            activeBanner = banner,
            roomObjective = objective,
            recentEngineEvents = events
        )
    }

    fun applyProjectileToInteractables(
        state: GameRunState,
        proj: ActiveProjectile,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): Pair<Boolean, GameRunState> {
        if (state.roomInteractables.isEmpty()) return false to state
        for (idx in state.roomInteractables.indices) {
            val item = state.roomInteractables[idx]
            if (item.destroyed && item.type != InteractableType.GLASS_CASE) continue
            if (item.type.category == InteractableCategory.AMBIENT) continue
            if (hypot(proj.x - item.x, proj.y - item.y) <= item.radius + proj.radius) {
                // Shooting a lever, hanging shelf, destructible, or barrel triggers it!
                if (item.type == InteractableType.PARTS_BARREL && proj.fromPlayer) {
                    val speed = hypot(proj.vx, proj.vy).coerceAtLeast(1f)
                    val list = state.roomInteractables.toMutableList()
                    list[idx] = item.copy(vx = (proj.vx / speed) * 380f, vy = (proj.vy / speed) * 380f)
                    onDiscovery("interactable", item.type.name)
                    return true to state.copy(roomInteractables = list)
                }
                if (item.type == InteractableType.WALL_LEVER && proj.fromPlayer) {
                    return true to activateInteractable(state, item.id, allocId, sfx, onDiscovery)
                }
                if (item.type.destructible && !item.destroyed && proj.fromPlayer) {
                    onDiscovery("interactable", item.type.name)
                    val nhp = item.hp - proj.damage
                    if (nhp <= 0f) {
                        return true to destroyInteractable(state, idx, allocId, sfx)
                    } else {
                        sfx.play(ToySfx.HIT_ENEMY)
                        val list = state.roomInteractables.toMutableList()
                        list[idx] = item.copy(hp = nhp)
                        return true to state.copy(roomInteractables = list)
                    }
                }
                if (item.type in setOf(InteractableType.WOOD_BLOCK, InteractableType.GIANT_DOLL_PROP)) {
                    return true to state
                }
            }
        }
        return false to state
    }

    private fun destroyInteractable(
        state: GameRunState,
        idx: Int,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): GameRunState {
        val list = state.roomInteractables.toMutableList()
        val item = list[idx]
        var enemies = state.enemies.toMutableList()
        var pickups = state.pickups.toMutableList()
        var banner = state.activeBanner
        var events = (state.recentEngineEvents + EngineEventType.OBJECT_DESTROYED.key).takeLast(12)

        when (item.type) {
            InteractableType.HANGING_SHELF -> {
                sfx.play(ToySfx.EXPLOSION)
                list[idx] = item.copy(destroyed = true, triggered = true)
                for (i in enemies.indices) {
                    if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= 105f) {
                        enemies[i] = enemies[i].copy(hp = enemies[i].hp - item.type.damage, stunnedTimer = 1.2f, hitFlashTimer = 0.25f)
                    }
                }
                banner = RunUiBanner("¡Estantería Derribada!", "Cayó con fuerza aplastando la zona.", 0xFFD98A2B, 2.2f)
            }
            InteractableType.GLASS_LAMP -> {
                sfx.play(ToySfx.HIT_ENEMY)
                list[idx] = item.copy(destroyed = true, activeTimer = 4.0f)
                for (i in enemies.indices) {
                    if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= 130f) {
                        enemies[i] = enemies[i].copy(hp = enemies[i].hp - 8f, stunnedTimer = 1.6f)
                    }
                }
            }
            InteractableType.GLASS_CASE -> {
                sfx.play(ToySfx.HIT_ENEMY)
                list[idx] = item.copy(destroyed = true, triggered = true)
            }
            InteractableType.LOOT_CRATE -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                list[idx] = item.copy(destroyed = true)
                val roll = Random.nextFloat()
                when {
                    state.backpackTier == null && roll < 0.22f -> {
                        pickups.add(
                            GroundPickup(
                                allocId(),
                                PickupKind.BACKPACK_TIER,
                                item.x,
                                item.y,
                                valueStr = BackpackTier.STITCHED_POUCH.name
                            )
                        )
                        banner = RunUiBanner("¡Bolsillo Encontrado!", "Entre los restos sellados había una bolsa cosida.", 0xFFC89948, 2.4f)
                    }
                    roll < 0.36f -> pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, item.x, item.y, valueStr = RepairMaterial.entries.random().name))
                    roll < 0.64f -> pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, item.x, item.y, amount = 2))
                    roll < 0.85f -> pickups.add(GroundPickup(allocId(), PickupKind.BACKPACK_ITEM, item.x, item.y, valueStr = BackpackSystem.randomDropKind().name))
                    else -> {
                        val sp = EnemyType.CLOCKWORK_SPIDER
                        enemies.add(ActiveEnemy(allocId(), sp, item.x, item.y, hp = sp.maxHp * state.difficulty.enemyHpMult, maxHp = sp.maxHp * state.difficulty.enemyHpMult, radius = 19f))
                        banner = RunUiBanner("¡Caja Sorpresa Viva!", "Una araña de cuerda saltó del interior.", 0xFFD13B42, 2.0f)
                    }
                }
            }
            InteractableType.CRACKED_WALL -> {
                sfx.play(ToySfx.EXPLOSION)
                list[idx] = item.copy(destroyed = true)
                pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, item.x, item.y, valueStr = RepairMaterial.GEAR.name))
                pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, item.x + 16f, item.y, amount = 2))
            }
            InteractableType.COMPANION_CAGE -> {
                sfx.play(ToySfx.EXPLOSION)
                list[idx] = item.copy(destroyed = true, triggered = true)
                val rescued = CompanionId.entries[item.variant % CompanionId.entries.size]
                pickups.add(
                    GroundPickup(
                        id = allocId(),
                        kind = PickupKind.COMPANION_SHRINE,
                        x = item.x,
                        y = item.y,
                        valueStr = rescued.name
                    )
                )
                enemies = enemies.map { e ->
                    if (e.protectedCageId == item.id) e.copy(enraged = true, protectedCageId = null) else e
                }.toMutableList()
                banner = RunUiBanner(
                    "¡Jaula Destruida!",
                    "Liberaste a ${rescued.displayName}. Acércate para que se una.",
                    0xFF597354,
                    2.8f
                )
            }
            else -> {
                list[idx] = item.copy(destroyed = true)
            }
        }

        return state.copy(
            roomInteractables = list,
            enemies = enemies,
            pickups = pickups,
            activeBanner = banner,
            recentEngineEvents = events
        )
    }

    fun onExplosionInteractables(state: GameRunState, bx: Float, by: Float, radius: Float, allocId: () -> Long, sfx: SoundEffectsEngine): GameRunState {
        var s = state
        for (i in s.roomInteractables.indices) {
            val it = s.roomInteractables[i]
            if (!it.destroyed && it.type.destructible && hypot(it.x - bx, it.y - by) <= radius + it.radius) {
                s = destroyInteractable(s, i, allocId, sfx)
            }
        }
        return s
    }

    fun findNearbyInteractablePrompt(state: GameRunState): Pair<RoomInteractable, String>? {
        val near = state.roomInteractables.filter {
            !it.destroyed &&
                !(it.type == InteractableType.PRIZE_MACHINE && it.triggered) &&
                it.type.category == InteractableCategory.ACTIVATABLE &&
                hypot(it.x - state.playerX, it.y - state.playerY) <= 68f
        }.minByOrNull { hypot(it.x - state.playerX, it.y - state.playerY) } ?: return null
        val verb = when (near.type) {
            InteractableType.ELECTRIC_TRAIN -> "Activar Tren"
            InteractableType.TOY_CAROUSEL -> "Activar Carrusel"
            InteractableType.WINDUP_JACK_TRAP -> "Dar Cuerda"
            InteractableType.SPRING_LAUNCHER -> "Disparar Resortes"
            InteractableType.WALL_LEVER -> "Accionar Palanca"
            InteractableType.CHRONO_CLOCK -> "Activar Reloj"
            InteractableType.PRIZE_MACHINE -> "Usar Máquina (2 ●)"
            else -> "Interactuar"
        }
        return near to verb
    }

    fun activateInteractable(
        state: GameRunState,
        targetId: Long,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        val idx = state.roomInteractables.indexOfFirst { it.id == targetId }
        if (idx == -1) return state
        val item = state.roomInteractables[idx]
        if (item.cooldownTimer > 0f && item.type != InteractableType.WALL_LEVER) return state
        onDiscovery("interactable", item.type.name)

        var s = state
        val list = s.roomInteractables.toMutableList()
        var pickups = s.pickups.toMutableList()
        var enemies = s.enemies.toMutableList()
        var bombs = s.bombs.toMutableList()
        var projs = s.projectiles.toMutableList()
        var banner = s.activeBanner
        var buttons = s.buttons

        when (item.type) {
            InteractableType.ELECTRIC_TRAIN -> {
                sfx.play(ToySfx.SYNERGY)
                list[idx] = item.copy(active = true, activeTimer = 7.5f, cooldownTimer = 0.1f)
                banner = RunUiBanner("¡Tren Eléctrico en Marcha!", "Atrae a los enemigos hacia la vía.", 0xFFC89948, 2.2f)
            }
            InteractableType.TOY_CAROUSEL -> {
                sfx.play(ToySfx.SYNERGY)
                list[idx] = item.copy(active = true, activeTimer = 6.5f, cooldownTimer = 0.2f)
                banner = RunUiBanner("Carrusel Girando", "Empuja y golpea a todo juguete cercano.", 0xFFC89948, 2.2f)
            }
            InteractableType.WINDUP_JACK_TRAP -> {
                sfx.play(ToySfx.EXPLOSION)
                list[idx] = item.copy(active = true, cooldownTimer = 7f)
                for (i in enemies.indices) {
                    if (hypot(enemies[i].x - item.x, enemies[i].y - item.y) <= 165f) {
                        enemies[i] = enemies[i].copy(hp = enemies[i].hp - item.type.damage, stunnedTimer = 2.0f, hitFlashTimer = 0.2f)
                    }
                }
                banner = RunUiBanner("¡Caja Sorpresa Detonada!", "Resorte aturdidor activado.", 0xFFD98A2B, 2.0f)
            }
            InteractableType.SPRING_LAUNCHER -> {
                sfx.play(ToySfx.SHOOT)
                list[idx] = item.copy(cooldownTimer = 4.5f)
                for (k in 0 until 6) {
                    val ang = (k / 6f) * (2f * PI.toFloat())
                    projs.add(
                        ActiveProjectile(
                            id = allocId(),
                            x = item.x,
                            y = item.y,
                            vx = cos(ang) * 480f,
                            vy = sin(ang) * 480f,
                            radius = 9f,
                            damage = 16f,
                            fromPlayer = true,
                            colorHex = 0xFFC89948,
                            style = AttackStyle.GEAR_PROJECTILE
                        )
                    )
                }
            }
            InteractableType.WALL_LEVER -> {
                sfx.play(ToySfx.DOOR_OPEN)
                list[idx] = item.copy(triggered = !item.triggered, cooldownTimer = 2.0f)
                for (j in list.indices) {
                    val other = list[j]
                    if (other.type == InteractableType.LIVE_WIRE) {
                        list[j] = other.copy(active = true, activeTimer = 5.5f)
                    } else if (other.type == InteractableType.HANGING_SHELF && !other.destroyed) {
                        s = s.copy(roomInteractables = list)
                        return destroyInteractable(s, j, allocId, sfx)
                    } else if (other.type == InteractableType.ELECTRIC_TRAIN) {
                        list[j] = other.copy(active = true, activeTimer = 6.5f)
                    }
                }
                banner = RunUiBanner("Palanca Accionada", "Los circuitos de la sala cobraron vida.", 0xFFC89948, 2.0f)
            }
            InteractableType.CHRONO_CLOCK -> {
                sfx.play(ToySfx.SYNERGY)
                list[idx] = item.copy(active = true, activeTimer = 6.0f, cooldownTimer = 10f)
                banner = RunUiBanner("Reloj Alterado", "El tiempo se ralentiza para los enemigos (6s).", 0xFF4C5F6E, 2.2f)
            }
            InteractableType.PRIZE_MACHINE -> {
                if (item.triggered) return s
                if (buttons < 2) {
                    return s.copy(activeBanner = RunUiBanner("Faltan Botones", "Necesitas 2 ● para activar la máquina.", 0xFFD98A2B, 1.8f))
                }
                buttons -= 2
                list[idx] = item.copy(triggered = true)
                if (item.variant == 0) {
                    sfx.play(ToySfx.PICKUP_ITEM)
                    pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, item.x, item.y + 34f, valueStr = RepairMaterial.entries.random().name))
                    pickups.add(GroundPickup(allocId(), PickupKind.CONSUMABLE, item.x + 24f, item.y + 34f, valueStr = ConsumableType.entries.random().name))
                    banner = RunUiBanner("¡Premio de Taller!", "Material y consumible obtenidos.", 0xFF597354, 2.2f)
                } else {
                    sfx.play(ToySfx.EXPLOSION)
                    bombs.add(HazardBomb(allocId(), item.x, item.y + 30f, fuseSec = 0.85f, fromPlayer = false, radius = 120f, damage = 24f))
                    banner = RunUiBanner("¡Máquina Averiada!", "Suelta una bomba defectuosa. ¡Aléjate!", 0xFFD13B42, 2.4f)
                }
            }
            else -> Unit
        }

        return s.copy(
            roomInteractables = list,
            pickups = pickups,
            enemies = enemies,
            bombs = bombs,
            projectiles = projs,
            buttons = buttons,
            activeBanner = banner,
            recentEngineEvents = (s.recentEngineEvents + EngineEventType.INTERACTABLE_ACTIVATED.key).takeLast(12)
        )
    }
}
