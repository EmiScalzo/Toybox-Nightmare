package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.GameCatalog
import com.example.data.model.*
import kotlin.math.*
import kotlin.random.Random

object RoomObjectiveSystem {

    fun maybeGenerateObjective(
        roomType: RoomType,
        mechanic: RoomMechanic,
        floorNumber: Int,
        rng: Random = Random.Default
    ): RoomObjectiveState? {
        if (roomType !in setOf(RoomType.COMBAT, RoomType.CHALLENGE)) return null
        // ~38% of combat/challenge rooms feature a clear alternative objective
        if (mechanic == RoomMechanic.NURSERY_MAZE && rng.nextFloat() < 0.7f) return null
        val roll = rng.nextFloat()
        if (roll > 0.38f + (floorNumber - 1) * 0.03f) return null

        val picked = ObjectiveType.entries.random(rng)
        return when (picked) {
            ObjectiveType.SURVIVE_TIME -> RoomObjectiveState(
                type = picked,
                targetCount = 1,
                timerSec = 15f,
                maxTimerSec = 15f
            )
            ObjectiveType.LAST_CHAIR_SAFE_ZONE -> RoomObjectiveState(
                type = picked,
                targetCount = 3,
                currentCount = 0,
                timerSec = 18f,
                maxTimerSec = 18f,
                safeZoneX = ARENA_W * 0.5f,
                safeZoneY = ARENA_H * 0.5f,
                safeZoneActive = false,
                safeZoneBeatTimer = 3.2f
            )
            ObjectiveType.PROTECT_CRYING_BOX -> RoomObjectiveState(
                type = picked,
                targetCount = 1,
                timerSec = 16f,
                maxTimerSec = 16f,
                entityX = ARENA_W * 0.5f,
                entityY = ARENA_H * 0.52f,
                entityHp = 65f,
                entityMaxHp = 65f
            )
            ObjectiveType.CATCH_LOST_TOY -> RoomObjectiveState(
                type = picked,
                targetCount = 1,
                entityX = ARENA_W * 0.72f,
                entityY = ARENA_H * 0.35f,
                entityHp = 3f,
                entityMaxHp = 3f
            )
            ObjectiveType.RECOVER_GEARS -> RoomObjectiveState(
                type = picked,
                targetCount = 3,
                currentCount = 0
            )
            ObjectiveType.WIND_THREE_MECHANISMS -> RoomObjectiveState(
                type = picked,
                targetCount = 3,
                currentCount = 0,
                windupPositions = listOf(
                    ARENA_W * 0.25f to ARENA_H * 0.26f,
                    ARENA_W * 0.75f to ARENA_H * 0.28f,
                    ARENA_W * 0.50f to ARENA_H * 0.74f
                )
            )
            ObjectiveType.ESCORT_PACKAGE -> RoomObjectiveState(
                type = picked,
                targetCount = 1,
                entityX = ARENA_W * 0.24f,
                entityY = ARENA_H * 0.50f,
                entityHp = 55f,
                entityMaxHp = 55f,
                deliveryX = ARENA_W * 0.80f,
                deliveryY = ARENA_H * 0.50f
            )
            ObjectiveType.DEFEAT_WITH_ENVIRONMENT -> RoomObjectiveState(
                type = picked,
                targetCount = 2,
                currentCount = 0
            )
        }
    }

    fun initPickupsForObjective(
        obj: RoomObjectiveState?,
        obstacles: List<ObstacleRect>,
        allocId: () -> Long
    ): List<GroundPickup> {
        if (obj == null || obj.type != ObjectiveType.RECOVER_GEARS) return emptyList()
        val spots = listOf(
            ARENA_W * 0.24f to ARENA_H * 0.28f,
            ARENA_W * 0.76f to ARENA_H * 0.28f,
            ARENA_W * 0.50f to ARENA_H * 0.75f
        )
        return spots.map { (sx, sy) ->
            val (rx, ry) = RoomMechanicSystem.clampToObstacles(sx, sy, 32f, obstacles)
            GroundPickup(allocId(), PickupKind.GEAR_TOKEN, rx, ry, valueStr = "Engranaje")
        }
    }

