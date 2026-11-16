package com.example.engine

import com.example.audio.SoundEffectsEngine
import com.example.audio.ToySfx
import com.example.data.catalog.AmpliacionCatalog
import com.example.data.model.*
import kotlin.math.hypot
import kotlin.random.Random

object PuzzleInteractionSystem {

    fun stepLoreProps(
        state: GameRunState,
        dt: Float,
        damagePlayerFn: (GameRunState) -> GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onLorePuzzleSolved: (String) -> Unit
    ): GameRunState {
        val lore = state.loreRoom ?: return state
        var s = state
        var props = lore.props.toMutableList()
        var changed = false
        var banner = s.activeBanner
        var stepHint = lore.hintStepText

        // 1. Player pushing draggable props (Chairs, Crates, Unlocked Shelves)
        for (i in props.indices) {
            val p = props[i]
            if (p.destroyed || !p.draggable) continue
            val dx = p.x - s.playerX
            val dy = p.y - s.playerY
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val minSep = p.radius + 20f
            if (dist < minSep) {
                val ux = dx / dist
                val uy = dy / dist
                val targetX = (p.x + ux * (minSep - dist + 2f)).coerceIn(WALL_THICKNESS + p.radius + 10f, ARENA_W - WALL_THICKNESS - p.radius - 10f)
                val targetY = (p.y + uy * (minSep - dist + 2f)).coerceIn(WALL_THICKNESS + p.radius + 10f, ARENA_H - WALL_THICKNESS - p.radius - 10f)
                val (cx, cy) = RoomMechanicSystem.clampToObstacles(targetX, targetY, p.radius, s.currentRoom.obstacles)
                props[i] = p.copy(x = cx, y = cy)
                changed = true
            }
        }

        // 2. Check spatial triggers (Chair -> Switch, Crates -> Electrified zones, Shelf -> Moved off compartment)
        if (lore.puzzleKind == LorePuzzleKind.CHAIR_AND_BUTTON && !lore.solved) {
            val chair = props.find { it.kind == LorePropKind.DRAGGABLE_CHAIR }
            val swIdx = props.indexOfFirst { it.kind == LorePropKind.FLOOR_SWITCH }
            if (chair != null && swIdx != -1) {
                val sw = props[swIdx]
                if (!sw.activated && hypot(chair.x - sw.x, chair.y - sw.y) <= 38f) {
                    props[swIdx] = sw.copy(activated = true, solved = true)
                    s = s.copy(loreRoom = lore.copy(props = props))
                    return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                }
            }
        }

        if (lore.puzzleKind == LorePuzzleKind.STUCK_BOOKSHELF && !lore.solved) {
            val shelf = props.find { it.kind == LorePropKind.STUCK_SHELF }
            if (shelf != null && shelf.stateInt >= 1 && hypot(shelf.x - ARENA_W * 0.50f, shelf.y - ARENA_H * 0.34f) >= 56f) {
                s = s.copy(loreRoom = lore.copy(props = props))
                return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
            }
        }

        if (lore.puzzleKind == LorePuzzleKind.FLOODED_FLOOR_PATH && !lore.solved) {
            val crates = props.filter { it.kind == LorePropKind.DRAGGABLE_CRATE }
            for (i in props.indices) {
                val p = props[i]
                if (p.kind == LorePropKind.ELECTRIFIED_ZONE) {
                    val covered = crates.any { hypot(it.x - p.x, it.y - p.y) <= 44f }
                    if (covered != p.activated) {
                        props[i] = p.copy(activated = covered)
                        changed = true
                        if (covered) {
                            sfx.play(ToySfx.DOOR_OPEN)
                            stepHint = "Zona aislada con caja. Llega a la palanca de la derecha."
                        }
                    }
                    // Uncovered active electric zone zaps player if stepped on
                    if (!covered && hypot(s.playerX - p.x, s.playerY - p.y) <= p.radius + 10f && s.invulnTimer <= 0f) {
                        s = damagePlayerFn(s)
                        banner = RunUiBanner("¡Chispas en el Suelo!", "Empuja una caja sobre el cable para cruzar sin daño.", 0xFFD13B42, 2.2f)
                    }
                }
            }
        }

        if (changed || banner != s.activeBanner || stepHint != lore.hintStepText) {
            s = s.copy(
                loreRoom = lore.copy(props = props, hintStepText = stepHint),
                activeBanner = banner
            )
        }
        return s
    }

