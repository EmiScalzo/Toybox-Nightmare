package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.GameCatalog
import com.example.data.model.*
import kotlin.math.hypot
import kotlin.random.Random

object SecretDiscoverySystem {

    fun generateAnomaliesForRoom(
        room: RoomBlueprint,
        dungeon: DungeonFloor,
        allocId: () -> Long,
        rng: Random = Random.Default
    ): List<RoomAnomaly> {
        if (room.type in setOf(RoomType.START, RoomType.BOSS, RoomType.SECRET, RoomType.MANUFACTURER)) {
            return emptyList()
        }
        val list = mutableListOf<RoomAnomaly>()
        if (room.secretDoors.isNotEmpty()) {
            val sDir = room.secretDoors.first()
            val secretNeighbor = dungeon.rooms[room.coord.neighbor(sDir)]
            val flavor = secretNeighbor?.secretFlavor ?: SecretRoomFlavor.HIDDEN_TREASURE
            val clue = SecretClueType.entries.random(rng)
            val (ax, ay) = when (sDir) {
                Direction.NORTH -> ARENA_W * 0.5f to WALL_THICKNESS + 36f
                Direction.SOUTH -> ARENA_W * 0.5f to ARENA_H - WALL_THICKNESS - 36f
                Direction.WEST -> WALL_THICKNESS + 36f to ARENA_H * 0.5f
                Direction.EAST -> ARENA_W - WALL_THICKNESS - 36f to ARENA_H * 0.5f
            }
            list.add(RoomAnomaly(allocId(), clue, ax, ay, flavor))
        } else if (room.type in setOf(RoomType.COMBAT, RoomType.EVENT, RoomType.CURSE) && rng.nextFloat() < 0.20f) {
            val clue = listOf(SecretClueType.CROOKED_FRAME, SecretClueType.WALL_GAZING_DOLL, SecretClueType.TICKING_CLOCK_PUZZLE).random(rng)
            list.add(
                RoomAnomaly(
                    id = allocId(),
                    clueType = clue,
                    x = ARENA_W * 0.18f,
                    y = WALL_THICKNESS + 44f,
                    targetFlavor = SecretRoomFlavor.MEMORY_VAULT
                )
            )
        }
        return list
    }

    fun stepAnomalies(
        state: GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        if (state.roomAnomalies.isEmpty()) return state
        var s = state
        for (a in state.roomAnomalies) {
            if (!a.resolved && a.clueType == SecretClueType.WRONG_SHADOW_PROP) {
                val blockPlaced = s.roomInteractables.any {
                    it.type.category == InteractableCategory.MOVABLE && hypot(it.x - a.x, it.y - a.y) <= 56f
                }
                if (blockPlaced) {
                    s = resolveAnomaly(s, a.id, allocId, sfx, onDiscovery)
                }
            }
        }
        return s
    }

    fun findNearbyAnomalyPrompt(state: GameRunState): Pair<RoomAnomaly, String>? {
        val near = state.roomAnomalies.firstOrNull {
            !it.resolved && hypot(it.x - state.playerX, it.y - state.playerY) <= 68f
        } ?: return null
        val label = when (near.clueType) {
            SecretClueType.CROOKED_FRAME -> "Enderezar Cuadro"
            SecretClueType.WALL_GAZING_DOLL -> "Inspeccionar Muñeca"
            SecretClueType.TICKING_CLOCK_PUZZLE -> "Ajustar Reloj"
            SecretClueType.KNOCKING_WALL -> "Escuchar Pared"
            SecretClueType.WRONG_SHADOW_PROP -> "Revisar Sombra"
        }
        return near to label
    }

