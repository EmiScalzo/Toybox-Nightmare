package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.DefectCatalog
import com.example.data.catalog.GameCatalog
import com.example.data.model.*
import kotlin.random.Random

object ManufacturerSystem {

    fun shouldSpawnNpcInRoom(room: RoomBlueprint, state: GameRunState, rng: Random = Random.Default): Boolean {
        if (room.type == RoomType.MANUFACTURER || room.secretFlavor == SecretRoomFlavor.MANUFACTURER_ROOM) {
            return true
        }
        if (room.type == RoomType.FORBIDDEN) return true
        if (room.type == RoomType.EVENT && room.visited.not()) {
            val criticalCount = state.equippedParts.values.count { it.isDetached || it.wearState == WearState.CRITICAL }
            if (criticalCount >= 2 || rng.nextFloat() < 0.28f) return true
        }
        return false
    }

    fun buildEncounter(state: GameRunState, rng: Random = Random.Default): ManufacturerEncounter {
        val damagedPart = state.equippedParts.values.minByOrNull { if (it.isDetached) -1 else it.durability }
        val replacementPart = GameCatalog.bodyParts
            .filter { it.rarity.ordinal >= 1 && it.zone == (damagedPart?.def?.zone ?: BodyZone.ARMS) }
            .randomOrNull(rng) ?: GameCatalog.bodyParts.random(rng)
        val relic = GameCatalog.passiveItems.filter { it !in state.passiveItems }.randomOrNull(rng)
        val removableDefect = state.activeDefects.firstOrNull()
        val curseDefect = DefectCatalog.allDefects.filter { it !in state.activeDefects && it.severity >= 2 }.randomOrNull(rng)

        val allOffers = listOf(
            ManufacturerOffer(
                id = "offer_body_swap",
                title = "Trasplante de taller: ${replacementPart.name}",
                category = ManufacturerDealCategory.BODY_SWAP,
                costSummary = "Entrega tu ${damagedPart?.def?.name ?: "pieza actual"} y 3 ●",
                rewardSummary = "Recibes ${replacementPart.name} al 100%",
                riskHint = "Tu antigua pieza queda archivada en el sótano.",
                bodyZone = replacementPart.zone,
                targetPartId = replacementPart.id,
                buttonsCost = 3.coerceAtMost(state.buttons)
            ),
            ManufacturerOffer(
                id = "offer_life_pact",
                title = relic?.name?.let { "Pacto por Reliquia: $it" } ?: "Sangre por Botones Dorados",
                category = ManufacturerDealCategory.LIFE_PACT,
                costSummary = if (state.maxHearts > 2) "-1 Corazón Máximo" else "-35 Durabilidad en Torso",
                rewardSummary = relic?.let { "${it.name} + 6 ●" } ?: "+10 ● y +2 Materiales Raros",
                riskHint = "El Fabricante no devuelve vitalidad prestada.",
                itemId = relic?.id,
                maxHeartsCost = if (state.maxHearts > 2) 1 else 0
            ),
            ManufacturerOffer(
                id = "offer_defect_alchemy",
                title = "Alquimia de Costuras",
                category = ManufacturerDealCategory.DEFECT_ALCHEMY,
                costSummary = if (removableDefect != null) "Cura '${removableDefect.name}', pero añade '${curseDefect?.name ?: "Desgaste"}'" else "Acepta un defecto medio",
                rewardSummary = "+1 Corazón Máximo y reparación total",
                riskHint = "Ningún defecto desaparece sin dejar cicatriz.",
                defectId = removableDefect?.id,
                grantDefectId = curseDefect?.id
            ),
            ManufacturerOffer(
                id = "offer_memory_bargain",
                title = "Trueque de Recuerdos",
                category = ManufacturerDealCategory.MEMORY_BARGAIN,
                costSummary = "4 ● de tu bolsillo",
                rewardSummary = "Desbloquea un Recuerdo y revela todas las salas secretas del piso",
                riskHint = "Los enemigos recordarán tu rostro antes.",
                memoryId = GameCatalog.memories.firstOrNull { it.id !in state.toyboxMemory.memoriesCollected }?.id ?: "mem_founder",
                buttonsCost = 4.coerceAtMost(state.buttons)
            ),
            ManufacturerOffer(
                id = "offer_closed_box",
                title = "La Caja Cerrada",
                category = ManufacturerDealCategory.WORLD_ALTER,
                costSummary = "2 ● y paciencia (se abrirá en 3 salas)",
                rewardSummary = "Contiene una reliquia legendaria... o un juguete enjaulado.",
                riskHint = "El Fabricante sonríe tras la máscara sin decir qué hay dentro.",
                worldEffectId = "closed_mystery_box",
                buttonsCost = 2.coerceAtMost(state.buttons)
            ),
            ManufacturerOffer(
                id = "offer_alter_world",
                title = "Plano del Siguiente Piso",
                category = ManufacturerDealCategory.WORLD_ALTER,
                costSummary = "5 ●",
                rewardSummary = "El próximo piso incluirá una Sala del Fabricante y más secretos.",
                riskHint = "Aumenta la atención de los jefes.",
                worldEffectId = "next_floor_manufacturer",
                buttonsCost = 5.coerceAtMost(state.buttons)
            )
        )

        val tripleChoice = state.manufacturerTrustDelta >= 1 || rng.nextFloat() < 0.35f
        val picked = allOffers.shuffled(rng).take(if (tripleChoice) 3 else 1)

        val greeting = when {
            state.manufacturerTrustDelta > 0 -> "\"Ah, mi cliente más constante. Tengo algo especial sobre el mostrador.\""
            state.manufacturerTrustDelta < 0 -> "\"Rechazaste mi aguja antes... veamos si hoy el miedo te convence.\""
            else -> "\"Cada costura tiene un precio, muñeco. Yo fabrico soluciones.\""
        }

        return ManufacturerEncounter(
            title = if (tripleChoice) "El Fabricante • Elección Triple" else "El Fabricante",
            dialogue = greeting,
            isTripleChoice = tripleChoice,
            offers = picked
        )
    }