    fun stepObjective(
        state: GameRunState,
        dt: Float,
        damagePlayerFn: (GameRunState) -> GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): GameRunState {
        val obj0 = state.roomObjective ?: return state
        if (obj0.completed || obj0.failed) return state

        var s = state
        var obj = obj0.copy(
            tookDamageDuringObjective = obj0.tookDamageDuringObjective || s.damagedInCurrentRoom
        )

        when (obj.type) {
            ObjectiveType.SURVIVE_TIME -> {
                val nt = (obj.timerSec - dt).coerceAtLeast(0f)
                obj = obj.copy(timerSec = nt)
                if (nt <= 0f) {
                    return completeObjective(s, obj, allocId, sfx)
                }
            }

            ObjectiveType.LAST_CHAIR_SAFE_ZONE -> {
                var beat = obj.safeZoneBeatTimer - dt
                if (beat <= 0f) {
                    if (!obj.safeZoneActive) {
                        // Music stopped! Check window begins; relocate safe spot
                        val sx = ARENA_W * (0.28f + Random.nextFloat() * 0.44f)
                        val sy = ARENA_H * (0.28f + Random.nextFloat() * 0.44f)
                        obj = obj.copy(
                            safeZoneActive = true,
                            safeZoneX = sx,
                            safeZoneY = sy,
                            safeZoneBeatTimer = 2.4f
                        )
                        s = s.copy(activeBanner = RunUiBanner("¡Música Detenida!", "Entra en la zona iluminada ahora.", 0xFFC89948, 2.0f))
                    } else {
                        // Evaluate!
                        val inside = hypot(s.playerX - obj.safeZoneX, s.playerY - obj.safeZoneY) <= 84f
                        if (inside) {
                            val nc = obj.currentCount + 1
                            sfx.play(ToySfx.PICKUP_BUTTON)
                            obj = obj.copy(
                                currentCount = nc,
                                safeZoneActive = false,
                                safeZoneBeatTimer = 3.2f
                            )
                            if (nc >= obj.targetCount) {
                                return completeObjective(s, obj, allocId, sfx)
                            }
                        } else {
                            s = damagePlayerFn(s)
                            obj = obj.copy(
                                safeZoneActive = false,
                                safeZoneBeatTimer = 3.2f,
                                tookDamageDuringObjective = true
                            )
                        }
                    }
                } else {
                    obj = obj.copy(safeZoneBeatTimer = beat)
                }
            }

            ObjectiveType.PROTECT_CRYING_BOX -> {
                val nt = (obj.timerSec - dt).coerceAtLeast(0f)
                var boxHp = obj.entityHp
                for (e in s.enemies) {
                    if (hypot(e.x - obj.entityX, e.y - obj.entityY) <= e.radius + 32f) {
                        boxHp -= 9.5f * dt
                    }
                }
                if (boxHp <= 0f) {
                    return failObjective(s, obj.copy(entityHp = 0f), allocId, sfx)
                }
                obj = obj.copy(timerSec = nt, entityHp = boxHp)
                if (nt <= 0f) {
                    return completeObjective(s, obj, allocId, sfx)
                }
            }

            ObjectiveType.CATCH_LOST_TOY -> {
                // Flee from player gently
                val dx = obj.entityX - s.playerX
                val dy = obj.entityY - s.playerY
                val d = hypot(dx, dy).coerceAtLeast(1f)
                var ex = obj.entityX
                var ey = obj.entityY
                if (d < 240f) {
                    ex = (ex + (dx / d) * 105f * dt).coerceIn(WALL_THICKNESS + 32f, ARENA_W - WALL_THICKNESS - 32f)
                    ey = (ey + (dy / d) * 105f * dt).coerceIn(WALL_THICKNESS + 32f, ARENA_H - WALL_THICKNESS - 32f)
                }
                obj = obj.copy(entityX = ex, entityY = ey)
                if (hypot(s.playerX - ex, s.playerY - ey) <= 36f) {
                    return completeObjective(s, obj.copy(currentCount = 1), allocId, sfx)
                }
            }

            ObjectiveType.ESCORT_PACKAGE -> {
                // Player pushes fragile box toward delivery pad
                var ex = obj.entityX
                var ey = obj.entityY
                var pkgHp = obj.entityHp
                val dx = ex - s.playerX
                val dy = ey - s.playerY
                val d = hypot(dx, dy).coerceAtLeast(1f)
                if (d < 46f) {
                    ex = (ex + (dx / d) * 155f * dt).coerceIn(WALL_THICKNESS + 28f, ARENA_W - WALL_THICKNESS - 28f)
                    ey = (ey + (dy / d) * 155f * dt).coerceIn(WALL_THICKNESS + 28f, ARENA_H - WALL_THICKNESS - 28f)
                }
                for (e in s.enemies) {
                    if (hypot(e.x - ex, e.y - ey) <= e.radius + 26f) {
                        pkgHp -= 8f * dt
                    }
                }
                if (pkgHp <= 0f) {
                    return failObjective(s, obj.copy(entityHp = 0f), allocId, sfx)
                }
                obj = obj.copy(entityX = ex, entityY = ey, entityHp = pkgHp)
                if (hypot(ex - obj.deliveryX, ey - obj.deliveryY) <= 44f) {
                    return completeObjective(s, obj.copy(currentCount = 1), allocId, sfx)
                }
            }

            ObjectiveType.DEFEAT_WITH_ENVIRONMENT -> {
                if (s.enemies.isEmpty() && obj.currentCount < obj.targetCount) {
                    // Prevent softlock if player cleared all enemies directly
                    return failObjective(s, obj, allocId, sfx, silent = true)
                }
            }

            else -> Unit
        }

        return s.copy(roomObjective = obj)
    }

