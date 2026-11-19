package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.catalog.GameCatalog
import com.example.data.model.BodyZone
import com.example.data.model.CharacterId
import com.example.data.model.CompanionId
import com.example.data.model.EnemyType
import com.example.data.model.InteractableType
import com.example.data.model.Rarity
import com.example.data.model.RepairMaterial
import com.example.data.model.SecretClueType
import com.example.ui.components.PixelArtSprites
import com.example.ui.components.StitchedDivider
import com.example.ui.components.ToyboxFrameCard
import com.example.ui.components.ToyboxScreenHeader
import com.example.ui.theme.*

enum class AlbumTab(
    val title: String,
    val rewardDesc: String
) {
    ENEMIES("Enemigos", "Completa todos los enemigos comunes: +20 Puntos de Desafío y maestría táctica."),
    BOSSES("Jefes", "Registra a los 10 guardianes de Wonderland: +35 Puntos de Desafío y título de Maestro."),
    PARTS("Partes", "Descubre todas las piezas corporales: desbloquea +15% probabilidad de piezas raras."),
    ITEMS("Objetos", "Cataloga las 20 reliquias pasivas: +25 Puntos de Desafío y logro Coleccionista."),
    SYNERGIES("Sinergias", "Despierta las 14 sinergias de etiquetas: +30 Puntos de Desafío."),
    MEMORIES("Recuerdos", "Reúne los 6 recuerdos ocultos: revela la historia completa del fundador."),
    COMPANIONS("Compañeros", "Encuentra a los 8 juguetes acompañantes y descubre sus condiciones."),
    MECHANISMS("Mecanismos", "Interactúa con los 23 mecanismos, objetos móviles y trampas del taller."),
    SECRETS("Secretos", "Descubre anomalías físicas, materiales de reparación y a El Fabricante."),
    CHARACTERS("Personajes", "Desbloquea los 6 muñecos jugables: Medalla de Vitrina Completa.")
}

private data class AlbumCardModel(
    val key: String,
    val title: String,
    val rarity: Rarity?,
    val statsLine: String,
    val description: String,
    val lore: String,
    val accentColor: Color,
    val shapeKind: Int,
    val enemyType: EnemyType? = null,
    val characterId: CharacterId? = null,
    val partInfo: Pair<String, BodyZone>? = null,
    val itemId: String? = null,
    val companionId: CompanionId? = null,
    val interactableType: InteractableType? = null,
    val secretClueType: SecretClueType? = null,
    val repairMaterial: RepairMaterial? = null,
    val isManufacturer: Boolean = false
)