    fun resolveOffer(
        state: GameRunState,
        offerId: String?,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        acquireItemFn: (GameRunState, PassiveItemDef) -> GameRunState,
        onRecordTrust: (accepted: Boolean) -> Unit
    ): GameRunState {
        val encounter = state.manufacturerEncounter ?: return state
        val npc = state.manufacturerNpc?.copy(interacted = true)

        if (offerId == null) {
            onRecordTrust(false)
            return state.copy(
                manufacturerEncounter = null,
                manufacturerNpc = npc,
                manufacturerTrustDelta = state.manufacturerTrustDelta - 1,
                activeBanner = RunUiBanner(
                    "Trato Rechazado",
                    "\"Sigue caminando por mi tienda, pues.\"",
                    0xFFD98A2B,
                    2.6f
                ),
                recentEngineEvents = (state.recentEngineEvents + EngineEventType.MANUFACTURER_OFFER_REJECTED.key).takeLast(12)
            )
        }

        val offer = encounter.offers.find { it.id == offerId } ?: return state.copy(manufacturerEncounter = null)
        var s = state.copy(
            manufacturerEncounter = null,
            manufacturerNpc = npc,
            manufacturerTrustDelta = state.manufacturerTrustDelta + 1,
            buttons = (state.buttons - offer.buttonsCost).coerceAtLeast(0)
        )
        onRecordTrust(true)
        onDiscovery("secret", "MANUFACTURER")
        sfx.play(ToySfx.SYNERGY)

        when (offer.category) {
            ManufacturerDealCategory.BODY_SWAP -> {
                val partId = offer.targetPartId
                if (partId != null) {
                    val newDef = GameCatalog.partById(partId)
                    onDiscovery("part", newDef.id)
                    val map = s.equippedParts.toMutableMap()
                    map[newDef.zone] = EquippedBodyPart(newDef)
                    s = s.copy(
                        equippedParts = map,
                        activeSynergies = GameCatalog.computeActiveSynergies(map, s.passiveItems),
                        activeBanner = RunUiBanner("Trasplante Completado", "${newDef.name} cosida por El Fabricante.", 0xFFC89948, 2.8f)
                    )
                }
            }

            ManufacturerDealCategory.LIFE_PACT -> {
                if (offer.maxHeartsCost > 0 && s.maxHearts > 2) {
                    val nhMax = s.maxHearts - offer.maxHeartsCost
                    s = s.copy(maxHearts = nhMax, hearts = s.hearts.coerceAtMost(nhMax))
                } else {
                    val torso = s.equippedParts[BodyZone.TORSO]
                    if (torso != null) {
                        val map = s.equippedParts.toMutableMap()
                        map[BodyZone.TORSO] = torso.copy(durability = (torso.durability - 35).coerceAtLeast(10))
                        s = s.copy(equippedParts = map)
                    }
                }
                val item = offer.itemId?.let { GameCatalog.itemById(it) }
                s = if (item != null) {
                    acquireItemFn(s, item).copy(buttons = s.buttons + 6)
                } else {
                    val matMap = s.materials.toMutableMap()
                    matMap[RepairMaterial.SPRING] = (matMap[RepairMaterial.SPRING] ?: 0) + 1
                    matMap[RepairMaterial.PORCELAIN_SHARD] = (matMap[RepairMaterial.PORCELAIN_SHARD] ?: 0) + 1
                    s.copy(buttons = s.buttons + 10, materials = matMap)
                }
            }

            ManufacturerDealCategory.DEFECT_ALCHEMY -> {
                val mutDefects = s.activeDefects.toMutableList()
                if (offer.defectId != null) mutDefects.removeAll { it.id == offer.defectId }
                offer.grantDefectId?.let { gid ->
                    DefectCatalog.byId(gid)?.let { if (it !in mutDefects) mutDefects.add(it) }
                }
                val healedParts = s.equippedParts.mapValues { (_, ep) -> ep.copy(isDetached = false, durability = ep.def.maxDurability) }
                s = s.copy(
                    activeDefects = mutDefects,
                    equippedParts = healedParts,
                    maxHearts = s.maxHearts + 1,
                    hearts = s.hearts + 1,
                    activeBanner = RunUiBanner("Alquimia del Fabricante", "+1 Corazón Máximo y costuras intactas.", 0xFF597354, 2.8f)
                )
            }

            ManufacturerDealCategory.MEMORY_BARGAIN -> {
                val mid = offer.memoryId
                val nextMems = if (mid != null && mid !in s.toyboxMemory.memoriesCollected) {
                    onDiscovery("memory", mid)
                    s.toyboxMemory.memoriesCollected + mid
                } else s.toyboxMemory.memoriesCollected
                s = s.copy(
                    toyboxMemory = s.toyboxMemory.copy(secretCluesRevealed = true, memoriesCollected = nextMems),
                    activeBanner = RunUiBanner("Recuerdo Revelado", "Los secretos del piso quedan expuestos.", 0xFFC89948, 2.8f)
                )
            }

            ManufacturerDealCategory.WORLD_ALTER -> {
                if (offer.worldEffectId == "closed_mystery_box") {
                    s = s.copy(
                        closedMysteryBoxTimer = 3,
                        activeBanner = RunUiBanner("La Caja Cerrada", "Llevas la caja sellada. Se abrirá en 3 salas...", 0xFFC89948, 3.0f)
                    )
                } else {
                    s = s.copy(
                        nextFloorBias = "manufacturer",
                        activeBanner = RunUiBanner("Plano Alterado", "El próximo piso abrirá la sala del Fabricante.", 0xFFC89948, 2.8f)
                    )
                }
            }
        }

        return s.copy(
            recentEngineEvents = (s.recentEngineEvents + EngineEventType.MANUFACTURER_OFFER_ACCEPTED.key).takeLast(12)
        )
    }

