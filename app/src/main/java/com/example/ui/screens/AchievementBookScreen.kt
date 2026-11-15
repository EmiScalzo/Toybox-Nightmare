package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.GameCatalog
import com.example.data.model.AchievementCategory
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxScreenHeader
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AchievementBookScreen(
    unlockedMap: Map<String, Long>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) GameCatalog.achievements
        else GameCatalog.achievements.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Libro de Logros",
            subtitle = "Registro de Wonderland Toys • ${unlockedMap.size}/${GameCatalog.achievements.size} sellados",
            onBack = onBack
        )

        // Aged Leather Ledger Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .padding(horizontal = 14.dp)
                .clip(CutCornerShape(6.dp))
                .border(1.5.dp, TarnishedBrass, CutCornerShape(6.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_achievement_book),
                contentDescription = "Libro antiguo de Wonderland Toys",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.55f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CharcoalBlack.copy(alpha = 0.45f))
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "«Cada costura rota y cada puerta abierta queda anotada con cera y tinta en este libro.»",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AgedIvory
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Category Filter Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CategoryTabChip("Todos", selectedCategory == null) { selectedCategory = null }
            AchievementCategory.entries.forEach { cat ->
                CategoryTabChip(cat.displayName, selectedCategory == cat) { selectedCategory = cat }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Ledger Pages
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtered.forEach { ach ->
                val unlockTime = unlockedMap[ach.id]
                val isUnlocked = unlockTime != null
                val hideDetails = ach.hidden && !isUnlocked

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isUnlocked) AgedWoodMid else AgedWoodDark.copy(alpha = 0.8f),
                            CutCornerShape(5.dp)
                        )
                        .border(
                            1.2.dp,
                            if (isUnlocked) TarnishedBrass else AgedWoodLight,
                            CutCornerShape(5.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wax Seal Stamp
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) CrimsonBlood else CharcoalBlack)
                            .border(1.5.dp, if (isUnlocked) TarnishedBrass else AgedWoodLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                            contentDescription = if (isUnlocked) "Sellado" else "Oculto",
                            tint = if (isUnlocked) TarnishedBrass else ParchmentDark.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (hideDetails) "Logro Secreto" else ach.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isUnlocked) AgedIvory else ParchmentDark
                            )
                            Text(
                                text = ach.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = TarnishedBrass
                            )
                        }
                        Text(
                            text = if (hideDetails) "Descubre este secreto jugando en Wonderland Toys." else ach.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Parchment
                        )
                        StitchedDivider(Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recompensa: ${ach.rewardText}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isUnlocked) MutedMoss else ParchmentDark
                            )
                            if (unlockTime != null) {
                                Text(
                                    text = dateFormat.format(Date(unlockTime)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TarnishedBrass
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CategoryTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(if (selected) CrimsonBlood else AgedWoodDark)
            .border(1.dp, if (selected) TarnishedBrass else TarnishedBrassDim, CutCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AgedIvory)
    }
}
