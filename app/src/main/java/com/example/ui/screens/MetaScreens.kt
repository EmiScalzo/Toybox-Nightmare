package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.RunHistoryEntity
import com.example.engine.GameCreditsSystem
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxPlaqueButton
import com.example.ui.components.ToyboxScreenHeader
import com.example.ui.theme.*

@Composable
fun ChallengesScreen(
    profile: PlayerProfileEntity,
    recentRuns: List<RunHistoryEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Caja de Desafíos",
            subtitle = "Medallas, Riesgos y Registro de Partidas",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToyboxFrameCard(modifier = Modifier.fillMaxWidth(), borderColor = TarnishedBrass) {
                Text("ESTADÍSTICAS PERMANENTES", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
                StitchedDivider(Modifier.padding(vertical = 6.dp))
                Text("Medallas Totales: ${profile.medals} ★", style = MaterialTheme.typography.bodyLarge, color = AgedIvory)
                Text("Puntos de Desafío: ${profile.challengePoints} pts", style = MaterialTheme.typography.bodyLarge, color = AgedIvory)
                Text("Partidas Jugadas: ${profile.totalRuns} • Victorias: ${profile.totalWins}", style = MaterialTheme.typography.bodyMedium, color = Parchment)
                Text("Juguetes Derrotados: ${profile.totalKills} • Salas: ${profile.totalRoomsCleared}", style = MaterialTheme.typography.bodyMedium, color = Parchment)
            }

            ToyboxFrameCard(modifier = Modifier.fillMaxWidth()) {
                Text("DESAFÍOS DE FÁBRICA", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
                Spacer(Modifier.height(6.dp))
                ChallengeIdeaItem("Costuras al Límite", "Supera un piso sin usar Kits ni Mesas de Costura.")
                ChallengeIdeaItem("Pesadilla de 3 Defectos", "Activa 3 defectos de fabricación y derrota al Vendedor.")
                ChallengeIdeaItem("Manos Desnudas", "Vence a un jefe sin usar bombas ni consumibles en su sala.")
            }

            ToyboxFrameCard(modifier = Modifier.fillMaxWidth()) {
                Text("HISTORIAL RECIENTE", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
                Spacer(Modifier.height(6.dp))
                if (recentRuns.isEmpty()) {
                    Text("Aún no hay partidas registradas en la caja.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                } else {
                    recentRuns.forEach { run ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${run.characterId} (${run.difficulty}) — ${if (run.victory) "VICTORIA" else "CAÍDA"}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (run.victory) TarnishedBrass else CrimsonBright
                                )
                                Text(
                                    text = "Piso ${run.floorReached} • ${run.roomsCleared} salas • ${run.enemiesDefeated} bajas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParchmentDark
                                )
                            }
                            Text("+${run.challengeScore} pts", style = MaterialTheme.typography.labelLarge, color = AgedIvory)
                        }
                        StitchedDivider()
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ChallengeIdeaItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("◆ $title", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
    }
}

@Composable
fun OptionsScreen(
    profile: PlayerProfileEntity,
    onUpdateSettings: (Boolean, Boolean, Boolean, Float) -> Unit,
    onOpenTutorial: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var sound by remember(profile.soundEnabled) { mutableStateOf(profile.soundEnabled) }
    var music by remember(profile.musicEnabled) { mutableStateOf(profile.musicEnabled) }
    var shake by remember(profile.screenShakeEnabled) { mutableStateOf(profile.screenShakeEnabled) }
    var vignette by remember(profile.darkVignetteIntensity) { mutableStateOf(profile.darkVignetteIntensity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Opciones",
            subtitle = "La Radio Antigua del Taller",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToyboxFrameCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Efectos de Sonido", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                        Text("Caja musical, disparos de engranaje y costuras.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                    }
                    Switch(
                        checked = sound,
                        onCheckedChange = {
                            sound = it
                            onUpdateSettings(sound, music, shake, vignette)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TarnishedBrass, checkedTrackColor = CrimsonBlood)
                    )
                }
                StitchedDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Música OST Dinámica", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                        Text("Se adapta a exploración, combate, jefes y peligro.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                    }
                    Switch(
                        checked = music,
                        onCheckedChange = {
                            music = it
                            onUpdateSettings(sound, music, shake, vignette)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TarnishedBrass, checkedTrackColor = CrimsonBlood)
                    )
                }
                StitchedDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Temblor de Cámara", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                        Text("Sacudida de pantalla en impactos y explosiones.", style = MaterialTheme.typography.bodyMedium, color = ParchmentDark)
                    }
                    Switch(
                        checked = shake,
                        onCheckedChange = {
                            shake = it
                            onUpdateSettings(sound, music, shake, vignette)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = TarnishedBrass, checkedTrackColor = CrimsonBlood)
                    )
                }
                StitchedDivider(Modifier.padding(vertical = 8.dp))
                Text("Oscuridad de Linterna (${(vignette * 100).toInt()}%)", style = MaterialTheme.typography.titleMedium, color = AgedIvory)
                Slider(
                    value = vignette,
                    onValueChange = { vignette = it },
                    onValueChangeFinished = { onUpdateSettings(sound, music, shake, vignette) },
                    valueRange = 0.4f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = TarnishedBrass, activeTrackColor = CrimsonBlood)
                )
            }

            ToyboxPlaqueButton(
                text = "VER TUTORIAL INTERACTIVO",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenTutorial
            )
        }
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Placa del Taller",
            subtitle = "Historia de Wonderland Toys",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToyboxFrameCard(modifier = Modifier.fillMaxWidth(), borderColor = TarnishedBrass) {
                Text("WONDERLAND TOYS — FUNDADA EN 1954", style = MaterialTheme.typography.titleLarge, color = TarnishedBrass)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Era conocida por fabricar juguetes únicos, muñecos artesanales y mecanismos de cuerda que parecían tener vida propia.\n\nUn día cerró sus puertas sin explicación. Nadie supo qué ocurrió con el dueño, pero en la penumbra de las vitrinas los juguetes nunca dejaron de jugar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Parchment
                )
                StitchedDivider(Modifier.padding(vertical = 10.dp))
                Text(
                    "Pilares de la Juguetería:\n1. Reconstrucción y desgaste corporal en 6 zonas.\n2. 24 Defectos de fabricación, mochilas y exploración narrativa.\n3. La juguetería recuerda tus decisiones y pistas resueltas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgedIvory
                )
            }

            // Bottom Formal Credits Plaque
            ToyboxFrameCard(
                modifier = Modifier.fillMaxWidth().testTag("credits_plaque_card"),
                borderColor = CrimsonBright
            ) {
                Text(
                    text = GameCreditsSystem.GAME_TITLE,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TarnishedBrass
                )
                StitchedDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    text = "VERSIÓN: ${GameCreditsSystem.VERSION}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AgedIvory
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ÚLTIMA ACTUALIZACIÓN: ${GameCreditsSystem.LAST_UPDATED}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AgedIvory
                )
                Spacer(Modifier.height(6.dp))
                val authorAnnotated = buildAnnotatedString {
                    withStyle(SpanStyle(color = Parchment)) {
                        append("REALIZADO POR: ")
                    }
                    withStyle(SpanStyle(color = AgedIvory, fontWeight = FontWeight.SemiBold)) {
                        append(GameCreditsSystem.AUTHOR_PREFIX)
                    }
                    withStyle(SpanStyle(color = TarnishedBrass, fontWeight = FontWeight.ExtraBold)) {
                        append(GameCreditsSystem.AUTHOR_HIGHLIGHT)
                    }
                    withStyle(SpanStyle(color = AgedIvory, fontWeight = FontWeight.SemiBold)) {
                        append(GameCreditsSystem.AUTHOR_SUFFIX)
                    }
                }
                Text(
                    text = authorAnnotated,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("credits_author_text")
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
