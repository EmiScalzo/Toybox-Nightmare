package com.example.data.model

enum class LoreRoomType(
    val displayName: String,
    val badgeColorHex: Long
) {
    MEMORY("Habitación de Recuerdos", 0xFFC89948),
    HISTORICAL("Habitación Histórica", 0xFF8C6D46),
    CHARACTER("Historia de Personaje", 0xFF597354),
    ANOMALOUS("Habitación Anómala", 0xFF7B5EA7),
    DOCUMENTAL("Sala Documental", 0xFFB5A17D),
    FORBIDDEN("Habitación Prohibida", 0xFF942229)
}

enum class PuzzleDifficulty(val displayName: String) {
    EASY("Fácil"),
    NORMAL("Normal"),
    HARD("Difícil"),
    SPECIAL("Especial")
}

enum class LorePuzzleKind(
    val displayName: String,
    val guideHint: String
) {
    CHAIR_AND_BUTTON(
        "La Silla y el Botón",
        "Empuja la silla de madera hasta colocarla encima del interruptor del suelo."
    ),
    STUCK_BOOKSHELF(
        "La Estantería Trabada",
        "Recoge la palanca o rompe el soporte trabado, luego empuja la estantería."
    ),
    CROOKED_PAINTING(
        "El Cuadro Torcido",
        "Gira el cuadro hasta alinear el marco y destrabar el cajón secreto."
    ),
    MUSIC_BOX_REPAIR(
        "La Caja Musical",
        "Encuentra la manivela y el engranaje en la sala y colócalos en la caja."
    ),
    FOUR_CLOCKS(
        "La Puerta de los Relojes",
        "Observa la hora marcada en la pista de la pared y ajusta los 4 relojes."
    ),
    TRAPPED_TOY(
        "El Juguete Atrapado",
        "Encuentra la llave o dispara a la cadena de la reja para liberar al muñeco."
    ),
    FLOODED_FLOOR_PATH(
        "Suelo Electrificado",
        "Empuja cajas sobre los sectores con chispas para llegar seguro a la palanca."
    ),
    THREE_DOLLS_DIRECTION(
        "Las Tres Muñecas",
        "Revisa el dibujo de la pared y gira las 3 muñecas mirando en el orden correcto."
    ),
    HOLLOW_FLOOR(
        "El Suelo Falso",
        "Toca las baldosas sospechosas para oír cuál suena hueca sin caer en la trampa."
    ),
    THREE_TOYS_PEDESTALS(
        "La Puerta de los Juguetes",
        "Coloca el soldado, la muñeca y el conejo en los pedestales según el boceto mural."
    )
}

enum class LorePropKind {
    DRAGGABLE_CHAIR,
    FLOOR_SWITCH,
    STUCK_SHELF,
    TOOL_CROWBAR,
    ROTATABLE_PAINTING,
    BROKEN_MUSIC_BOX,
    TOOL_CRANK,
    TOOL_GEAR,
    ADJUSTABLE_CLOCK,
    TRAPPED_DOLL_CAGE,
    CAGE_KEY,
    DRAGGABLE_CRATE,
    POWER_LEVER,
    ELECTRIFIED_ZONE,
    ROTATABLE_DOLL,
    WALL_HINT_PANEL,
    FLOOR_TILE_HOLLOW,
    FLOOR_TILE_TRAP,
    TOY_FIGURE_SOLDIER,
    TOY_FIGURE_DOLL,
    TOY_FIGURE_BUNNY,
    PEDESTAL_SLOT,
    DESTRUCTIBLE_CRATE,
    SECRET_COMPARTMENT,
    DIARY_DESK
}

data class LoreProp(
    val id: Long,
    val kind: LorePropKind,
    val x: Float,
    val y: Float,
    val radius: Float = 26f,
    val label: String = "",
    val stateInt: Int = 0, // rotation 0..3, clock hour (3,6,9,12), figure type 1..3, or opened 1
    val targetInt: Int = 0,
    val draggable: Boolean = false,
    val destructible: Boolean = false,
    val fragilePhotoInside: Boolean = false,
    val hp: Float = 20f,
    val activated: Boolean = false,
    val solved: Boolean = false,
    val destroyed: Boolean = false,
    val hintText: String = "",
    val figureToken: String? = null
)

data class LoreRoomState(
    val loreRoomType: LoreRoomType,
    val templateId: String,
    val title: String,
    val subtitle: String,
    val difficulty: PuzzleDifficulty,
    val puzzleKind: LorePuzzleKind,
    val hintSummary: String,
    val props: List<LoreProp>,
    val heldTokens: List<String> = emptyList(),
    val solved: Boolean = false,
    val rewardArchiveId: String? = null,
    val rewardClaimed: Boolean = false,
    val fragilePhotoBurned: Boolean = false,
    val hintStepText: String = ""
)

