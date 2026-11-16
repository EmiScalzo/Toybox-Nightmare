package com.example.data.catalog

import com.example.data.model.*

data class LoreRoomTemplate(
    val id: String,
    val loreRoomType: LoreRoomType,
    val title: String,
    val subtitle: String,
    val difficulty: PuzzleDifficulty,
    val puzzleKind: LorePuzzleKind,
    val rewardArchiveId: String,
    val hintSummary: String
)

object AmpliacionCatalog {

    val archiveEntries: List<ArchiveEntryDef> = listOf(
        // FOTOGRAFÍAS
        ArchiveEntryDef(
            id = "arch_first_shop",
            title = "Fotografía: La Primera Juguetería",
            category = ArchiveCategory.PHOTOS,
            eraOrDate = "14 de Octubre, 1954",
            locationFound = "Sala de Exhibición / Habitación Histórica",
            summary = "Fotografía sepia del día de inauguración de Wonderland Toys. Estanterías rebosantes de osos, soldados de latón y el mostrador principal reluciente.",
            extraFragmentText = "En el margen derecho, junto al reloj de pie, un empleado con guantes oscuros sostiene un carrete de hilo dorado.",
            requiredAdditionalKey = "archive:arch_workshop_photo",
            drawableResName = "img_archive_first_shop",
            visualCharacterTags = listOf("wonderland_exterior", "founder", "vintage_sepia")
        ),
        ArchiveEntryDef(
            id = "arch_workshop_photo",
            title = "Fotografía: El Taller Nocturno",
            category = ArchiveCategory.PHOTOS,
            eraOrDate = "Invierno de 1959",
            locationFound = "Taller de Reparaciones / Puzle de Estantería",
            summary = "Siete artesanos trabajan bajo lámparas bajas cosiendo extremidades de felpa y calibrando resortes de bronce.",
            extraFragmentText = "Uno de los artesanos al fondo lleva una máscara pálida muy similar a la silueta de El Fabricante.",
            requiredAdditionalKey = "secret:MANUFACTURER",
            drawableResName = "img_lore_workshop",
            visualCharacterTags = listOf("workshop", "manufacturer_hint", "employees")
        ),
        ArchiveEntryDef(
            id = "arch_shop_corridor",
            title = "Fotografía: Pasillo Central Cerrado",
            category = ArchiveCategory.PHOTOS,
            eraOrDate = "Noviembre de 1962",
            locationFound = "Depósito / Puzle de Silla y Botón",
            summary = "El pasillo principal fotografiado la mañana posterior al cierre indefinido. Las vitrinas quedaron intactas.",
            extraFragmentText = "Ningún cliente volvió a cruzar la puerta, pero las cerraduras interiores muestran marcas de uñas de felpa.",
            requiredAdditionalKey = "archive:arch_last_night",
            drawableResName = "img_menu_toyshop",
            visualCharacterTags = listOf("wonderland_interior", "corridor")
        ),

        // DIBUJOS
        ArchiveEntryDef(
            id = "arch_best_friend",
            title = "Dibujo: Mi Mejor Amigo",
            category = ArchiveCategory.DRAWINGS,
            eraOrDate = "Sin fecha (Trazo infantil)",
            locationFound = "Habitación de Cumpleaños / Guardería",
            summary = "Dibujo a crayón rojo sobre papel manchado de té: un niño sonriendo de la mano de un oso remendado con un botón en el ojo.",
            extraFragmentText = "En el reverso está escrito con letra temblorosa: «Patch nunca se queda dormido cuando llueve».",
            requiredAdditionalKey = "character:PATCH",
            drawableResName = "img_archive_best_friend",
            visualCharacterTags = listOf("patch", "child", "crayon_drawing")
        ),
        ArchiveEntryDef(
            id = "arch_bear_portrait",
            title = "Retrato: El Osito Antes del Cierre",
            category = ArchiveCategory.CHARACTERS,
            eraOrDate = "Edición Especial 1956",
            locationFound = "Sala de Personaje / Puzle del Juguete Atrapado",
            summary = "Ilustración oficial del osito de felpa antes de deteriorarse: moño carmesí limpio, ojos de vidrio brillante y costuras doradas.",
            extraFragmentText = "Fue retirado del catálogo cuando los compradores reportaron que cambiaba de postura durante la noche.",
            requiredAdditionalKey = "companion:FORGOTTEN_BEAR",
            drawableResName = "img_archive_bear_portrait",
            visualCharacterTags = listOf("plush_bear", "forgotten_bear", "before_decay")
        ),

        // RECUERDOS
        ArchiveEntryDef(
            id = "arch_last_night",
            title = "Recuerdo: La Última Noche",
            category = ArchiveCategory.MEMORIES,
            eraOrDate = "Noche de Tormenta, 1962",
            locationFound = "Habitación de Recuerdos / Puzle de Caja Musical",
            summary = "Un dormitorio infantil iluminado por relámpagos. La caja de juguetes quedó entreabierta mientras el reloj marcaba las 11:58.",
            extraFragmentText = "A las 11:59 todos los muñecos del escaparate escucharon por primera vez el tic-tac de La Madre.",
            requiredAdditionalKey = "memory:mem_clock",
            drawableResName = "img_archive_last_night",
            visualCharacterTags = listOf("storm", "wooden_toybox", "bedroom")
        ),
        ArchiveEntryDef(
            id = "arch_birthday_memory",
            title = "Recuerdo: Cumpleaños Detenido",
            category = ArchiveCategory.MEMORIES,
            eraOrDate = "1960",
            locationFound = "Habitación de Cumpleaños Abandonada",
            summary = "Una torta cubierta de polvo, globos desinflados y cuatro juguetes sentados frente a un regalo que jamás fue abierto.",
            extraFragmentText = "Dentro del regalo solo había un botón de latón con las iniciales E.S. grabadas en el borde.",
            requiredAdditionalKey = "archive:arch_best_friend",
            drawableResName = "img_menu_toyshop",
            visualCharacterTags = listOf("birthday", "abandoned_table", "patch")
        ),

        // PERSONAJES
        ArchiveEntryDef(
            id = "arch_patch_origin",
            title = "Ficha de Patch: Modelo 01",
            category = ArchiveCategory.CHARACTERS,
            eraOrDate = "1955",
            locationFound = "Caja Inicial / Sala de Personaje",
            summary = "Prototipo de muñeco de trapo diseñado para soportar reparaciones repetidas con hilo común y botones reciclados.",
            extraFragmentText = "Su ojo de botón izquierdo fue reemplazado tres veces por el propio Fabricante tras perder el relleno original.",
            requiredAdditionalKey = "character:PATCH",
            drawableResName = "img_patch_portrait",
            visualCharacterTags = listOf("patch", "prototype", "ragdoll")
        ),
        ArchiveEntryDef(
            id = "arch_manufacturer_file",
            title = "Expediente: El Fabricante",
            category = ArchiveCategory.SECRETS,
            eraOrDate = "Clasificado",
            locationFound = "Habitación Prohibida / Pacto del Taller",
            summary = "Figura alta con máscara de porcelana lisa. Afirma que ningún juguete nace completo y que cada pacto corrige un error.",
            extraFragmentText = "Los registros contables demuestran que seguía pagando envíos de tela negra diez años después del cierre.",
            requiredAdditionalKey = "archive:arch_forbidden_blueprint",
            drawableResName = "img_lore_manufacturer",
            visualCharacterTags = listOf("manufacturer", "mask", "shadow")
        ),

        // DOCUMENTOS & HISTORIA PRINCIPAL
        ArchiveEntryDef(
            id = "arch_ledger_1954",
            title = "Libro de Turnos de 1954",
            category = ArchiveCategory.DOCUMENTS,
            eraOrDate = "1954 - 1962",
            locationFound = "Oficina Administrativa / Sala Documental",
            summary = "Registro de empleados de Wonderland Toys. En las últimas páginas, los nombres fueron tachados y reemplazados por códigos de piezas.",
            extraFragmentText = "«Turno nocturno: no abrir el depósito B después de medianoche sin llevar una campana.»",
            requiredAdditionalKey = "archive:arch_first_shop",
            drawableResName = "img_achievement_book",
            visualCharacterTags = listOf("document", "ledger", "employees")
        ),
        ArchiveEntryDef(
            id = "arch_forbidden_blueprint",
            title = "Plano Prohibido: Anatomía de Hilo",
            category = ArchiveCategory.MAIN_STORY,
            eraOrDate = "1961 (Confidencial)",
            locationFound = "Habitación Prohibida / Puzle de las Tres Muñecas",
            summary = "Esquema anatómico de engranajes, tendones de alambre y bolsas de recuerdo cosidas al torso de los juguetes conscientes.",
            extraFragmentText = "La anotación inferior dice: «Si un recuerdo pesa demasiado, la mochila del Fabricante evitará que pudra la costura».",
            requiredAdditionalKey = "archive:arch_manufacturer_file",
            drawableResName = "img_archive_forbidden_blueprint",
            visualCharacterTags = listOf("blueprint", "clockwork", "manufacturer")
        ),

        // FABRICACIÓN (RECETAS & MOCHILAS)
        ArchiveEntryDef(
            id = "arch_recipe_kids_pack",
            title = "Patrón: Mochila Infantil",
            category = ArchiveCategory.CRAFTING,
            eraOrDate = "Manual del Taller p. 12",
            locationFound = "Mesa de Costura / Sala Histórica",
            summary = "Patrón sencillo de 4 espacios: requiere Hilo, Agujas y Tornillos para reforzar las correas.",
            extraFragmentText = "Consejo del sastre: añade pegamento de taller en las costuras laterales para evitar que se desgarre.",
            drawableResName = "img_lore_workshop",
            visualCharacterTags = listOf("crafting", "backpack", "sewing")
        ),
        ArchiveEntryDef(
            id = "arch_recipe_explorer_pack",
            title = "Patrón: Mochila de Explorador",
            category = ArchiveCategory.CRAFTING,
            eraOrDate = "Manual del Taller p. 28",
            locationFound = "Depósito Antiguo / Puzle de Suelo Electrificado",
            summary = "Mochila reforzada de 6 espacios pensada para transportar piezas de repuesto e insumos por varios pisos.",
            extraFragmentText = "Incluye anillos metálicos hechos con engranajes descartados del taller.",
            drawableResName = "img_lore_workshop",
            visualCharacterTags = listOf("crafting", "explorer_pack")
        ),
        ArchiveEntryDef(
            id = "arch_recipe_manufacturer_pack",
            title = "Patrón Prohibido: Mochila del Fabricante",
            category = ArchiveCategory.CRAFTING,
            eraOrDate = "Plano Reservado",
            locationFound = "Sala Prohibida / Acuerdo con El Fabricante",
            summary = "Bolsa de tela oscura con 5 espacios sellados que detiene el deterioro de piezas guardadas.",
            extraFragmentText = "Cosida con hilo encerado, porcelana fina y un fragmento de recuerdo.",
            drawableResName = "img_archive_forbidden_blueprint",
            visualCharacterTags = listOf("manufacturer_pack", "forbidden_craft")
        ),

        // HABITACIONES
        ArchiveEntryDef(
            id = "arch_room_birthday",
            title = "Sala: El Cumpleaños Olvidado",
            category = ArchiveCategory.ROOMS,
            eraOrDate = "Piso 1 - 3",
            locationFound = "Habitación de Lore (Recuerdos)",
            summary = "Mesa puesta para una fiesta que nunca comenzó. El silencio pesa más que los enemigos.",
            extraFragmentText = "Quien mueve las sillas descubre que el suelo guarda interruptores bajo el polvo.",
            drawableResName = "img_archive_last_night",
            visualCharacterTags = listOf("lore_room", "memory_room")
        ),
        ArchiveEntryDef(
            id = "arch_room_anomalous",
            title = "Sala: El Pasillo de los Cuatro Relojes",
            category = ArchiveCategory.ROOMS,
            eraOrDate = "Fuera del Tiempo",
            locationFound = "Habitación Anómala",
            summary = "Una habitación cuyas paredes vibran con el compás de cuatro relojes desincronizados.",
            extraFragmentText = "Alinear sus agujas abre compartimentos sellados desde antes del cierre de 1962.",
            drawableResName = "img_archive_first_shop",
            visualCharacterTags = listOf("anomalous_room", "clocks")
        ),
        ArchiveEntryDef(
            id = "arch_room_forbidden",
            title = "Sala: El Sótano del Primer Hilo",
            category = ArchiveCategory.MAIN_STORY,
            eraOrDate = "Anterior a 1954",
            locationFound = "Habitación Prohibida",
            summary = "El recinto oculto donde las primeras marionetas dejaron de depender de manos humanas.",
            extraFragmentText = "Aquí El Fabricante selló el contrato original entre tela, memoria y cuerda.",
            drawableResName = "img_lore_manufacturer",
            visualCharacterTags = listOf("forbidden_room", "origin")
        )
    )

