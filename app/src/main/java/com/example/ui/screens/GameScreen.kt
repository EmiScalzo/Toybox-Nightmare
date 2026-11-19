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
import com.example.data.model.BackpackRecipe
import com.example.data.model.BodyZone
import com.example.data.model.CompanionId
import com.example.data.model.ContractDef
import com.example.data.model.LightSourceType
import com.example.data.model.ObjectiveType
import com.example.data.model.PatchEmotion
import com.example.data.model.RepairActionMode
import com.example.data.model.RoomMechanic
import com.example.data.model.RoomType
import com.example.data.model.WearState
import com.example.data.model.bossMechanicKind
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
    onInteract: () -> Unit,
    onAbandonObjective: () -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onOpenTutorial: () -> Unit,
    onResolvePartChoice: (Boolean) -> Unit,
    onResolveEvent: (String) -> Unit,
    onPurchaseShopOffer: (String) -> Unit,
    onCloseShop: () -> Unit,
    onRepairAtBench: (BodyZone?) -> Unit,
    onStartRepairMinigame: (BodyZone, RepairActionMode) -> Unit,
    onRepairMinigameAction: (String) -> Unit,
    onCloseRepairBench: () -> Unit,
    onReopenFacility: () -> Unit,
    onInspectCompanion: (CompanionId) -> Unit,
    onResolveCompanionDialog: (String) -> Unit,
    onResolveManufacturerOffer: (String?) -> Unit,
    onUseBackpackItem: (Long) -> Unit,
    onDiscardBackpackItem: (Long) -> Unit,
    onCraftBackpack: (BackpackRecipe) -> Unit,
    onDismissArchiveDiscovery: () -> Unit,
    onToggleLight: () -> Unit,
    onCycleLightSource: () -> Unit,
    onOpenContractBoard: () -> Unit,
    onCloseContractBoard: () -> Unit,
    onAcceptContract: (ContractDef) -> Unit,
    onAbandonContract: (String) -> Unit,
    onAbandonRun: () -> Unit
) {
    var pauseTab by remember { mutableStateOf(0) } // 0 = Menu/Stats, 1 = Body/Synergies, 2 = Toybox Memory, 3 = Map
    var showMemoryInspector by remember { mutableStateOf(false) }
    var showBackpack by remember { mutableStateOf(false) }
    var showEmotionDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            state.contractBoardOpen -> onCloseContractBoard()
            showEmotionDialog -> {
                showEmotionDialog = false
                onTogglePause(false)
            }
            showBackpack -> {
                showBackpack = false
                onTogglePause(false)
            }
            showMemoryInspector -> {
                showMemoryInspector = false
                onTogglePause(false)
            }
            else -> onTogglePause(!state.isPaused)
        }
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

                // Boss Health Bar at top of arena (with Phase, Armor & Mechanic Hint)
                val activeBoss = state.enemies.firstOrNull { it.type.isBoss }
                if (activeBoss != null) {
                    val phaseLabel = when (activeBoss.bossCurrentPhase) {
                        3 -> "FASE III"
                        2 -> "FASE II"
                        else -> "FASE I"
                    }
                    val armorTag = if (activeBoss.bossArmorActive) "BLINDADO" else "VULNERABLE"
                    val mechKind = activeBoss.type.bossMechanicKind()
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.86f)
                            .padding(top = 8.dp)
                            .background(CharcoalBlack.copy(alpha = 0.84f), CutCornerShape(4.dp))
                            .border(1.2.dp, if (activeBoss.bossArmorActive) TarnishedBrass else CrimsonBright, CutCornerShape(4.dp))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${activeBoss.type.displayName.uppercase()} • $phaseLabel • $armorTag",
                            style = MaterialTheme.typography.labelLarge,
                            color = AgedIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = mechKind.hint,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = TarnishedBrass,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top-left Arena Column: Companions, Light Source Chip, Emotion Chip, Contracts Chip
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = if (activeBoss != null) 72.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.activeCompanions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                            modifier = Modifier.size(24.dp)
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

                    // Light Source Toggle & Cycle Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val lightCol = if (state.lightOn && state.lightSource != LightSourceType.NONE && state.lightSourceCharge > 0f) {
                            AmberWarning
                        } else AgedWoodLight
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
                                .clip(CutCornerShape(5.dp))
                                .background(CharcoalBlack.copy(alpha = 0.84f))
                                .border(1.2.dp, lightCol, CutCornerShape(5.dp))
                                .testTag("btn_light_toggle")
                                .clickable(onClick = onToggleLight)
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Image(
                                bitmap = PixelArtSprites.forLightSource(state.lightSource),
                                contentDescription = "Alternar luz",
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (state.lightOn && state.lightSource != LightSourceType.NONE) "LUZ ON" else "LUZ OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = if (state.lightOn) AmberWarning else ParchmentDark
                                )
                                Text(
                                    text = if (state.lightSource.drainPerSec == 0f) "∞" else "${state.lightSourceCharge.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AgedIvory
                                )
                            }
                        }
                        if (state.ownedLightSources.size > 1) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CutCornerShape(5.dp))
                                    .background(CharcoalBlack.copy(alpha = 0.84f))
                                    .border(1.dp, TarnishedBrassDim, CutCornerShape(5.dp))
                                    .testTag("btn_light_cycle")
                                    .clickable(onClick = onCycleLightSource),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar fuente de luz", tint = TarnishedBrass, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Emotion & Contracts Compact Row
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val em = state.emotionState
                        val emCol = Color(em.fusion?.colorHex ?: em.primary.colorHex)
                        val emLabel = em.fusion?.displayName
                            ?: if (em.primary == PatchEmotion.NEUTRAL) "Estable" else "${em.primary.displayName} ${em.level}"
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(CutCornerShape(5.dp))
                                .background(CharcoalBlack.copy(alpha = 0.84f))
                                .border(1.2.dp, emCol, CutCornerShape(5.dp))
                                .testTag("btn_emotion_status")
                                .clickable {
                                    showEmotionDialog = true
                                    onTogglePause(true)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Mood, contentDescription = "Estado emocional", tint = emCol, modifier = Modifier.size(16.dp))
                            Text(
                                text = emLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AgedIvory,
                                maxLines = 1
                            )
                        }

                        val openContracts = state.activeContracts.count { !it.completed && !it.failed }
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
                                .clip(CutCornerShape(5.dp))
                                .background(CharcoalBlack.copy(alpha = 0.84f))
                                .border(1.2.dp, if (openContracts > 0) TarnishedBrass else AgedWoodLight, CutCornerShape(5.dp))
                                .testTag("btn_contracts")
                                .clickable(onClick = onOpenContractBoard)
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = "Contratos", tint = TarnishedBrass, modifier = Modifier.size(16.dp))
                            Text(
                                text = "$openContracts/2",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AgedIvory
                            )
                        }
                    }
                }

                // Top-right Arena Column: Mochila Button, Dynamic Room Mechanic Pill & Closed Mystery Box badge
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val packTier = state.backpackTier
                    val bpUsed = BackpackSystem.usedSlots(state)
                    val bpCap = BackpackSystem.capacity(state)
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 76.dp, minHeight = 48.dp)
                            .clip(CutCornerShape(5.dp))
                            .background(CharcoalBlack.copy(alpha = 0.85f))
                            .border(1.5.dp, packTier?.rarity?.color ?: AgedWoodLight, CutCornerShape(5.dp))
                            .testTag("btn_backpack")
                            .clickable {
                                showBackpack = true
                                onTogglePause(true)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (packTier != null) {
                            Image(
                                bitmap = PixelArtSprites.forBackpackTier(packTier),
                                contentDescription = "Abrir Mochila",
                                filterQuality = FilterQuality.None,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Backpack,
                                contentDescription = "Sin Mochila",
                                tint = ParchmentDark.copy(alpha = 0.55f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (packTier != null) "MOCHILA" else "SIN MOCHILA",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = ParchmentDark
                            )
                            Text(
                                text = if (packTier != null) "$bpUsed/$bpCap" else "0/0",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    packTier == null -> ParchmentDark.copy(alpha = 0.7f)
                                    bpUsed >= bpCap -> CrimsonBright
                                    else -> TarnishedBrass
                                }
                            )
                        }
                    }

                    if (state.currentRoom.mechanic != RoomMechanic.NONE) {
                        Box(
                            modifier = Modifier
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
                    if (state.closedMysteryBoxTimer > 0) {
                        Box(
                            modifier = Modifier
                                .background(CharcoalBlack.copy(alpha = 0.82f), CutCornerShape(4.dp))
                                .border(1.dp, CrimsonBright, CutCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Caja Cerrada: ${state.closedMysteryBoxTimer} salas",
                                style = MaterialTheme.typography.labelSmall,
                                color = AgedIvory
                            )
                        }
                    }
                }

                // Alternative Room Objective HUD Pill (top-center when active & no boss bar)
                val obj = state.roomObjective
                val loreRoom = state.loreRoom
                val labState = state.labyrinthState
                if (obj != null && !obj.completed && !obj.failed && activeBoss == null) {
                    val statusText = when {
                        obj.maxTimerSec > 0f -> "${obj.timerSec.toInt()}s"
                        obj.type == ObjectiveType.ESCORT_PACKAGE -> "HP ${obj.entityHp.toInt()}"
                        else -> "${obj.currentCount}/${obj.targetCount}"
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 58.dp, end = 92.dp)
                            .background(CharcoalBlack.copy(alpha = 0.84f), CutCornerShape(5.dp))
                            .border(1.2.dp, TarnishedBrass, CutCornerShape(5.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = TarnishedBrass, modifier = Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "${obj.type.displayName} • $statusText",
                                style = MaterialTheme.typography.labelMedium,
                                color = AgedIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (obj.canAbandon) {
                            IconButton(
                                onClick = onAbandonObjective,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_abandon_objective")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Abandonar objetivo", tint = ParchmentDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else if (loreRoom != null && activeBoss == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 58.dp, end = 92.dp)
                            .background(CharcoalBlack.copy(alpha = 0.86f), CutCornerShape(5.dp))
                            .border(1.2.dp, Color(loreRoom.loreRoomType.badgeColorHex), CutCornerShape(5.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${loreRoom.loreRoomType.displayName.uppercase()} • ${if (loreRoom.solved) "RESUELTO ✓" else loreRoom.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (loreRoom.solved) MutedMoss else TarnishedBrass,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val infoLine = when {
                            loreRoom.heldTokens.isNotEmpty() -> "En mano: ${loreRoom.heldTokens.joinToString(", ")}"
                            loreRoom.hintStepText.isNotBlank() -> loreRoom.hintStepText
                            else -> loreRoom.hintSummary
                        }
                        Text(
                            text = infoLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = AgedIvory,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (labState != null && activeBoss == null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp, start = 90.dp, end = 92.dp)
                            .background(CharcoalBlack.copy(alpha = 0.86f), CutCornerShape(5.dp))
                            .border(1.2.dp, if (labState.perfectRewardClaimed) MutedMoss else AmberWarning, CutCornerShape(5.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (labState.perfectRewardClaimed) "${labState.type.displayName} • PERFECTA ✓" else labState.type.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (labState.perfectRewardClaimed) MutedMoss else AmberWarning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "C:${labState.chestsOpened}/${labState.chestsTotal} J:${labState.lostToysFound}/${labState.lostToysTotal} P:${if (labState.hiddenPocketFound) "✓" else "✗"} T:${labState.trapsTriggered}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = AgedIvory,
                            maxLines = 1
                        )
                    }
                }

                // Dynamic Toast Banner (Synergy, Floor, Toybox Memory, Loot)
                state.activeBanner?.let { banner ->
                    val hasTopPill = activeBoss != null || state.activeCompanions.isNotEmpty() || (obj != null && !obj.completed && !obj.failed) || labState != null
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = if (hasTopPill) 58.dp else 10.dp,
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

                // Bottom-center contextual action buttons: INTERACTUAR + Room Facility chips
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.interactionPrompt?.let { prompt ->
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 140.dp, minHeight = 48.dp)
                                .clip(CutCornerShape(6.dp))
                                .background(CrimsonBlood)
                                .border(1.5.dp, TarnishedBrass, CutCornerShape(6.dp))
                                .testTag("btn_interact")
                                .clickable(onClick = onInteract)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.TouchApp, contentDescription = prompt, tint = TarnishedBrass, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "INTERACTUAR: ${prompt.uppercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AgedIvory
                                )
                            }
                        }
                    }

                    if (state.currentRoom.type.name == "SHOP" && !state.shopOpen) {
                        FloatingRoomChip(
                            text = "ABRIR PUESTO ($)",
                            onClick = onReopenFacility
                        )
                    } else if (state.currentRoom.type.name == "REPAIR" && !state.repairBenchOpen) {
                        FloatingRoomChip(
                            text = "USAR MESA DE COSTURA (✂)",
                            onClick = onReopenFacility
                        )
                    }
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
                onStartRepairMinigame = onStartRepairMinigame,
                onCraftBackpack = onCraftBackpack,
                onClose = onCloseRepairBench
            )
        }

        if (state.pendingRepairMinigame != null) {
            RepairMinigameDialog(
                minigame = state.pendingRepairMinigame,
                currentButtons = state.buttons,
                onAction = onRepairMinigameAction
            )
        }

        if (state.manufacturerEncounter != null) {
            ManufacturerDialog(
                encounter = state.manufacturerEncounter,
                currentButtons = state.buttons,
                onSelectOffer = onResolveManufacturerOffer
            )
        }

        if (state.pendingCompanionDialog != null) {
            CompanionInteractionDialog(
                dialog = state.pendingCompanionDialog,
                currentButtons = state.buttons,
                onSelectOption = onResolveCompanionDialog
            )
        }

        state.pendingArchiveDiscovery?.let { discovery ->
            ArchiveDiscoveryDialog(
                discovery = discovery,
                onDismiss = onDismissArchiveDiscovery
            )
        }

        if (state.contractBoardOpen) {
            ContractBoardDialog(
                state = state,
                onAccept = onAcceptContract,
                onAbandon = onAbandonContract,
                onClose = onCloseContractBoard
            )
        } else if (showEmotionDialog) {
            EmotionStatusDialog(
                state = state,
                onClose = {
                    showEmotionDialog = false
                    onTogglePause(false)
                }
            )
        } else if (showBackpack) {
            BackpackDialog(
                state = state,
                onUseItem = onUseBackpackItem,
                onDiscardItem = onDiscardBackpackItem,
                onClose = {
                    showBackpack = false
                    onTogglePause(false)
                }
            )
        } else if (showMemoryInspector) {
            ToyboxMemoryDialog(
                state = state,
                onClose = {
                    showMemoryInspector = false
                    onTogglePause(false)
                }
            )
        } else if (state.isPaused && state.pendingArchiveDiscovery == null) {
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
                val roomLabel = state.currentRoom.subRoomName.ifBlank { state.currentRoom.type.displayName }
                Text(
                    text = "P${state.floorNumber}: $roomLabel",
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
    val secretSense = EmotionSystem.computeModifiers(state.emotionState).secretSenseBonus >= 0.35f
    val visitedSet = remember(state.dungeon.rooms, state.currentCoord) {
        state.dungeon.rooms.values.filter { it.visited || it.coord == state.currentCoord }.map { it.coord }.toSet()
    }
    val oneHopNeighbors = remember(state.dungeon.rooms, visitedSet) {
        buildSet {
            visitedSet.forEach { vc ->
                val vr = state.dungeon.rooms[vc] ?: return@forEach
                vr.doors.forEach { dir -> add(vc.neighbor(dir)) }
                vr.secretDoors.forEach { dir -> add(vc.neighbor(dir)) }
            }
        }
    }
    val twoHopNeighbors = remember(state.dungeon.rooms, oneHopNeighbors, secretSense) {
        if (!secretSense) emptySet() else buildSet {
            oneHopNeighbors.forEach { nc ->
                val nr = state.dungeon.rooms[nc] ?: return@forEach
                nr.doors.forEach { dir -> add(nc.neighbor(dir)) }
            }
        }
    }

    Canvas(modifier = modifier) {
        val cell = 6.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cur = state.currentCoord
        state.dungeon.rooms.values.forEach { r ->
            val isSecret = r.type == RoomType.SECRET
            val showSecret = isSecret && (r.visited || state.toyboxMemory.secretCluesRevealed || (secretSense && r.coord in oneHopNeighbors))
            if (isSecret && !showSecret) return@forEach

            val isKnownVisited = r.visited || r.coord == cur
            val isSilhouette = !isKnownVisited && (r.coord in oneHopNeighbors || r.coord in twoHopNeighbors)
            if (!isKnownVisited && !isSilhouette) return@forEach

            val dx = (r.coord.x - cur.x) * (cell + 1.5f)
            val dy = (r.coord.y - cur.y) * (cell + 1.5f)
            val rx = cx + dx - cell / 2f
            val ry = cy + dy - cell / 2f
            if (rx in 0f..(size.width - cell) && ry in 0f..(size.height - cell)) {
                val col = when {
                    r.coord == cur -> TarnishedBrass
                    forgetful && isKnownVisited -> ParchmentDark.copy(alpha = 0.45f)
                    isSilhouette -> AgedWoodLight.copy(alpha = 0.42f)
                    r.type == RoomType.BOSS -> CrimsonBright
                    r.type == RoomType.TREASURE -> Color(0xFFD9C05B)
                    r.type == RoomType.SHOP -> MutedMoss
                    r.type == RoomType.REPAIR -> MutedMoss.copy(alpha = 0.85f)
                    r.type == RoomType.LABYRINTH -> AmberWarning
                    r.type == RoomType.MANUFACTURER -> Color(0xFFB388EB)
                    r.type == RoomType.LORE -> Color(0xFF9E7AC8)
                    r.type == RoomType.FORBIDDEN -> CrimsonBlood
                    else -> AgedIvory.copy(alpha = 0.72f)
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