    fun interactWithAnomaly(
        state: GameRunState,
        anomalyId: Long,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        val anomaly = state.roomAnomalies.find { it.id == anomalyId } ?: return state
        if (anomaly.resolved) return state
        onDiscovery("clue", anomaly.clueType.name)

        if (anomaly.clueType == SecretClueType.KNOCKING_WALL || anomaly.clueType == SecretClueType.WRONG_SHADOW_PROP) {
            // Marks inspected & gives clear physical instruction; bomb or movable still works, OR interacting twice forces open for accessibility
            if (!anomaly.inspected) {
                val updated = state.roomAnomalies.map { if (it.id == anomalyId) it.copy(inspected = true) else it }
                return state.copy(
                    roomAnomalies = updated,
                    activeBanner = RunUiBanner(anomaly.clueType.displayName, anomaly.clueType.hintText, 0xFFC89948, 2.8f),
                    recentEngineEvents = (state.recentEngineEvents + EngineEventType.SECRET_CLUE_FOUND.key).takeLast(12)
                )
            }
        }
        return resolveAnomaly(state, anomalyId, allocId, sfx, onDiscovery)
    }

    fun onExplosionNearAnomaly(
        state: GameRunState,
        bx: Float,
        by: Float,
        radius: Float,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        var s = state
        for (a in s.roomAnomalies) {
            if (!a.resolved && hypot(a.x - bx, a.y - by) <= radius + 40f) {
                onDiscovery("clue", a.clueType.name)
                s = resolveAnomaly(s, a.id, allocId, sfx, onDiscovery)
            }
        }
        return s
    }

    fun resolveAnomaly(
        state: GameRunState,
        anomalyId: Long,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit
    ): GameRunState {
        val target = state.roomAnomalies.find { it.id == anomalyId } ?: return state
        if (target.resolved) return state
        onDiscovery("clue", target.clueType.name)
        sfx.play(ToySfx.DOOR_OPEN)

        val updatedAnomalies = state.roomAnomalies.map { if (it.id == anomalyId) it.copy(resolved = true, inspected = true) else it }
        var room = state.currentRoom
        val rooms = state.dungeon.rooms.toMutableMap()
        var banner: RunUiBanner
        val pickups = state.pickups.toMutableList()
        var mem = state.toyboxMemory
        val events = (state.recentEngineEvents + listOf(
            EngineEventType.SECRET_CLUE_FOUND.key,
            EngineEventType.SECRET_ROOM_UNLOCKED.key
        )).takeLast(12)

        if (room.secretDoors.isNotEmpty()) {
            room = room.copy(doors = room.doors + room.secretDoors, secretDoors = emptySet())
            rooms[state.currentCoord] = room
            banner = RunUiBanner(
                "¡Pasadizo Secreto Abierto!",
                "Destino: ${target.targetFlavor.displayName}",
                0xFFC89948,
                3.0f
            )
        } else {
            // Standalone physical secret cache inside the room
            pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, target.x, (target.y + 42f).coerceAtMost(ARENA_H - WALL_THICKNESS - 40f)))
            pickups.add(GroundPickup(allocId(), PickupKind.MATERIAL, (target.x + 28f).coerceAtMost(ARENA_W - WALL_THICKNESS - 40f), (target.y + 42f).coerceAtMost(ARENA_H - WALL_THICKNESS - 40f), valueStr = RepairMaterial.PORCELAIN_SHARD.name))
            val uncollectedMem = GameCatalog.memories.firstOrNull { it.id !in mem.memoriesCollected }
            if (uncollectedMem != null) {
                onDiscovery("memory", uncollectedMem.id)
                mem = mem.copy(memoriesCollected = mem.memoriesCollected + uncollectedMem.id)
            }
            banner = RunUiBanner(
                "Secreto Descubierto: ${target.clueType.displayName}",
                "Compartimento oculto abierto (+Botón Dorado y Material).",
                0xFFC89948,
                3.0f
            )
        }

        return state.copy(
            roomAnomalies = updatedAnomalies,
            currentRoom = room,
            dungeon = state.dungeon.copy(rooms = rooms),
            pickups = pickups,
            toyboxMemory = mem,
            activeBanner = banner,
            recentEngineEvents = events
        )
    }
}