    fun tickClosedMysteryBoxOnRoomEnter(
        state: GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): GameRunState {
        if (state.closedMysteryBoxTimer <= 0) return state
        val rem = state.closedMysteryBoxTimer - 1
        if (rem > 0) {
            return state.copy(closedMysteryBoxTimer = rem)
        }
        // Opens now!
        val pickups = state.pickups.toMutableList()
        val enemies = state.enemies.toMutableList()
        val roll = Random.nextFloat()
        val msg: String
        if (roll < 0.65f) {
            sfx.play(ToySfx.SYNERGY)
            val rarePart = GameCatalog.bodyParts.filter { it.rarity.ordinal >= 2 }.randomOrNull() ?: GameCatalog.bodyParts.random()
            pickups.add(GroundPickup(allocId(), PickupKind.NEW_PART_PEDESTAL, ARENA_W * 0.5f, ARENA_H * 0.65f, valueStr = rarePart.id))
            pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, ARENA_W * 0.4f, ARENA_H * 0.65f))
            msg = "¡Contenía una pieza rara (${rarePart.name}) y oro!"
        } else {
            sfx.play(ToySfx.BOSS_ROAR)
            enemies.add(ActiveEnemy(allocId(), EnemyType.MIMIC_CHEST, ARENA_W * 0.5f, ARENA_H * 0.35f, hp = 56f, maxHp = 56f, radius = 23f, enraged = true))
            msg = "¡Era un cofre enjaulado! Ha saltado al combate."
        }
        return state.copy(
            closedMysteryBoxTimer = 0,
            pickups = pickups,
            enemies = enemies,
            activeBanner = RunUiBanner("¡La Caja Cerrada se Abrió!", msg, 0xFFC89948, 3.2f)
        )
    }
}
