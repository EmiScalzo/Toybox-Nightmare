package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.AmpliacionCatalog
import com.example.data.model.ArchiveCategory
import com.example.data.model.ArchiveEntryDef
import com.example.engine.AIImageArchiveSystem
import com.example.engine.WonderlandArchiveSystem
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxPlaqueButton
import com.example.ui.components.ToyboxScreenHeader
import com.example.ui.theme.*

@Composable
fun WonderlandArchiveScreen(
    discoveredKeys: Set<String>,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ArchiveCategory?>(null) }
    var zoomedEntry by remember { mutableStateOf<ArchiveEntryDef?>(null) }

    BackHandler {
        if (zoomedEntry != null) {
            zoomedEntry = null
        } else {
            onBack()
        }
    }

    val unlockedCount = remember(discoveredKeys) {
        WonderlandArchiveSystem.countUnlocked(discoveredKeys)
    }
    val totalCount = AmpliacionCatalog.archiveEntries.size
    val visibleEntries = remember(selectedCategory) {
        WonderlandArchiveSystem.entriesForCategory(selectedCategory)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ToyboxScreenHeader(
                title = "Archivo de Wonderland",
                subtitle = "Expedientes y Recuerdos ($unlockedCount/$totalCount descubiertos)",
                onBack = onBack
            )

            // Horizontal Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArchiveFilterChip(
                    label = "Todos ($totalCount)",
                    selected = selectedCategory == null,
                    testTag = "archive_filter_ALL",
                    onClick = { selectedCategory = null }
                )
                ArchiveCategory.entries.forEach { cat ->
                    ArchiveFilterChip(
                        label = cat.displayName,
                        selected = selectedCategory == cat,
                        testTag = "archive_filter_${cat.name}",
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    val unlocked = WonderlandArchiveSystem.isUnlocked(entry, discoveredKeys)
                    val extraUnlocked = WonderlandArchiveSystem.isExtraFragmentUnlocked(entry, discoveredKeys)

                    ToyboxFrameCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive_card_${entry.id}"),
                        borderColor = if (unlocked) TarnishedBrass else AgedWoodLight
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.category.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (unlocked) TarnishedBrass else ParchmentDark
                            )
                            Text(
                                text = if (unlocked) entry.eraOrDate else "Bloqueado",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (unlocked) MutedMoss else CrimsonBright
                            )
                        }
                        Spacer(Modifier.height(4.dp))

                        if (unlocked) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = AgedIvory
                            )
                            Text(
                                text = "Hallado en: ${entry.locationFound}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ParchmentDark
                            )
                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(CutCornerShape(6.dp))
                                    .border(1.2.dp, TarnishedBrassDim, CutCornerShape(6.dp))
                                    .testTag("archive_thumb_${entry.id}")
                                    .clickable { zoomedEntry = entry }
                            ) {
                                Image(
                                    painter = painterResource(id = AIImageArchiveSystem.drawableForArchive(entry)),
                                    contentDescription = entry.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .background(CharcoalBlack.copy(alpha = 0.78f), CutCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Ampliar", tint = TarnishedBrass, modifier = Modifier.size(14.dp))
                                    Text("Ampliar", style = MaterialTheme.typography.labelSmall, color = AgedIvory)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = Parchment)

                            StitchedDivider(Modifier.padding(vertical = 8.dp))

                            if (extraUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AgedWoodDark.copy(alpha = 0.8f), CutCornerShape(4.dp))
                                        .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Nota Completa: ${entry.extraFragmentText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AgedIvory
                                    )
                                }
                            } else {
                                Text(
                                    text = "[Fragmento incompleto — descubre más pistas para revelar esta nota]",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = AmberWarning
                                )
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = AIImageArchiveSystem.visualConsistencySummary(entry.id),
                                style = MaterialTheme.typography.labelSmall,
                                color = ParchmentDark
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(CharcoalBlack, CutCornerShape(4.dp))
                                        .border(1.dp, AgedWoodLight, CutCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = ParchmentDark)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Documento aún sin descubrir",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ParchmentDark
                                    )
                                    Text(
                                        text = "Pista de exploración: ${entry.locationFound}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ParchmentDark.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Zoom Modal
        zoomedEntry?.let { entry ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalBlack.copy(alpha = 0.94f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ToyboxFrameCard(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    borderColor = TarnishedBrass
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.title, style = MaterialTheme.typography.titleLarge, color = TarnishedBrass, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { zoomedEntry = null },
                                modifier = Modifier.size(48.dp).testTag("archive_zoom_close")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar zoom", tint = AgedIvory)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Image(
                            painter = painterResource(id = AIImageArchiveSystem.drawableForArchive(entry)),
                            contentDescription = entry.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp, max = 380.dp)
                                .clip(CutCornerShape(6.dp))
                                .border(1.5.dp, TarnishedBrass, CutCornerShape(6.dp))
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(entry.summary, style = MaterialTheme.typography.bodyLarge, color = Parchment)
                        Spacer(Modifier.height(12.dp))
                        ToyboxPlaqueButton(
                            text = "Volver al Archivo",
                            onClick = { zoomedEntry = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveFilterChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CutCornerShape(4.dp))
            .background(if (selected) CrimsonBlood else AgedWoodMid)
            .border(1.dp, if (selected) TarnishedBrass else AgedWoodLight, CutCornerShape(4.dp))
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AgedIvory else Parchment
        )
    }
}
