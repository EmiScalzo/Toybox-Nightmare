package com.example.engine

import com.example.data.model.Direction
import com.example.data.model.EnemyType
import com.example.data.model.RoomMechanic
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object RoomMechanicSystem {

    fun initForRoom(state: GameRunState, room: RoomBlueprint): GameRunState {
        val sectors = if (room.mechanic == RoomMechanic.VANISHING_FLOOR) {
            buildList {
                var idx = 0
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        add(
                            VanishingSector(
                                index = idx++,
                                col = c,
                                row = r,
                                state = SectorState.STABLE,
                                timer = 1.6f + ((c + r) % 3) * 0.9f
                            )
                        )
                    }
                }
            }
        } else emptyList()

        val initialTilt = if (room.mechanic == RoomMechanic.TILTED_FLOOR) 0.85f to 0.35f else 0f to 0f

        return state.copy(
            tiltX = initialTilt.first,
            tiltY = initialTilt.second,
            tiltTimer = 4.5f,
            vanishingSectors = sectors,
            doorShiftTimer = 5.5f,
            doorMapping = Direction.entries.associateWith { it },
            blackoutAlpha = 0f
        )
    }

    fun stepMechanics(
        state: GameRunState,
        dt: Float,
        damagePlayerFn: (GameRunState) -> GameRunState
    ): GameRunState {
        val mechanic = state.currentRoom.mechanic
        if (mechanic == RoomMechanic.NONE) return state

        var s = state
        when (mechanic) {
            RoomMechanic.TILTED_FLOOR -> {
                s = stepTiltedFloor(s, dt)
            }
            RoomMechanic.VANISHING_FLOOR -> {
                s = stepVanishingFloor(s, dt, damagePlayerFn)
            }
            RoomMechanic.SHIFTING_DOORS -> {
                val nextT = s.doorShiftTimer - dt
                if (nextT <= 0f) {
                    val rotated = s.doorMapping.mapKeys { (physWall, _) -> rotateClockwise(physWall) }
                    s = s.copy(
                        doorMapping = rotated,
                        doorShiftTimer = 5.5f,
                        activeBanner = if (s.activeBanner == null) {
                            RunUiBanner("Puertas Cambiantes", "Los marcos de salida han rotado de muro.", 0xFFC89948, 1.8f)
                        } else s.activeBanner
                    )
                } else {
                    s = s.copy(doorShiftTimer = nextT)
                }
            }
            RoomMechanic.FLICKERING_LIGHTS -> {
                val wave = sin(s.elapsedSeconds * 2.1f) * 0.5f + 0.5f
                val pulse = if (wave > 0.72f) ((wave - 0.72f) / 0.28f) * 0.62f else 0.08f
                s = s.copy(blackoutAlpha = pulse)
            }
            RoomMechanic.SPINNING_CAROUSEL -> {
                s = stepCarouselSwirl(s, dt)
            }
            else -> Unit
        }
        return s
    }

    private fun rotateClockwise(dir: Direction): Direction = when (dir) {
        Direction.NORTH -> Direction.EAST
        Direction.EAST -> Direction.SOUTH
        Direction.SOUTH -> Direction.WEST
        Direction.WEST -> Direction.NORTH
    }

    private fun stepTiltedFloor(state: GameRunState, dt: Float): GameRunState {
        var tx = state.tiltX
        var ty = state.tiltY
        var timer = state.tiltTimer - dt
        if (timer <= 0f) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            tx = cos(angle)
            ty = sin(angle)
            timer = 4.8f
        }
        val slidePlayer = 92f * dt
        val slideObj = 78f * dt
        val playerR = 22f
        val (nx, ny) = clampToObstacles(
            (state.playerX + tx * slidePlayer).coerceIn(WALL_THICKNESS + playerR, ARENA_W - WALL_THICKNESS - playerR),
            (state.playerY + ty * slidePlayer).coerceIn(WALL_THICKNESS + playerR, ARENA_H - WALL_THICKNESS - playerR),
            playerR,
            state.currentRoom.obstacles
        )
        val enemies = state.enemies.map { e ->
            if (e.type.isBoss || e.stunnedTimer > 0f) e
            else e.copy(
                x = (e.x + tx * slideObj * 0.7f).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius),
                y = (e.y + ty * slideObj * 0.7f).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
            )
        }
        val pickups = state.pickups.map { p ->
            if (p.kind == PickupKind.NEW_PART_PEDESTAL || p.kind == PickupKind.PASSIVE_ITEM_PEDESTAL || p.kind == PickupKind.COMPANION_SHRINE) p
            else p.copy(
                x = (p.x + tx * slideObj).coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f),
                y = (p.y + ty * slideObj).coerceIn(WALL_THICKNESS + 24f, ARENA_H - WALL_THICKNESS - 24f)
            )
        }
        val bombs = state.bombs.map { b ->
            b.copy(
                x = (b.x + tx * slideObj * 1.15f).coerceIn(WALL_THICKNESS + 20f, ARENA_W - WALL_THICKNESS - 20f),
                y = (b.y + ty * slideObj * 1.15f).coerceIn(WALL_THICKNESS + 20f, ARENA_H - WALL_THICKNESS - 20f)
            )
        }
        return state.copy(
            tiltX = tx,
            tiltY = ty,
            tiltTimer = timer,
            playerX = nx,
            playerY = ny,
            enemies = enemies,
            pickups = pickups,
            bombs = bombs
        )
    }

    private fun stepVanishingFloor(
        state: GameRunState,
        dt: Float,
        damagePlayerFn: (GameRunState) -> GameRunState
    ): GameRunState {
        if (state.vanishingSectors.isEmpty()) return state
        val curHazardCount = state.vanishingSectors.count { it.state != SectorState.STABLE }
        var hazards = curHazardCount
        val nextSectors = state.vanishingSectors.map { sec ->
            val nt = sec.timer - dt
            when (sec.state) {
                SectorState.STABLE -> {
                    if (nt <= 0f && hazards < 3) {
                        hazards++
                        sec.copy(state = SectorState.WARNING, timer = 1.75f)
                    } else if (nt <= 0f) {
                        sec.copy(timer = 1.2f)
                    } else {
                        sec.copy(timer = nt)
                    }
                }
                SectorState.WARNING -> {
                    if (nt <= 0f) sec.copy(state = SectorState.GONE, timer = 2.6f)
                    else sec.copy(timer = nt)
                }
                SectorState.GONE -> {
                    if (nt <= 0f) sec.copy(state = SectorState.STABLE, timer = 3.0f + Random.nextFloat() * 1.8f)
                    else sec.copy(timer = nt)
                }
            }
        }

        var s = state.copy(vanishingSectors = nextSectors)
        if (s.dodgeTimer <= 0f && s.invulnTimer <= 0f) {
            val fellIn = nextSectors.firstOrNull { sec ->
                sec.state == SectorState.GONE && isPointInSector(s.playerX, s.playerY, sec)
            }
            if (fellIn != null) {
                s = damagePlayerFn(s)
                // Gently rescue player toward center-safe board edge
                val safeTargetX = if (s.playerX < ARENA_W / 2f) WALL_THICKNESS + 40f else ARENA_W - WALL_THICKNESS - 40f
                s = s.copy(
                    playerX = (s.playerX * 0.7f + safeTargetX * 0.3f),
                    activeBanner = RunUiBanner("¡Suelo Caído!", "Evita los sectores agrietados al desaparecer.", 0xFFD13B42, 2.0f)
                )
            }
        }
        return s
    }

    private fun stepCarouselSwirl(state: GameRunState, dt: Float): GameRunState {
        val cx = ARENA_W * 0.5f
        val cy = ARENA_H * 0.5f
        val dx = state.playerX - cx
        val dy = state.playerY - cy
        val d = hypot(dx, dy).coerceAtLeast(1f)
        val swirl = 58f * dt
        val tx = -dy / d
        val ty = dx / d
        val nx = (state.playerX + tx * swirl).coerceIn(WALL_THICKNESS + 22f, ARENA_W - WALL_THICKNESS - 22f)
        val ny = (state.playerY + ty * swirl).coerceIn(WALL_THICKNESS + 22f, ARENA_H - WALL_THICKNESS - 22f)
        return state.copy(playerX = nx, playerY = ny)
    }

    fun sectorBounds(sec: VanishingSector): FloatArray {
        val marginX = WALL_THICKNESS + 68f
        val marginY = WALL_THICKNESS + 68f
        val gridW = (ARENA_W - marginX * 2f) / 3f
        val gridH = (ARENA_H - marginY * 2f) / 3f
        val x0 = marginX + sec.col * gridW + 6f
        val y0 = marginY + sec.row * gridH + 6f
        return floatArrayOf(x0, y0, gridW - 12f, gridH - 12f)
    }

    fun isPointInSector(px: Float, py: Float, sec: VanishingSector): Boolean {
        val b = sectorBounds(sec)
        return px in b[0]..(b[0] + b[2]) && py in b[1]..(b[1] + b[3])
    }

    fun isWeepingToyWatched(
        enemyX: Float,
        enemyY: Float,
        playerX: Float,
        playerY: Float,
        facingAngle: Float
    ): Boolean {
        val dx = enemyX - playerX
        val dy = enemyY - playerY
        val d = hypot(dx, dy)
        if (d < 1f || d > 620f) return false
        val lookX = cos(facingAngle)
        val lookY = sin(facingAngle)
        val dot = (dx / d) * lookX + (dy / d) * lookY
        return dot >= 0.42f // ~65 degree gaze cone!
    }

    fun mirrorShotsForCopies(
        state: GameRunState,
        playerProjectilesSpawned: List<ActiveProjectile>,
        allocId: () -> Long
    ): List<ActiveProjectile> {
        val copies = state.enemies.filter { it.type == EnemyType.MIRROR_COPY && it.stunnedTimer <= 0f }
        if (copies.isEmpty() || playerProjectilesSpawned.isEmpty()) return emptyList()
        val firstShot = playerProjectilesSpawned.first()
        return copies.mapNotNull { copy ->
            if (Random.nextFloat() < 0.72f) {
                ActiveProjectile(
                    id = allocId(),
                    x = copy.x,
                    y = copy.y,
                    vx = -firstShot.vx * 0.62f,
                    vy = -firstShot.vy * 0.62f,
                    radius = 8.5f,
                    damage = 1f,
                    fromPlayer = false,
                    colorHex = 0xFFE8DCC4
                )
            } else null
        }
    }

    fun clampToObstacles(
        x: Float,
        y: Float,
        radius: Float,
        obstacles: List<ObstacleRect>
    ): Pair<Float, Float> {
        var nx = x
        var ny = y
        for (obs in obstacles) {
            val ox = obs.x * ARENA_W
            val oy = obs.y * ARENA_H
            val ow = obs.w * ARENA_W
            val oh = obs.h * ARENA_H
            val closestX = nx.coerceIn(ox, ox + ow)
            val closestY = ny.coerceIn(oy, oy + oh)
            val dx = nx - closestX
            val dy = ny - closestY
            val dist = hypot(dx, dy)
            if (dist < radius) {
                if (dist > 0.001f) {
                    val push = radius - dist
                    nx += (dx / dist) * push
                    ny += (dy / dist) * push
                } else {
                    ny = oy - radius
                }
            }
        }
        return nx to ny
    }
}
