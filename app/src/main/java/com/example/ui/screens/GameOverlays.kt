package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.GameCatalog
import com.example.data.model.BodyZone
import com.example.data.model.CompanionId
import com.example.data.model.ConsumableType
import com.example.data.model.ManufacturerEncounter
import com.example.data.model.RepairActionMode
import com.example.data.model.RepairMaterial
import com.example.data.model.RepairMinigameType
import com.example.data.model.RoomEventDef
import com.example.data.model.RoomMechanic
import com.example.data.model.WearState
import com.example.engine.*
import com.example.ui.components.PixelArtSprites
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxPlaqueButton
import com.example.ui.theme.*

@Composable
fun PartChoiceDialog(
    choice: PendingPartChoice,
    onEquip: () -> Unit,
    onSalvage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.84f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            borderColor = TarnishedBrass
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CharcoalBlack, CutCornerShape(4.dp))
                        .border(1.dp, choice.newPart.rarity.color, CutCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = PixelArtSprites.forBodyPart(choice.newPart.id, choice.newPart.zone),
                        contentDescription = choice.newPart.name,
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PIEZA CORPORAL ENCONTRADA",
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedBrass
                    )
                    Text(
                        text = "${choice.newPart.name} (${choice.newPart.zone.displayName})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AgedIvory
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${choice.newPart.rarity.displayName} • ${choice.newPart.intactDesc}",
                style = MaterialTheme.typography.bodyLarge,
                color = Parchment
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Etiquetas: ${choice.newPart.tags.joinToString(", ")}",
                style = MaterialTheme.typography.labelMedium,
                color = MutedMoss
            )

            choice.currentInZone?.let { cur ->
                StitchedDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Actual en ${cur.def.zone.displayName}: ${cur.def.name} (${cur.durability}/${cur.def.maxDurability} • ${cur.wearState.displayName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentDark
                )
            }

            if (choice.resultingSynergies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sinergias activas al equipar: ${choice.resultingSynergies.joinToString { it.name }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TarnishedBrass
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToyboxPlaqueButton(
                    text = "Desguazar (+3 ●)",
                    onClick = onSalvage,
                    modifier = Modifier.weight(1f),
                    testTag = "part_salvage_btn"
                )
                ToyboxPlaqueButton(
                    text = "Equipar Pieza",
                    primary = true,
                    onClick = onEquip,
                    modifier = Modifier.weight(1f),
                    testTag = "part_equip_btn"
                )
            }
        }
    }
}

@Composable
fun RoomEventDialog(
    event: RoomEventDef,
    currentButtons: Int,
    onSelectOption: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.85f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            borderColor = TarnishedBrass
        ) {
            Text("EVENTO DE WONDERLAND", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
            Text(event.title, style = MaterialTheme.typography.headlineMedium, color = AgedIvory)
            Spacer(Modifier.height(8.dp))
            Text(event.narrative, style = MaterialTheme.typography.bodyLarge, color = Parchment)
            StitchedDivider(Modifier.padding(vertical = 10.dp))

            event.options.forEach { opt ->
                val canAfford = currentButtons >= opt.buttonCost
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CutCornerShape(4.dp))
                        .background(if (canAfford) AgedWoodMid else CharcoalBlack)
                        .border(1.dp, if (canAfford) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
                        .clickable(enabled = canAfford) { onSelectOption(opt.id) }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(opt.label, style = MaterialTheme.typography.titleMedium, color = if (canAfford) AgedIvory else ParchmentDark)
                        if (opt.buttonCost > 0) {
                            Text("${opt.buttonCost} ●", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                        }
                    }
                    Text(opt.description, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                }
            }
        }
    }
}