    fun findNearbyLorePrompt(state: GameRunState): Pair<LoreProp, String>? {
        val lore = state.loreRoom ?: return null
        val near = lore.props.filter { p ->
            !p.destroyed && hypot(p.x - state.playerX, p.y - state.playerY) <= (p.radius + 42f) &&
                isPromptableProp(p, lore)
        }.minByOrNull { hypot(it.x - state.playerX, it.y - state.playerY) } ?: return null

        val verb = when (near.kind) {
            LorePropKind.TOOL_CROWBAR, LorePropKind.TOOL_CRANK, LorePropKind.TOOL_GEAR, LorePropKind.CAGE_KEY ->
                "Recoger ${near.label}"
            LorePropKind.TOY_FIGURE_SOLDIER, LorePropKind.TOY_FIGURE_DOLL, LorePropKind.TOY_FIGURE_BUNNY ->
                "Tomar ${near.label}"
            LorePropKind.STUCK_SHELF ->
                if (near.stateInt == 0) {
                    if ("CROWBAR" in lore.heldTokens) "Destrabar Estantería" else "Examinar Estantería"
                } else "Apartar Estantería"
            LorePropKind.ROTATABLE_PAINTING -> "Girar Cuadro (${near.stateInt * 90}°)"
            LorePropKind.BROKEN_MUSIC_BOX -> "Colocar Piezas Musicales (${near.stateInt}/2)"
            LorePropKind.ADJUSTABLE_CLOCK -> "Ajustar Reloj (${near.stateInt}:00)"
            LorePropKind.TRAPPED_DOLL_CAGE ->
                if ("KEY" in lore.heldTokens) "Abrir Jaula con Llave" else "Revisar Jaula (Busca Llave o Dispara)"
            LorePropKind.POWER_LEVER -> "Cortar Corriente"
            LorePropKind.ROTATABLE_DOLL -> "Girar Muñeca (${dirLabel(near.stateInt)})"
            LorePropKind.WALL_HINT_PANEL -> "Leer Pista"
            LorePropKind.FLOOR_TILE_HOLLOW ->
                if (near.stateInt == 0) "Golpear Baldosa" else "Levantar Suelo Hueco"
            LorePropKind.FLOOR_TILE_TRAP -> "Tocar Baldosa"
            LorePropKind.PEDESTAL_SLOT -> {
                val placed = if (near.stateInt == 0) "Vacío" else figureName(near.stateInt)
                "Pedestal ($placed)"
            }
            LorePropKind.DESTRUCTIBLE_CRATE -> "Abrir Caja con Cuidado"
            LorePropKind.DIARY_DESK -> "Leer Registro"
            LorePropKind.SECRET_COMPARTMENT -> "Revisar Compartimento"
            else -> return null
        }
        return near to verb
    }

    private fun isPromptableProp(p: LoreProp, lore: LoreRoomState): Boolean {
        return when (p.kind) {
            LorePropKind.DRAGGABLE_CHAIR, LorePropKind.DRAGGABLE_CRATE,
            LorePropKind.FLOOR_SWITCH, LorePropKind.ELECTRIFIED_ZONE -> false
            LorePropKind.SECRET_COMPARTMENT -> !p.solved || !lore.solved
            LorePropKind.STUCK_SHELF -> !lore.solved
            LorePropKind.DESTRUCTIBLE_CRATE -> !p.destroyed
            else -> !p.solved
        }
    }

