package com.example.engine

import com.example.data.model.ActiveContract
import com.example.data.model.ConsumableType
import com.example.data.model.ContractDef
import com.example.data.model.ContractRestriction
import com.example.data.model.ContractType
import com.example.data.model.EnemyHpTier
import com.example.data.model.LightSourceType
import com.example.data.model.PatchEmotion
import com.example.data.model.RepairMaterial
import com.example.data.model.hpTier
import kotlin.random.Random

object ContractSystem {

    private val contractPool: List<ContractDef> = listOf(
        ContractDef(
            id = "c_hunter",
            type = ContractType.HUNTER,
            title = "Contrato de Cazador",
            description = "Derrota 8 juguetes enemigos en el piso actual.",
            targetCount = 8,
            restriction = ContractRestriction.NO_BOMBS_3_ROOMS,
            incompatibleIds = listOf("c_protector"),
            rewardButtons = 9,
            rewardDesc = "+9 ● y +2 Bombas de Pólvora"
        ),
        ContractDef(
            id = "c_collector",
            type = ContractType.COLLECTOR,
            title = "Contrato de Recolector",
            description = "Reúne 5 materiales de costura o engranajes.",
            targetCount = 5,
            restriction = ContractRestriction.NONE,
            incompatibleIds = listOf("c_repairer"),
            rewardButtons = 8,
            rewardDesc = "+8 ● y +1 Botón Dorado"
        ),
        ContractDef(
            id = "c_explorer",
            type = ContractType.EXPLORER,
            title = "Contrato de Explorador",
            description = "Completa 4 habitaciones nuevas en la juguetería.",
            targetCount = 4,
            restriction = ContractRestriction.NO_HEART_PICKUPS,
            incompatibleIds = listOf("c_survivor"),
            rewardButtons = 10,
            rewardDesc = "+10 ● y Fuente de Luz (Farol/Linterna)"
        ),
        ContractDef(
            id = "c_protector",
            type = ContractType.PROTECTOR,
            title = "Contrato de Protector",
            description = "Rescata 1 mascota o supera 2 salas con tu compañero ileso.",
            targetCount = 2,
            restriction = ContractRestriction.NONE,
            incompatibleIds = listOf("c_hunter"),
            rewardButtons = 9,
            rewardDesc = "+9 ● y +1 Parche de Tela"
        ),
        ContractDef(
            id = "c_survivor",
            type = ContractType.SURVIVOR,
            title = "Contrato de Superviviente",
            description = "Supera 2 combates consecutivos sin recibir daño.",
            targetCount = 2,
            restriction = ContractRestriction.NO_DAMAGE_2_COMBATS,
            incompatibleIds = listOf("c_explorer"),
            rewardButtons = 12,
            rewardDesc = "+12 ● y +2 Kits de Costura"
        ),
        ContractDef(
            id = "c_repairer",
            type = ContractType.REPAIRER,
            title = "Contrato de Reparador",
            description = "Realiza 2 reparaciones en mesas o con kits.",
            targetCount = 2,
            restriction = ContractRestriction.NONE,
            incompatibleIds = listOf("c_collector"),
            rewardButtons = 8,
            rewardDesc = "+8 ● y +4 Materiales variados"
        ),
        ContractDef(
            id = "c_relic",
            type = ContractType.RELIC_SEEKER,
            title = "Buscador de Reliquias",
            description = "Descubre 2 pistas secretas, salas de lore o juguetes perdidos.",
            targetCount = 2,
            restriction = ContractRestriction.NO_REPAIR_ON_FLOOR,
            incompatibleIds = emptyList(),
            rewardButtons = 11,
            rewardDesc = "+11 ● y +2 Llaves de Latón"
        ),
        ContractDef(
            id = "c_elite",
            type = ContractType.ELITE_HUNTER,
            title = "Caza Élites",
            description = "Derrota 2 enemigos Resistentes, Élites o Jefes.",
            targetCount = 2,
            restriction = ContractRestriction.NONE,
            incompatibleIds = emptyList(),
            rewardButtons = 13,
            rewardDesc = "+13 ● y +2 Botones Dorados"
        )
    )

