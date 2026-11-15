package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CharacterId
import com.example.data.model.ToyCharacterDef
import com.example.ui.theme.*

@Composable
fun ToyboxFrameCard(
    modifier: Modifier = Modifier,
    borderColor: Color = TarnishedBrassDim,
    backgroundColor: Color = AgedWoodDark,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = CutCornerShape(6.dp)
    Surface(
        modifier = modifier
            .border(1.5.dp, borderColor, shape)
            .clip(shape),
        color = backgroundColor,
        shape = shape,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            backgroundColor.copy(alpha = 0.98f),
                            CharcoalBlack.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
fun ToyboxPlaqueButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val shape = CutCornerShape(5.dp)
    val bgBrush = if (primary) {
        Brush.horizontalGradient(listOf(CrimsonBlood, Color(0xFF6B151B)))
    } else {
        Brush.horizontalGradient(listOf(AgedWoodMid, AgedWoodDark))
    }
    val borderCol = if (!enabled) AgedWoodLight.copy(alpha = 0.4f)
    else if (primary) TarnishedBrass else TarnishedBrassDim

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(bgBrush)
            .border(1.5.dp, borderCol, shape)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (enabled) TarnishedBrass else ParchmentDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp
                ),
                color = if (enabled) AgedIvory else ParchmentDark.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StitchedDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val y = size.height / 2f
        var x = 4f
        while (x < size.width - 4f) {
            drawLine(
                color = TarnishedBrassDim.copy(alpha = 0.65f),
                start = Offset(x, y),
                end = Offset((x + 12f).coerceAtMost(size.width - 4f), y),
                strokeWidth = 2f
            )
            x += 20f
        }
    }
}

@Composable
fun ToyboxScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                .background(AgedWoodDark, CutCornerShape(4.dp))
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = AgedIvory
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedBrass
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentDark
                )
            }
        }
    }
}

@Composable
fun DollPortraitCanvas(
    character: ToyCharacterDef,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val accentCol = Color(character.accentColorHex)

        // Wooden vitrine frame glow
        drawRect(
            brush = Brush.radialGradient(
                listOf(accentCol.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.6f
            )
        )

        val bmp = PixelArtSprites.forCharacter(character.id)
        val pad = (w * 0.12f).toInt()
        drawImage(
            image = bmp,
            dstOffset = IntOffset(pad, pad),
            dstSize = IntSize((w - pad * 2).toInt().coerceAtLeast(1), (h - pad * 2).toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None
        )
    }
}

private data class TutorialPage(
    val title: String,
    val badge: String,
    val icon: ImageVector,
    val body: String,
    val tip: String
)

private val tutorialPages = listOf(
    TutorialPage(
        title = "1. Moverse y Esquivar",
        badge = "CONTROLES",
        icon = Icons.Default.Gamepad,
        body = "Usa el JOYSTICK IZQUIERDO en la parte inferior para mover a Patch en cualquier dirección.\n\nPulsa ESQUIVA para deslizarte con un breve instante de invulnerabilidad y atravesar proyectiles enemigos.",
        tip = "Consejo: Si esquivas sin parar, la juguetería aprenderá tu ritmo."
    ),
    TutorialPage(
        title = "2. Apuntado y Disparo Manual",
        badge = "COMBATE",
        icon = Icons.Default.MyLocation,
        body = "Arrastra el JOYSTICK DERECHO hacia donde quieras disparar. Puedes moverte y disparar al mismo tiempo.\n\nLos botones cerca de tu mano derecha activan tu HABILIDAD ESPECIAL y tu CONSUMIBLE (bombas, kits de costura, llaves).",
        tip = "Consejo: Toca el chip pequeño junto al consumible para alternar entre bombas, llaves o kits."
    ),
    TutorialPage(
        title = "3. Desgaste Corporal (6 Zonas)",
        badge = "SISTEMA ÚNICO",
        icon = Icons.Default.AccessibilityNew,
        body = "Tu cuerpo tiene 6 piezas: Cabeza, Ojos, Brazos, Torso, Piernas y Espalda. Cada una empieza INTACTA, pasa a DAÑADA (amarillo) y luego CRÍTICA (rojo).\n\nSi una pieza crítica recibe otro golpe, puede DESPRENDERSE al suelo.",
        tip = "Consejo: ¡Corre sobre la pieza caída antes de que desaparezca o la robe un Duende Ladrón!"
    ),
    TutorialPage(
        title = "4. Reparar o Reemplazar",
        badge = "DECISIONES",
        icon = Icons.Default.Build,
        body = "Usa Kits de Costura, Mesas de Costura (✂) o la habilidad de Patch para reparar piezas.\n\nAl encontrar piezas nuevas en cofres o jefes, elige si equiparlas para ganar nuevas estadísticas o desguazarlas por Botones.",
        tip = "Consejo: En la barra superior puedes ver el estado de las 6 zonas de un vistazo."
    ),
    TutorialPage(
        title = "5. Defectos de Fabricación",
        badge = "RIESGO Y RECOMPENSA",
        icon = Icons.Default.Warning,
        body = "Ningún muñeco sale perfecto. Antes de empezar puedes activar 1, 2 o 3 Defectos (como Miope, Cojo o Costuras Débiles).\n\nMuchos tienen una pequeña ventaja oculta y multiplican tus Medallas y Puntos de Desafío.",
        tip = "Consejo: Si buscas aprender primero, prueba dificultad Suave o Normal."
    ),
    TutorialPage(
        title = "6. Sinergias y Memoria Viva",
        badge = "SECRETOS",
        icon = Icons.Default.AutoAwesome,
        body = "Combinar piezas y objetos con etiquetas afines (ej. Resorte + Precisión, Payaso + Resorte, Música + Ojo Rojo) desbloquea SINERGIAS poderosas.\n\nAdemás, «La Juguetería Recuerda» tus tácticas y tus elecciones en eventos.",
        tip = "Consejo: Explora las salas con signo de tesoro (★), tienda ($) y evento (!), y busca el Jefe (♛) para bajar al siguiente piso."
    )
)

@Composable
fun TutorialOverlayDialog(
    visible: Boolean,
    step: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        val page = tutorialPages[step.coerceIn(0, tutorialPages.lastIndex)]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlack.copy(alpha = 0.86f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ToyboxFrameCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                borderColor = TarnishedBrass
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${page.badge} • ${step + 1}/${tutorialPages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedBrass
                    )
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("tutorial_skip_button")
                    ) {
                        Text("OMITIR", color = ParchmentDark, style = MaterialTheme.typography.labelLarge)
                    }
                }
                StitchedDivider(Modifier.padding(vertical = 6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(CrimsonBlood.copy(alpha = 0.35f), CutCornerShape(6.dp))
                            .border(1.dp, TarnishedBrass, CutCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(page.icon, contentDescription = null, tint = TarnishedBrass, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AgedIvory
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Parchment
                )

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgedWoodMid.copy(alpha = 0.65f), CutCornerShape(4.dp))
                        .border(1.dp, MutedMoss, CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = page.tip,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AgedIvory
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onPrev,
                        enabled = step > 0,
                        border = BorderStroke(1.dp, TarnishedBrassDim),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AgedIvory)
                        Spacer(Modifier.width(6.dp))
                        Text("ATRÁS", color = AgedIvory)
                    }

                    ToyboxPlaqueButton(
                        text = if (step + 1 >= tutorialPages.size) "¡A JUGAR!" else "SIGUIENTE",
                        icon = if (step + 1 >= tutorialPages.size) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onNext,
                        primary = true,
                        testTag = "tutorial_next_button"
                    )
                }
            }
        }
    }
}
