package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class EngineEventType(val key: String) {
    INTERACTABLE_ACTIVATED("interactable_activated"),
    OBJECT_DESTROYED("object_destroyed"),
    ROOM_OBJECTIVE_COMPLETED("room_objective_completed"),
    ROOM_OBJECTIVE_FAILED("room_objective_failed"),
    ENEMY_PROTECTED_ALLY("enemy_protected_ally"),
    ENEMY_STOLE_PART("enemy_stole_part"),
    SECRET_CLUE_FOUND("secret_clue_found"),
    SECRET_ROOM_UNLOCKED("secret_room_unlocked"),
    REPAIR_STARTED("repair_started"),
    REPAIR_COMPLETED("repair_completed"),
    MANUFACTURER_OFFER_ACCEPTED("manufacturer_offer_accepted"),
    MANUFACTURER_OFFER_REJECTED("manufacturer_offer_rejected")
}

enum class InteractableCategory(val displayName: String) {
    MOVABLE("Objeto Móvil"),
    ACTIVATABLE("Mecanismo Activable"),
    DESTRUCTIBLE("Elemento Destructible"),
    AMBIENT("Peligro / Zona Ambiental")
}

enum class InteractableType(
    val displayName: String,
    val category: InteractableCategory,
    val rarity: Rarity,
    val description: String,
    val lore: String,
    val damage: Float = 0f,
    val affectsPlayer: Boolean = true,
    val affectsEnemies: Boolean = true,
    val destructible: Boolean = false,
    val maxHp: Float = 30f,
    val tags: List<String> = emptyList()
) {
    // A. Móviles
    WOOD_BLOCK(
        "Bloque de Madera", InteractableCategory.MOVABLE, Rarity.COMMON,
        "Empújalo para bloquear proyectiles enemigos o cerrar pasillos.",
        "Bloques infantiles con letras gastadas.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, tags = listOf("movable", "cover")
    ),
    CRATE_COVER(
        "Caja Desplazable", InteractableCategory.MOVABLE, Rarity.COMMON,
        "Cobertura móvil que detiene disparos hasta romperse.",
        "Embalaje original de Wonderland Toys.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, destructible = true, maxHp = 35f, tags = listOf("movable", "cover")
    ),
    TOY_CART(
        "Carrito de Juguete", InteractableCategory.MOVABLE, Rarity.UNCOMMON,
        "Al empujarlo sale disparado atropellando enemigos a su paso.",
        "Sus ruedas chirrían buscando velocidad.",
        damage = 24f, affectsPlayer = false, affectsEnemies = true, tags = listOf("movable", "hazard")
    ),
    PARTS_BARREL(
        "Barril de Piezas", InteractableCategory.MOVABLE, Rarity.UNCOMMON,
        "Rueda tras recibir un golpe o empuje y golpea al impactar.",
        "Repleto de tornillos y brazos sueltos.",
        damage = 22f, affectsPlayer = true, affectsEnemies = true, destructible = true, maxHp = 28f, tags = listOf("movable", "mechanical")
    ),
    GIANT_DOLL_PROP(
        "Muñeco Pesado", InteractableCategory.MOVABLE, Rarity.UNCOMMON,
        "Puedes arrastrarlo para tapar pasos o cubrir balas.",
        "Demasiado grande para una vitrina.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, tags = listOf("movable", "cover")
    ),
    BATTERY_CUBE(
        "Cubo Magnético", InteractableCategory.MOVABLE, Rarity.RARE,
        "Al empujarlo cerca de cables o charcos amplifica descargas eléctricas.",
        "Zumbe con carga estática del taller.",
        damage = 18f, affectsPlayer = true, affectsEnemies = true, tags = listOf("movable", "electric")
    ),

    // B. Mecanismos activables
    ELECTRIC_TRAIN(
        "Tren Eléctrico", InteractableCategory.ACTIVATABLE, Rarity.RARE,
        "Circula por su circuito cerrado atropellando todo; tiene variante rápida o explosiva.",
        "Locomotora de hojalata que nunca frena a tiempo.",
        damage = 22f, affectsPlayer = true, affectsEnemies = true, tags = listOf("mechanical", "hazard", "moving")
    ),
    TOY_CAROUSEL(
        "Carrusel Mecánico", InteractableCategory.ACTIVATABLE, Rarity.RARE,
        "Gira durante varios segundos empujando y dañando a quienes se acerquen.",
        "Caballitos pintados con sonrisa fija.",
        damage = 16f, affectsPlayer = true, affectsEnemies = true, tags = listOf("mechanical", "hazard")
    ),
    WINDUP_JACK_TRAP(
        "Caja Sorpresa de Cuerda", InteractableCategory.ACTIVATABLE, Rarity.UNCOMMON,
        "Al darle cuerda detona un resorte aturdidor que daña a enemigos cercanos.",
        "Un muelle tensado bajo tapa cerrada.",
        damage = 18f, affectsPlayer = false, affectsEnemies = true, tags = listOf("mechanical", "spring")
    ),
    SPRING_LAUNCHER(
        "Lanzador de Resortes", InteractableCategory.ACTIVATABLE, Rarity.UNCOMMON,
        "Dispara una salva de piezas metálicas y pernos por la habitación.",
        "Prototipo de juguete ballesta sin seguro.",
        damage = 15f, affectsPlayer = true, affectsEnemies = true, tags = listOf("mechanical", "projectile")
    ),
    WALL_LEVER(
        "Palanca de Taller", InteractableCategory.ACTIVATABLE, Rarity.COMMON,
        "Activa mecanismos, cables eléctricos o suelta estanterías colgadas.",
        "Hierro frío conectado a cables ocultos.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, tags = listOf("switch")
    ),
    CHRONO_CLOCK(
        "Reloj de Péndulo", InteractableCategory.ACTIVATABLE, Rarity.RARE,
        "Al activarlo ralentiza temporalmente a los enemigos durante 6 segundos.",
        "Marca un compás distinto al del resto del edificio.",
        damage = 0f, affectsPlayer = false, affectsEnemies = true, tags = listOf("mechanical", "clock")
    ),
    PRIZE_MACHINE(
        "Máquina de Premios", InteractableCategory.ACTIVATABLE, Rarity.RARE,
        "Gasto 2 botones o un golpe: suelta botín útil... o una bomba defectuosa.",
        "Vitrina oxidada que aún conserva fichas dentro.",
        damage = 20f, affectsPlayer = true, affectsEnemies = true, tags = listOf("mechanical", "loot")
    ),

    // C. Destructibles
    HANGING_SHELF(
        "Estantería Colgante", InteractableCategory.DESTRUCTIBLE, Rarity.UNCOMMON,
        "Dispárale o acciona la palanca: cae aplastando debajo y bloquea el paso.",
        "Sostenida por una sola cuerda deshilachada.",
        damage = 38f, affectsPlayer = true, affectsEnemies = true, destructible = true, maxHp = 18f, tags = listOf("destructible", "heavy")
    ),
    GLASS_LAMP(
        "Lámpara de Cristal", InteractableCategory.DESTRUCTIBLE, Rarity.COMMON,
        "Se quiebra al dispararle, cegando/aturdiendo cercanos y creando sombra.",
        "Bombilla cálida llena de polvo.",
        damage = 8f, affectsPlayer = false, affectsEnemies = true, destructible = true, maxHp = 12f, tags = listOf("destructible", "light")
    ),
    GLASS_CASE(
        "Vitrina Frágil", InteractableCategory.DESTRUCTIBLE, Rarity.COMMON,
        "Al romperse deja fragmentos cortantes en el suelo que hieren al pisarlos.",
        "Exhibía muñecos que ya no están.",
        damage = 12f, affectsPlayer = true, affectsEnemies = true, destructible = true, maxHp = 16f, tags = listOf("destructible", "hazard")
    ),
    LOOT_CRATE(
        "Caja Sellada", InteractableCategory.DESTRUCTIBLE, Rarity.COMMON,
        "Al destruirse entrega botones, materiales de reparación o revela un juguete.",
        "Cuidado: algunas cajas esconden sorpresas vivas.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, destructible = true, maxHp = 22f, tags = listOf("destructible", "loot")
    ),
    CRACKED_WALL(
        "Muro Agrietado", InteractableCategory.DESTRUCTIBLE, Rarity.UNCOMMON,
        "Puede derribarse con explosivos, barriles o tren explosivo para abrir paso.",
        "Tablas podridas a punto de ceder.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, destructible = true, maxHp = 45f, tags = listOf("destructible", "secret")
    ),
    COMPANION_CAGE(
        "Jaula de Mascota", InteractableCategory.DESTRUCTIBLE, Rarity.RARE,
        "Una mascota atrapada detrás de barrotes. Destruye la jaula con tus bolitas o una bomba para rescatarla.",
        "Los juguetes hostiles custodian a los pocos aliados que quedan.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, destructible = true, maxHp = 28f, tags = listOf("destructible", "companion", "cage")
    ),

    // D. Ambientales
    OIL_PUDDLE(
        "Charco de Aceite", InteractableCategory.AMBIENT, Rarity.COMMON,
        "Hace resbalar al cruzarlo; si hay electricidad cerca propaga la descarga.",
        "Aceite oscuro goteando de viejos engranajes.",
        damage = 0f, affectsPlayer = true, affectsEnemies = true, tags = listOf("ambient", "slippery")
    ),
    LIVE_WIRE(
        "Cable Eléctrico", InteractableCategory.AMBIENT, Rarity.UNCOMMON,
        "Al activarse electrifica la zona y cualquier charco cercano durante unos segundos.",
        "Chispas azules bailan entre el cobre pelado.",
        damage = 18f, affectsPlayer = true, affectsEnemies = true, tags = listOf("ambient", "electric")
    ),
    STEAM_VENT(
        "Rejilla de Vapor", InteractableCategory.AMBIENT, Rarity.UNCOMMON,
        "Libera vapor espeso que oculta brevemente a Patch de la puntería enemiga.",
        "Tubería antigua del sótano de Wonderland.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, tags = listOf("ambient", "stealth")
    ),
    PRESSURE_TILE(
        "Baldosa de Presión", InteractableCategory.AMBIENT, Rarity.UNCOMMON,
        "Se activa al pisarla o empujar un bloque encima para disparar mecanismos.",
        "Cede ligeramente con un clic metálico.",
        damage = 0f, affectsPlayer = false, affectsEnemies = false, tags = listOf("ambient", "switch")
    ),
    FAN_BLOWER(
        "Ventilador de Piso", InteractableCategory.AMBIENT, Rarity.UNCOMMON,
        "Corriente de aire constante que desvía proyectiles y empuja suavemente.",
        "Aspas oxidadas girando sin descanso.",
        damage = 0f, affectsPlayer = true, affectsEnemies = true, tags = listOf("ambient", "wind")
    )
}

