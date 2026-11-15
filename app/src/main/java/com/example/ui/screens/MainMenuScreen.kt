package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.PlayerProfileEntity
import com.example.ui.AppRoute
import com.example.ui.components.StitchedDivider
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    profile: PlayerProfileEntity,
    unlockedAchievementCount: Int,
    totalAchievements: Int,
    onNavigate: (AppRoute) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenLore: () -> Unit = {}
) {
    val flickerAnim = rememberInfiniteTransition(label = "lantern")
    val signAlpha by flickerAnim.animateFloat(
        initialValue = 0.84f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signFlicker"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        // Atmospheric Victorian Toyshop Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_menu_toyshop),
            contentDescription = "Interior de Wonderland Toys",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f)
        )

        // Moody vertical gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CharcoalBlack.copy(alpha = 0.78f),
                            CharcoalBlack.copy(alpha = 0.35f),
                            CharcoalBlack.copy(alpha = 0.88f),
                            CharcoalBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hanging Vintage Sign: Wonderland Toys
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(signAlpha)
                    .border(2.dp, TarnishedBrass, CutCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(listOf(AgedWoodMid, AgedWoodDark)),
                        CutCornerShape(6.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "WONDERLAND TOYS",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                        color = TarnishedBrass
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "TOYBOX NIGHTMARE",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 27.sp),
                        color = AgedIvory,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ningún juguete fue fabricado perfectamente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentDark
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Centerpiece: Patch in Wooden Crate + Quick Stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, TarnishedBrassDim, CutCornerShape(8.dp))
                    .background(AgedWoodDark.copy(alpha = 0.84f), CutCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_patch_portrait),
                        contentDescription = "Patch en su caja de madera",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CutCornerShape(6.dp))
                            .border(1.5.dp, TarnishedBrass, CutCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LA CAJA DE MADERA",
                            style = MaterialTheme.typography.labelLarge,
                            color = TarnishedBrass
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Patch despierta entre estantes polvorientos. La puerta de salida aguarda al final de la juguetería.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Parchment
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill("Medallas", "${profile.medals}")
                            StatPill("Logros", "$unlockedAchievementCount/$totalAchievements")
                            StatPill("Piso máx.", "${profile.highestFloor}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Primary Action: Puerta de Salida -> JUGAR
            DiegeticMenuObject(
                title = "JUGAR",
                objectLabel = "Puerta de Salida",
                subtitle = "Configurar muñeco, dificultad y defectos de fábrica",
                icon = Icons.Default.DoorFront,
                accentColor = CrimsonBright,
                isPrimary = true,
                testTag = "menu_play_button",
                onClick = { onNavigate(AppRoute.RUN_SETUP) }
            )

            Spacer(Modifier.height(10.dp))

            // Diegetic Grid of Physical Toyshop Objects
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiegeticMenuTile(
                    title = "PERSONAJES",
                    objectLabel = "Estantería de Muñecos",
                    icon = Icons.Default.Face,
                    accent = TarnishedBrass,
                    testTag = "menu_characters_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppRoute.RUN_SETUP) }
                )
                DiegeticMenuTile(
                    title = "COLECCIÓN",
                    objectLabel = "Vitrina Ilustrada",
                    icon = Icons.Default.Inventory2,
                    accent = MutedMoss,
                    testTag = "menu_collection_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppRoute.COLLECTION_ALBUM) }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiegeticMenuTile(
                    title = "LOGROS",
                    objectLabel = "Libro Antiguo",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = TarnishedBrass,
                    testTag = "menu_achievements_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppRoute.ACHIEVEMENT_BOOK) }
                )
                DiegeticMenuTile(
                    title = "DESAFÍOS",
                    objectLabel = "Caja Cerrada",
                    icon = Icons.Default.Lock,
                    accent = CrimsonBright,
                    testTag = "menu_challenges_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppRoute.CHALLENGES) }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiegeticMenuTile(
                    title = "OPCIONES",
                    objectLabel = "Radio Antigua",
                    icon = Icons.Default.Radio,
                    accent = SlateBlue,
                    testTag = "menu_options_button",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppRoute.OPTIONS) }
                )
                DiegeticMenuTile(
                    title = "TUTORIAL",
                    objectLabel = "Guía de Costura",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    accent = MutedMoss,
                    testTag = "menu_tutorial_button",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTutorial
                )
            }

            Spacer(Modifier.height(10.dp))
            StitchedDivider()
            Spacer(Modifier.height(8.dp))

            // Replay Lore Intro
            DiegeticMenuObject(
                title = "VER HISTORIA",
                objectLabel = "Diario de Wonderland",
                subtitle = "Revive el prólogo ilustrado de Patch y El Fabricante",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = TarnishedBrass,
                isPrimary = false,
                testTag = "menu_lore_button",
                onClick = onOpenLore
            )
            Spacer(Modifier.height(10.dp))

            // Metal Plaque: Credits
            DiegeticMenuObject(
                title = "CRÉDITOS Y ARCHIVOS",
                objectLabel = "Placa Metálica del Taller",
                subtitle = "Archivos de Wonderland Toys • Versión Vertical Android",
                icon = Icons.Default.Badge,
                accentColor = ParchmentDark,
                isPrimary = false,
                testTag = "menu_credits_button",
                onClick = { onNavigate(AppRoute.CREDITS) }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(CharcoalBlack.copy(alpha = 0.7f), CutCornerShape(3.dp))
            .border(1.dp, TarnishedBrassDim.copy(alpha = 0.6f), CutCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ParchmentDark)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TarnishedBrass)
    }
}

@Composable
private fun DiegeticMenuObject(
    title: String,
    objectLabel: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isPrimary: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(6.dp)
    val bg = if (isPrimary) {
        Brush.horizontalGradient(listOf(CrimsonBlood.copy(alpha = 0.92f), AgedWoodDark))
    } else {
        Brush.horizontalGradient(listOf(AgedWoodMid.copy(alpha = 0.88f), AgedWoodDark.copy(alpha = 0.94f)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, if (isPrimary) TarnishedBrass else TarnishedBrassDim, shape)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(CharcoalBlack.copy(alpha = 0.55f), CutCornerShape(4.dp))
                .border(1.dp, accentColor, CutCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = objectLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TarnishedBrass
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AgedIvory
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Parchment
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TarnishedBrass
        )
    }
}

@Composable
private fun DiegeticMenuTile(
    title: String,
    objectLabel: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(6.dp)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 86.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(AgedWoodMid.copy(alpha = 0.9f), AgedWoodDark.copy(alpha = 0.95f)))
            )
            .border(1.2.dp, TarnishedBrassDim, shape)
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            Text(
                text = objectLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentDark,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AgedIvory
        )
    }
}
