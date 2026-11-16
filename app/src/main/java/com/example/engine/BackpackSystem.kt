package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.AmpliacionCatalog
import com.example.data.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object BackpackSystem {

    fun usedSlots(items: List<BackpackSlotItem>): Int {
        return items.sumOf { it.kind.slotsRequired }
    }

    fun usedSlots(state: GameRunState): Int = usedSlots(state.backpackItems)

    fun hasBackpack(state: GameRunState): Boolean = state.backpackTier != null

    fun capacity(state: GameRunState): Int = state.backpackTier?.capacity ?: 0

    fun remainingSlots(state: GameRunState): Int = (capacity(state) - usedSlots(state)).coerceAtLeast(0)

    fun speedPenaltyMult(state: GameRunState): Float {
        return if (state.backpackTier == BackpackTier.BOTTOMLESS_PACK && usedSlots(state) >= 6) {
            0.90f
        } else 1f
    }

    fun isInCombat(state: GameRunState): Boolean {
        return !state.currentRoom.cleared && state.enemies.isNotEmpty()
    }

    fun addItem(
        state: GameRunState,
        kind: BackpackItemKind,
        quantity: Int = 1,
        partZone: BodyZone? = null,
        storedDurability: Int = 100,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): Pair<Boolean, GameRunState> {
        if (state.backpackTier == null) {
            val banner = if (state.activeBanner?.title == "Necesitas una Mochila") {
                state.activeBanner
            } else {
                RunUiBanner(
                    "Necesitas una Mochila",
                    "Consigue una en el Puesto, Mesa de Costura, El Fabricante o cajas.",
                    0xFFD98A2B,
                    2.6f
                )
            }
            return false to state.copy(activeBanner = banner)
        }
        val current = state.backpackItems.toMutableList()
        if (kind.stackable) {
            val idx = current.indexOfFirst { it.kind == kind && it.quantity < 5 }
            if (idx != -1) {
                val existing = current[idx]
                current[idx] = existing.copy(quantity = existing.quantity + quantity)
                sfx.play(ToySfx.PICKUP_ITEM)
                return true to state.copy(
                    backpackItems = current,
                    activeBanner = RunUiBanner(
                        "Guardado en Mochila",
                        "${kind.displayName} (x${current[idx].quantity})",
                        0xFFC89948,
                        2.0f
                    )
                )
            }
        }

        if (remainingSlots(state) < kind.slotsRequired) {
            return false to state.copy(
                activeBanner = RunUiBanner(
                    "Mochila Llena (${usedSlots(state)}/${capacity(state)})",
                    "${kind.displayName} requiere ${kind.slotsRequired} espacio(s).",
                    0xFFD13B42,
                    2.2f
                )
            )
        }

        current.add(
            BackpackSlotItem(
                uid = allocId(),
                kind = kind,
                quantity = quantity,
                partZone = partZone,
                storedDurability = storedDurability
            )
        )
        sfx.play(ToySfx.PICKUP_ITEM)
        return true to state.copy(
            backpackItems = current,
            activeBanner = RunUiBanner(
                "Guardado en Mochila",
                "${kind.displayName} (+${kind.slotsRequired} esp.)",
                0xFF597354,
                2.0f
            )
        )
    }

    fun useItem(
        state: GameRunState,
        uid: Long,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit
    ): GameRunState {
        val idx = state.backpackItems.indexOfFirst { it.uid == uid }
        if (idx == -1) return state
        val slot = state.backpackItems[idx]

        if (!slot.kind.usableInCombat && isInCombat(state)) {
            return state.copy(
                activeBanner = RunUiBanner(
                    "No usable en combate",
                    "Termina de limpiar la sala antes de usar ${slot.kind.displayName}.",
                    0xFFD98A2B,
                    2.2f
                )
            )
        }

        var s = state
        when (slot.kind) {
            BackpackItemKind.EXTRA_HEART -> {
                if (s.hearts >= s.maxHearts) {
                    return s.copy(activeBanner = RunUiBanner("Salud Completa", "Reserva tu corazón de algodón para después.", 0xFFC89948, 1.8f))
                }
                sfx.play(ToySfx.PICKUP_ITEM)
                s = s.copy(
                    hearts = (s.hearts + 1).coerceAtMost(s.maxHearts),
                    activeBanner = RunUiBanner("Corazón de Algodón", "+1 Corazón restaurado.", 0xFF597354, 1.8f)
                )
            }
            BackpackItemKind.BOMB_PACK -> {
                s = s.copy(
                    bombs = s.bombs + HazardBomb(allocId(), s.playerX, s.playerY, fuseSec = 1.25f),
                    activeBanner = RunUiBanner("Bomba Colocada", "¡Aléjate antes de que detone!", 0xFFD13B42, 1.8f)
                )
            }
            BackpackItemKind.BRASS_KEY -> {
                if (s.currentRoom.secretDoors.isNotEmpty()) {
                    val opened = s.currentRoom.copy(
                        doors = s.currentRoom.doors + s.currentRoom.secretDoors,
                        secretDoors = emptySet()
                    )
                    val rooms = s.dungeon.rooms.toMutableMap().apply { this[s.currentCoord] = opened }
                    sfx.play(ToySfx.DOOR_OPEN)
                    s = s.copy(
                        currentRoom = opened,
                        dungeon = s.dungeon.copy(rooms = rooms),
                        activeBanner = RunUiBanner("Pasadizo Abierto", "La llave antigua reveló una salida oculta.", 0xFFC89948, 2.2f)
                    )
                } else {
                    sfx.play(ToySfx.PICKUP_ITEM)
                    s = s.copy(
                        consumables = s.consumables + ConsumableType.KEY,
                        activeBanner = RunUiBanner("Llave Preparada", "Añadida a tus consumibles activos.", 0xFFC89948, 1.8f)
                    )
                }
            }
            BackpackItemKind.SEWING_KIT_ITEM -> {
                val target = s.equippedParts.values.minByOrNull { if (it.isDetached) -1 else it.durability }
                if (target != null) {
                    val map = s.equippedParts.toMutableMap()
                    map[target.def.zone] = target.copy(
                        isDetached = false,
                        durability = (target.durability + 45).coerceAtMost(target.def.maxDurability)
                    )
                    sfx.play(ToySfx.REPAIR_STITCH)
                    s = s.copy(
                        equippedParts = map,
                        usedAnyRepair = true,
                        activeBanner = RunUiBanner("Kit de Mochila", "${target.def.name} reparada (+45).", 0xFF597354, 2.0f)
                    )
                }
            }
            BackpackItemKind.OFFENSIVE_SPRING -> {
                sfx.play(ToySfx.SHOOT)
                val bolts = (0 until 8).map { i ->
                    val ang = (i / 8f) * (2f * PI.toFloat())
                    ActiveProjectile(
                        id = allocId(),
                        x = s.playerX,
                        y = s.playerY,
                        vx = cos(ang) * 510f,
                        vy = sin(ang) * 510f,
                        radius = 10f,
                        damage = 18f,
                        fromPlayer = true,
                        colorHex = 0xFFC89948,
                        style = AttackStyle.GEAR_PROJECTILE,
                        pierceLeft = 1
                    )
                }
                s = s.copy(
                    projectiles = s.projectiles + bolts,
                    activeBanner = RunUiBanner("Resorte Ofensivo", "¡Ráfaga radial liberada!", 0xFFD98A2B, 1.8f)
                )
            }
            BackpackItemKind.SHIELD_CHARM -> {
                sfx.play(ToySfx.SYNERGY)
                s = s.copy(
                    invulnTimer = maxOf(s.invulnTimer, 3.5f),
                    activeBanner = RunUiBanner("Amuleto de Felpa", "Escudo protector activo (3.5s).", 0xFF4C5F6E, 2.0f)
                )
            }
            BackpackItemKind.CRAFTING_BUNDLE -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                val mats = s.materials.toMutableMap()
                mats[RepairMaterial.THREAD] = (mats[RepairMaterial.THREAD] ?: 0) + 2
                mats[RepairMaterial.NEEDLE] = (mats[RepairMaterial.NEEDLE] ?: 0) + 1
                mats[RepairMaterial.SCREW] = (mats[RepairMaterial.SCREW] ?: 0) + 1
                onDiscovery("material", RepairMaterial.THREAD.name)
                onDiscovery("material", RepairMaterial.NEEDLE.name)
                s = s.copy(
                    materials = mats,
                    activeBanner = RunUiBanner("Fardo Desempacado", "+2 Hilo, +1 Aguja, +1 Tornillo.", 0xFF597354, 2.2f)
                )
            }
            BackpackItemKind.SMALL_PART_SPARE -> {
                val targetZone = slot.partZone ?: s.equippedParts.values.minByOrNull { if (it.isDetached) -1 else it.durability }?.def?.zone
                val part = targetZone?.let { s.equippedParts[it] }
                if (part != null) {
                    val map = s.equippedParts.toMutableMap()
                    val restoredDur = if (s.backpackTier == BackpackTier.MANUFACTURER_PACK) {
                        part.def.maxDurability
                    } else {
                        ((part.def.maxDurability * slot.storedDurability) / 100).coerceIn(45, part.def.maxDurability)
                    }
                    map[part.def.zone] = part.copy(isDetached = false, durability = restoredDur)
                    sfx.play(ToySfx.REPAIR_STITCH)
                    s = s.copy(
                        equippedParts = map,
                        usedAnyRepair = true,
                        activeBanner = RunUiBanner("Pieza de Repuesto", "${part.def.name} restaurada al $restoredDur%.", 0xFF597354, 2.2f)
                    )
                }
            }
            BackpackItemKind.LARGE_RELIC_BOX -> {
                sfx.play(ToySfx.SYNERGY)
                val mats = s.materials.toMutableMap()
                mats[RepairMaterial.GEAR] = (mats[RepairMaterial.GEAR] ?: 0) + 1
                mats[RepairMaterial.PORCELAIN_SHARD] = (mats[RepairMaterial.PORCELAIN_SHARD] ?: 0) + 1
                val lockedArch = AmpliacionCatalog.archiveEntries.firstOrNull { it.id !in s.unlockedArchiveIdsThisRun }
                s = s.copy(
                    buttons = s.buttons + 6,
                    materials = mats,
                    activeBanner = RunUiBanner("Caja de Reliquia", "+6 Botones, +1 Engranaje, +1 Porcelana.", 0xFFC89948, 2.4f)
                )
                if (lockedArch != null) {
                    s = WonderlandArchiveSystem.unlockArchiveEntry(
                        s,
                        lockedArch.id,
                        bonusText = "Recuperado de la Caja de Reliquia (+6 ●).",
                        onDiscovery = onDiscovery,
                        onAchievement = onAchievement
                    )
                }
            }
            BackpackItemKind.CONSUMABLE_MEMORY -> {
                sfx.play(ToySfx.SYNERGY)
                s = s.copy(
                    hearts = (s.hearts + 1).coerceAtMost(s.maxHearts),
                    abilityBuffTimer = maxOf(s.abilityBuffTimer, 6.0f),
                    activeBanner = RunUiBanner("Frasco de Recuerdo", "+1 Corazón y ataque sobrecargado (6s).", 0xFF942229, 2.4f)
                )
            }
        }

        val nextItems = s.backpackItems.toMutableList()
        val consumedIdx = nextItems.indexOfFirst { it.uid == uid }
        if (consumedIdx != -1) {
            val cur = nextItems[consumedIdx]
            if (cur.quantity > 1) {
                nextItems[consumedIdx] = cur.copy(quantity = cur.quantity - 1)
            } else {
                nextItems.removeAt(consumedIdx)
            }
        }
        return s.copy(backpackItems = nextItems)
    }

    fun discardItem(state: GameRunState, uid: Long): GameRunState {
        val target = state.backpackItems.find { it.uid == uid } ?: return state
        return state.copy(
            backpackItems = state.backpackItems.filterNot { it.uid == uid },
            activeBanner = RunUiBanner(
                "Objeto Descartado",
                "${target.kind.displayName} retirado de la mochila.",
                0xFFB5A17D,
                1.8f
            )
        )
    }

    fun equipTier(
        state: GameRunState,
        tier: BackpackTier,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onUnlockTier: (BackpackTier) -> Unit
    ): GameRunState {
        onUnlockTier(tier)
        sfx.play(ToySfx.SYNERGY)
        var kept = state.backpackItems.toMutableList()
        val dropped = mutableListOf<GroundPickup>()
        while (usedSlots(kept) > tier.capacity && kept.isNotEmpty()) {
            val removed = kept.removeAt(kept.lastIndex)
            repeat(removed.quantity) { idx ->
                dropped.add(
                    GroundPickup(
                        id = allocId(),
                        kind = PickupKind.BACKPACK_ITEM,
                        x = (state.playerX + 36f + idx * 14f).coerceIn(WALL_THICKNESS + 36f, ARENA_W - WALL_THICKNESS - 36f),
                        y = (state.playerY + 34f).coerceIn(WALL_THICKNESS + 36f, ARENA_H - WALL_THICKNESS - 36f),
                        valueStr = removed.kind.name
                    )
                )
            }
        }
        return state.copy(
            backpackTier = tier,
            backpackItems = kept,
            pickups = state.pickups + dropped,
            activeBanner = RunUiBanner(
                "Mochila Equipada: ${tier.displayName}",
                tier.specialTrait,
                0xFFC89948,
                2.8f
            )
        )
    }

    fun canCraft(state: GameRunState, recipe: BackpackRecipe): Boolean {
        if (state.backpackTier == recipe.tier) return false
        if (state.buttons < recipe.buttonCost) return false
        return recipe.requirements.all { (mat, qty) -> (state.materials[mat] ?: 0) >= qty }
    }

    fun craftBackpack(
        state: GameRunState,
        recipe: BackpackRecipe,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onUnlockTier: (BackpackTier) -> Unit,
        onDiscoverRecipe: (String) -> Unit
    ): GameRunState {
        if (!canCraft(state, recipe)) {
            return state.copy(
                activeBanner = RunUiBanner(
                    "Materiales Insuficientes",
                    "Reúne botones y materiales del taller para fabricar ${recipe.tier.displayName}.",
                    0xFFD13B42,
                    2.2f
                )
            )
        }
        val nextMats = state.materials.toMutableMap()
        recipe.requirements.forEach { (mat, qty) ->
            nextMats[mat] = ((nextMats[mat] ?: 0) - qty).coerceAtLeast(0)
        }
        onDiscoverRecipe(recipe.id)
        onAchievement("ach_backpack_craft")

        var s = state.copy(
            buttons = state.buttons - recipe.buttonCost,
            materials = nextMats
        )
        s = equipTier(s, recipe.tier, allocId, sfx, onUnlockTier)
        val recipeArchId = when (recipe.tier) {
            BackpackTier.KIDS_BACKPACK -> "arch_recipe_kids_pack"
            BackpackTier.EXPLORER_PACK -> "arch_recipe_explorer_pack"
            BackpackTier.MANUFACTURER_PACK -> "arch_recipe_manufacturer_pack"
            else -> null
        }
        if (recipeArchId != null) {
            onDiscovery("archive", recipeArchId)
            s = s.copy(unlockedArchiveIdsThisRun = s.unlockedArchiveIdsThisRun + recipeArchId)
        }
        return s
    }

    fun degradeStoredSparesOnRoomAdvance(state: GameRunState): GameRunState {
        if (state.backpackTier == BackpackTier.MANUFACTURER_PACK) return state
        if (state.backpackItems.none { it.kind == BackpackItemKind.SMALL_PART_SPARE }) return state
        val updated = state.backpackItems.map { item ->
            if (item.kind == BackpackItemKind.SMALL_PART_SPARE) {
                item.copy(storedDurability = (item.storedDurability - 5).coerceAtLeast(35))
            } else item
        }
        return state.copy(backpackItems = updated)
    }

    fun randomDropKind(rng: Random = Random.Default): BackpackItemKind {
        return listOf(
            BackpackItemKind.EXTRA_HEART,
            BackpackItemKind.BOMB_PACK,
            BackpackItemKind.SEWING_KIT_ITEM,
            BackpackItemKind.OFFENSIVE_SPRING,
            BackpackItemKind.SHIELD_CHARM,
            BackpackItemKind.CRAFTING_BUNDLE,
            BackpackItemKind.SMALL_PART_SPARE,
            BackpackItemKind.BRASS_KEY
        ).random(rng)
    }
}