    val backpackRecipes: List<BackpackRecipe> = listOf(
        BackpackRecipe(
            id = "recipe_stitched_pouch",
            tier = BackpackTier.STITCHED_POUCH,
            buttonCost = 2,
            requirements = mapOf(
                RepairMaterial.THREAD to 1,
                RepairMaterial.NEEDLE to 1
            ),
            description = "Tu primera bolsa básica de 2 espacios cosida en el taller."
        ),
        BackpackRecipe(
            id = "recipe_kids_pack",
            tier = BackpackTier.KIDS_BACKPACK,
            buttonCost = 3,
            requirements = mapOf(
                RepairMaterial.THREAD to 2,
                RepairMaterial.NEEDLE to 1,
                RepairMaterial.SCREW to 1
            ),
            description = "Amplía tu capacidad a 4 espacios equilibrados."
        ),
        BackpackRecipe(
            id = "recipe_explorer_pack",
            tier = BackpackTier.EXPLORER_PACK,
            buttonCost = 5,
            requirements = mapOf(
                RepairMaterial.THREAD to 3,
                RepairMaterial.GEAR to 2,
                RepairMaterial.GLUE to 1
            ),
            description = "Gran mochila de 6 espacios para piezas y reliquias."
        ),
        BackpackRecipe(
            id = "recipe_manufacturer_pack",
            tier = BackpackTier.MANUFACTURER_PACK,
            buttonCost = 6,
            requirements = mapOf(
                RepairMaterial.THREAD to 2,
                RepairMaterial.PORCELAIN_SHARD to 1,
                RepairMaterial.SPRING to 1
            ),
            description = "5 espacios sellados: protege piezas guardadas de todo desgaste."
        )
    )

