package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementDef
import com.example.engine.GameRunState
import com.example.engine.ToyboxMemorySystem
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxPlaqueButton
import com.example.ui.theme.*

@Composable
fun RunSummaryScreen(
    state: GameRunState,
    newlyUnlocked: List<AchievementDef>,
    onReturnToMenu: () -> Unit
) {
    BackHandler(onBack = onReturnToMenu)

    val defectPts = state.activeDefects.sumOf { it.challengePoints }
    val baseScore = state.roomsClearedCount * 2 +
        state.bossesDefeatedCount * 15 +
        state.enemiesDefeatedCount +
        (defectPts * 5) +
        (if (state.isVictory) 40 else 0)
    val challengeScore = (baseScore * state.difficulty.medalMultiplier).toInt()
    val medals = (challengeScore / 20).coerceAtLeast(if (state.roomsClearedCount > 0) 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.isVictory) "ESCAPE DE WONDERLAND" else "COSTURAS ROTAS",
            style = MaterialTheme.typography.headlineLarge,
            color = if (state.isVictory) TarnishedBrass else CrimsonBright,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (state.isVictory) {
                "${state.character.name} cruzó la última puerta de la juguetería."
            } else {
                "${state.character.name} cayó desarmado sobre las tablas del Piso ${state.floorNumber}."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Parchment,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        // Scene Canvas: Disassembled doll on wooden floor OR triumphant open exit light
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(AgedWoodDark, CutCornerShape(6.dp))
                .border(1.5.dp, if (state.isVictory) TarnishedBrass else CrimsonBlood, CutCornerShape(6.dp))
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val col = Color(state.character.primaryColorHex)
            if (state.isVictory) {
                drawRect(TarnishedBrass.copy(alpha = 0.2f), Offset(cx - 40f, 12f), Size(80f, size.height - 24f))
                drawCircle(col, 24f, Offset(cx, cy - 12f))
                drawRoundRect(col, Offset(cx - 18f, cy + 12f), Size(36f, 36f), CornerRadius(6f))
            } else {
                // Disassembled head, torso, detached arm & cotton puffs
                drawCircle(CottonWhite.copy(alpha = 0.7f), 10f, Offset(cx - 70f, cy + 20f))
                drawCircle(CottonWhite.copy(alpha = 0.6f), 8f, Offset(cx + 62f, cy - 18f))
                rotate(-28f, Offset(cx - 35f, cy)) {
                    drawRoundRect(col, Offset(cx - 55f, cy - 18f), Size(34f, 40f), CornerRadius(6f))
                }
                rotate(42f, Offset(cx + 30f, cy + 16f)) {
                    drawRoundRect(col, Offset(cx + 12f, cy + 8f), Size(42f, 12f), CornerRadius(4f))
                }
                drawCircle(col, 22f, Offset(cx + 22f, cy - 18f))
                drawCircle(CharcoalBlack, 4f, Offset(cx + 15f, cy - 22f))
                drawCircle(Color(state.character.accentColorHex), 4f, Offset(cx + 29f, cy - 22f))
            }
        }

        Spacer(Modifier.height(14.dp))

        ToyboxFrameCard(modifier = Modifier.fillMaxWidth()) {
            Text("INFORME DE LA PARTIDA", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
            StitchedDivider(Modifier.padding(vertical = 6.dp))
            SummaryRow("Personaje", state.character.name)
            SummaryRow("Dificultad", state.difficulty.displayName)
            SummaryRow("Tiempo sobrevivido", "${state.elapsedSeconds.toInt()} seg")
            SummaryRow("Piso alcanzado", "Piso ${state.floorNumber}: ${state.floorTheme.name}")
            SummaryRow("Habitaciones completadas", "${state.roomsClearedCount}")
            SummaryRow("Enemigos derrotados", "${state.enemiesDefeatedCount}")
            SummaryRow("Jefes vencidos", "${state.bossesDefeatedCount}")
            SummaryRow("Objetos encontrados", "${state.passiveItems.size}")
            SummaryRow("Sinergias despertadas", "${state.activeSynergies.size}")
            SummaryRow("Defectos activos", if (state.activeDefects.isEmpty()) "Ninguno" else state.activeDefects.joinToString { it.name })
            SummaryRow("Rasgos de Memoria", "${ToyboxMemorySystem.activeTraits(state.toyboxMemory).size} activados")
            StitchedDivider(Modifier.padding(vertical = 6.dp))
            SummaryRow("Puntuación de Desafío", "+$challengeScore pts", highlight = true)
            SummaryRow("Medallas Obtenidas", "+$medals ★", highlight = true)
        }

        val combinedUnlocks = (newlyUnlocked + state.unlockedAchievementsThisRun).distinctBy { it.id }
        if (combinedUnlocks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ToyboxFrameCard(modifier = Modifier.fillMaxWidth(), borderColor = TarnishedBrass) {
                Text("LOGROS DESBLOQUEADOS", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
                Spacer(Modifier.height(6.dp))
                combinedUnlocks.forEach { ach ->
                    Text("★ ${ach.name} — ${ach.rewardText}", style = MaterialTheme.typography.bodyLarge, color = AgedIvory)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        ToyboxPlaqueButton(
            text = "VOLVER A LA JUGUETERÍA",
            icon = Icons.Default.Home,
            primary = true,
            testTag = "return_to_menu_button",
            modifier = Modifier.fillMaxWidth(),
            onClick = onReturnToMenu
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (highlight) TarnishedBrass else AgedIvory,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