@Composable
fun CollectionAlbumScreen(
    discoveredKeys: Set<String>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(AlbumTab.ENEMIES) }

    val cards: List<AlbumCardModel> = remember(selectedTab) {
        when (selectedTab) {
            AlbumTab.ENEMIES -> EnemyType.entries.filter { !it.isBoss }.map { e ->
                AlbumCardModel(
                    key = "enemy:${e.name}",
                    title = e.displayName,
                    rarity = Rarity.COMMON,
                    statsLine = "Vida base: ${e.maxHp.toInt()} • Velocidad: ${e.speed.toInt()}",
                    description = e.description,
                    lore = "Un juguete que permaneció despierto demasiado tiempo en los pasillos oscuros.",
                    accentColor = CrimsonBright,
                    shapeKind = 0,
                    enemyType = e
                )
            }
            AlbumTab.BOSSES -> EnemyType.entries.filter { it.isBoss }.mapIndexed { idx, b ->
                AlbumCardModel(
                    key = "boss:${b.name}",
                    title = b.displayName,
                    rarity = Rarity.LEGENDARY,
                    statsLine = "Piso ${idx + 1} • Vida base: ${b.maxHp.toInt()} • Vel: ${b.speed.toInt()}",
                    description = b.description,
                    lore = "Gobernante de su piso dentro de la jerarquía de Wonderland Toys.",
                    accentColor = TarnishedBrass,
                    shapeKind = 1,
                    enemyType = b
                )
            }
            AlbumTab.PARTS -> GameCatalog.bodyParts.map { p ->
                AlbumCardModel(
                    key = "part:${p.id}",
                    title = "${p.name} (${p.zone.displayName})",
                    rarity = p.rarity,
                    statsLine = "Durabilidad: ${p.maxDurability} • Etiquetas: ${p.tags.joinToString(", ")}",
                    description = "Intacto: ${p.intactDesc}\nDañado: ${p.damagedDesc}",
                    lore = p.lore,
                    accentColor = p.rarity.color,
                    shapeKind = 2,
                    partInfo = p.id to p.zone
                )
            }
            AlbumTab.ITEMS -> GameCatalog.passiveItems.map { it ->
                AlbumCardModel(
                    key = "item:${it.id}",
                    title = it.name,
                    rarity = it.rarity,
                    statsLine = "Etiquetas: ${it.tags.joinToString(", ")}",
                    description = it.description,
                    lore = it.lore,
                    accentColor = it.rarity.color,
                    shapeKind = 3,
                    itemId = it.id
                )
            }
            AlbumTab.SYNERGIES -> GameCatalog.synergies.map { s ->
                AlbumCardModel(
                    key = "synergy:${s.id}",
                    title = s.name,
                    rarity = Rarity.RARE,
                    statsLine = "Etiquetas requeridas: ${s.requiredTags.joinToString(" + ")}",
                    description = s.description,
                    lore = "Cuando las piezas encajan más allá del diseño original del fabricante.",
                    accentColor = TarnishedBrass,
                    shapeKind = 4
                )
            }
            AlbumTab.MEMORIES -> GameCatalog.memories.map { m ->
                AlbumCardModel(
                    key = "memory:${m.id}",
                    title = m.title,
                    rarity = Rarity.CURSED,
                    statsLine = "Fragmento narrativo permanente",
                    description = "Desbloquea diálogos especiales en eventos y jefes de piso.",
                    lore = m.text,
                    accentColor = CrimsonBright,
                    shapeKind = 5
                )
            }
            AlbumTab.COMPANIONS -> CompanionId.entries.map { c ->
                AlbumCardModel(
                    key = "companion:${c.name}",
                    title = c.displayName,
                    rarity = c.rarity,
                    statsLine = "Interés: ${c.interestDesc}",
                    description = "Condición: ${c.conditionDesc}",
                    lore = c.lore,
                    accentColor = c.rarity.color,
                    shapeKind = 7,
                    companionId = c
                )
            }
            AlbumTab.MECHANISMS -> InteractableType.entries.map { inter ->
                AlbumCardModel(
                    key = "interactable:${inter.name}",
                    title = inter.displayName,
                    rarity = inter.rarity,
                    statsLine = "${inter.category.displayName} • ${if (inter.affectsEnemies && inter.affectsPlayer) "Afecta a todos" else if (inter.affectsEnemies) "Daña enemigos" else "Utilidad"}",
                    description = inter.description,
                    lore = inter.lore,
                    accentColor = inter.rarity.color,
                    shapeKind = 8,
                    interactableType = inter
                )
            }
            AlbumTab.SECRETS -> buildList {
                SecretClueType.entries.forEach { clue ->
                    add(
                        AlbumCardModel(
                            key = "clue:${clue.name}",
                            title = clue.displayName,
                            rarity = Rarity.RARE,
                            statsLine = "Anomalía física en sala",
                            description = clue.hintText,
                            lore = clue.lore,
                            accentColor = TarnishedBrass,
                            shapeKind = 9,
                            secretClueType = clue
                        )
                    )
                }
                add(
                    AlbumCardModel(
                        key = "secret:MANUFACTURER",
                        title = "El Fabricante",
                        rarity = Rarity.LEGENDARY,
                        statsLine = "Entidad oculta • Pactos y trasplantes",
                        description = "Figura alta con máscara que ofrece intercambios corporales, pactos de vida, alquimia de defectos y La Caja Cerrada.",
                        lore = "«Todo juguete roto puede volver a andar, si aceptas el hilo adecuado.»",
                        accentColor = CrimsonBright,
                        shapeKind = 10,
                        isManufacturer = true
                    )
                )
                RepairMaterial.entries.forEach { mat ->
                    add(
                        AlbumCardModel(
                            key = "material:${mat.name}",
                            title = "Material: ${mat.displayName}",
                            rarity = mat.rarity,
                            statsLine = "Insumo de Mesa de Costura",
                            description = "Otorga durabilidad adicional al usarlo en los minijuegos de reparación, refuerzo o modificación.",
                            lore = "Pieza rescatada de los cajones originales de Wonderland Toys.",
                            accentColor = mat.rarity.color,
                            shapeKind = 11,
                            repairMaterial = mat
                        )
                    )
                }
            }
            AlbumTab.CHARACTERS -> GameCatalog.characters.map { c ->
                AlbumCardModel(
                    key = "character:${c.id.name}",
                    title = "${c.name} — ${c.title}",
                    rarity = if (c.unlockedByDefault) Rarity.COMMON else Rarity.RARE,
                    statsLine = "Vida: ${c.maxHearts} ❤ • Daño: ${c.baseDamage.toInt()} • Vel: ${c.baseSpeed.toInt()}",
                    description = "${c.description} Habilidad: ${c.abilityName}.",
                    lore = c.lore,
                    accentColor = Color(c.accentColorHex),
                    shapeKind = 6,
                    characterId = c.id
                )
            }
        }
    }

    val unlockedCount = cards.count { it.key in discoveredKeys }
    val completionRatio = if (cards.isEmpty()) 0f else unlockedCount.toFloat() / cards.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ToyboxScreenHeader(
            title = "Álbum de Colección",
            subtitle = "Fichas Ilustradas de Wonderland Toys",
            onBack = onBack
        )

        com.example.ui.components.ToyboxVisualBanner(
            drawableRes = com.example.R.drawable.img_menu_collection_vitrine,
            badge = "Vitrina de Coleccionista • ${discoveredKeys.size} descubiertos",
            caption = "Piezas, reliquias, juguetes y anomalías catalogadas en la tienda",
            contentDescription = "Vitrina ilustrada de Wonderland Toys",
            heightDp = 96,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(Modifier.height(10.dp))

        // Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AlbumTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(if (selected) CrimsonBlood else AgedWoodDark)
                        .border(1.dp, if (selected) TarnishedBrass else TarnishedBrassDim, CutCornerShape(4.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(tab.title, style = MaterialTheme.typography.labelMedium, color = AgedIvory)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Category Completion & Reward Card
        ToyboxFrameCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            borderColor = if (completionRatio >= 1f) TarnishedBrass else TarnishedBrassDim
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TarnishedBrass, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${selectedTab.title.uppercase()}: $unlockedCount/${cards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedBrass
                    )
                }
                Text(
                    text = if (completionRatio >= 1f) "COMPLETADO" else "EN PROGRESO",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completionRatio >= 1f) MutedMoss else ParchmentDark
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { completionRatio },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = TarnishedBrass,
                trackColor = CharcoalBlack
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Recompensa de colección: ${selectedTab.rewardDesc}",
                style = MaterialTheme.typography.bodyMedium,
                color = Parchment
            )
        }

        Spacer(Modifier.height(10.dp))

        // Illustrated Cards List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cards.forEach { card ->
                val discovered = card.key in discoveredKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (discovered) AgedWoodDark else CharcoalBlack.copy(alpha = 0.7f),
                            CutCornerShape(6.dp)
                        )
                        .border(
                            1.2.dp,
                            if (discovered) card.accentColor.copy(alpha = 0.8f) else AgedWoodLight,
                            CutCornerShape(6.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Illustrated Canvas Thumbnail
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(CharcoalBlack, CutCornerShape(4.dp))
                            .border(1.dp, if (discovered) card.accentColor else AgedWoodLight, CutCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (discovered) {
                            AlbumSpecimenCanvas(
                                card = card,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = "Sin descubrir", tint = ParchmentDark.copy(alpha = 0.45f))
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (discovered) card.title else "Entrada Desconocida",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (discovered) AgedIvory else ParchmentDark,
                                modifier = Modifier.weight(1f)
                            )
                            if (discovered && card.rarity != null) {
                                Text(
                                    text = card.rarity.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = card.rarity.color
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        if (discovered) {
                            Text(card.statsLine, style = MaterialTheme.typography.labelMedium, color = TarnishedBrass)
                            Spacer(Modifier.height(4.dp))
                            Text(card.description, style = MaterialTheme.typography.bodyMedium, color = Parchment)
                            StitchedDivider(Modifier.padding(vertical = 5.dp))
                            Text(
                                text = "«${card.lore}»",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = ParchmentDark
                            )
                        } else {
                            Text(
                                text = "Explora Wonderland Toys, derrota juguetes y combina piezas para registrar esta ficha en tu álbum.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ParchmentDark.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun AlbumSpecimenCanvas(
    card: AlbumCardModel,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val accent = card.accentColor
        val pixelSprite = when {
            card.enemyType != null -> PixelArtSprites.forEnemy(card.enemyType, disguised = false)
            card.characterId != null -> PixelArtSprites.forCharacter(card.characterId)
            card.partInfo != null -> PixelArtSprites.forBodyPart(card.partInfo.first, card.partInfo.second)
            card.itemId != null -> PixelArtSprites.forPassiveItem(card.itemId)
            card.companionId != null -> PixelArtSprites.forCompanion(card.companionId)
            card.interactableType != null -> PixelArtSprites.forInteractable(card.interactableType)
            card.secretClueType != null -> PixelArtSprites.forSecretClue(card.secretClueType)
            card.repairMaterial != null -> PixelArtSprites.forMaterial(card.repairMaterial)
            card.isManufacturer -> PixelArtSprites.manufacturerNpc()
            else -> null
        }
        if (pixelSprite != null) {
            drawImage(
                image = pixelSprite,
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.None
            )
            return@Canvas
        }
        when (card.shapeKind) {
            4 -> { // Synergy twin interlocking rings
                drawCircle(TarnishedBrass, w * 0.24f, Offset(cx - w * 0.12f, cy), style = Stroke(3.5f))
                drawCircle(CrimsonBright, w * 0.24f, Offset(cx + w * 0.12f, cy), style = Stroke(3.5f))
            }
            else -> { // Memory wax seal / scroll
                drawRoundRect(Parchment, Offset(w * 0.18f, h * 0.14f), Size(w * 0.64f, h * 0.72f), CornerRadius(4f))
                drawCircle(CrimsonBlood, w * 0.16f, Offset(cx, cy + h * 0.1f))
            }
        }
    }
}
