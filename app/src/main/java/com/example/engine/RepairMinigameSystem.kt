package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.model.*
import kotlin.math.abs

object RepairMinigameSystem {

    fun chooseMinigame(zone: BodyZone, mode: RepairActionMode): RepairMinigameType {
        if (mode == RepairActionMode.MODIFY) return RepairMinigameType.WIRING
        return when (zone) {
            BodyZone.TORSO, BodyZone.BACK -> RepairMinigameType.SEWING
            BodyZone.HEAD, BodyZone.EYES -> RepairMinigameType.PORCELAIN
            BodyZone.LEGS -> RepairMinigameType.SPRING_TIMING
            BodyZone.ARMS -> RepairMinigameType.GEARS
        }
    }

    fun startMinigame(state: GameRunState, zone: BodyZone, mode: RepairActionMode): GameRunState {
        val mgType = chooseMinigame(zone, mode)
        val seq = listOf(1, 2, 3, 4)
        val pending = PendingRepairMinigame(
            zone = zone,
            mode = mode,
            minigameType = mgType,
            sequenceTarget = seq,
            currentStep = 0,
            gearStates = listOf(0, 1, 0),
            springCursor = 0.15f,
            springDir = 1f,
            attemptsLeft = 5
        )
        return state.copy(
            pendingRepairMinigame = pending,
            recentEngineEvents = (state.recentEngineEvents + EngineEventType.REPAIR_STARTED.key).takeLast(12)
        )
    }

    fun tickSpringMinigame(state: GameRunState, dt: Float): GameRunState {
        val mg = state.pendingRepairMinigame ?: return state
        if (mg.finished || mg.minigameType != RepairMinigameType.SPRING_TIMING) return state
        var next = mg.springCursor + mg.springDir * dt * 1.15f
        var dir = mg.springDir
        if (next >= 1f) {
            next = 1f
            dir = -1f
        } else if (next <= 0f) {
            next = 0f
            dir = 1f
        }
        return state.copy(pendingRepairMinigame = mg.copy(springCursor = next, springDir = dir))
    }

    fun handleAction(state: GameRunState, action: String, sfx: SoundEffectsEngine): GameRunState {
        if (action == "cancel") {
            return state.copy(pendingRepairMinigame = null)
        }
        val mg = state.pendingRepairMinigame ?: return state
        if (mg.finished) return state

        if (action == "quick_repair") {
            return finishRepair(state, mg, RepairQuality.IMPERFECT, sfx, buttonCost = 2)
        }

        when (mg.minigameType) {
            RepairMinigameType.SEWING -> {
                val tapped = action.removePrefix("sew_tap:").toIntOrNull() ?: return state
                val expected = mg.sequenceTarget.getOrNull(mg.currentStep)
                if (tapped == expected) {
                    sfx.play(ToySfx.REPAIR_STITCH)
                    val nextStep = mg.currentStep + 1
                    if (nextStep >= mg.sequenceTarget.size) {
                        val quality = if (mg.attemptsLeft >= 4) RepairQuality.EXCELLENT else RepairQuality.GOOD
                        return finishRepair(state, mg, quality, sfx)
                    } else {
                        return state.copy(pendingRepairMinigame = mg.copy(currentStep = nextStep))
                    }
                } else {
                    val left = (mg.attemptsLeft - 1).coerceAtLeast(0)
                    if (left == 0) return finishRepair(state, mg, RepairQuality.IMPERFECT, sfx)
                    return state.copy(pendingRepairMinigame = mg.copy(attemptsLeft = left))
                }
            }

            RepairMinigameType.GEARS -> {
                val idx = action.removePrefix("gear_rotate:").toIntOrNull() ?: return state
                if (idx !in mg.gearStates.indices) return state
                sfx.play(ToySfx.PICKUP_BUTTON)
                val updated = mg.gearStates.toMutableList()
                updated[idx] = (updated[idx] + 1) % 3
                val left = (mg.attemptsLeft - 1).coerceAtLeast(0)
                if (updated.all { it == 2 }) {
                    val quality = if (left >= 2) RepairQuality.EXCELLENT else RepairQuality.GOOD
                    return finishRepair(state, mg.copy(gearStates = updated), quality, sfx)
                }
                return if (left == 0) {
                    finishRepair(state, mg.copy(gearStates = updated), RepairQuality.IMPERFECT, sfx)
                } else {
                    state.copy(pendingRepairMinigame = mg.copy(gearStates = updated, attemptsLeft = left))
                }
            }

            RepairMinigameType.SPRING_TIMING -> {
                if (action != "spring_stop") return state
                val distFromCenter = abs(mg.springCursor - 0.5f)
                val quality = when {
                    distFromCenter <= 0.12f -> RepairQuality.EXCELLENT
                    distFromCenter <= 0.25f -> RepairQuality.GOOD
                    else -> RepairQuality.IMPERFECT
                }
                return finishRepair(state, mg, quality, sfx)
            }

            RepairMinigameType.PORCELAIN -> {
                val pairIdx = action.removePrefix("porcelain_match:").toIntOrNull() ?: return state
                sfx.play(ToySfx.PICKUP_BUTTON)
                val nextSet = mg.porcelainMatched + pairIdx
                return if (nextSet.size >= 3) {
                    val quality = if (mg.attemptsLeft >= 4) RepairQuality.EXCELLENT else RepairQuality.GOOD
                    finishRepair(state, mg.copy(porcelainMatched = nextSet), quality, sfx)
                } else {
                    state.copy(pendingRepairMinigame = mg.copy(porcelainMatched = nextSet))
                }
            }

            RepairMinigameType.WIRING -> {
                val nodeIdx = action.removePrefix("wiring_pair:").toIntOrNull() ?: return state
                sfx.play(ToySfx.PICKUP_BUTTON)
                val nextSet = mg.wiringMatched + nodeIdx
                return if (nextSet.size >= 3) {
                    val quality = if (mg.attemptsLeft >= 4) RepairQuality.EXCELLENT else RepairQuality.GOOD
                    finishRepair(state, mg.copy(wiringMatched = nextSet), quality, sfx)
                } else {
                    state.copy(pendingRepairMinigame = mg.copy(wiringMatched = nextSet))
                }
            }
        }
    }

