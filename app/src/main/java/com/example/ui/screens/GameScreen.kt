package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyZone
import com.example.data.model.CompanionId
import com.example.data.model.RoomMechanic
import com.example.data.model.WearState
import com.example.engine.*
import com.example.ui.components.GameArenaCanvas
import com.example.ui.components.PixelArtSprites
import com.example.ui.theme.*
import kotlin.math.hypot

@Composable
fun GameScreen(
    state: GameRunState,
    vignetteStrength: Float,
    screenShakeEnabled: Boolean,
    onMoveInput: (Float, Float) -> Unit,
    onAimInput: (Float, Float, Boolean) -> Unit,
    onDodge: () -> Unit,
    onAbility: () -> Unit,
    onUseConsumable: () -> Unit,
    onCycleConsumable: () -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onResolvePartChoice: (Boolean) -> Unit,
    onResolveEvent: (String) -> Unit,
    onPurchaseShopOffer: (String) -> Unit,
    onCloseShop: () -> Unit,
    onRepairAtBench: (BodyZone?) -> Unit,
    onCloseRepairBench: () -> Unit,
    onReopenFacility: () -> Unit,
    onInspectCompanion: (CompanionId) -> Unit,
    onResolveCompanionDialog: (String) -> Unit,
    onAbandonRun: () -> Unit
) {
    var pauseTab by remember { mutableStateOf(0) } // 0 = Menu/Stats, 1 = Body/Synergies, 2 = Toybox Memory, 3 = Map
    var showMemoryInspector by remember { mutableStateOf(false) }

    BackHandler {
        onTogglePause(!state.isPaused)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP HUD (Section 20.6 GDD)
            TopGameplayHud(
                state = state,
                onPauseClick = {
                    pauseTab = 0
                    onTogglePause(true)
                },
                onInspectBodyClick = {
                    pauseTab = 1
                    onTogglePause(true)
                },
                onMemoryClick = {
                    showMemoryInspector = true
                    onTogglePause(true)
                },
                onTutorialClick = onOpenTutorial
            )

            // CENTER ARENA CANVAS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameArenaCanvas(
                    state = state,
                    vignetteStrength = vignetteStrength,
                    screenShakeEnabled = screenShakeEnabled,
                    modifier = Modifier.fillMaxSize()
                )

                // Boss Health Bar at top of arena
                val activeBoss = state.enemies.firstOrNull { it.type.isBoss }
                if (activeBoss != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.86f)
                            .padding(top = 8.dp)
                            .background(CharcoalBlack.copy(alpha = 0.78f), CutCornerShape(4.dp))
                            .border(1.dp, CrimsonBright, CutCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeBoss.type.displayName.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = AgedIvory
                        )
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (activeBoss.hp / activeBoss.maxHp).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = CrimsonBright,
                            trackColor = AgedWoodDark
                        )
                    }
                }

                // Companion HUD Badges (top-left of arena, clickable to inspect/repair/dismiss)
                if (state.activeCompanions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.activeCompanions.forEach { comp ->
                            val alert = comp.broken || comp.needsAttention
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CutCornerShape(5.dp))
                                    .background(CharcoalBlack.copy(alpha = 0.82f))
                                    .border(
                                        1.5.dp,
                                        when {
                                            comp.broken -> CrimsonBright
                                            comp.needsAttention -> AmberWarning
                                            else -> TarnishedBrassDim
                                        },
                                        CutCornerShape(5.dp)
                                    )
                                    .testTag("hud_companion_${comp.id.name}")
                                    .clickable { onInspectCompanion(comp.id) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Image(
                                        bitmap = PixelArtSprites.forCompanion(comp.id, comp.broken),
                                        contentDescription = comp.id.displayName,
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (alert) {
                                        Text(
                                            text = "!",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (comp.broken) CrimsonBright else AmberWarning
                                        )
                                    } else if (comp.id == CompanionId.BROKEN_SOLDIER) {
                                        Text(
                                            text = "${comp.hp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AgedIvory
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dynamic Room Mechanic Pill (top-right of arena)
                if (state.currentRoom.mechanic != RoomMechanic.NONE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp, top = 8.dp)
                            .background(CharcoalBlack.copy(alpha = 0.78f), CutCornerShape(4.dp))
                            .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.currentRoom.mechanic.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TarnishedBrass,
                            maxLines = 1
                        )
                    }
                }

                // Dynamic Toast Banner (Synergy, Floor, Toybox Memory, Loot)
                state.activeBanner?.let { banner ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = if (activeBoss != null) 54.dp else if (state.activeCompanions.isNotEmpty()) 58.dp else 10.dp,
                                start = 20.dp,
                                end = 20.dp
                            )
                            .background(CharcoalBlack.copy(alpha = 0.88f), CutCornerShape(6.dp))
                            .border(1.5.dp, Color(banner.colorHex), CutCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = banner.title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(banner.colorHex)
                        )
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgedIvory
                        )
                    }
                }

                // Interactive room re-open buttons when inside cleared shop/repair room
                if (state.currentRoom.type.name == "SHOP" && !state.shopOpen) {
                    FloatingRoomChip(
                        text = "ABRIR PUESTO ($)",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
                        onClick = onReopenFacility
                    )
                } else if (state.currentRoom.type.name == "REPAIR" && !state.repairBenchOpen) {
                    FloatingRoomChip(
                        text = "USAR MESA DE COSTURA (✂)",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
                        onClick = onReopenFacility
                    )
                }
            }

            // BOTTOM CONTROLS BAR (Portrait Ergonomic Dual-Stick + Actions)
            BottomTouchControls(
                state = state,
                onMoveInput = onMoveInput,
                onAimInput = onAimInput,
                onDodge = onDodge,
                onAbility = onAbility,
                onUseConsumable = onUseConsumable,
                onCycleConsumable = onCycleConsumable
            )
        }

        // Interactive Room & Pause Overlays
        if (state.pendingPartChoice != null) {
            PartChoiceDialog(
                choice = state.pendingPartChoice,
                onEquip = { onResolvePartChoice(true) },
                onSalvage = { onResolvePartChoice(false) }
            )
        }

        if (state.activeEvent != null) {
            RoomEventDialog(
                event = state.activeEvent,
                currentButtons = state.buttons,
                onSelectOption = onResolveEvent
            )
        }

        if (state.shopOpen) {
            ShopDialog(
                room = state.currentRoom,
                buttons = state.buttons,
                onBuy = onPurchaseShopOffer,
                onClose = onCloseShop
            )
        }

        if (state.repairBenchOpen) {
            RepairBenchDialog(
                state = state,
                onRepairZone = onRepairAtBench,
                onClose = onCloseRepairBench
            )
        }

        if (state.pendingCompanionDialog != null) {
            CompanionInteractionDialog(
                dialog = state.pendingCompanionDialog,
                currentButtons = state.buttons,
                onSelectOption = onResolveCompanionDialog
            )
        }

        if (showMemoryInspector) {
            ToyboxMemoryDialog(
                state = state,
                onClose = {
                    showMemoryInspector = false
                    onTogglePause(false)
                }
            )
        } else if (state.isPaused) {
            PauseOverlayDialog(
                state = state,
                selectedTab = pauseTab,
                onSelectTab = { pauseTab = it },
                onResume = { onTogglePause(false) },
                onOpenTutorial = onOpenTutorial,
                onAbandon = onAbandonRun
            )
        }
    }
}

