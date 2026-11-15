package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.DefectCatalog
import com.example.data.catalog.GameCatalog
import com.example.data.model.CharacterId
import com.example.data.model.DefectDef
import com.example.data.model.Difficulty
import com.example.ui.RunSetupState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RunSetupScreen(
    setup: RunSetupState,
    unlockedCharacters: Set<CharacterId>,
    onSelectCharacter: (CharacterId) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onToggleDefectMode: (Boolean) -> Unit,
    onRandomizeDefects: () -> Unit,
    onToggleManualDefect: (DefectDef) -> Unit,
    onStartRun: () -> Unit,
    onBack: () -> Unit
) {
    val selectedChar = GameCatalog.characterById(setup.selectedCharacter)
    val isUnlocked = selectedChar.id in unlockedCharacters
    var showDefectPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Preparar Partida",
            subtitle = "Vitrina de Muñecos y Defectos de Fábrica",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Character Selector Carousel
            Text(
                text = "ESTANTERÍA DE PERSONAJES",
                style = MaterialTheme.typography.labelLarge,
                color = TarnishedBrass
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameCatalog.characters.forEach { c ->
                    val unlocked = c.id in unlockedCharacters
                    val isSelected = c.id == setup.selectedCharacter
                    val border = if (isSelected) TarnishedBrass else TarnishedBrassDim.copy(alpha = 0.5f)
                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .clip(CutCornerShape(6.dp))
                            .background(if (isSelected) AgedWoodMid else AgedWoodDark)
                            .border(if (isSelected) 2.dp else 1.dp, border, CutCornerShape(6.dp))
                            .testTag("select_char_${c.id.name.lowercase()}")
                            .clickable { onSelectCharacter(c.id) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            DollPortraitCanvas(
                                character = c,
                                modifier = Modifier.size(62.dp)
                            )
                            if (!unlocked) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = CrimsonBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = c.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (unlocked) AgedIvory else ParchmentDark
                        )
                    }
                }
            }

            // Selected Character Vitrine Card
            ToyboxFrameCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isUnlocked) TarnishedBrass else CrimsonBlood
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DollPortraitCanvas(
                        character = selectedChar,
                        modifier = Modifier
                            .size(92.dp)
                            .border(1.dp, TarnishedBrassDim, CutCornerShape(6.dp))
                            .background(CharcoalBlack.copy(alpha = 0.6f), CutCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedChar.name} — ${selectedChar.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TarnishedBrass
                        )
                        Text(
                            text = selectedChar.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgedIvory
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Habilidad: ${selectedChar.abilityName} — ${selectedChar.abilityDesc}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Parchment
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge("Vida", "${selectedChar.maxHearts} ❤")
                    StatBadge("Daño", "${selectedChar.baseDamage.toInt()}")
                    StatBadge("Vel.", "${selectedChar.baseSpeed.toInt()}")
                    StatBadge("Objeto", GameCatalog.itemById(selectedChar.startingItemId)?.name ?: "-")
                }
                if (!isUnlocked) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "BLOQUEADO: ${selectedChar.unlockRequirementDesc}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonBright
                    )
                }
            }

            // Difficulty Selection
            Text(
                text = "DIFICULTAD",
                style = MaterialTheme.typography.labelLarge,
                color = TarnishedBrass
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Difficulty.entries.chunked(2).forEach { rowDiffs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDiffs.forEach { diff ->
                            val sel = diff == setup.selectedDifficulty
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CutCornerShape(5.dp))
                                    .background(if (sel) CrimsonBlood.copy(alpha = 0.45f) else AgedWoodDark)
                                    .border(if (sel) 2.dp else 1.dp, if (sel) TarnishedBrass else TarnishedBrassDim, CutCornerShape(5.dp))
                                    .testTag("diff_${diff.name.lowercase()}")
                                    .clickable { onSelectDifficulty(diff) }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(diff.displayName, style = MaterialTheme.typography.titleLarge, color = AgedIvory)
                                    Text("x${diff.medalMultiplier}", style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
                                }
                                Text(diff.description, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                            }
                        }
                    }
                }
            }

            // Manufacturing Defects Panel
            ToyboxFrameCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DEFECTOS DE FABRICACIÓN",
                            style = MaterialTheme.typography.titleLarge,
                            color = TarnishedBrass
                        )
                        val pts = setup.chosenDefects.sumOf { it.challengePoints }
                        Text(
                            text = "${setup.chosenDefects.size} activos • +$pts pts de desafío",
                            style = MaterialTheme.typography.labelMedium,
                            color = Parchment
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onRandomizeDefects,
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                                .testTag("randomize_defects_button")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Aleatorizar defectos", tint = TarnishedBrass)
                        }
                        IconButton(
                            onClick = { showDefectPicker = !showDefectPicker },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                                .testTag("toggle_manual_defects_button")
                        ) {
                            Icon(
                                if (showDefectPicker) Icons.Default.ExpandLess else Icons.Default.Tune,
                                contentDescription = "Configurar defectos manualmente",
                                tint = AgedIvory
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                if (setup.chosenDefects.isEmpty()) {
                    Text(
                        text = "Sin defectos activos en esta configuración.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentDark
                    )
                } else {
                    setup.chosenDefects.forEach { def ->
                        DefectInfoRow(def)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (showDefectPicker) {
                    StitchedDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catálogo (${DefectCatalog.allDefects.size}) • Máx ${setup.selectedDifficulty.maxDefects}:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TarnishedBrass
                        )
                        TextButton(onClick = { onToggleDefectMode(true) }) {
                            Text("Modo Aleatorio", color = Parchment)
                        }
                    }
                    DefectCatalog.allDefects.forEach { candidate ->
                        val active = setup.chosenDefects.any { it.id == candidate.id }
                        val compatible = active || (setup.chosenDefects.size < setup.selectedDifficulty.maxDefects.coerceAtLeast(1) &&
                            DefectCatalog.isCompatible(setup.chosenDefects, candidate))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(CutCornerShape(4.dp))
                                .background(if (active) CrimsonBlood.copy(alpha = 0.35f) else CharcoalBlack.copy(alpha = 0.5f))
                                .border(1.dp, if (active) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
                                .clickable(enabled = compatible) { onToggleManualDefect(candidate) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = active,
                                onCheckedChange = null,
                                enabled = compatible
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${candidate.name} (Sev. ${candidate.severity})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (compatible) AgedIvory else ParchmentDark.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = candidate.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParchmentDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom Start Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AgedWoodDark)
                .border(1.dp, TarnishedBrassDim)
                .padding(14.dp)
        ) {
            ToyboxPlaqueButton(
                text = if (isUnlocked) "ABRIR LA CAJA Y ENTRAR" else "PERSONAJE BLOQUEADO",
                icon = Icons.Default.PlayArrow,
                primary = true,
                enabled = isUnlocked,
                testTag = "start_run_button",
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartRun
            )
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(CharcoalBlack.copy(alpha = 0.65f), CutCornerShape(3.dp))
            .border(1.dp, TarnishedBrassDim, CutCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ParchmentDark)
        Text(value, style = MaterialTheme.typography.labelLarge, color = AgedIvory)
    }
}

@Composable
private fun DefectInfoRow(def: DefectDef) {
    val sevColor = when (def.severity) {
        1 -> MutedMoss
        2 -> AmberWarning
        else -> CrimsonBright
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalBlack.copy(alpha = 0.55f), CutCornerShape(4.dp))
            .border(1.dp, sevColor.copy(alpha = 0.7f), CutCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${def.name} • ${def.category.displayName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AgedIvory
            )
            Text(
                text = "+${def.challengePoints} pts",
                style = MaterialTheme.typography.labelMedium,
                color = sevColor
            )
        }
        Text(def.description, style = MaterialTheme.typography.bodyMedium, color = Parchment)
        if (def.upsideText.isNotEmpty()) {
            Text("Compensación: ${def.upsideText}", style = MaterialTheme.typography.bodyMedium, color = MutedMoss)
        }
    }
}