    val loreTemplates: List<LoreRoomTemplate> = listOf(
        LoreRoomTemplate(
            id = "lore_birthday_chair",
            loreRoomType = LoreRoomType.MEMORY,
            title = "Cumpleaños Detenido",
            subtitle = "Sillas polvorientas alrededor de un regalo cerrado",
            difficulty = PuzzleDifficulty.EASY,
            puzzleKind = LorePuzzleKind.CHAIR_AND_BUTTON,
            rewardArchiveId = "arch_birthday_memory",
            hintSummary = "Arrastra la silla hasta el interruptor rojo del suelo para destrabar el compartimento."
        ),
        LoreRoomTemplate(
            id = "lore_workshop_shelf",
            loreRoomType = LoreRoomType.HISTORICAL,
            title = "Depósito del Taller",
            subtitle = "Estanterías trabadas y viejos bancos de costura",
            difficulty = PuzzleDifficulty.NORMAL,
            puzzleKind = LorePuzzleKind.STUCK_BOOKSHELF,
            rewardArchiveId = "arch_workshop_photo",
            hintSummary = "Toma la palanca (o dispara al soporte) y empuja la estantería para revelar la foto."
        ),
        LoreRoomTemplate(
            id = "lore_office_painting",
            loreRoomType = LoreRoomType.DOCUMENTAL,
            title = "Oficina del Gerente",
            subtitle = "Un retrato inclinado sobre el escritorio de archivos",
            difficulty = PuzzleDifficulty.EASY,
            puzzleKind = LorePuzzleKind.CROOKED_PAINTING,
            rewardArchiveId = "arch_ledger_1954",
            hintSummary = "Gira el cuadro torcido hasta que haga clic y abra el cajón de documentos."
        ),
        LoreRoomTemplate(
            id = "lore_music_parlor",
            loreRoomType = LoreRoomType.MEMORY,
            title = "Sala de la Caja Musical",
            subtitle = "Una melodía incompleta aguarda bajo la lluvia",
            difficulty = PuzzleDifficulty.NORMAL,
            puzzleKind = LorePuzzleKind.MUSIC_BOX_REPAIR,
            rewardArchiveId = "arch_last_night",
            hintSummary = "Recoge la manivela y el engranaje en la sala y colócalos en la caja musical."
        ),
        LoreRoomTemplate(
            id = "lore_anomalous_clocks",
            loreRoomType = LoreRoomType.ANOMALOUS,
            title = "Pasillo de los Relojes",
            subtitle = "Cuatro relojes laten fuera de tiempo",
            difficulty = PuzzleDifficulty.HARD,
            puzzleKind = LorePuzzleKind.FOUR_CLOCKS,
            rewardArchiveId = "arch_room_anomalous",
            hintSummary = "Mira la nota de la pared (marca las 3:00 en cada reloj) y ajústalos."
        ),
        LoreRoomTemplate(
            id = "lore_bear_cage",
            loreRoomType = LoreRoomType.CHARACTER,
            title = "Vitrina del Osito",
            subtitle = "Un muñeco intacto observa tras barrotes oxidados",
            difficulty = PuzzleDifficulty.NORMAL,
            puzzleKind = LorePuzzleKind.TRAPPED_TOY,
            rewardArchiveId = "arch_bear_portrait",
            hintSummary = "Consigue la llave en el rincón o rompe la cadena disparando para liberarlo."
        ),
        LoreRoomTemplate(
            id = "lore_flooded_generator",
            loreRoomType = LoreRoomType.HISTORICAL,
            title = "Subestación del Sótano",
            subtitle = "Cables pelados cubren el suelo hasta la palanca",
            difficulty = PuzzleDifficulty.NORMAL,
            puzzleKind = LorePuzzleKind.FLOODED_FLOOR_PATH,
            rewardArchiveId = "arch_first_shop",
            hintSummary = "Empuja las cajas de madera sobre las zonas electrificadas para cortar la corriente."
        ),
        LoreRoomTemplate(
            id = "lore_forbidden_dolls",
            loreRoomType = LoreRoomType.FORBIDDEN,
            title = "Altar de las Tres Muñecas",
            subtitle = "Están giradas hacia sombras distintas de la pared",
            difficulty = PuzzleDifficulty.SPECIAL,
            puzzleKind = LorePuzzleKind.THREE_DOLLS_DIRECTION,
            rewardArchiveId = "arch_forbidden_blueprint",
            hintSummary = "Consulta el dibujo de la pared y gira las tres muñecas en la dirección indicada."
        ),
        LoreRoomTemplate(
            id = "lore_hollow_archive",
            loreRoomType = LoreRoomType.DOCUMENTAL,
            title = "Suelo del Archivo Antiguo",
            subtitle = "Algunas losas esconden secretos bajo la madera",
            difficulty = PuzzleDifficulty.NORMAL,
            puzzleKind = LorePuzzleKind.HOLLOW_FLOOR,
            rewardArchiveId = "arch_best_friend",
            hintSummary = "Inspecciona las losas sospechosas hasta oír la que suena hueca y ábrela."
        ),
        LoreRoomTemplate(
            id = "lore_forbidden_pedestals",
            loreRoomType = LoreRoomType.FORBIDDEN,
            title = "La Puerta de los Juguetes",
            subtitle = "Tres pedestales aguardan a los elegidos del taller",
            difficulty = PuzzleDifficulty.SPECIAL,
            puzzleKind = LorePuzzleKind.THREE_TOYS_PEDESTALS,
            rewardArchiveId = "arch_manufacturer_file",
            hintSummary = "Recoge soldado, muñeca y conejo y colócalos en orden según el mural."
        )
    )

    fun archiveById(id: String): ArchiveEntryDef? = archiveEntries.find { it.id == id }
}