enum class ArchiveCategory(val displayName: String) {
    MEMORIES("Recuerdos"),
    PHOTOS("Fotografías"),
    DRAWINGS("Dibujos"),
    DOCUMENTS("Documentos"),
    CHARACTERS("Personajes"),
    CRAFTING("Fabricación"),
    SECRETS("Secretos"),
    ROOMS("Habitaciones"),
    MAIN_STORY("Historia Principal")
}

data class ArchiveEntryDef(
    val id: String,
    val title: String,
    val category: ArchiveCategory,
    val eraOrDate: String,
    val locationFound: String,
    val summary: String,
    val extraFragmentText: String,
    val requiredAdditionalKey: String? = null,
    val drawableResName: String,
    val visualCharacterTags: List<String> = emptyList(),
    val isAiArtwork: Boolean = true
)

data class PendingArchiveDiscovery(
    val entry: ArchiveEntryDef,
    val isNewlyUnlocked: Boolean,
    val bonusText: String? = null
)

enum class BackpackTier(
    val displayName: String,
    val capacity: Int,
    val rarity: Rarity,
    val specialTrait: String
) {
    STITCHED_POUCH("Bolsillo Cosido", 2, Rarity.COMMON, "Capacidad básica de 2 espacios."),
    KIDS_BACKPACK("Mochila Infantil", 4, Rarity.UNCOMMON, "Equilibrada con 4 espacios de carga."),
    EXPLORER_PACK("Mochila de Explorador", 6, Rarity.RARE, "Amplia mochila de 6 espacios."),
    MANUFACTURER_PACK(
        "Mochila del Fabricante",
        5,
        Rarity.LEGENDARY,
        "Conserva piezas almacenadas sin que pierdan durabilidad."
    ),
    BOTTOMLESS_PACK(
        "Mochila sin Fondo",
        8,
        Rarity.CURSED,
        "8 espacios inmensos, pero al llevar 6+ espacios ocupados reduce -10% la velocidad."
    )
}

enum class BackpackItemKind(
    val displayName: String,
    val slotsRequired: Int,
    val stackable: Boolean,
    val usableInCombat: Boolean,
    val description: String,
    val rarity: Rarity = Rarity.COMMON
) {
    EXTRA_HEART("Corazón de Algodón", 1, true, true, "Recupera +1 corazón completo al instante.", Rarity.UNCOMMON),
    BOMB_PACK("Bomba de Pólvora", 1, true, true, "Coloca una bomba detonante en tu posición actual.", Rarity.COMMON),
    BRASS_KEY("Llave Antigua", 1, true, true, "Entrega una Llave de Latón a tus consumibles activos.", Rarity.UNCOMMON),
    SEWING_KIT_ITEM("Kit de Costura", 1, true, true, "Repara +45 durabilidad en tu pieza más desgastada.", Rarity.COMMON),
    OFFENSIVE_SPRING("Resorte Ofensivo", 1, true, true, "Lanza 8 pernos radiales que dañan a los enemigos.", Rarity.RARE),
    SHIELD_CHARM("Amuleto de Felpa", 1, true, true, "Concede 3.5s de invulnerabilidad protectora.", Rarity.RARE),
    CRAFTING_BUNDLE("Fardo de Telas", 1, true, false, "Desempaca materiales de costura (+Hilo, +Aguja). Solo fuera de combate.", Rarity.COMMON),
    SMALL_PART_SPARE("Pieza de Repuesto", 2, false, false, "Ocupa 2 espacios. Restaura al 100% una zona dañada fuera de combate.", Rarity.RARE),
    LARGE_RELIC_BOX("Caja de Reliquia", 3, false, false, "Ocupa 3 espacios. Al abrirla fuera de combate entrega botones y pistas.", Rarity.LEGENDARY),
    CONSUMABLE_MEMORY("Frasco de Recuerdo", 1, false, true, "Cura +1 corazón y potencia tu ataque (+100% cadencia 6s).", Rarity.CURSED)
}

data class BackpackSlotItem(
    val uid: Long,
    val kind: BackpackItemKind,
    val quantity: Int = 1,
    val partZone: BodyZone? = null,
    val storedDurability: Int = 100
)

data class BackpackRecipe(
    val id: String,
    val tier: BackpackTier,
    val buttonCost: Int,
    val requirements: Map<RepairMaterial, Int>,
    val description: String
)