enum class ObjectiveType(
    val displayName: String,
    val categoryLabel: String,
    val description: String,
    val rewardText: String
) {
    SURVIVE_TIME(
        "Resistencia en Caja", "Supervivencia",
        "Sobrevive al asedio hasta que el reloj marque 0.",
        "Botones dorados y materiales de reparación."
    ),
    LAST_CHAIR_SAFE_ZONE(
        "La Última Silla", "Supervivencia",
        "Quédate dentro de la zona iluminada cada vez que la música se detenga.",
        "Reliquia pasiva o pieza inusual si no fallas."
    ),
    PROTECT_CRYING_BOX(
        "La Caja que Llora", "Protección",
        "Impide que los juguetes destruyan la caja palpitante antes de que termine el tiempo.",
        "Pieza corporal rara (o enemigo furioso si se rompe)."
    ),
    CATCH_LOST_TOY(
        "El Juguete Perdido", "Recuperación",
        "Atrapa al conejito perdido tocándolo sin destruirlo a tiros.",
        "Botín extra y revelación de pista secreta."
    ),
    RECOVER_GEARS(
        "Engranajes Dispersos", "Recuperación",
        "Recoge los 3 engranajes dorados repartidos por la sala.",
        "Materiales de reparación + botones."
    ),
    WIND_THREE_MECHANISMS(
        "La Máquina sin Cuerda", "Activación",
        "Da cuerda a los 3 mecanismos de la sala mientras esquivas enemigos.",
        "Apertura especial y recompensa de taller."
    ),
    ESCORT_PACKAGE(
        "El Paquete Frágil", "Escape / Escolta",
        "Empuja la caja frágil hasta la baldosa de entrega antes de que la destruyan.",
        "Botín doble al entregarla intacta."
    ),
    DEFEAT_WITH_ENVIRONMENT(
        "Ingenio del Taller", "Combate Condicionado",
        "Usa trenes, barriles, estanterías o cables para eliminar enemigos.",
        "Medalla adicional y botones."
    )
}

