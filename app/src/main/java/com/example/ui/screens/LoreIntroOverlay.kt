package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxPlaqueButton
import com.example.ui.theme.*

private data class LoreSlide(
    val chapter: String,
    val title: String,
    val drawableRes: Int,
    val body: String,
    val quote: String
)

private val loreSlides = listOf(
    LoreSlide(
        chapter = "PRÓLOGO I • WONDERLAND TOYS",
        title = "El Último Turno de Wonderland Toys",
        drawableRes = R.drawable.img_menu_toyshop,
        body = "Cuando la antigua juguetería cerró sus persianas para siempre, las luces del sótano quedaron encendidas. " +
            "Entre vitrinas cubiertas de polvo y cajas apiladas hasta el techo, los juguetes sin dueño aprendieron a moverse solos al caer la noche.",
        quote = "«Nadie volvió a girar las llaves desde afuera… pero adentro la cuerda nunca dejó de sonar.»"
    ),
    LoreSlide(
        chapter = "PRÓLOGO II • EL TALLER",
        title = "La Mesa de Costura y los Defectos",
        drawableRes = R.drawable.img_lore_workshop,
        body = "Ningún muñeco salió perfecto de aquella línea de montaje. Algunos fueron descartados por un ojo de botón, una pierna rígida o costuras débiles. " +
            "Aquí cada golpe desgasta tu cuerpo zona por zona y obliga a coser, reforzar o modificar piezas en las mesas del taller.",
        quote = "«La perfección era demasiado cara. El resto acabó en el cajón de saldos.»"
    ),
    LoreSlide(
        chapter = "PRÓLOGO III • SUPERVIVENCIA",
        title = "La Caja de Madera y los Compañeros",
        drawableRes = R.drawable.img_patch_portrait,
        body = "Patch despierta dentro de su caja de embalaje con un solo objetivo: cruzar las salas cambiantes hasta la salida. " +
            "En el camino podrás usar trenes, estanterías y resortes del escenario, salvar cajas que lloran y pactar con juguetes que recuerdan cómo los trataste.",
        quote = "«Si abandonas a quien te siguió en la oscuridad, volverás a escucharlo tras la siguiente puerta.»"
    ),
    LoreSlide(
        chapter = "PRÓLOGO IV • EL MISTERIO",
        title = "El Fabricante en las Sombras",
        drawableRes = R.drawable.img_lore_manufacturer,
        body = "Más allá de las paredes con grietas y los relojes torcidos aguarda una figura alta de máscara pálida: El Fabricante. " +
            "Ofrece trasplantes, cajas cerradas y trueques prohibidos capaces de cambiar las reglas de la tienda… siempre por un precio.",
        quote = "«Todo juguete roto puede volver a andar, si aceptas el hilo adecuado.»"
    )
)

@Composable
fun LoreIntroOverlay(
    visible: Boolean,
    step: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = visible) {
            if (step > 0) onPrev() else onSkip()
        }
        val slide = loreSlides[step.coerceIn(0, loreSlides.lastIndex)]
        val isLast = step + 1 >= loreSlides.size

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CharcoalBlack.copy(alpha = 0.94f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            ToyboxFrameCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                borderColor = TarnishedBrass
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${slide.chapter} (${step + 1}/${loreSlides.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedBrass,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("lore_skip_button")
                    ) {
                        Text("OMITIR", color = Parchment, style = MaterialTheme.typography.labelLarge)
                    }
                }

                StitchedDivider(Modifier.padding(vertical = 4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Image(
                        painter = painterResource(id = slide.drawableRes),
                        contentDescription = slide.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(CutCornerShape(6.dp))
                            .border(1.5.dp, TarnishedBrassDim, CutCornerShape(6.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AgedIvory
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = slide.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Parchment
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AgedWoodMid.copy(alpha = 0.55f), CutCornerShape(4.dp))
                            .border(1.dp, TarnishedBrassDim, CutCornerShape(4.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = slide.quote,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = TarnishedBrass
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (step > 0) onPrev() else onSkip() },
                        border = BorderStroke(1.dp, TarnishedBrassDim),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("lore_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (step > 0) "Atrás" else "Omitir",
                            tint = AgedIvory
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (step > 0) "ATRÁS" else "CERRAR", color = AgedIvory)
                    }

                    ToyboxPlaqueButton(
                        text = if (isLast) "ENTRAR A LA TIENDA" else "SIGUIENTE",
                        icon = if (isLast) Icons.Default.DoorFront else Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onNext,
                        primary = true,
                        testTag = "lore_next_button"
                    )
                }
            }
        }
    }
}