    private fun finishRepair(
        state: GameRunState,
        mg: PendingRepairMinigame,
        quality: RepairQuality,
        sfx: SoundEffectsEngine,
        buttonCost: Int = 1
    ): GameRunState {
        val part = state.equippedParts[mg.zone] ?: return state.copy(pendingRepairMinigame = null)
        val finalCost = if (state.buttons >= buttonCost) buttonCost else 0

        // Consume matching material if owned for bonus durability!
        val preferredMat = when (mg.minigameType) {
            RepairMinigameType.SEWING -> RepairMaterial.THREAD
            RepairMinigameType.GEARS -> RepairMaterial.GEAR
            RepairMinigameType.SPRING_TIMING -> RepairMaterial.SPRING
            RepairMinigameType.PORCELAIN -> RepairMaterial.PORCELAIN_SHARD
            RepairMinigameType.WIRING -> RepairMaterial.SCREW
        }
        val materials = state.materials.toMutableMap()
        val hasMat = (materials[preferredMat] ?: 0) > 0
        if (hasMat) {
            materials[preferredMat] = (materials[preferredMat]!! - 1).coerceAtLeast(0)
        }

        val gainRatio = quality.multiplier + (if (hasMat) 0.15f else 0f)
        val restored = (part.def.maxDurability * gainRatio).toInt().coerceAtLeast(25)
        val newDurability = (part.durability + restored).coerceAtMost(part.def.maxDurability)

        val partsMap = state.equippedParts.toMutableMap()
        partsMap[mg.zone] = part.copy(isDetached = false, durability = newDurability)

        val reinforced = state.partReinforcedRooms.toMutableMap()
        var modArm = state.modifiedArmExplosiveGears
        var speedMsg = ""
        if (mg.mode == RepairActionMode.REINFORCE) {
            reinforced[mg.zone] = 3
            speedMsg = " + Reforzada (3 salas)"
        } else if (mg.mode == RepairActionMode.MODIFY) {
            modArm = true
            speedMsg = " + Modificación Explosiva"
        }

        sfx.play(ToySfx.REPAIR_STITCH)
        return state.copy(
            buttons = (state.buttons - finalCost).coerceAtLeast(0),
            materials = materials,
            equippedParts = partsMap,
            partReinforcedRooms = reinforced,
            modifiedArmExplosiveGears = modArm,
            pendingRepairMinigame = null,
            repairBenchOpen = false,
            usedAnyRepair = true,
            activeBanner = RunUiBanner(
                "Reparación ${quality.displayName}",
                "${part.def.name}: $newDurability/${part.def.maxDurability}$speedMsg",
                quality.colorHex,
                2.8f
            ),
            recentEngineEvents = (state.recentEngineEvents + EngineEventType.REPAIR_COMPLETED.key).takeLast(12)
        )
    }
}