    val secretLastCompanionContract = ContractDef(
        id = "c_last_companion",
        type = ContractType.LAST_COMPANION,
        title = "El Último Compañero",
        description = "Historia Secreta: Lleva un compañero vivo y completa 3 salas sin abandonarlo.",
        targetCount = 3,
        restriction = ContractRestriction.NO_HEART_PICKUPS,
        incompatibleIds = emptyList(),
        rewardButtons = 15,
        rewardDesc = "+15 ●, +1 Corazón y +2 Botones Dorados",
        isStorySecret = true
    )

    fun generateBoardContracts(floorNumber: Int, rng: Random = Random.Default): List<ContractDef> {
        val picks = contractPool.shuffled(rng).take(2).toMutableList()
        if (floorNumber >= 2 || rng.nextFloat() < 0.30f) {
            picks.add(secretLastCompanionContract)
        } else {
            picks.add(contractPool.first { it !in picks })
        }
        return picks
    }

    fun acceptContract(state: GameRunState, def: ContractDef): GameRunState {
        if (state.activeContracts.any { it.def.id == def.id }) return state
        val openActive = state.activeContracts.filter { !it.completed && !it.failed }
        if (openActive.size >= 2) {
            return state.copy(
                activeBanner = RunUiBanner("Límite de Contratos", "Solo puedes llevar 2 encargos activos a la vez.", 0xFFD98A2B, 2.2f)
            )
        }
        val conflict = openActive.firstOrNull {
            it.def.id in def.incompatibleIds || def.id in it.def.incompatibleIds
        }
        if (conflict != null) {
            return state.copy(
                activeBanner = RunUiBanner("Contrato Incompatible", "${def.title} es incompatible con ${conflict.def.title}.", 0xFFD13B42, 2.6f)
            )
        }
        return state.copy(
            activeContracts = state.activeContracts + ActiveContract(def = def),
            contractBoardOpen = false,
            activeBanner = RunUiBanner("Contrato Aceptado: ${def.title}", def.description, 0xFF597354, 2.6f)
        )
    }

    fun abandonContract(state: GameRunState, contractId: String): GameRunState {
        return state.copy(
            activeContracts = state.activeContracts.filterNot { it.def.id == contractId }
        )
    }

    fun onEnemyKilled(state: GameRunState, enemy: ActiveEnemy): GameRunState {
        val isElite = enemy.type.hpTier().ordinal >= EnemyHpTier.RESISTANT.ordinal || enemy.type.isBoss
        return advanceContracts(state) { c ->
            when (c.def.type) {
                ContractType.HUNTER -> c.copy(progress = c.progress + 1)
                ContractType.ELITE_HUNTER -> if (isElite) c.copy(progress = c.progress + 1) else c
                else -> c
            }
        }
    }

    fun onMaterialOrItemCollected(state: GameRunState, count: Int = 1): GameRunState {
        return advanceContracts(state) { c ->
            if (c.def.type == ContractType.COLLECTOR) c.copy(progress = c.progress + count) else c
        }
    }

    fun onRelicOrClueFound(state: GameRunState): GameRunState {
        return advanceContracts(state) { c ->
            if (c.def.type == ContractType.RELIC_SEEKER) c.copy(progress = c.progress + 1) else c
        }
    }

    fun onRepairDone(state: GameRunState, viaBench: Boolean): GameRunState {
        var s = state
        if (viaBench) {
            s = failMatchingRestriction(s, ContractRestriction.NO_REPAIR_ON_FLOOR)
        }
        return advanceContracts(s) { c ->
            if (c.def.type == ContractType.REPAIRER) c.copy(progress = c.progress + 1) else c
        }
    }

    fun onBombUsed(state: GameRunState): GameRunState {
        return failMatchingRestriction(state, ContractRestriction.NO_BOMBS_3_ROOMS)
    }

    fun onHeartPicked(state: GameRunState): GameRunState {
        return failMatchingRestriction(state, ContractRestriction.NO_HEART_PICKUPS)
    }

    fun onPlayerDamaged(state: GameRunState): GameRunState {
        return failMatchingRestriction(state, ContractRestriction.NO_DAMAGE_2_COMBATS)
    }