@Composable
fun CompanionInteractionDialog(
    dialog: PendingCompanionDialog,
    currentButtons: Int,
    onSelectOption: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.86f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            borderColor = dialog.companionId.rarity.color
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(CharcoalBlack, CutCornerShape(4.dp))
                        .border(1.5.dp, dialog.companionId.rarity.color, CutCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = PixelArtSprites.forCompanion(dialog.companionId),
                        contentDescription = dialog.companionId.displayName,
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dialog.subtitle.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedBrass
                    )
                    Text(
                        text = dialog.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AgedIvory
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = dialog.narrative,
                style = MaterialTheme.typography.bodyLarge,
                color = Parchment
            )
            StitchedDivider(Modifier.padding(vertical = 10.dp))

            Column(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dialog.options.forEach { opt ->
                    val canAfford = currentButtons >= opt.buttonCost
                    val borderCol = when {
                        !canAfford -> AgedWoodLight
                        opt.isDanger -> CrimsonBright
                        else -> TarnishedBrass
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CutCornerShape(4.dp))
                            .background(if (canAfford) AgedWoodMid else CharcoalBlack)
                            .border(1.2.dp, borderCol, CutCornerShape(4.dp))
                            .testTag("companion_opt_${opt.id}")
                            .clickable(enabled = canAfford) { onSelectOption(opt.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    !canAfford -> ParchmentDark
                                    opt.isDanger -> CrimsonBright
                                    else -> AgedIvory
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (opt.buttonCost > 0) {
                                Text(
                                    text = "${opt.buttonCost} ●",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (canAfford) TarnishedBrass else CrimsonBright
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = opt.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParchmentDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
    room: RoomBlueprint,
    buttons: Int,
    onBuy: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.85f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            borderColor = MutedMoss
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PUESTO DE BOTONES", style = MaterialTheme.typography.headlineMedium, color = TarnishedBrass)
                Text("Tienes: $buttons ●", style = MaterialTheme.typography.labelLarge, color = AgedIvory)
            }
            StitchedDivider(Modifier.padding(vertical = 8.dp))

            room.shopStock.forEach { offer ->
                val canBuy = !offer.purchased && buttons >= offer.price
                val offerBmp = when {
                    offer.isHeal -> PixelArtSprites.pickupHeart()
                    offer.isConsumable && offer.itemId != null ->
                        runCatching { PixelArtSprites.forConsumable(ConsumableType.valueOf(offer.itemId)) }.getOrNull()
                    offer.isBodyPart && offer.itemId != null -> {
                        val pt = GameCatalog.partById(offer.itemId)
                        PixelArtSprites.forBodyPart(pt.id, pt.zone)
                    }
                    offer.itemId != null -> PixelArtSprites.forPassiveItem(offer.itemId)
                    else -> null
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CutCornerShape(4.dp))
                        .background(if (offer.purchased) CharcoalBlack else AgedWoodMid)
                        .border(1.dp, if (canBuy) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
                        .clickable(enabled = canBuy) { onBuy(offer.id) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (offerBmp != null) {
                        Image(
                            bitmap = offerBmp,
                            contentDescription = offer.label,
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(28.dp).padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = if (offer.purchased) "${offer.label} (Agotado)" else offer.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (canBuy) AgedIvory else ParchmentDark,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${offer.price} ●",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canBuy) TarnishedBrass else CrimsonBright
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            ToyboxPlaqueButton(text = "Salir del Puesto", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RepairBenchDialog(
    state: GameRunState,
    onRepairZone: (BodyZone?) -> Unit,
    onStartRepairMinigame: (BodyZone, RepairActionMode) -> Unit,
    onClose: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(RepairActionMode.REPAIR) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.86f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).heightIn(max = 620.dp),
            borderColor = TarnishedBrass
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MESA DE COSTURA", style = MaterialTheme.typography.headlineMedium, color = TarnishedBrass)
                Text("${state.buttons} ●", style = MaterialTheme.typography.labelLarge, color = AgedIvory)
            }
            Text(selectedMode.desc, style = MaterialTheme.typography.bodyMedium, color = Parchment)

            Spacer(Modifier.height(8.dp))
            // Action mode tabs: Reparar / Reforzar / Modificar
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                RepairActionMode.entries.forEach { mode ->
                    val active = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CutCornerShape(4.dp))
                            .background(if (active) CrimsonBlood else AgedWoodMid)
                            .border(1.dp, if (active) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
                            .testTag("repair_mode_${mode.name}")
                            .clickable { selectedMode = mode }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.displayName, style = MaterialTheme.typography.labelLarge, color = AgedIvory)
                    }
                }
            }

            // Materials inventory strip
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RepairMaterial.entries.forEach { mat ->
                    val count = state.materials[mat] ?: 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CharcoalBlack.copy(alpha = 0.7f), CutCornerShape(4.dp))
                            .border(1.dp, if (count > 0) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Image(
                            bitmap = PixelArtSprites.forMaterial(mat),
                            contentDescription = mat.displayName,
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${mat.displayName.split(" ").first()}: $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count > 0) AgedIvory else ParchmentDark
                        )
                    }
                }
            }

            StitchedDivider(Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                BodyZone.entries.forEach { z ->
                    val part = state.equippedParts[z] ?: return@forEach
                    val needsRepair = part.isDetached || part.durability < part.def.maxDurability
                    val canOperate = (needsRepair || selectedMode != RepairActionMode.REPAIR) && state.buttons >= 1
                    val reinforcedLeft = state.partReinforcedRooms[z] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .background(AgedWoodMid, CutCornerShape(4.dp))
                            .border(1.dp, if (canOperate) MutedMoss else AgedWoodLight, CutCornerShape(4.dp))
                            .testTag("repair_zone_${z.name}")
                            .clickable(enabled = canOperate) { onStartRepairMinigame(z, selectedMode) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = PixelArtSprites.forBodyPart(part.def.id, z),
                            contentDescription = part.def.name,
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(30.dp).padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${z.displayName}: ${part.def.name}", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                            val extra = if (reinforcedLeft > 0) " • Reforzado ($reinforcedLeft)" else ""
                            Text("${part.wearState.displayName} (${part.durability}/${part.def.maxDurability})$extra", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                        }
                        if (needsRepair && state.buttons >= 2) {
                            TextButton(
                                onClick = { onRepairZone(z) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Rápido 2●", style = MaterialTheme.typography.labelSmall, color = ParchmentDark)
                            }
                        }
                        Text(
                            text = if (canOperate) "Taller 1●" else "Intacto",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (canOperate) TarnishedBrass else MutedMoss
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToyboxPlaqueButton(text = "Cerrar", onClick = onClose, modifier = Modifier.weight(1f), testTag = "repair_close_btn")
                ToyboxPlaqueButton(
                    text = "Reparar Todo (5 ●)",
                    primary = true,
                    enabled = state.buttons >= 5,
                    onClick = { onRepairZone(null) },
                    modifier = Modifier.weight(1f),
                    testTag = "repair_all_btn"
                )
            }
        }
    }
}

@Composable
fun RepairMinigameDialog(
    minigame: PendingRepairMinigame,
    currentButtons: Int,
    onAction: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.88f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
            borderColor = TarnishedBrass
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(minigame.minigameType.displayName.uppercase(), style = MaterialTheme.typography.headlineMedium, color = TarnishedBrass)
                Text("Intentos: ${minigame.attemptsLeft}", style = MaterialTheme.typography.labelLarge, color = AgedIvory)
            }
            Text(
                text = "${minigame.mode.displayName} • ${minigame.zone.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = CrimsonBright
            )
            Spacer(Modifier.height(4.dp))
            Text(minigame.minigameType.instruction, style = MaterialTheme.typography.bodyMedium, color = Parchment)
            StitchedDivider(Modifier.padding(vertical = 10.dp))

            when (minigame.minigameType) {
                RepairMinigameType.SEWING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        minigame.sequenceTarget.forEachIndexed { idx, num ->
                            val done = idx < minigame.currentStep
                            val isNext = idx == minigame.currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .background(if (done) MutedMoss else if (isNext) CrimsonBlood else AgedWoodMid)
                                    .border(1.5.dp, if (isNext) TarnishedBrass else AgedWoodLight, CutCornerShape(6.dp))
                                    .testTag("minigame_sew_$num")
                                    .clickable(enabled = !done) { onAction("sew_tap:$num") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (done) "✓" else "Punto $num",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AgedIvory
                                )
                            }
                        }
                    }
                }

                RepairMinigameType.GEARS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        minigame.gearStates.forEachIndexed { idx, rotState ->
                            val aligned = rotState == 2
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .background(if (aligned) MutedMoss else AgedWoodMid)
                                    .border(1.5.dp, if (aligned) TarnishedBrass else CrimsonBright, CutCornerShape(6.dp))
                                    .testTag("minigame_gear_$idx")
                                    .clickable { onAction("gear_rotate:$idx") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚙ #${idx + 1}", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                                    Text(
                                        text = when (rotState) {
                                            2 -> "Alineado"
                                            1 -> "120°"
                                            else -> "0°"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TarnishedBrass
                                    )
                                }
                            }
                        }
                    }
                }

                RepairMinigameType.SPRING_TIMING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(CharcoalBlack, CutCornerShape(4.dp))
                                .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                        ) {
                            // Golden sweet spot bar (center 38%..62%)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.24f)
                                    .align(Alignment.Center)
                                    .background(TarnishedBrass.copy(alpha = 0.42f))
                            )
                            // Moving cursor
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val cursorX = maxWidth * minigame.springCursor.coerceIn(0f, 0.96f)
                                Box(
                                    modifier = Modifier
                                        .offset(x = cursorX)
                                        .width(10.dp)
                                        .fillMaxHeight()
                                        .background(CrimsonBright)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ToyboxPlaqueButton(
                            text = "DETENER RESORTE",
                            icon = Icons.Default.PanTool,
                            primary = true,
                            onClick = { onAction("spring_stop") },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "minigame_spring_stop"
                        )
                    }
                }

                RepairMinigameType.PORCELAIN -> {
                    val crackLabels = listOf("Grieta Ocular", "Fisura Mejilla", "Borde Sien")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        crackLabels.forEachIndexed { idx, label ->
                            val done = idx in minigame.porcelainMatched
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .background(if (done) MutedMoss else AgedWoodMid)
                                    .border(1.5.dp, if (done) TarnishedBrass else AgedWoodLight, CutCornerShape(6.dp))
                                    .testTag("minigame_porcelain_$idx")
                                    .clickable(enabled = !done) { onAction("porcelain_match:$idx") }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (done) "Unida ✓" else label, style = MaterialTheme.typography.labelMedium, color = AgedIvory)
                            }
                        }
                    }
                }

                RepairMinigameType.WIRING -> {
                    val wireColors = listOf("Núcleo Latón" to TarnishedBrass, "Chispa Roja" to CrimsonBright, "Bobina Verde" to MutedMoss)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wireColors.forEachIndexed { idx, (label, col) ->
                            val done = idx in minigame.wiringMatched
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .background(if (done) col.copy(alpha = 0.45f) else AgedWoodMid)
                                    .border(1.5.dp, col, CutCornerShape(6.dp))
                                    .testTag("minigame_wiring_$idx")
                                    .clickable(enabled = !done) { onAction("wiring_pair:$idx") }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (done) "Conectado ✓" else label, style = MaterialTheme.typography.labelMedium, color = AgedIvory)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToyboxPlaqueButton(
                    text = "Cancelar",
                    onClick = { onAction("cancel") },
                    modifier = Modifier.weight(1f),
                    testTag = "minigame_cancel"
                )
                ToyboxPlaqueButton(
                    text = "Rápida (2 ●)",
                    enabled = currentButtons >= 2,
                    onClick = { onAction("quick_repair") },
                    modifier = Modifier.weight(1f),
                    testTag = "minigame_quick"
                )
            }
        }
    }
}