enum class EnemyRole(val displayName: String) {
    STANDARD("Común"),
    LEADER("Líder"),
    PROTECTOR("Protector"),
    COWARD("Cobarde"),
    RIVAL("Rival"),
    COLLECTOR("Recolector"),
    REPAIRER("Reparador")
}

enum class EnemyFaction(val displayName: String) {
    TIN_ARMY("Ejército de Plomo"),
    RAG_PLUSH("Felpa y Trapo"),
    PORCELAIN("Porcelana"),
    VOID_SHADOW("Sombras")
}

enum class SecretClueType(
    val displayName: String,
    val hintText: String,
    val lore: String
) {
    CROOKED_FRAME(
        "Cuadro Torcido",
        "Un marco desalineado en la pared. Acércate e interactúa para enderezarlo.",
        "Detrás del retrato polvoriento se oculta un pestillo."
    ),
    WALL_GAZING_DOLL(
        "Muñeca Fija en el Muro",
        "Esta muñeca no aparta la mirada de una pared vacía. Muévela o inspecciónala.",
        "Sus ojos de vidrio apuntan hacia una cerradura."
    ),
    TICKING_CLOCK_PUZZLE(
        "Reloj con Tic Irregular",
        "Las manecillas titubean cerca de las 3:15. Ajústalo al interactuar.",
        "Cada golpe de aguja resuena detrás del entarimado."
    ),
    KNOCKING_WALL(
        "Pared con Golpes Suaves",
        "Se oyen nudillos desde el otro lado del tabique. Detona una bomba o barril cerca.",
        "Alguien o algo sigue esperando del otro lado."
    ),
    WRONG_SHADOW_PROP(
        "Sombra con Forma de Llave",
        "La luz proyecta una sombra imposible. Empuja el bloque sobre la baldosa marcada.",
        "La juguetería no siempre proyecta la sombra correcta."
    )
}