    fun onRoomCleared(state: GameRunState, tookDamageInRoom: Boolean): GameRunState {
        return advanceContracts(state) { c ->
            var next = c
            when (c.def.type) {
                ContractType.EXPLORER -> next = next.copy(progress = next.progress + 1)
                ContractType.PROTECTOR -> if (state.activeCompanions.isNotEmpty()) {
                    next = next.copy(progress = next.progress + 1)
                }
                ContractType.SURVIVOR -> if (!tookDamageInRoom) {
                    next = next.copy(progress = next.progress + 1)
                }
                ContractType.LAST_COMPANION -> if (state.activeCompanions.any { !it.broken }) {
                    next = next.copy(progress = next.progress + 1)
                }
                else -> Unit
            }
            if (c.def.restriction == ContractRestriction.NO_BOMBS_3_ROOMS) {
                next = next.copy(restrictionCounter = next.restrictionCounter + 1)
            }
            next
        }
    }

    private fun failMatchingRestriction(state: GameRunState, restriction: ContractRestriction): GameRunState {
        var anyFailed = false
        val updated = state.activeContracts.map { c ->
            if (!c.completed && !c.failed && c.def.restriction == restriction) {
                anyFailed = true
                c.copy(failed = true)
            } else c
        }
        return if (anyFailed) {
            state.copy(
                activeContracts = updated,
                activeBanner = RunUiBanner("Contrato Fallido", "Incumpliste la condición: ${restriction.label}.", 0xFFD13B42, 2.5f)
            )
        } else state
    }

    private fun advanceContracts(state: GameRunState, mapper: (ActiveContract) -> ActiveContract): GameRunState {
        if (state.activeContracts.isEmpty()) return state
        var s = state
        val updated = s.activeContracts.map { c ->
            if (c.completed || c.failed) return@map c
            val next = mapper(c)
            if (next.progress >= next.def.targetCount) {
                s = grantContractReward(s, next.def)
                next.copy(progress = next.def.targetCount, completed = true)
            } else next
        }
        return s.copy(activeContracts = updated)
    }

    private fun grantContractReward(state: GameRunState, def: ContractDef): GameRunState {
        var s = state.copy(
            buttons = state.buttons + def.rewardButtons,
            completedContractsCount = state.completedContractsCount + 1,
            activeBanner = RunUiBanner("¡Contrato Cumplido: ${def.title}!", def.rewardDesc, 0xFFE5A93B, 3.2f)
        )
        s = when (def.type) {
            ContractType.HUNTER -> s.copy(consumables = s.consumables + listOf(ConsumableType.BOMB, ConsumableType.BOMB))
            ContractType.COLLECTOR -> s.copy(goldenButtons = s.goldenButtons + 1)
            ContractType.EXPLORER -> {
                val giftLight = if (LightSourceType.MECHANICAL_LANTERN !in s.ownedLightSources) {
                    LightSourceType.MECHANICAL_LANTERN
                } else LightSourceType.FLASHLIGHT
                LightingSystem.grantLightSource(s, giftLight)
            }
            ContractType.PROTECTOR -> s.copy(consumables = s.consumables + ConsumableType.REPAIR_PATCH)
            ContractType.SURVIVOR -> s.copy(consumables = s.consumables + listOf(ConsumableType.SEWING_KIT, ConsumableType.SEWING_KIT))
            ContractType.REPAIRER -> {
                val mats = s.materials.toMutableMap()
                mats[RepairMaterial.THREAD] = (mats[RepairMaterial.THREAD] ?: 0) + 2
                mats[RepairMaterial.GEAR] = (mats[RepairMaterial.GEAR] ?: 0) + 2
                s.copy(materials = mats)
            }
            ContractType.RELIC_SEEKER -> s.copy(consumables = s.consumables + listOf(ConsumableType.KEY, ConsumableType.KEY))
            ContractType.ELITE_HUNTER -> s.copy(goldenButtons = s.goldenButtons + 2)
            ContractType.LAST_COMPANION -> s.copy(
                hearts = (s.hearts + 1).coerceAtMost(s.maxHearts),
                goldenButtons = s.goldenButtons + 2
            )
        }
        return EmotionSystem.triggerEmotion(s, PatchEmotion.JOY, 25f, 20f, "Satisfacción al completar un encargo.")
    }
}