    fun tryInteractObjective(
        state: GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): Pair<Boolean, GameRunState> {
        val obj = state.roomObjective ?: return false to state
        if (obj.completed || obj.failed) return false to state
        if (obj.type != ObjectiveType.WIND_THREE_MECHANISMS) return false to state

        obj.windupPositions.forEachIndexed { idx, (wx, wy) ->
            if (idx !in obj.woundIndices && hypot(state.playerX - wx, state.playerY - wy) <= 62f) {
                sfx.play(ToySfx.REPAIR_STITCH)
                val nextSet = obj.woundIndices + idx
                val updated = obj.copy(woundIndices = nextSet, currentCount = nextSet.size)
                return if (nextSet.size >= obj.targetCount) {
                    true to completeObjective(state, updated, allocId, sfx)
                } else {
                    true to state.copy(
                        roomObjective = updated,
                        activeBanner = RunUiBanner("Mecanismo con Cuerda", "${nextSet.size}/${obj.targetCount} activados.", 0xFFC89948, 1.8f)
                    )
                }
            }
        }
        return false to state
    }

    fun findNearbyObjectivePrompt(state: GameRunState): String? {
        val obj = state.roomObjective ?: return null
        if (obj.completed || obj.failed) return null
        if (obj.type == ObjectiveType.WIND_THREE_MECHANISMS) {
            obj.windupPositions.forEachIndexed { idx, (wx, wy) ->
                if (idx !in obj.woundIndices && hypot(state.playerX - wx, state.playerY - wy) <= 62f) {
                    return "Dar Cuerda (${idx + 1}/3)"
                }
            }
        }
        return null
    }