@Composable
fun ManufacturerDialog(
    encounter: ManufacturerEncounter,
    currentButtons: Int,
    onSelectOffer: (String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.88f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 470.dp).heightIn(max = 640.dp),
            borderColor = CrimsonBright
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(CharcoalBlack, CutCornerShape(4.dp))
                        .border(1.5.dp, TarnishedBrass, CutCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = PixelArtSprites.manufacturerNpc(),
                        contentDescription = "El Fabricante",
                        filterQuality = FilterQuality.None,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(46.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRESENCIA ANTIGUA • $currentButtons ●", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                    Text(encounter.title, style = MaterialTheme.typography.headlineMedium, color = AgedIvory)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(encounter.dialogue, style = MaterialTheme.typography.bodyLarge, color = Parchment)
            StitchedDivider(Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                encounter.offers.forEach { offer ->
                    val canAfford = currentButtons >= offer.buttonsCost
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CutCornerShape(5.dp))
                            .background(if (canAfford) AgedWoodMid else CharcoalBlack)
                            .border(1.2.dp, if (canAfford) TarnishedBrass else AgedWoodLight, CutCornerShape(5.dp))
                            .testTag("manufacturer_offer_${offer.id}")
                            .clickable(enabled = canAfford) { onSelectOffer(offer.id) }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(offer.category.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = CrimsonBright)
                            if (offer.buttonsCost > 0) {
                                Text("${offer.buttonsCost} ●", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                            }
                        }
                        Text(offer.title, style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                        Text("Precio: ${offer.costSummary}", style = MaterialTheme.typography.bodyMedium, color = AmberWarning)
                        Text("Beneficio: ${offer.rewardSummary}", style = MaterialTheme.typography.bodyMedium, color = MutedMoss)
                        Text(offer.riskHint, style = MaterialTheme.typography.labelSmall, color = ParchmentDark)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            ToyboxPlaqueButton(
                text = "Rechazar Oferta",
                icon = Icons.Default.Close,
                onClick = { onSelectOffer(null) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "manufacturer_reject_btn"
            )
        }
    }
}

@Composable
fun ToyboxMemoryDialog(
    state: GameRunState,
    onClose: () -> Unit
) {
    val activeTraits = ToyboxMemorySystem.activeTraits(state.toyboxMemory)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.88f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            borderColor = CrimsonBright
        ) {
            Text("LA JUGUETERÍA RECUERDA", style = MaterialTheme.typography.headlineMedium, color = TarnishedBrass)
            Text(
                "Wonderland Toys observa cómo juegas y responde de manera legible.",
                style = MaterialTheme.typography.bodyMedium,
                color = Parchment
            )
            StitchedDivider(Modifier.padding(vertical = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MemoryCounterChip("Esquivas", "${state.toyboxMemory.dodgeUseCount}/8")
                MemoryCounterChip("Explosivos", "${state.toyboxMemory.explosiveUseCount}/4")
                MemoryCounterChip("Recuerdos", "${state.toyboxMemory.memoriesCollected.size}")
            }

            Spacer(Modifier.height(10.dp))
            if (activeTraits.isEmpty()) {
                Text(
                    "La tienda aún te observa en silencio. Tus hábitos (esquivar en exceso, abusar de bombas, ayudar o saquear juguetes) despertarán reacciones aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentDark
                )
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeTraits.forEach { tr ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalBlack.copy(alpha = 0.65f), CutCornerShape(4.dp))
                                .border(1.dp, if (tr.isPositive) MutedMoss else CrimsonBright, CutCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(tr.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AgedIvory)
                            Text("Motivo: ${tr.triggerReason}", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                            Text("Efecto: ${tr.gameplayEffect}", style = MaterialTheme.typography.bodyMedium, color = TarnishedBrass)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ToyboxPlaqueButton(text = "Continuar Partida", primary = true, onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MemoryCounterChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(CharcoalBlack, CutCornerShape(3.dp))
            .border(1.dp, TarnishedBrassDim, CutCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ParchmentDark)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
    }
}

@Composable
fun PauseOverlayDialog(
    state: GameRunState,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onResume: () -> Unit,
    onOpenTutorial: () -> Unit,
    onAbandon: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack.copy(alpha = 0.88f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ToyboxFrameCard(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(max = 600.dp),
            borderColor = TarnishedBrass
        ) {
            Text("PAUSA • ${state.character.name.uppercase()}", style = MaterialTheme.typography.headlineMedium, color = TarnishedBrass)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Menú", "Cuerpo & Sinergias", "Defectos & Objetos").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CutCornerShape(4.dp))
                            .background(if (selectedTab == idx) CrimsonBlood else AgedWoodMid)
                            .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                            .clickable { onSelectTab(idx) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = AgedIvory)
                    }
                }
            }
            StitchedDivider(Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        Text("Piso ${state.floorNumber}: ${state.floorTheme.name}", style = MaterialTheme.typography.titleLarge, color = AgedIvory)
                        if (state.currentRoom.mechanic != RoomMechanic.NONE) {
                            Text(
                                "Sala actual: ${state.currentRoom.mechanic.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TarnishedBrass
                            )
                            Text(
                                state.currentRoom.mechanic.hintText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ParchmentDark
                            )
                        }
                        Text("Salas limpiadas: ${state.roomsClearedCount} • Enemigos: ${state.enemiesDefeatedCount}", style = MaterialTheme.typography.bodyMedium, color = Parchment)
                        Text("Tiempo: ${state.elapsedSeconds.toInt()}s • Botones: ${state.buttons}", style = MaterialTheme.typography.bodyMedium, color = Parchment)
                        Spacer(Modifier.height(8.dp))
                        ToyboxPlaqueButton("Continuar", Icons.Default.PlayArrow, onResume, primary = true, modifier = Modifier.fillMaxWidth())
                        ToyboxPlaqueButton("Ver Tutorial", Icons.AutoMirrored.Filled.HelpOutline, onOpenTutorial, modifier = Modifier.fillMaxWidth())
                        ToyboxPlaqueButton("Abandonar Partida", Icons.AutoMirrored.Filled.ExitToApp, onAbandon, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        Text("PARTES CORPORALES (6 ZONAS)", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                        BodyZone.entries.forEach { z ->
                            val ep = state.equippedParts[z]
                            if (ep != null) {
                                val col = when (ep.wearState) {
                                    WearState.INTACT -> MutedMoss
                                    WearState.DAMAGED -> AmberWarning
                                    WearState.CRITICAL -> CrimsonBright
                                    WearState.DETACHED -> ParchmentDark
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(CharcoalBlack.copy(alpha = 0.6f), CutCornerShape(4.dp))
                                        .border(1.dp, col, CutCornerShape(4.dp)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        bitmap = PixelArtSprites.forBodyPart(ep.def.id, z),
                                        contentDescription = ep.def.name,
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(30.dp).padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${z.displayName}: ${ep.def.name} (${ep.wearState.displayName} ${ep.durability}%)", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                                        val desc = when (ep.wearState) {
                                            WearState.INTACT -> ep.def.intactDesc
                                            WearState.DAMAGED -> ep.def.damagedDesc
                                            else -> ep.def.criticalDesc
                                        }
                                        Text(desc, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("SINERGIAS ACTIVAS (${state.activeSynergies.size})", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                        if (state.activeSynergies.isEmpty()) {
                            Text("Combina piezas y objetos con etiquetas afines para despertar sinergias.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                        } else {
                            state.activeSynergies.forEach { syn ->
                                Text("★ ${syn.name}: ${syn.description}", style = MaterialTheme.typography.bodyMedium, color = AgedIvory)
                            }
                        }
                    }
                    else -> {
                        Text("DEFECTOS ACTIVOS (${state.activeDefects.size})", style = MaterialTheme.typography.labelLarge, color = CrimsonBright)
                        state.activeDefects.forEach { d ->
                            Text("• ${d.name}: ${d.description}", style = MaterialTheme.typography.bodyMedium, color = Parchment)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("COMPAÑEROS ACTIVOS (${state.activeCompanions.size}/2)", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                        if (state.activeCompanions.isEmpty()) {
                            Text("Ningún juguete te acompaña todavía.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                        } else {
                            state.activeCompanions.forEach { comp ->
                                val status = when {
                                    comp.broken -> "AVERIADO"
                                    comp.needsAttention -> "INQUIETO"
                                    comp.id == CompanionId.BROKEN_SOLDIER -> "Resistencia ${comp.hp}/${comp.maxHp}"
                                    else -> "Activo"
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CharcoalBlack.copy(alpha = 0.55f), CutCornerShape(4.dp))
                                        .border(1.dp, if (comp.broken) CrimsonBright else TarnishedBrassDim, CutCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Image(
                                        bitmap = PixelArtSprites.forCompanion(comp.id, comp.broken),
                                        contentDescription = comp.id.displayName,
                                        filterQuality = FilterQuality.None,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(28.dp).padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${comp.id.displayName} ($status)", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                                        Text(comp.id.conditionDesc, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("OBJETOS PASIVOS (${state.passiveItems.size})", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                        state.passiveItems.forEach { it ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Image(
                                    bitmap = PixelArtSprites.forPassiveItem(it.id),
                                    contentDescription = it.name,
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(26.dp).padding(end = 8.dp)
                                )
                                Text("${it.name}: ${it.description}", style = MaterialTheme.typography.bodyMedium, color = AgedIvory)
                            }
                        }
                    }
                }
            }
            if (selectedTab != 0) {
                Spacer(Modifier.height(10.dp))
                ToyboxPlaqueButton("Reanudar", Icons.Default.PlayArrow, onResume, primary = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