@Composable
private fun TopGameplayHud(
    state: GameRunState,
    onPauseClick: () -> Unit,
    onInspectBodyClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AgedWoodDark)
            .border(1.dp, TarnishedBrassDim)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hearts, 6 Body-Part Wear Chips, Buttons
        Column(modifier = Modifier.weight(1.2f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(state.maxHearts) { idx ->
                    Icon(
                        imageVector = if (idx < state.hearts) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Corazón",
                        tint = if (idx < state.hearts) CrimsonBright else ParchmentDark.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(CharcoalBlack, CutCornerShape(3.dp))
                        .border(1.dp, TarnishedBrassDim, CutCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "● ${state.buttons}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TarnishedBrass
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 6 Body Zone Wear Status Strip
            Row(
                modifier = Modifier
                    .clickable(onClick = onInspectBodyClick)
                    .testTag("hud_body_strip"),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BodyZone.entries.forEach { zone ->
                    val part = state.equippedParts[zone]
                    val col = when (part?.wearState) {
                        WearState.INTACT -> MutedMoss
                        WearState.DAMAGED -> AmberWarning
                        WearState.CRITICAL -> CrimsonBright
                        WearState.DETACHED, null -> Color(0xFF444444)
                    }
                    Box(
                        modifier = Modifier
                            .background(col.copy(alpha = 0.25f), CutCornerShape(2.dp))
                            .border(1.dp, col, CutCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = zone.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = AgedIvory
                        )
                    }
                }
            }
        }

        // Right: Floor/Room, Minimap, Toybox Memory Eye, Help, Pause
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "P${state.floorNumber}: ${state.currentRoom.type.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AgedIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val memCount = ToyboxMemorySystem.activeTraits(state.toyboxMemory).size
                Row(
                    modifier = Modifier
                        .clip(CutCornerShape(3.dp))
                        .background(if (memCount > 0) CrimsonBlood.copy(alpha = 0.35f) else CharcoalBlack)
                        .border(1.dp, if (memCount > 0) TarnishedBrass else AgedWoodLight, CutCornerShape(3.dp))
                        .clickable(onClick = onMemoryClick)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "La juguetería recuerda", tint = TarnishedBrass, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Recuerda ($memCount)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Parchment)
                }
            }

            // Compact Minimap
            MiniMapCanvas(
                state = state,
                modifier = Modifier
                    .size(44.dp)
                    .background(CharcoalBlack, CutCornerShape(3.dp))
                    .border(1.dp, TarnishedBrassDim, CutCornerShape(3.dp))
            )

            IconButton(
                onClick = onTutorialClick,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Tutorial", tint = Parchment, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, TarnishedBrass, CutCornerShape(4.dp))
                    .testTag("hud_pause_button")
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = AgedIvory, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MiniMapCanvas(state: GameRunState, modifier: Modifier = Modifier) {
    val forgetful = state.activeDefects.any { it.effects.fogForgetfulness }
    Canvas(modifier = modifier) {
        val cell = 6.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cur = state.currentCoord
        state.dungeon.rooms.values.forEach { r ->
            val showSecret = r.type.name == "SECRET" && (r.visited || state.toyboxMemory.secretCluesRevealed)
            if (!r.visited && r.type.name == "SECRET" && !showSecret) return@forEach
            val dx = (r.coord.x - cur.x) * (cell + 1.5f)
            val dy = (r.coord.y - cur.y) * (cell + 1.5f)
            val rx = cx + dx - cell / 2f
            val ry = cy + dy - cell / 2f
            if (rx in 0f..(size.width - cell) && ry in 0f..(size.height - cell)) {
                val col = when {
                    r.coord == cur -> TarnishedBrass
                    forgetful && r.visited -> ParchmentDark.copy(alpha = 0.5f)
                    r.type.name == "BOSS" -> CrimsonBright
                    r.type.name == "TREASURE" -> Color(0xFFD9C05B)
                    r.type.name == "SHOP" -> MutedMoss
                    r.visited -> AgedIvory.copy(alpha = 0.7f)
                    else -> AgedWoodLight.copy(alpha = 0.5f)
                }
                drawRect(col, Offset(rx, ry), Size(cell, cell))
            }
        }
    }
}

@Composable
private fun BottomTouchControls(
    state: GameRunState,
    onMoveInput: (Float, Float) -> Unit,
    onAimInput: (Float, Float, Boolean) -> Unit,
    onDodge: () -> Unit,
    onAbility: () -> Unit,
    onUseConsumable: () -> Unit,
    onCycleConsumable: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .background(AgedWoodDark)
            .border(1.dp, TarnishedBrassDim)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Virtual Movement Joystick
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VirtualJoystickPad(
                sizeDp = 124,
                knobColor = AgedIvory,
                ringColor = TarnishedBrassDim,
                label = "MOVER",
                onVector = { x, y, _ -> onMoveInput(x, y) }
            )
        }

        // Center: Action Buttons (Dodge, Ability, Consumable)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dodge Button
            val dodgeReady = state.dodgeCooldownTimer <= 0f
            ActionCircleButton(
                label = if (dodgeReady) "ESQUIVA" else "${String.format("%.1f", state.dodgeCooldownTimer)}s",
                icon = Icons.Default.Bolt,
                ready = dodgeReady,
                accentColor = MutedMoss,
                testTag = "btn_dodge",
                onClick = onDodge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val abReady = state.abilityCooldownTimer <= 0f
                ActionCircleButton(
                    label = if (abReady) "PODER" else "${state.abilityCooldownTimer.toInt()}s",
                    icon = Icons.Default.AutoAwesome,
                    ready = abReady,
                    accentColor = TarnishedBrass,
                    testTag = "btn_ability",
                    onClick = onAbility
                )

                val curCons = state.consumables.getOrNull(state.selectedConsumableIdx)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ActionCircleButton(
                        label = curCons?.displayName?.take(5) ?: "VACÍO",
                        icon = Icons.Default.Backpack,
                        pixelIcon = curCons?.let { PixelArtSprites.forConsumable(it) },
                        ready = curCons != null,
                        accentColor = CrimsonBright,
                        testTag = "btn_consumable",
                        onClick = onUseConsumable
                    )
                    if (state.consumables.size > 1) {
                        Text(
                            text = "Cambiar (${state.consumables.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TarnishedBrass,
                            modifier = Modifier
                                .clickable(onClick = onCycleConsumable)
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Right: Manual Aim & Fire Joystick
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VirtualJoystickPad(
                sizeDp = 124,
                knobColor = CrimsonBright,
                ringColor = TarnishedBrass,
                label = "APUNTAR",
                onVector = { x, y, active -> onAimInput(x, y, active) }
            )
        }
    }
}

@Composable
private fun VirtualJoystickPad(
    sizeDp: Int,
    knobColor: Color,
    ringColor: Color,
    label: String,
    onVector: (Float, Float, Boolean) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.width * 0.38f

                    fun dispatch(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > maxR) Offset(dx / dist * maxR, dy / dist * maxR) else Offset(dx, dy)
                        knobOffset = clamped
                        onVector(clamped.x / maxR, clamped.y / maxR, true)
                    }

                    dispatch(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            knobOffset = Offset.Zero
                            onVector(0f, 0f, false)
                            break
                        }
                        dispatch(change.position)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.width * 0.44f
            drawCircle(CharcoalBlack.copy(alpha = 0.65f), r, c)
            drawCircle(ringColor, r, c, style = Stroke(2.5f))
            drawCircle(ringColor.copy(alpha = 0.25f), r * 0.45f, c, style = Stroke(1.5f))
            drawCircle(knobColor, r * 0.34f, c + knobOffset)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = ParchmentDark.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp)
        )
    }
}

@Composable
private fun ActionCircleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ready: Boolean,
    accentColor: Color,
    testTag: String,
    pixelIcon: ImageBitmap? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 54.dp, minHeight = 54.dp)
            .clip(CircleShape)
            .background(if (ready) AgedWoodMid else CharcoalBlack)
            .border(1.5.dp, if (ready) accentColor else AgedWoodLight, CircleShape)
            .testTag(testTag)
            .clickable(enabled = ready, onClick = onClick)
            .padding(6.dp)
    ) {
        if (pixelIcon != null) {
            Image(
                bitmap = pixelIcon,
                contentDescription = label,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (ready) accentColor else ParchmentDark.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = if (ready) AgedIvory else ParchmentDark.copy(alpha = 0.5f),
            maxLines = 1
        )
    }
}

@Composable
private fun FloatingRoomChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(AgedWoodDark, CutCornerShape(4.dp))
            .border(1.dp, TarnishedBrass, CutCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
    }
}