enum class SecretRoomFlavor(val displayName: String, val subtitle: String) {
    HIDDEN_TREASURE("Vitrina Oculta", "Reliquias selladas tras la pared"),
    MEMORY_VAULT("Habitación de Memoria", "Archivos olvidados de Wonderland Toys"),
    FORBIDDEN_CHAMBER("Sala Prohibida", "Poder maldito a cambio de un riesgo"),
    SHORTCUT_PASSAGE("Pasadizo de Servicio", "Atajo entre pasillos de la tienda"),
    MANUFACTURER_ROOM("Taller del Fabricante", "Una presencia antigua aguarda aquí")
}

enum class RepairMaterial(val displayName: String, val rarity: Rarity) {
    THREAD("Hilo Encerado", Rarity.COMMON),
    NEEDLE("Aguja de Acero", Rarity.COMMON),
    GEAR("Engranaje", Rarity.UNCOMMON),
    SCREW("Tornillo", Rarity.COMMON),
    GLUE("Pegamento de Taller", Rarity.UNCOMMON),
    PORCELAIN_SHARD("Porcelana Fina", Rarity.RARE),
    SPRING("Resorte Templado", Rarity.RARE)
}

enum class RepairMinigameType(val displayName: String, val instruction: String) {
    SEWING("Minijuego de Costura", "Toca los puntos de sutura en orden: 1 → 2 → 3 → 4"),
    GEARS("Minijuego de Engranajes", "Alinea los 3 engranajes hasta que todos giren en verde"),
    SPRING_TIMING("Ajuste de Resorte", "Detén el indicador dentro de la franja dorada"),
    PORCELAIN("Restauración de Porcelana", "Une las 3 grietas compatibles sin gastar intentos"),
    WIRING("Cableado de Precisión", "Conecta los 3 pares de nodos del mismo color")
}

enum class RepairActionMode(val displayName: String, val desc: String) {
    REPAIR("Reparar", "Restaura la durabilidad de la pieza o vuelve a coserla."),
    REINFORCE("Reforzar", "Restaura durabilidad y reduce desgaste por 3 salas."),
    MODIFY("Modificar", "Cambia un rasgo táctico (ej. proyectiles con chispa/engranaje).")
}

enum class RepairQuality(val displayName: String, val multiplier: Float, val colorHex: Long) {
    IMPERFECT("Imperfecta", 0.55f, 0xFFD98A2B),
    GOOD("Correcta", 0.85f, 0xFF597354),
    EXCELLENT("Excelente", 1.0f, 0xFFC89948)
}

enum class ManufacturerDealCategory(val displayName: String) {
    BODY_SWAP("Intercambio Corporal"),
    LIFE_PACT("Intercambio de Vida"),
    DEFECT_ALCHEMY("Intercambio de Defectos"),
    MEMORY_BARGAIN("Intercambio de Recuerdos"),
    WORLD_ALTER("Modificación del Mundo")
}

data class ManufacturerOffer(
    val id: String,
    val title: String,
    val category: ManufacturerDealCategory,
    val costSummary: String,
    val rewardSummary: String,
    val riskHint: String,
    val hiddenPrice: String? = null,
    val bodyZone: BodyZone? = null,
    val targetPartId: String? = null,
    val itemId: String? = null,
    val defectId: String? = null,
    val grantDefectId: String? = null,
    val memoryId: String? = null,
    val worldEffectId: String? = null,
    val buttonsCost: Int = 0,
    val maxHeartsCost: Int = 0
)

data class ManufacturerEncounter(
    val title: String,
    val dialogue: String,
    val isTripleChoice: Boolean = false,
    val offers: List<ManufacturerOffer>
)