    fun interactLoreProp(
        state: GameRunState,
        propId: Long,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onLorePuzzleSolved: (String) -> Unit
    ): GameRunState {
        val lore = state.loreRoom ?: return state
        val idx = lore.props.indexOfFirst { it.id == propId }
        if (idx == -1) return state
        val prop = lore.props[idx]
        var props = lore.props.toMutableList()
        var tokens = lore.heldTokens.toMutableList()
        var s = state

        when (prop.kind) {
            LorePropKind.TOOL_CROWBAR -> {
                tokens.add("CROWBAR")
                props[idx] = prop.copy(destroyed = true, solved = true)
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(props = props, heldTokens = tokens, hintStepText = "Palanca recogida. Úsala en la estantería."),
                    activeBanner = RunUiBanner("Palanca Recogida", "Acércate a la estantería trabada para liberarla.", 0xFFC89948, 2.2f)
                )
            }
            LorePropKind.TOOL_CRANK -> {
                tokens.add("CRANK")
                props[idx] = prop.copy(destroyed = true, solved = true)
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(props = props, heldTokens = tokens),
                    activeBanner = RunUiBanner("Manivela Encontrada", "Llévala a la Caja Musical.", 0xFFC89948, 2.0f)
                )
            }
            LorePropKind.TOOL_GEAR -> {
                tokens.add("GEAR")
                props[idx] = prop.copy(destroyed = true, solved = true)
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(props = props, heldTokens = tokens),
                    activeBanner = RunUiBanner("Engranaje Encontrado", "Llévalo a la Caja Musical.", 0xFFC89948, 2.0f)
                )
            }
            LorePropKind.CAGE_KEY -> {
                tokens.add("KEY")
                props[idx] = prop.copy(destroyed = true, solved = true)
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(props = props, heldTokens = tokens),
                    activeBanner = RunUiBanner("Llave de Jaula", "Úsala en la vitrina del osito atrapado.", 0xFFC89948, 2.2f)
                )
            }
            LorePropKind.TOY_FIGURE_SOLDIER, LorePropKind.TOY_FIGURE_DOLL, LorePropKind.TOY_FIGURE_BUNNY -> {
                prop.figureToken?.let { tokens.add(it) }
                props[idx] = prop.copy(destroyed = true, solved = true)
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(props = props, heldTokens = tokens),
                    activeBanner = RunUiBanner("Figura Recogida", "${prop.label} guardada en mano.", 0xFFC89948, 2.0f)
                )
            }

            LorePropKind.STUCK_SHELF -> {
                if (prop.stateInt == 0) {
                    if ("CROWBAR" in tokens) {
                        sfx.play(ToySfx.DOOR_OPEN)
                        props[idx] = prop.copy(stateInt = 1, draggable = true, label = "Estantería Suelta")
                        return s.copy(
                            loreRoom = lore.copy(props = props, hintStepText = "Soporte liberado. Empuja la estantería o vuelve a interactuar."),
                            activeBanner = RunUiBanner("Estantería Liberada", "Ahora puedes moverla para descubrir el hueco.", 0xFF597354, 2.2f)
                        )
                    } else {
                        return s.copy(activeBanner = RunUiBanner("Trabada", "Necesitas la palanca del rincón o disparar al soporte.", 0xFFD98A2B, 2.2f))
                    }
                } else {
                    props[idx] = prop.copy(x = prop.x + 84f, solved = true)
                    s = s.copy(loreRoom = lore.copy(props = props))
                    return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                }
            }

            LorePropKind.ROTATABLE_PAINTING -> {
                sfx.play(ToySfx.PICKUP_BUTTON)
                val nextRot = (prop.stateInt + 1) % 4
                props[idx] = prop.copy(stateInt = nextRot, solved = (nextRot == prop.targetInt))
                s = s.copy(loreRoom = lore.copy(props = props))
                return if (nextRot == prop.targetInt) {
                    completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                } else {
                    s.copy(activeBanner = RunUiBanner("Cuadro Girado", "Ángulo actual: ${nextRot * 90}° (busca 0° recto).", 0xFFC89948, 1.6f))
                }
            }

            LorePropKind.BROKEN_MUSIC_BOX -> {
                var inserted = prop.stateInt
                if ("CRANK" in tokens) {
                    tokens.remove("CRANK")
                    inserted++
                }
                if ("GEAR" in tokens) {
                    tokens.remove("GEAR")
                    inserted++
                }
                if (inserted == prop.stateInt && inserted < 2) {
                    return s.copy(activeBanner = RunUiBanner("Faltan Piezas", "Busca la Manivela y el Engranaje en la habitación.", 0xFFD98A2B, 2.2f))
                }
                sfx.play(ToySfx.REPAIR_STITCH)
                props[idx] = prop.copy(stateInt = inserted.coerceAtMost(2), solved = (inserted >= 2))
                s = s.copy(loreRoom = lore.copy(props = props, heldTokens = tokens))
                return if (inserted >= 2) {
                    completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                } else {
                    s.copy(activeBanner = RunUiBanner("Pieza Colocada", "Falta otra pieza para hacer sonar la melodía.", 0xFF597354, 2.0f))
                }
            }

            LorePropKind.ADJUSTABLE_CLOCK -> {
                sfx.play(ToySfx.PICKUP_BUTTON)
                val nextHour = when (prop.stateInt) {
                    12 -> 3
                    3 -> 6
                    6 -> 9
                    else -> 12
                }
                props[idx] = prop.copy(stateInt = nextHour, solved = (nextHour == prop.targetInt))
                s = s.copy(loreRoom = lore.copy(props = props))
                val allAligned = props.filter { it.kind == LorePropKind.ADJUSTABLE_CLOCK }.all { it.stateInt == it.targetInt }
                return if (allAligned) {
                    completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                } else {
                    s.copy(activeBanner = RunUiBanner(prop.label, "Marca las $nextHour:00 (objetivo 3:00).", 0xFFC89948, 1.6f))
                }
            }

            LorePropKind.TRAPPED_DOLL_CAGE -> {
                if ("KEY" in tokens) {
                    tokens.remove("KEY")
                    props[idx] = prop.copy(solved = true, activated = true, label = "Jaula Abierta")
                    s = s.copy(loreRoom = lore.copy(props = props, heldTokens = tokens))
                    return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                }
                return s.copy(activeBanner = RunUiBanner("Jaula Cerrada", "Busca la llave oxidada o dispara a la cadena.", 0xFFD98A2B, 2.2f))
            }

            LorePropKind.POWER_LEVER -> {
                sfx.play(ToySfx.DOOR_OPEN)
                props[idx] = prop.copy(activated = true, solved = true)
                for (j in props.indices) {
                    if (props[j].kind == LorePropKind.ELECTRIFIED_ZONE) {
                        props[j] = props[j].copy(activated = true, solved = true)
                    }
                }
                s = s.copy(loreRoom = lore.copy(props = props))
                return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
            }

            LorePropKind.ROTATABLE_DOLL -> {
                sfx.play(ToySfx.PICKUP_BUTTON)
                val nextDir = (prop.stateInt + 1) % 4
                props[idx] = prop.copy(stateInt = nextDir, solved = (nextDir == prop.targetInt))
                s = s.copy(loreRoom = lore.copy(props = props))
                val allMatch = props.filter { it.kind == LorePropKind.ROTATABLE_DOLL }.all { it.stateInt == it.targetInt }
                return if (allMatch) {
                    completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                } else {
                    s.copy(activeBanner = RunUiBanner(prop.label, "Ahora mira al ${dirLabel(nextDir)}.", 0xFFC89948, 1.5f))
                }
            }

            LorePropKind.WALL_HINT_PANEL, LorePropKind.DIARY_DESK -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                return s.copy(
                    loreRoom = lore.copy(hintStepText = prop.hintText),
                    activeBanner = RunUiBanner(prop.label, prop.hintText, 0xFFC89948, 3.8f)
                )
            }

            LorePropKind.FLOOR_TILE_HOLLOW -> {
                if (prop.stateInt == 0) {
                    sfx.play(ToySfx.PICKUP_BUTTON)
                    props[idx] = prop.copy(stateInt = 1)
                    return s.copy(
                        loreRoom = lore.copy(props = props, hintStepText = "¡Esta baldosa suena HUECA! Levántala ahora."),
                        activeBanner = RunUiBanner("¡Sonido Hueco!", "Las tablas sueltas ceden; interactúa de nuevo para abrir.", 0xFF597354, 2.5f)
                    )
                } else {
                    props[idx] = prop.copy(solved = true, activated = true, destroyed = true)
                    s = s.copy(loreRoom = lore.copy(props = props))
                    return completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                }
            }

            LorePropKind.FLOOR_TILE_TRAP -> {
                sfx.play(ToySfx.HIT_ENEMY)
                props[idx] = prop.copy(activated = true)
                return s.copy(
                    loreRoom = lore.copy(props = props),
                    activeBanner = RunUiBanner("Sonido Sólido", "Esta baldosa es maciza y oculta un cepo. Prueba otra.", 0xFFD13B42, 2.2f)
                )
            }

            LorePropKind.PEDESTAL_SLOT -> {
                val expectedToken = when (prop.targetInt) {
                    1 -> "SOLDIER"
                    2 -> "DOLL"
                    else -> "BUNNY"
                }
                val pickToken = when {
                    expectedToken in tokens -> expectedToken
                    tokens.isNotEmpty() -> tokens.first()
                    else -> null
                }
                if (prop.stateInt != 0) {
                    // Remove placed figure back to hand so player can swap order if mistaken
                    val returnedToken = when (prop.stateInt) {
                        1 -> "SOLDIER"
                        2 -> "DOLL"
                        else -> "BUNNY"
                    }
                    tokens.add(returnedToken)
                    props[idx] = prop.copy(stateInt = 0, solved = false)
                    return s.copy(loreRoom = lore.copy(props = props, heldTokens = tokens))
                }
                if (pickToken == null) {
                    return s.copy(activeBanner = RunUiBanner("Pedestal Vacío", "Recoge las figuras repartidas por la sala.", 0xFFD98A2B, 2.0f))
                }
                tokens.remove(pickToken)
                val placedVal = when (pickToken) {
                    "SOLDIER" -> 1
                    "DOLL" -> 2
                    else -> 3
                }
                sfx.play(ToySfx.DOOR_OPEN)
                props[idx] = prop.copy(stateInt = placedVal, solved = (placedVal == prop.targetInt))
                s = s.copy(loreRoom = lore.copy(props = props, heldTokens = tokens))
                val allPlaced = props.filter { it.kind == LorePropKind.PEDESTAL_SLOT }.all { it.stateInt == it.targetInt }
                return if (allPlaced) {
                    completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                } else {
                    s.copy(activeBanner = RunUiBanner("Figura Colocada", "${figureName(placedVal)} en ${prop.label}.", 0xFFC89948, 1.8f))
                }
            }

            LorePropKind.DESTRUCTIBLE_CRATE -> {
                sfx.play(ToySfx.PICKUP_ITEM)
                props[idx] = prop.copy(destroyed = true, solved = true)
                val pickups = s.pickups.toMutableList()
                pickups.add(GroundPickup(allocId(), PickupKind.BUTTON, prop.x - 14f, prop.y + 18f, amount = 2))
                pickups.add(GroundPickup(allocId(), PickupKind.BACKPACK_ITEM, prop.x + 14f, prop.y + 18f, valueStr = BackpackSystem.randomDropKind().name))
                return s.copy(
                    loreRoom = lore.copy(props = props),
                    pickups = pickups,
                    activeBanner = RunUiBanner(
                        "Caja Abierta con Cuidado",
                        if (prop.fragilePhotoInside) "Salvaste intacto su contenido y un insumo de mochila." else "Botín recuperado.",
                        0xFF597354,
                        2.4f
                    )
                )
            }

            LorePropKind.SECRET_COMPARTMENT -> {
                return if (lore.solved) {
                    s.copy(activeBanner = RunUiBanner("Compartimento Vacío", "El recuerdo principal ya fue recuperado.", 0xFFB5A17D, 2.0f))
                } else {
                    s.copy(activeBanner = RunUiBanner("Cerradura Sellada", lore.hintSummary, 0xFFD98A2B, 2.6f))
                }
            }

            else -> return s
        }
    }

    fun applyProjectileToLoreProps(
        state: GameRunState,
        proj: ActiveProjectile,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onLorePuzzleSolved: (String) -> Unit
    ): Pair<Boolean, GameRunState> {
        val lore = state.loreRoom ?: return false to state
        if (!proj.fromPlayer) return false to state
        for (i in lore.props.indices) {
            val p = lore.props[i]
            if (p.destroyed) continue
            if (!p.destructible) continue
            if (hypot(proj.x - p.x, proj.y - p.y) <= p.radius + proj.radius) {
                val nhp = p.hp - proj.damage
                val props = lore.props.toMutableList()
                if (nhp <= 0f) {
                    when (p.kind) {
                        LorePropKind.STUCK_SHELF -> {
                            sfx.play(ToySfx.EXPLOSION)
                            props[i] = p.copy(stateInt = 1, draggable = true, destructible = false, label = "Estantería Suelta")
                            return true to state.copy(
                                loreRoom = lore.copy(props = props),
                                activeBanner = RunUiBanner("Soporte Roto", "Ya puedes empujar la estantería.", 0xFF597354, 2.2f)
                            )
                        }
                        LorePropKind.TRAPPED_DOLL_CAGE -> {
                            sfx.play(ToySfx.DOOR_OPEN)
                            props[i] = p.copy(solved = true, activated = true, destructible = false, label = "Jaula Rota")
                            val s = state.copy(loreRoom = lore.copy(props = props))
                            return true to completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                        }
                        LorePropKind.FLOOR_TILE_HOLLOW -> {
                            sfx.play(ToySfx.DOOR_OPEN)
                            props[i] = p.copy(destroyed = true, solved = true)
                            val s = state.copy(loreRoom = lore.copy(props = props))
                            return true to completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
                        }
                        LorePropKind.DESTRUCTIBLE_CRATE -> {
                            sfx.play(ToySfx.PICKUP_ITEM)
                            props[i] = p.copy(destroyed = true, solved = true)
                            val pickups = state.pickups + GroundPickup(allocId(), PickupKind.MATERIAL, p.x, p.y, valueStr = RepairMaterial.entries.random().name)
                            return true to state.copy(loreRoom = lore.copy(props = props), pickups = pickups)
                        }
                        else -> Unit
                    }
                } else {
                    props[i] = p.copy(hp = nhp)
                    return true to state.copy(loreRoom = lore.copy(props = props))
                }
            }
        }
        return false to state
    }

    fun onExplosionLoreProps(
        state: GameRunState,
        bx: Float,
        by: Float,
        radius: Float,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onLorePuzzleSolved: (String) -> Unit
    ): GameRunState {
        val lore = state.loreRoom ?: return state
        var s = state
        val props = lore.props.toMutableList()
        var burnedPhoto = lore.fragilePhotoBurned
        var banner = s.activeBanner
        var solvedNow = false

        for (i in props.indices) {
            val p = props[i]
            if (p.destroyed) continue
            if (hypot(p.x - bx, p.y - by) <= radius + p.radius) {
                if (p.kind == LorePropKind.DESTRUCTIBLE_CRATE) {
                    props[i] = p.copy(destroyed = true, solved = true)
                    if (p.fragilePhotoInside) {
                        burnedPhoto = true
                        banner = RunUiBanner(
                            "¡Fotografía Frágil Quemada!",
                            "La explosión destruyó una fotografía guardada en la caja.",
                            0xFFD13B42,
                            3.0f
                        )
                    }
                } else if (p.kind == LorePropKind.TRAPPED_DOLL_CAGE && !lore.solved) {
                    props[i] = p.copy(solved = true, activated = true)
                    solvedNow = true
                }
            }
        }
        s = s.copy(
            loreRoom = lore.copy(props = props, fragilePhotoBurned = burnedPhoto),
            activeBanner = banner
        )
        if (solvedNow) {
            s = completeLorePuzzle(s, allocId, sfx, onDiscovery, onAchievement, onLorePuzzleSolved)
        }
        return s
    }

    private fun completeLorePuzzle(
        state: GameRunState,
        allocId: () -> Long,
        sfx: SoundEffectsEngine,
        onDiscovery: (String, String) -> Unit,
        onAchievement: (String) -> Unit,
        onLorePuzzleSolved: (String) -> Unit
    ): GameRunState {
        val lore = state.loreRoom ?: return state
        if (lore.solved) return state

        sfx.play(ToySfx.SYNERGY)
        onLorePuzzleSolved(lore.templateId)
        val totalSolvedRun = state.lorePuzzlesSolvedThisRun + 1

        val updatedProps = lore.props.map {
            if (it.kind == LorePropKind.SECRET_COMPARTMENT) it.copy(solved = true, activated = true) else it
        }
        val pickups = state.pickups.toMutableList()
        pickups.add(GroundPickup(allocId(), PickupKind.GOLD_BUTTON, ARENA_W * 0.44f, ARENA_H * 0.56f))
        pickups.add(
            GroundPickup(
                allocId(),
                PickupKind.BACKPACK_ITEM,
                ARENA_W * 0.56f,
                ARENA_H * 0.56f,
                valueStr = BackpackItemKind.CRAFTING_BUNDLE.name
            )
        )
        if (lore.loreRoomType == LoreRoomType.FORBIDDEN || Random.nextFloat() < 0.35f) {
            pickups.add(
                GroundPickup(
                    allocId(),
                    PickupKind.MATERIAL,
                    ARENA_W * 0.50f,
                    ARENA_H * 0.64f,
                    valueStr = RepairMaterial.PORCELAIN_SHARD.name
                )
            )
        }

        var nextState = state.copy(
            loreRoom = lore.copy(props = updatedProps, solved = true, rewardClaimed = true),
            lorePuzzlesSolvedThisRun = totalSolvedRun,
            pickups = pickups,
            activeBanner = RunUiBanner(
                "¡Misterio Resuelto: ${lore.title}!",
                "Recuerdo recuperado y botín en la sala.",
                0xFFC89948,
                3.0f
            )
        )

        val archId = lore.rewardArchiveId
        if (archId != null && !lore.fragilePhotoBurned) {
            val roomArchiveBonus = if (lore.loreRoomType == LoreRoomType.FORBIDDEN) {
                "Descubierto en una Habitación Prohibida (+Botón Dorado y Material)."
            } else {
                "Botín de exploración desbloqueado en el suelo."
            }
            nextState = WonderlandArchiveSystem.unlockArchiveEntry(
                state = nextState,
                archiveId = archId,
                bonusText = roomArchiveBonus,
                onDiscovery = onDiscovery,
                onAchievement = onAchievement
            )
            // Also unlock matching room archive when applicable
            if (lore.puzzleKind == LorePuzzleKind.CHAIR_AND_BUTTON) {
                onDiscovery("archive", "arch_room_birthday")
                nextState = nextState.copy(unlockedArchiveIdsThisRun = nextState.unlockedArchiveIdsThisRun + "arch_room_birthday")
            }
        }
        return nextState
    }

    private fun dirLabel(dir: Int): String = when (dir % 4) {
        0 -> "Norte (↑)"
        1 -> "Este (→)"
        2 -> "Sur (↓)"
        else -> "Oeste (←)"
    }

    private fun figureName(tokenId: Int): String = when (tokenId) {
        1 -> "Soldado"
        2 -> "Muñeca"
        else -> "Conejo"
    }
}