    fun completeObjective(
        state: GameRunState,
        obj: RoomObjectiveState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): GameRunState {
        sfx.play(ToySfx.SYNERGY)
        val completed = obj.copy(completed = true)
        val pickups = state.pickups.toMutableList()
        val dropX = ARENA_W * 0.5f
        val dropY = ARENA_H * 0.5f

        when (completed.type) {
            ObjectiveType.PROTECT_CRYING_BOX -> {
                val part = GameCatalog.bodyParts.filter { it.rarity.ordinal >= 1 }.random()
                pickups.add(GroundPickup(allocId(), PickupKind.NEW_PART_PEDESTAL, dropX, dropY, valueStr = part.id))
            }
            ObjectiveType.LAST_CHAIR_SAFE_ZONE -> {
                val item = GameCatalog.passiveItems.filter { it !in state.passiveItems }.randomOrNull()
                if (item != null) {
                    pickups.add(GroundPickup(allocId(), PickupKind.PASSIVE_ITEM_PEDESTAL, dropX, dropY, valueStr = item.id))
                } else {
                    pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, dropX, dropY))
                }
            }
            ObjectiveType.CATCH_LOST_TOY -> {
                pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, dropX, dropY, amount = 3))
                pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, dropX + 28f, dropY, valueStr = RepairMaterial.THREAD.name))
            }
            ObjectiveType.ESCORT_PACKAGE -> {
                pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, dropX, dropY))
                pickups.add(GroundPickup(allocId(), PickupKind.CONSUMABLE, dropX - 30f, dropY, valueStr = ConsumableType.REPAIR_PATCH.name))
            }
            else -> {
                pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, dropX, dropY))
                pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, dropX + 30f, dropY, valueStr = RepairMaterial.GEAR.name))
            }
        }

        // Bonus for no damage during objective
        val bonusBtns = if (!completed.tookDamageDuringObjective) 3 else 0
        if (bonusBtns > 0) {
            pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, dropX - 30f, dropY + 26f, amount = bonusBtns))
        }

        val clearedRoom = state.currentRoom.copy(cleared = true, rewardSpawned = true)
        val rooms = state.dungeon.rooms.toMutableMap().apply { put(state.currentCoord, clearedRoom) }
        val revealSecrets = state.toyboxMemory.copy(
            secretCluesRevealed = state.toyboxMemory.secretCluesRevealed || completed.type == ObjectiveType.CATCH_LOST_TOY
        )

        return state.copy(
            currentRoom = clearedRoom,
            dungeon = state.dungeon.copy(rooms = rooms),
            enemies = emptyList(),
            roomObjective = completed,
            pickups = pickups,
            toyboxMemory = revealSecrets,
            activeBanner = RunUiBanner(
                "¡Objetivo Cumplido!",
                "${completed.type.displayName}${if (bonusBtns > 0) " (Sin daño +$bonusBtns ●)" else ""}",
                0xFF597354,
                3.0f
            ),
            recentEngineEvents = (state.recentEngineEvents + EngineEventType.ROOM_OBJECTIVE_COMPLETED.key).takeLast(12)
        )
    }

    fun failObjective(
        state: GameRunState,
        obj: RoomObjectiveState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        silent: Boolean = false
    ): GameRunState {
        val failed = obj.copy(failed = true)
        val enemies = state.enemies.toMutableList()
        if (failed.type == ObjectiveType.PROTECT_CRYING_BOX) {
            val sp = EnemyType.SHADOW_PUPPET
            enemies.add(ActiveEnemy(allocId(), sp, failed.entityX, failed.entityY, hp = sp.maxHp * 1.2f, maxHp = sp.maxHp * 1.2f, radius = 23f, enraged = true))
        }
        return state.copy(
            roomObjective = failed,
            enemies = enemies,
            activeBanner = if (silent) state.activeBanner else RunUiBanner(
                "Objetivo Fallido",
                "Elimina a los juguetes restantes para abrir las puertas.",
                0xFFD13B42,
                2.8f
            ),
            recentEngineEvents = (state.recentEngineEvents + EngineEventType.ROOM_OBJECTIVE_FAILED.key).takeLast(12)
        )
    }

    fun abandonObjective(state: GameRunState): GameRunState {
        val obj = state.roomObjective ?: return state
        if (!obj.canAbandon || obj.completed || obj.failed) return state
        return state.copy(
            roomObjective = obj.copy(failed = true),
            activeBanner = RunUiBanner("Desafío Abandonado", "Derrota a los enemigos para continuar.", 0xFFD98A2B, 2.2f),
            recentEngineEvents = (state.recentEngineEvents + EngineEventType.ROOM_OBJECTIVE_FAILED.key).takeLast(12)
        )
    }
}
