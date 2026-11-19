package com.example.engine

import com.example.data.model.ConsumableType
import com.example.data.model.InteractableType
import com.example.data.model.LabyrinthState
import com.example.data.model.LabyrinthType
import com.example.data.model.LightSourceType
import com.example.data.model.PatchEmotion
import com.example.data.model.RoomType
import kotlin.random.Random

object LabyrinthSystem {

    fun initLabyrinthRoom(
        state: GameRunState,
        room: RoomBlueprint,
        allocId: () -> Long
    ): GameRunState {
        if (room.type != RoomType.LABYRINTH) {
            return state.copy(labyrinthState = null)
        }
        val saved = state.savedRoomsByCoord[room.coord]?.labyrinthState
        if (saved != null) {
            return state.copy(labyrinthState = saved)
        }
        val lType = room.labyrinthType ?: LabyrinthType.FACTORY_MAZE
        val labState = LabyrinthState(
            type = lType,
            chestsTotal = 2,
            lostToysTotal = 3,
            conveyorDirX = if (Random.nextBoolean()) 1f else -1f
        )

        // Spawn 2 chests, 3 Lost Toy Tokens in far alcoves, and labyrinth mechanical traps/clues
        val chests = listOf(
            RoomInteractable(allocId(), InteractableType.LOOT_CRATE, ARENA_W * 0.16f, ARENA_H * 0.22f, hp = 26f, maxHp = 26f),
            RoomInteractable(allocId(), InteractableType.LOOT_CRATE, ARENA_W * 0.84f, ARENA_H * 0.78f, hp = 26f, maxHp = 26f)
        )
        val extraTraps = listOf(
            RoomInteractable(allocId(), InteractableType.PRESSURE_TILE, ARENA_W * 0.42f, ARENA_H * 0.50f),
            RoomInteractable(allocId(), InteractableType.WALL_LEVER, ARENA_W * 0.50f, ARENA_H * 0.14f)
        )
        val lostToyPickups = listOf(
            GroundPickup(allocId(), PickupKind.LOST_TOY_TOKEN, ARENA_W * 0.16f, ARENA_H * 0.78f, "Juguete Perdido #1"),
            GroundPickup(allocId(), PickupKind.LOST_TOY_TOKEN, ARENA_W * 0.84f, ARENA_H * 0.22f, "Juguete Perdido #2"),
            GroundPickup(allocId(), PickupKind.LOST_TOY_TOKEN, ARENA_W * 0.50f, ARENA_H * 0.82f, "Juguete Perdido #3")
        )
        return state.copy(
            labyrinthState = labState,
            roomInteractables = state.roomInteractables + chests + extraTraps,
            pickups = state.pickups + lostToyPickups,
            activeBanner = RunUiBanner(
                lType.displayName,
                "Encuentra 3 juguetes perdidos, 2 cofres y la palanca oculta sin activar trampas.",
                0xFFE5A93B,
                3.2f
            )
        )
    }

    fun stepLabyrinth(state: GameRunState, dt: Float, allocId: () -> Long): GameRunState {
        val lab = state.labyrinthState ?: return state
        var s = state

        // Factory maze applies gentle conveyor belt push along center lane
        if (lab.type == LabyrinthType.FACTORY_MAZE && s.dodgeTimer <= 0f) {
            val nextPx = (s.playerX + lab.conveyorDirX * 38f * dt)
                .coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f)
            s = s.copy(playerX = nextPx)
        }

        // Track chests opened (LOOT_CRATE destroyed) and hidden lever (WALL_LEVER activated)
        val destroyedChests = s.roomInteractables.count { it.type == InteractableType.LOOT_CRATE && it.destroyed }
        val pocketOpened = lab.hiddenPocketFound || s.roomInteractables.any { it.type == InteractableType.WALL_LEVER && it.active }
        val guardianDown = lab.guardianDefeated || s.enemies.none { it.hp > 0f }

        var nextLab = lab.copy(
            chestsOpened = destroyedChests.coerceAtMost(lab.chestsTotal),
            hiddenPocketFound = pocketOpened,
            guardianDefeated = guardianDown
        )

        if (nextLab.isPerfectExploration && !nextLab.perfectRewardClaimed) {
            nextLab = nextLab.copy(perfectRewardClaimed = true)
            val rewardLight = if (LightSourceType.MECHANICAL_LANTERN !in s.ownedLightSources) {
                GroundPickup(allocId(), PickupKind.LIGHT_SOURCE, ARENA_W * 0.5f, ARENA_H * 0.45f, LightSourceType.MECHANICAL_LANTERN.name)
            } else {
                GroundPickup(allocId(), PickupKind.CONSUMABLE, ARENA_W * 0.5f, ARENA_H * 0.45f, ConsumableType.KEY.name)
            }
            s = s.copy(
                pickups = s.pickups + listOf(
                    rewardLight,
                    GroundPickup(allocId(), PickupKind.GOLD_BUTTON, ARENA_W * 0.5f - 28f, ARENA_H * 0.52f, "Botón Dorado", amount = 2),
                    GroundPickup(allocId(), PickupKind.BUTTON, ARENA_W * 0.5f + 28f, ARENA_H * 0.52f, "Botón", amount = 4)
                ),
                activeBanner = RunUiBanner(
                    "¡Exploración Perfecta!",
                    "Completaste todos los objetivos del ${lab.type.displayName} sin activar trampas.",
                    0xFFE5A93B,
                    3.5f
                )
            )
            s = EmotionSystem.triggerEmotion(s, PatchEmotion.JOY, 35f, 25f, "Triunfo en el laberinto.")
        }
        return s.copy(labyrinthState = nextLab)
    }

    fun onTrapTriggered(state: GameRunState): GameRunState {
        val lab = state.labyrinthState ?: return state
        return state.copy(labyrinthState = lab.copy(trapsTriggered = lab.trapsTriggered + 1))
    }

    fun onLostToyCollected(state: GameRunState): GameRunState {
        val lab = state.labyrinthState ?: return state
        val nextFound = (lab.lostToysFound + 1).coerceAtMost(lab.lostToysTotal)
        var s = state.copy(
            labyrinthState = lab.copy(lostToysFound = nextFound),
            buttons = state.buttons + 2,
            activeBanner = RunUiBanner(
                "Juguete Perdido ($nextFound/${lab.lostToysTotal})",
                "Recompensa del laberinto (+2 ●).",
                0xFF597354,
                1.8f
            )
        )
        s = ContractSystem.onRelicOrClueFound(s)
        return s
    }
}
