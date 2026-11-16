package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.GameCatalog
import com.example.data.model.BodyZone
import com.example.data.model.CompanionId
import com.example.data.model.ConsumableType
import com.example.data.model.EnemyType
import com.example.data.model.RoomMechanic
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object CompanionSystem {

    fun buildShrineDialog(
        id: CompanionId,
        activeCompanions: List<ActiveCompanion>
    ): PendingCompanionDialog {
        val fullWarning = if (activeCompanions.size >= 2) {
            val replaced = activeCompanions.first().id
            " Ya llevas 2 acompañantes: aceptar sustituirá a ${replaced.displayName}."
        } else ""
        return PendingCompanionDialog(
            companionId = id,
            title = id.displayName,
            subtitle = "${id.rarity.displayName} • Compañero con condición",
            narrative = "${id.interestDesc}\n\nCondición: ${id.conditionDesc}$fullWarning",
            options = listOf(
                CompanionChoiceOption(
                    id = "recruit_accept",
                    label = "Aceptar su compañía",
                    description = "Se unirá a tu recorrido con sus propias reglas."
                ),
                CompanionChoiceOption(
                    id = "recruit_reject",
                    label = if (id == CompanionId.BUNNY) "Dejarlo atrás" else "Rechazar encuentro",
                    description = if (id == CompanionId.BUNNY) {
                        "Advertencia: El Conejito no olvida el abandono."
                    } else {
                        "Seguir solo por ahora."
                    },
                    isDanger = id == CompanionId.BUNNY
                )
            )
        )
    }

    fun buildInspectDialog(
        companion: ActiveCompanion,
        state: GameRunState
    ): PendingCompanionDialog {
        val opts = mutableListOf<CompanionChoiceOption>()
        when (companion.id) {
            CompanionId.BROKEN_SOLDIER -> {
                if (companion.broken) {
                    opts.add(
                        CompanionChoiceOption(
                            id = "repair_soldier_buttons",
                            label = "Reparar mecanismo (3 ●)",
                            description = "Reemplaza su resorte para que vuelva a disparar.",
                            buttonCost = 3
                        )
                    )
                    if (ConsumableType.SEWING_KIT in state.consumables) {
                        opts.add(
                            CompanionChoiceOption(
                                id = "repair_soldier_kit",
                                label = "Usar Kit de Costura",
                                description = "Repara al Soldadito gratis consumiendo 1 Kit."
                            )
                        )
                    }
                }
            }
            CompanionId.RUSTY_BALLERINA -> {
                if (companion.broken) {
                    opts.add(
                        CompanionChoiceOption(
                            id = "oil_ballerina",
                            label = "Aceitar engranajes (2 ●)",
                            description = "Devuelve la melodía que frena balas enemigas.",
                            buttonCost = 2
                        )
                    )
                }
            }
            CompanionId.WHISPER_PUPPET -> {
                opts.add(
                    CompanionChoiceOption(
                        id = "silence_puppet",
                        label = "Coser boca temporalmente (2 ●)",
                        description = "Reinicia su paciencia (${companion.roomsHeld}/4 salas) antes de que traicione.",
                        buttonCost = 2
                    )
                )
            }
            CompanionId.CLOCKWORK_CAT -> {
                opts.add(
                    CompanionChoiceOption(
                        id = "wind_cat",
                        label = "Dar cuerda al gato (1 ●)",
                        description = "Calma su codicia felina durante varias salas.",
                        buttonCost = 1
                    )
                )
            }
            CompanionId.FACELESS_DOLL -> {
                opts.add(
                    CompanionChoiceOption(
                        id = "doll_offer_now",
                        label = "Pedir un regalo",
                        description = "Ofrece una reliquia gratuita... con precio oculto."
                    )
                )
            }
            CompanionId.BUNNY -> {
                opts.add(
                    CompanionChoiceOption(
                        id = "pet_bunny",
                        label = "Acariciar orejas",
                        description = "Sigue recogiendo botones con lealtad."
                    )
                )
            }
            else -> Unit
        }
        opts.add(
            CompanionChoiceOption(
                id = "abandon_companion",
                label = if (companion.id == CompanionId.BUNNY) "Abandonar al Conejito" else "Despedir compañero",
                description = if (companion.id == CompanionId.BUNNY) {
                    "Se marchará herido... y quizá regrese con rencor en otra partida."
                } else {
                    "Libera su espacio en tu grupo."
                },
                isDanger = companion.id == CompanionId.BUNNY
            )
        )
        opts.add(
            CompanionChoiceOption(
                id = "close_inspect",
                label = "Cerrar",
                description = "Continuar explorando."
            )
        )
        val statusTag = when {
            companion.broken -> "AVERIADO — Requiere reparación"
            companion.needsAttention -> "INQUIETO — Requiere atención"
            companion.id == CompanionId.BROKEN_SOLDIER -> "Operativo (${companion.hp}/${companion.maxHp} resistencia)"
            companion.id == CompanionId.WHISPER_PUPPET -> "Paciencia: ${companion.roomsHeld}/4 salas"
            else -> "Activo"
        }
        return PendingCompanionDialog(
            companionId = companion.id,
            title = companion.id.displayName,
            subtitle = statusTag,
            narrative = "${companion.id.interestDesc}\nCondición: ${companion.id.conditionDesc}",
            options = opts
        )
    }

    fun buildFacelessDollGiftDialog(state: GameRunState): PendingCompanionDialog? {
        val candidate = GameCatalog.passiveItems.filter { it !in state.passiveItems }.randomOrNull()
        val itemDesc = candidate?.let { "${it.name}: ${it.description}" }
            ?: "Un frasco de botones sellados (+6 ● y +1 Parche de Tela)."
        return PendingCompanionDialog(
            companionId = CompanionId.FACELESS_DOLL,
            title = "Regalo de la Muñeca sin Cara",
            subtitle = "Un obsequio silencioso",
            narrative = "Extiende sus manos de porcelana sin rostro y te ofrece:\n\n$itemDesc\n\n(No pide nada visible a cambio...)",
            offeredItemId = candidate?.id,
            options = listOf(
                CompanionChoiceOption(
                    id = "doll_accept_gift",
                    label = "Aceptar el regalo",
                    description = "Tomar su obsequio ahora."
                ),
                CompanionChoiceOption(
                    id = "doll_refuse_gift",
                    label = "Rechazar",
                    description = "No aceptar favores de una muñeca sin rostro."
                )
            )
        )
    }

    fun resolveCompanionDialog(
        state: GameRunState,
        optionId: String,
        sfx: SoundEffectsEngine,
        allocId: () -> Long,
        onDiscovery: (String, String) -> Unit,
        onGrudge: (String) -> Unit,
        acquireItemFn: (GameRunState, com.example.data.model.PassiveItemDef) -> GameRunState
    ): GameRunState {
        val dlg = state.pendingCompanionDialog ?: return state
        var s = state.copy(pendingCompanionDialog = null)

        when (optionId) {
            "close_inspect", "doll_refuse_gift" -> return s

            "recruit_accept" -> {
                onDiscovery("companion", dlg.companionId.name)
                sfx.play(ToySfx.PICKUP_ITEM)
                var list = s.activeCompanions.toMutableList()
                if (list.any { it.id == dlg.companionId }) {
                    return s.copy(
                        activeBanner = RunUiBanner(dlg.companionId.displayName, "Ya te acompaña en esta partida.", 0xFFC89948, 2.2f)
                    )
                }
                if (list.size >= 2) {
                    val dropped = list.removeAt(0)
                    if (dropped.id == CompanionId.BUNNY) {
                        onGrudge(CompanionId.BUNNY.name)
                    }
                }
                list.add(
                    ActiveCompanion(
                        id = dlg.companionId,
                        x = (s.playerX - 36f).coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f),
                        y = s.playerY,
                        hp = if (dlg.companionId == CompanionId.BROKEN_SOLDIER) 4 else 3,
                        maxHp = if (dlg.companionId == CompanionId.BROKEN_SOLDIER) 4 else 3
                    )
                )
                return s.copy(
                    activeCompanions = list,
                    activeBanner = RunUiBanner(
                        "Nuevo Compañero: ${dlg.companionId.displayName}",
                        dlg.companionId.interestDesc,
                        0xFF597354,
                        3.0f
                    )
                )
            }

            "recruit_reject" -> {
                if (dlg.companionId == CompanionId.BUNNY) {
                    onGrudge(CompanionId.BUNNY.name)
                    return s.copy(
                        activeBanner = RunUiBanner(
                            "Abandono Recordado",
                            "El Conejito baja las orejas y desaparece entre las cajas...",
                            0xFFD13B42,
                            3.0f
                        )
                    )
                }
                return s
            }

            "abandon_companion" -> {
                if (dlg.companionId == CompanionId.BUNNY) {
                    onGrudge(CompanionId.BUNNY.name)
                }
                val rem = s.activeCompanions.filterNot { it.id == dlg.companionId }
                return s.copy(
                    activeCompanions = rem,
                    activeBanner = RunUiBanner(
                        if (dlg.companionId == CompanionId.BUNNY) "Conejito Abandonado" else "${dlg.companionId.displayName} se marchó",
                        if (dlg.companionId == CompanionId.BUNNY) "La tienda recuerda tu traición para otra partida." else "Ya no te sigue.",
                        if (dlg.companionId == CompanionId.BUNNY) 0xFFD13B42 else 0xFFC89948,
                        2.8f
                    )
                )
            }

            "pet_bunny" -> {
                sfx.play(ToySfx.PICKUP_BUTTON)
                return s.copy(
                    activeBanner = RunUiBanner("El Conejito", "Mueve feliz la cola y vigila tus botones.", 0xFF597354, 2.0f)
                )
            }

            "repair_soldier_buttons" -> {
                if (s.buttons < 3) return s
                sfx.play(ToySfx.REPAIR_STITCH)
                val updated = s.activeCompanions.map {
                    if (it.id == CompanionId.BROKEN_SOLDIER) it.copy(broken = false, hp = it.maxHp, needsAttention = false)
                    else it
                }
                return s.copy(
                    buttons = s.buttons - 3,
                    activeCompanions = updated,
                    activeBanner = RunUiBanner("Soldadito Reparado", "Su resorte vuelve a girar firme en combate.", 0xFF597354, 2.4f)
                )
            }

            "repair_soldier_kit" -> {
                val idx = s.consumables.indexOf(ConsumableType.SEWING_KIT)
                if (idx == -1) return s
                sfx.play(ToySfx.REPAIR_STITCH)
                val remCons = s.consumables.toMutableList().apply { removeAt(idx) }
                val updated = s.activeCompanions.map {
                    if (it.id == CompanionId.BROKEN_SOLDIER) it.copy(broken = false, hp = it.maxHp, needsAttention = false)
                    else it
                }
                return s.copy(
                    consumables = remCons,
                    selectedConsumableIdx = 0,
                    activeCompanions = updated,
                    activeBanner = RunUiBanner("Soldadito Cosido", "Vuelve a cubrir tus espaldas.", 0xFF597354, 2.4f)
                )
            }

            "oil_ballerina" -> {
                if (s.buttons < 2) return s
                sfx.play(ToySfx.REPAIR_STITCH)
                val updated = s.activeCompanions.map {
                    if (it.id == CompanionId.RUSTY_BALLERINA) it.copy(broken = false, consecutiveDamagedRooms = 0, needsAttention = false)
                    else it
                }
                return s.copy(
                    buttons = s.buttons - 2,
                    activeCompanions = updated,
                    activeBanner = RunUiBanner("Bailarina Aceitada", "Su cajita musical vuelve a calmar las balas.", 0xFF597354, 2.4f)
                )
            }

            "silence_puppet" -> {
                if (s.buttons < 2) return s
                sfx.play(ToySfx.REPAIR_STITCH)
                val updated = s.activeCompanions.map {
                    if (it.id == CompanionId.WHISPER_PUPPET) it.copy(roomsHeld = 0, needsAttention = false)
                    else it
                }
                return s.copy(
                    buttons = s.buttons - 2,
                    activeCompanions = updated,
                    activeBanner = RunUiBanner("Títere Silenciado", "Los hilos de su boca ceden por unas salas más.", 0xFFC89948, 2.3f)
                )
            }

            "wind_cat" -> {
                if (s.buttons < 1) return s
                sfx.play(ToySfx.SYNERGY)
                val updated = s.activeCompanions.map {
                    if (it.id == CompanionId.CLOCKWORK_CAT) it.copy(roomsHeld = 0, needsAttention = false)
                    else it
                }
                return s.copy(
                    buttons = s.buttons - 1,
                    activeCompanions = updated,
                    activeBanner = RunUiBanner("Gato de Cuerda", "Ronronea satisfecho con cuerda nueva.", 0xFFC89948, 2.2f)
                )
            }

            "doll_offer_now" -> {
                return s.copy(pendingCompanionDialog = buildFacelessDollGiftDialog(s))
            }

            "doll_accept_gift" -> {
                val item = dlg.offeredItemId?.let { GameCatalog.itemById(it) }
                s = if (item != null) {
                    acquireItemFn(s, item)
                } else {
                    sfx.play(ToySfx.PICKUP_ITEM)
                    s.copy(buttons = s.buttons + 6, consumables = s.consumables + ConsumableType.REPAIR_PATCH)
                }

                // Apply Hidden Price!
                val priceRoll = Random.nextInt(4)
                val priceMsg: String
                when (priceRoll) {
                    0 -> {
                        if (s.hearts > 1) {
                            s = s.copy(hearts = s.hearts - 1)
                            priceMsg = "Un dolor sordo en el pecho: -1 Corazón cobrado en silencio."
                        } else {
                            val stolen = minOf(s.buttons, 4)
                            s = s.copy(buttons = s.buttons - stolen)
                            priceMsg = "Tus bolsillos suenan vacíos: -$stolen Botones desaparecieron."
                        }
                    }
                    1 -> {
                        val stolen = maxOf(2, s.buttons / 2).coerceAtMost(s.buttons)
                        s = s.copy(buttons = (s.buttons - stolen).coerceAtLeast(0))
                        priceMsg = "La muñeca se cobró $stolen Botones sin que lo notaras."
                    }
                    2 -> {
                        val part = s.equippedParts.values.firstOrNull { !it.isDetached }
                        if (part != null) {
                            val mut = s.equippedParts.toMutableMap()
                            mut[part.def.zone] = part.copy(durability = (part.durability - 35).coerceAtLeast(8))
                            s = s.copy(equippedParts = mut)
                            priceMsg = "Tu ${part.def.name} se rasgó misteriosamente (-35 durabilidad)."
                        } else {
                            priceMsg = "Un frío recorrió tus costuras..."
                        }
                    }
                    else -> {
                        s = s.copy(
                            enemies = s.enemies + ActiveEnemy(
                                id = allocId(),
                                type = EnemyType.MIRROR_COPY,
                                x = ARENA_W * 0.5f,
                                y = ARENA_H * 0.25f,
                                hp = EnemyType.MIRROR_COPY.maxHp * s.difficulty.enemyHpMult,
                                maxHp = EnemyType.MIRROR_COPY.maxHp * s.difficulty.enemyHpMult,
                                radius = 20f
                            )
                        )
                        priceMsg = "Tu reflejo cobró vida propia para cobrarse el trato."
                    }
                }
                val updatedDoll = s.activeCompanions.map {
                    if (it.id == CompanionId.FACELESS_DOLL) it.copy(giftsOffered = it.giftsOffered + 1) else it
                }
                return s.copy(
                    activeCompanions = updatedDoll,
                    activeBanner = RunUiBanner("Precio Oculto de la Muñeca", priceMsg, 0xFFD13B42, 3.6f)
                )
            }
        }
        return s
    }

    fun onEnterRoomCompanions(
        state: GameRunState,
        room: RoomBlueprint,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        wasVisited: Boolean = room.visited
    ): GameRunState {
        var s = state
        val updatedRoom = room
        var roomsMap = s.dungeon.rooms.toMutableMap()
        val extraEnemies = mutableListOf<ActiveEnemy>()
        val extraPickups = mutableListOf<GroundPickup>()
        var banner = s.activeBanner
        var pendingDlg = s.pendingCompanionDialog

        // Repair Room automatically fixes broken soldier & ballerina!
        if (updatedRoom.type == com.example.data.model.RoomType.REPAIR && s.activeCompanions.any { it.broken }) {
            sfx.play(ToySfx.REPAIR_STITCH)
            s = s.copy(
                activeCompanions = s.activeCompanions.map {
                    it.copy(broken = false, hp = it.maxHp, consecutiveDamagedRooms = 0, needsAttention = false)
                },
                activeBanner = RunUiBanner("Mesa de Costura", "Tus compañeros averiados fueron reparados.", 0xFF597354, 2.5f)
            )
            banner = s.activeBanner
        }

        // Cross-run Grudge: Betrayed Bunny ambushes in a combat room!
        if (!s.betrayedBunnySpawnedThisRun &&
            CompanionId.BUNNY in s.pendingGrudgeCompanions &&
            updatedRoom.type == com.example.data.model.RoomType.COMBAT &&
            !updatedRoom.cleared
        ) {
            val hp = EnemyType.BETRAYED_BUNNY.maxHp * s.difficulty.enemyHpMult * (1f + (s.floorNumber - 1) * 0.14f)
            extraEnemies.add(
                ActiveEnemy(
                    id = allocId(),
                    type = EnemyType.BETRAYED_BUNNY,
                    x = ARENA_W * 0.5f,
                    y = ARENA_H * 0.25f,
                    hp = hp,
                    maxHp = hp,
                    radius = 22f,
                    telegraphTimer = 0.8f
                )
            )
            s = s.copy(
                betrayedBunnySpawnedThisRun = true,
                pendingGrudgeCompanions = s.pendingGrudgeCompanions - CompanionId.BUNNY
            )
            banner = RunUiBanner(
                "¡EL CONEJITO NO OLVIDA!",
                "El compañero que abandonaste regresó para vengarse.",
                0xFFD13B42,
                3.8f
            )
        }

        // Process each active companion room-entry condition
        val nextCompanions = mutableListOf<ActiveCompanion>()
        for (comp in s.activeCompanions) {
            var c = comp.copy(
                x = (s.playerX - 38f).coerceIn(WALL_THICKNESS + 24f, ARENA_W - WALL_THICKNESS - 24f),
                y = (s.playerY + 18f).coerceIn(WALL_THICKNESS + 24f, ARENA_H - WALL_THICKNESS - 24f),
                roomsHeld = comp.roomsHeld + 1
            )
            when (c.id) {
                CompanionId.CLOCKWORK_CAT -> {
                    val curRoom = roomsMap[s.currentCoord] ?: updatedRoom
                    if (curRoom.secretDoors.isNotEmpty()) {
                        val revealed = curRoom.copy(
                            doors = curRoom.doors + curRoom.secretDoors,
                            secretDoors = emptySet()
                        )
                        roomsMap[s.currentCoord] = revealed
                        s = s.copy(currentRoom = revealed, dungeon = s.dungeon.copy(rooms = roomsMap))
                        banner = RunUiBanner("Gato de Cuerda", "¡Maullido metálico! Reveló una puerta secreta.", 0xFFC89948, 2.8f)
                    }
                    if (s.buttons > 14 && c.roomsHeld >= 2) {
                        s = s.copy(buttons = s.buttons - 1)
                        c = c.copy(needsAttention = true)
                        banner = RunUiBanner("Gato de Cuerda", "Se cobró 1 Botón de tus bolsillos llenos.", 0xFFD98A2B, 2.2f)
                    }
                }
                CompanionId.RUSTY_BALLERINA -> {
                    if (!c.broken && s.damagedInPrevRoom) {
                        val streak = c.consecutiveDamagedRooms + 1
                        if (streak >= 2) {
                            c = c.copy(broken = true, needsAttention = true, consecutiveDamagedRooms = streak)
                            banner = RunUiBanner("Bailarina Atascada", "Recibiste daño seguido; necesita aceite (toca su icono).", 0xFFD13B42, 2.8f)
                        } else {
                            c = c.copy(consecutiveDamagedRooms = streak)
                        }
                    } else if (!s.damagedInPrevRoom) {
                        c = c.copy(consecutiveDamagedRooms = 0)
                    }
                }
                CompanionId.WHISPER_PUPPET -> {
                    if (c.roomsHeld >= 5 || s.toyboxMemory.compassionScore <= -2) {
                        // Betrays player!
                        extraEnemies.add(
                            ActiveEnemy(
                                id = allocId(),
                                type = EnemyType.SHADOW_PUPPET,
                                x = c.x,
                                y = c.y,
                                hp = EnemyType.SHADOW_PUPPET.maxHp * 1.4f,
                                maxHp = EnemyType.SHADOW_PUPPET.maxHp * 1.4f,
                                radius = 21f
                            )
                        )
                        banner = RunUiBanner("Traición del Títere", "El Títere Murmurante cortó tus hilos y ataca.", 0xFFD13B42, 3.2f)
                        continue // removed from companions!
                    } else if (c.roomsHeld >= 3) {
                        c = c.copy(needsAttention = true)
                    }
                }
                CompanionId.FACELESS_DOLL -> {
                    if (pendingDlg == null && c.roomsHeld % 3 == 0 && c.giftsOffered < 3 && !updatedRoom.cleared) {
                        pendingDlg = buildFacelessDollGiftDialog(s)
                    }
                }
                else -> Unit
            }
            nextCompanions.add(c)
        }

        // Spawn a random free Companion Shrine across any unvisited non-start/non-boss room (~16% chance) if no cage is in the room
        val hasCage = s.roomInteractables.any { it.type == com.example.data.model.InteractableType.COMPANION_CAGE }
        if (!hasCage &&
            updatedRoom.type in setOf(
                com.example.data.model.RoomType.COMBAT,
                com.example.data.model.RoomType.EVENT,
                com.example.data.model.RoomType.TREASURE,
                com.example.data.model.RoomType.SECRET,
                com.example.data.model.RoomType.LORE,
                com.example.data.model.RoomType.FORBIDDEN
            ) && !wasVisited && s.activeCompanions.size < 2 && Random.nextFloat() < 0.16f
        ) {
            val pool = CompanionId.entries.filter { id ->
                s.activeCompanions.none { it.id == id } && id !in s.pendingGrudgeCompanions
            }
            val chosen = pool.randomOrNull()
            if (chosen != null) {
                extraPickups.add(
                    GroundPickup(
                        id = allocId(),
                        kind = PickupKind.COMPANION_SHRINE,
                        x = ARENA_W * 0.5f,
                        y = ARENA_H * 0.68f,
                        valueStr = chosen.name
                    )
                )
            }
        }

        return s.copy(
            activeCompanions = nextCompanions,
            enemies = s.enemies + extraEnemies,
            pickups = s.pickups + extraPickups,
            activeBanner = banner,
            pendingCompanionDialog = pendingDlg
        )
    }

    fun updateCompanions(
        state: GameRunState,
        dt: Float,
        allocId: () -> Long,
        sfx: SoundEffectsEngine
    ): GameRunState {
        if (state.activeCompanions.isEmpty()) return state
        var s = state
        val updated = mutableListOf<ActiveCompanion>()
        val newProjs = mutableListOf<ActiveProjectile>()
        val collectedPickups = mutableSetOf<Long>()
        var addedButtons = 0
        var addedGolden = 0
        var updatedEnemies = s.enemies
        var updatedProjs = s.projectiles
        var banner = s.activeBanner

        s.activeCompanions.forEachIndexed { idx, comp0 ->
            var c = comp0.copy(actionTimer = (comp0.actionTimer - dt).coerceAtLeast(0f))

            // Determine desired follow or action target
            var targetX = s.playerX + cos(s.facingAngle + (idx + 1) * 2.1f) * 44f
            var targetY = s.playerY + sin(s.facingAngle + (idx + 1) * 2.1f) * 44f
            var moveSpeed = 230f

            if (c.id == CompanionId.BUNNY) {
                val coin = s.pickups.firstOrNull {
                    (it.kind == PickupKind.BUTTON || it.kind == PickupKind.GOLD_BUTTON) &&
                        it.id !in collectedPickups &&
                        hypot(it.x - c.x, it.y - c.y) < 380f
                }
                if (coin != null) {
                    targetX = coin.x
                    targetY = coin.y
                    moveSpeed = 310f
                    if (hypot(coin.x - c.x, coin.y - c.y) < 24f) {
                        collectedPickups.add(coin.id)
                        sfx.play(ToySfx.PICKUP_BUTTON)
                        if (coin.kind == PickupKind.GOLD_BUTTON) {
                            addedGolden += 1
                            addedButtons += 5
                        } else {
                            addedButtons += coin.amount
                        }
                    }
                }
            }

            // Smooth movement unless Broken Soldier / Ballerina is broken & stalled
            if (!(c.id == CompanionId.BROKEN_SOLDIER && c.broken)) {
                val dx = targetX - c.x
                val dy = targetY - c.y
                val d = hypot(dx, dy)
                if (d > 8f) {
                    val step = minOf(d, moveSpeed * dt)
                    c = c.copy(
                        x = (c.x + (dx / d) * step).coerceIn(WALL_THICKNESS + 20f, ARENA_W - WALL_THICKNESS - 20f),
                        y = (c.y + (dy / d) * step).coerceIn(WALL_THICKNESS + 20f, ARENA_H - WALL_THICKNESS - 20f)
                    )
                }
            }

            // Combat & condition ticks
            when (c.id) {
                CompanionId.BROKEN_SOLDIER -> {
                    if (!c.broken) {
                        if (c.actionTimer <= 0f && updatedEnemies.isNotEmpty()) {
                            val nearest = updatedEnemies.minByOrNull { hypot(it.x - c.x, it.y - c.y) }
                            if (nearest != null && hypot(nearest.x - c.x, nearest.y - c.y) < 520f) {
                                val ang = atan2(nearest.y - c.y, nearest.x - c.x)
                                newProjs.add(
                                    ActiveProjectile(
                                        id = allocId(),
                                        x = c.x, y = c.y,
                                        vx = cos(ang) * 460f, vy = sin(ang) * 460f,
                                        radius = 7f, damage = 5.5f,
                                        fromPlayer = true, colorHex = 0xFFE5A93B
                                    )
                                )
                                c = c.copy(actionTimer = 1.25f)
                            }
                        }
                        // Check incoming enemy projectiles damaging the soldier!
                        val hitProj = updatedProjs.firstOrNull { !it.fromPlayer && hypot(it.x - c.x, it.y - c.y) < 18f + it.radius }
                        if (hitProj != null) {
                            updatedProjs = updatedProjs.filterNot { it.id == hitProj.id }
                            val nhp = (c.hp - 1).coerceAtLeast(0)
                            val nowBroken = nhp == 0
                            c = c.copy(hp = nhp, broken = nowBroken, needsAttention = nowBroken)
                            if (nowBroken) {
                                sfx.play(ToySfx.PART_DETACH)
                                banner = RunUiBanner(
                                    "¡Soldadito Descompuesto!",
                                    "Recibió demasiado daño. Toca su icono arriba para repararlo.",
                                    0xFFD13B42,
                                    3.2f
                                )
                            }
                        }
                    }
                }
                CompanionId.WHISPER_PUPPET -> {
                    if (c.actionTimer <= 0f && updatedEnemies.isNotEmpty()) {
                        updatedEnemies = updatedEnemies.map { e ->
                            val d = hypot(e.x - c.x, e.y - c.y)
                            if (!e.type.isBoss && d < 240f && d > 12f) {
                                val pullX = (c.x - e.x) / d * 28f
                                val pullY = (c.y - e.y) / d * 28f
                                e.copy(x = e.x + pullX, y = e.y + pullY, hp = e.hp - 1.5f)
                            } else e
                        }
                        c = c.copy(actionTimer = 2.6f)
                    }
                }
                CompanionId.RUSTY_BALLERINA -> {
                    if (!c.broken) {
                        updatedProjs = updatedProjs.map { p ->
                            if (!p.fromPlayer && hypot(p.x - c.x, p.y - c.y) < 170f) {
                                p.copy(vx = p.vx * (1f - 0.55f * dt), vy = p.vy * (1f - 0.55f * dt))
                            } else p
                        }
                    }
                }
                else -> Unit
            }

            updated.add(c)
        }

        val finalPickups = if (collectedPickups.isNotEmpty()) {
            s.pickups.filterNot { it.id in collectedPickups }
        } else s.pickups

        return s.copy(
            activeCompanions = updated,
            pickups = finalPickups,
            buttons = s.buttons + addedButtons,
            goldenButtons = s.goldenButtons + addedGolden,
            enemies = updatedEnemies,
            projectiles = updatedProjs + newProjs,
            activeBanner = banner
        )
    }

    fun onExplosionNearCompanions(state: GameRunState, bx: Float, by: Float): GameRunState {
        val moth = state.activeCompanions.find { it.id == CompanionId.PAPER_MOTH } ?: return state
        if (hypot(moth.x - bx, moth.y - by) <= 280f) {
            return state.copy(
                activeCompanions = state.activeCompanions.filterNot { it.id == CompanionId.PAPER_MOTH },
                activeBanner = RunUiBanner(
                    "¡La Polilla de Papel huyó!",
                    "El estallido de pólvora la aterrorizó y escapó de la sala.",
                    0xFFD98A2B,
                    3.0f
                )
            )
        }
        return state
    }

    fun tryBearFatalIntercept(state: GameRunState, pendingHearts: Int): Pair<Boolean, GameRunState> {
        if (pendingHearts > 0) return false to state
        val bear = state.activeCompanions.find { it.id == CompanionId.FORGOTTEN_BEAR } ?: return false to state
        val ring = (0 until 14).map { i ->
            val a = (i / 14f) * (2f * PI.toFloat())
            ParticleFx(bear.x, bear.y, cos(a) * 140f, sin(a) * 140f, 5f, 0xFFC89948, 0.55f, 0.55f)
        }
        return true to state.copy(
            hearts = 1,
            invulnTimer = 2.0f,
            screenShake = 16f,
            activeCompanions = state.activeCompanions.filterNot { it.id == CompanionId.FORGOTTEN_BEAR },
            particles = state.particles + ring,
            activeBanner = RunUiBanner(
                "El Osito Olvidado te salvó",
                "Bloqueó el golpe mortal por ti... y se desvaneció en felpa.",
                0xFFC89948,
                3.8f
            )
        )
    }
}
