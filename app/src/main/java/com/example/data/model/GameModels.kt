package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class BodyZone(val displayName: String, val shortLabel: String) {
    HEAD("Cabeza", "CAB"),
    EYES("Ojos", "OJO"),
    ARMS("Brazos", "BRA"),
    TORSO("Torso", "TOR"),
    LEGS("Piernas", "PIE"),
    BACK("Espalda", "ESP")
}

enum class WearState(val displayName: String) {
    INTACT("Intacto"),
    DAMAGED("Dañado"),
    CRITICAL("Crítico"),
    DETACHED("Desprendido")
}

enum class Rarity(val displayName: String, val colorHex: Long) {
    COMMON("Común", 0xFFB5A17D),
    UNCOMMON("Inusual", 0xFF597354),
    RARE("Raro", 0xFF4C5F6E),
    CURSED("Maldito", 0xFF942229),
    LEGENDARY("Legendario", 0xFFC89948);

    val color: Color get() = Color(colorHex)
}

enum class AttackStyle {
    BUTTON_SHOT,
    GEAR_PROJECTILE,
    SCISSOR_SLASH,
    EXPLOSIVE_TOY,
    RAPID_NEEDLES,
    DOLL_BOLT
}

data class PartEffects(
    val attackStyle: AttackStyle? = null,
    val damageBonus: Float = 0f,
    val fireRateMult: Float = 1f,
    val moveSpeedMult: Float = 1f,
    val dodgeLengthMult: Float = 1f,
    val dodgeCooldownMult: Float = 1f,
    val visionRangeMult: Float = 1f,
    val accuracyBonus: Float = 0f,
    val maxHeartsBonus: Int = 0,
    val periodicSoundWave: Boolean = false,
    val armorChance: Float = 0f
)

data class BodyPartDef(
    val id: String,
    val name: String,
    val zone: BodyZone,
    val rarity: Rarity,
    val maxDurability: Int = 100,
    val tags: List<String>,
    val effects: PartEffects,
    val intactDesc: String,
    val damagedDesc: String,
    val criticalDesc: String,
    val lore: String
)

data class EquippedBodyPart(
    val def: BodyPartDef,
    val durability: Int = def.maxDurability,
    val isDetached: Boolean = false
) {
    val wearState: WearState
        get() = when {
            isDetached -> WearState.DETACHED
            durability <= 30 -> WearState.CRITICAL
            durability <= 65 -> WearState.DAMAGED
            else -> WearState.INTACT
        }
}

enum class DefectCategory(val displayName: String) {
    PERCEPTION("Percepción"),
    HEARING("Audición y estímulos"),
    MOVEMENT("Movimiento"),
    COMBAT("Manipulación y combate"),
    PERSONALITY("Personalidad"),
    STRUCTURE("Estructura")
}

data class DefectEffects(
    val leftBlindness: Boolean = false,
    val visionScale: Float = 1f,
    val blurPulses: Boolean = false,
    val frontalPrecisionBonus: Float = 0f,
    val lightSensitivity: Boolean = false,
    val deafOffscreen: Boolean = false,
    val selectiveAudio: Boolean = false,
    val noiseDisorient: Boolean = false,
    val delayedTelegraph: Boolean = false,
    val speedMult: Float = 1f,
    val irregularWalk: Boolean = false,
    val dodgeDistanceMult: Float = 1f,
    val stiffAccel: Boolean = false,
    val noisySteps: Boolean = false,
    val dodgeCooldownMult: Float = 1f,
    val dodgeShockwave: Boolean = false,
    val shortRangeBonusDamage: Boolean = false,
    val spreadPenalty: Float = 0f,
    val slowConsumable: Boolean = false,
    val weakHeavyFastLight: Boolean = false,
    val patternBonus: Boolean = false,
    val sensoryDistract: Boolean = false,
    val impulseDelayThenBurst: Boolean = false,
    val fogForgetfulness: Boolean = false,
    val wearRateMult: Float = 1f,
    val repairBonusMult: Float = 1f,
    val fragileBody: Boolean = false
)

data class DefectDef(
    val id: String,
    val name: String,
    val category: DefectCategory,
    val severity: Int, // 1 Leve, 2 Medio, 3 Severo
    val challengePoints: Int,
    val description: String,
    val upsideText: String = "",
    val incompatibleIds: List<String> = emptyList(),
    val effects: DefectEffects = DefectEffects()
)

data class ItemEffects(
    val damageBonus: Float = 0f,
    val speedMult: Float = 1f,
    val fireRateMult: Float = 1f,
    val enemyDetectionMult: Float = 1f,
    val periodicStunWave: Boolean = false,
    val secretRoomChanceBonus: Float = 0f,
    val slowEnemyBulletsMult: Float = 1f,
    val spawnDecoyOnHit: Boolean = false,
    val bonusDropsAndAggro: Boolean = false,
    val autoRecoverPartPerRoom: Boolean = false,
    val maxHeartsBonus: Int = 0,
    val pierceBullets: Boolean = false,
    val bounceBullets: Boolean = false,
    val homingBullets: Boolean = false,
    val extraBounces: Int = 0,
    val dodgeLengthMult: Float = 1f,
    val dodgeCooldownMult: Float = 1f,
    val armorWearReduction: Float = 0f,
    val visionBoost: Float = 1f,
    val spawnDecoyOnRoomEnter: Boolean = false,
    val critChance: Float = 0f
)

data class PassiveItemDef(
    val id: String,
    val name: String,
    val rarity: Rarity,
    val tags: List<String>,
    val description: String,
    val memoryFragmentId: String? = null,
    val lore: String,
    val effects: ItemEffects
)

enum class ConsumableType(
    val displayName: String,
    val shortDesc: String,
    val shopPrice: Int
) {
    BOMB("Bomba de Pólvora", "Explosión en área que daña enemigos y abre grietas.", 5),
    SEWING_KIT("Kit de Costura", "Repara +45 de durabilidad en la pieza más dañada.", 4),
    KEY("Llave de Latón", "Abre cofres cerrados o puertas de tesoro/secretas.", 4),
    WINDUP_STRING("Cuerda de Reloj", "Aumenta la cadencia de disparo temporalmente en la sala.", 3),
    SURPRISE_BOX("Caja Sorpresa", "Efecto impredecible: cura, botones, bomba o aliados.", 5),
    EXPLOSIVE_TOY("Juguete Explosivo", "Se lanza hacia adelante y detona al impactar.", 6),
    SPRING_COIL("Muelle Reforzado", "Reinicia tu esquiva y otorga velocidad breve.", 3),
    REPAIR_PATCH("Parche de Tela", "Cura 1 corazón y restaura +20 durabilidad general.", 5)
}

data class SynergyDef(
    val id: String,
    val name: String,
    val requiredTags: List<String>,
    val description: String,
    val effectId: String
)

enum class CharacterId {
    PATCH, TINNY, DOLLY, HOPS, JACK, STITCH
}

data class ToyCharacterDef(
    val id: CharacterId,
    val name: String,
    val title: String,
    val description: String,
    val lore: String,
    val maxHearts: Int,
    val usesEnergyShield: Boolean = false,
    val baseDamage: Float,
    val baseSpeed: Float,
    val baseFireRate: Float,
    val dodgeCooldownSec: Float,
    val abilityName: String,
    val abilityDesc: String,
    val abilityCooldownSec: Float,
    val startingItemId: String,
    val startingParts: Map<BodyZone, String>,
    val suggestedDefectIds: List<String>,
    val unlockRequirementDesc: String,
    val unlockedByDefault: Boolean = false,
    val primaryColorHex: Long,
    val accentColorHex: Long
)

enum class Difficulty(
    val displayName: String,
    val description: String,
    val minDefects: Int,
    val maxDefects: Int,
    val enemyHpMult: Float,
    val enemySpeedMult: Float,
    val damageTakenHearts: Int,
    val resourceMult: Float,
    val repairStationChance: Float,
    val medalMultiplier: Float
) {
    SUAVE(
        "Suave",
        "Menor daño recibido, más recursos y hasta 1 defecto.",
        0, 1, 0.85f, 0.9f, 1, 1.35f, 0.85f, 0.8f
    ),
    NORMAL(
        "Normal",
        "Experiencia equilibrada de Wonderland Toys. 1–2 defectos.",
        1, 2, 1.0f, 1.0f, 1, 1.0f, 0.65f, 1.0f
    ),
    DIFICIL(
        "Difícil",
        "Enemigos agresivos, menos recursos y 2 defectos obligatorios.",
        2, 2, 1.25f, 1.12f, 2, 0.8f, 0.45f, 1.4f
    ),
    PESADILLA(
        "Pesadilla",
        "Pocas reparaciones, daño letal, 3 defectos y grandes recompensas.",
        3, 3, 1.5f, 1.22f, 2, 0.65f, 0.25f, 2.0f
    )
}

enum class RoomType(val displayName: String, val symbol: String) {
    START("Caja Inicial", "I"),
    COMBAT("Habitación", "⚔"),
    TREASURE("Sala de Tesoro", "★"),
    SHOP("Puesto de Botones", "$"),
    SECRET("Sala Secreta", "?"),
    CURSE("Sala de Maldición", "☠"),
    EVENT("Evento", "!"),
    CHALLENGE("Sala de Desafío", "⚡"),
    REPAIR("Mesa de Costura", "✂"),
    MANUFACTURER("Sala del Fabricante", "⚙"),
    FORBIDDEN("Sala Prohibida", "☣"),
    BOSS("Jefe del Piso", "♛")
}

enum class Direction { NORTH, SOUTH, EAST, WEST }

enum class EnemyType(
    val displayName: String,
    val maxHp: Float,
    val speed: Float,
    val isBoss: Boolean = false,
    val description: String
) {
    TIN_SOLDIER("Soldadito de Plomo", 28f, 75f, false, "Marcha en línea y dispara perdigones de latón."),
    CREEPY_DOLL("La Muñeca", 24f, 118f, false, "Finge ser un adorno hasta que te acercas."),
    PLUSH_BEAR("Osito de Felpa", 52f, 62f, false, "Resistente. Telegrafía embestidas pesadas."),
    JACK_IN_BOX("Jack Sorpresa", 32f, 48f, false, "Emerge de su caja lanzando ráfagas radiales."),
    CLOCKWORK_SPIDER("Araña de Cuerda", 20f, 138f, false, "Se desliza por muros y salta hacia ti."),
    MIRROR_DOLL("Muñeco Espejo", 34f, 95f, false, "Imita tus movimientos con inquietante precisión."),
    THIEF_GREMLIN("Duende Ladrón", 26f, 132f, false, "Persigue piezas corporales caídas o bombas para robarlas."),
    MIMIC_CHEST("Cofre Imitador", 46f, 90f, false, "Se oculta como recompensa hasta despertar."),
    PORCELAIN_CLOWN("Payaso de Porcelana", 38f, 82f, false, "Salta erráticamente y lanza ráfagas triples de cascabeles."),
    WINDUP_CHOMPER("Dentadura de Cuerda", 22f, 148f, false, "Mandíbula mecánica veloz que avanza a dentelladas."),
    NEEDLE_MARIONETTE("Marioneta de Agujas", 30f, 88f, false, "Mantiene distancia y dispara agujas afiladas en abanico."),
    SHADOW_PUPPET("Sombra Cosida", 26f, 125f, false, "Se desliza entre las sombras y aparece cerca de ti."),
    RUSTY_TIN_KNIGHT("Caballero Oxidado", 68f, 54f, false, "Blindado y pesado; avanza firme disparando pernos."),
    RAG_BOMBER("Muñeco Pirotécnico", 34f, 76f, false, "Cargado de pólvora; detona en un estallido al caer."),
    BETRAYED_BUNNY("Conejito Traicionado", 48f, 134f, false, "Antiguo compañero abandonado. Regresó en otra partida para cobrarse botones y rencor."),
    MIRROR_COPY("Reflejo Invertido", 30f, 112f, false, "Copia nacida de los espejos que imita tu movimiento y devuelve disparos."),
    WEEPING_TOY("Estatuilla Inmóvil", 38f, 178f, false, "Se congela cuando la miras directamente, pero se abalanza al darle la espalda."),
    TIN_CAPTAIN("Capitán de Plomo", 56f, 68f, false, "Líder táctico: coordina a los soldaditos; si cae, su escuadrón se desorganiza."),
    BODYGUARD_BEAR("Osito Guardaespaldas", 62f, 80f, false, "Protector: intercepta disparos frente a aliados frágiles y enfurece si caen."),
    CLOCKWORK_COURIER("Conejo de Cuerda", 24f, 146f, false, "Cobarde: huye hacia la campana de alarma para atraer refuerzos."),
    STITCH_MECHANIC("Costurero Reparador", 32f, 92f, false, "Reparador: acude a curar enemigos dañados si no lo detienes."),
    BOSS_VENDEDOR("El Vendedor", 240f, 72f, true, "Piso 1: Lanza juguetes armados y reordena cajas de exhibición."),
    BOSS_BOTONERO("El Botonero", 265f, 80f, true, "Piso 1: Dispara salvas de botones dorados en cruz."),
    BOSS_COSTURERA("La Costurera", 320f, 84f, true, "Piso 2: Teje hilos tensos que restringen el movimiento."),
    BOSS_TITIRITERO("El Titiritero", 340f, 76f, true, "Piso 2: Maneja marionetas de agujas y ráfagas diagonales."),
    BOSS_NINERA("La Niñera", 400f, 78f, true, "Piso 3: Invoca muñecos y coordina oleadas."),
    BOSS_RELOJERO("El Relojero", 420f, 88f, true, "Piso 3: Lanza espirales horarias y dentaduras de cuerda."),
    BOSS_MAESTRO("El Maestro de Ceremonias", 480f, 92f, true, "Piso 4: Convierte el suelo en un carrusel letal."),
    BOSS_BUHONERO("El Buhonero Sombrío", 510f, 94f, true, "Piso 4: Arroja proyectiles explosivos y sombras veloces."),
    BOSS_COLECCIONISTA("El Coleccionista", 560f, 86f, true, "Piso 5: Usa piezas robadas de otros juguetes."),
    BOSS_MADRE("La Madre de los Juguetes", 700f, 82f, true, "Piso 6: Núcleo de Wonderland. Altera las reglas del combate.")
}

enum class CompanionId(
    val displayName: String,
    val rarity: Rarity,
    val interestDesc: String,
    val conditionDesc: String,
    val lore: String
) {
    BUNNY(
        displayName = "El Conejito",
        rarity = Rarity.UNCOMMON,
        interestDesc = "Te sigue y salta a recoger botones caídos por ti.",
        conditionDesc = "Si lo rechazas o abandonas, volverá como enemigo en otra partida.",
        lore = "Sus orejas de felpa aún recuerdan cada puerta cerrada en sus narices."
    ),
    FACELESS_DOLL(
        displayName = "La Muñeca sin Cara",
        rarity = Rarity.RARE,
        interestDesc = "Te ofrece objetos pasivos y reliquias al avanzar por la tienda.",
        conditionDesc = "Cada regalo cobra un precio oculto en salud, botones o costura.",
        lore = "Donde debían ir sus ojos solo hay hilo negro esperando un trato."
    ),
    BROKEN_SOLDIER(
        displayName = "El Soldadito Descompuesto",
        rarity = Rarity.COMMON,
        interestDesc = "Te respalda disparando perdigones contra enemigos cercanos.",
        conditionDesc = "Si recibe demasiado daño se avería y necesita reparación para volver a disparar.",
        lore = "Su resorte lateral chisporrotea cada vez que cubre tu flanco."
    ),
    FORGOTTEN_BEAR(
        displayName = "El Osito Olvidado",
        rarity = Rarity.LEGENDARY,
        interestDesc = "Permanece a tu lado protegiendo tu último hilo de vida.",
        conditionDesc = "Bloquea un ataque mortal una única vez salvándote con 1 corazón; después desaparece.",
        lore = "Nadie recuerda el nombre del niño que lo abrazaba, pero él no olvida cómo cuidar."
    ),
    CLOCKWORK_CAT(
        displayName = "El Gato de Cuerda",
        rarity = Rarity.UNCOMMON,
        interestDesc = "Detecta y abre grietas hacia salas secretas al instante.",
        conditionDesc = "Si acumulas más de 14 botones te roba 1 por sala hasta que le des cuerda.",
        lore = "Sus ojos de latón giran al compás de las monedas que tintinean."
    ),
    PAPER_MOTH(
        displayName = "La Polilla de Papel",
        rarity = Rarity.COMMON,
        interestDesc = "Amplía la luz de tu linterna (+28% visión) y atenúa la ceguera.",
        conditionDesc = "Le aterra el fuego: detonar bombas cerca la hace entrar en pánico y huir.",
        lore = "Recortada de viejos planos de la tienda; busca luz pero teme a la pólvora."
    ),
    WHISPER_PUPPET(
        displayName = "El Títere Murmurante",
        rarity = Rarity.CURSED,
        interestDesc = "Susurra atrayendo enemigos lejos de ti y debilita su guardia.",
        conditionDesc = "Si lo conservas más de 4 salas sin silenciarlo (2 ●), se vuelve contra ti.",
        lore = "Susurra viejos secretos de Wonderland, pero disfruta viendo costuras rotas."
    ),
    RUSTY_BALLERINA(
        displayName = "La Bailarina Oxidada",
        rarity = Rarity.RARE,
        interestDesc = "Su melodía lenta frena los proyectiles enemigos cercanos.",
        conditionDesc = "Si sufres daño en dos salas seguidas su mecanismo se atasca y requiere aceite (2 ●).",
        lore = "Gira sobre una pierna de acero buscando un compás que ya no chirríe."
    )
}

enum class RoomMechanic(
    val displayName: String,
    val hintText: String,
    val minDifficultyOrdinal: Int = 0
) {
    NONE("Sala Estándar", "", 0),
    NURSERY_MAZE("Laberinto Infantil", "Bloques de madera gigantes dividen la sala en pasillos.", 0),
    MIRROR_COPIES("Sala de Espejos", "Espejos inquietantes proyectan reflejos que copian tus pasos y disparos.", 0),
    TILTED_FLOOR("Suelo Inclinado", "El entarimado se inclina haciendo deslizar muñecos, bombas y botones.", 0),
    VANISHING_FLOOR("Suelo Quebradizo", "Los sectores agrietados ceden y desaparecen al vacío tras un aviso.", 0),
    DONT_BLINK_STATUES("Juguetes Inmóviles", "Las estatuillas se congelan si las miras, pero corren cuando das la espalda.", 0),
    SHIFTING_DOORS("Puertas Cambiantes", "Los engranajes rotan los marcos de salida de muro periódicamente.", 1),
    FLICKERING_LIGHTS("Apagón de Vitrina", "Las lámparas titilan sumergiendo la habitación en breves sombras.", 1),
    SPINNING_CAROUSEL("Carrusel Giratorio", "El suelo gira arrastrando suavemente a todos alrededor del centro.", 2)
}

data class FloorTheme(
    val floorNumber: Int,
    val name: String,
    val subtitle: String,
    val bossType: EnemyType,
    val floorColorHex: Long,
    val wallColorHex: Long,
    val ambientTintHex: Long,
    val altBossType: EnemyType? = null
)

enum class AchievementCategory(val displayName: String) {
    SURVIVAL("Supervivencia"),
    COMBAT("Combate"),
    COLLECTION("Coleccionismo"),
    DEFECTS("Defectos"),
    CHARACTERS("Personajes"),
    SECRETS("Secretos"),
    CHALLENGES("Desafíos")
}

data class AchievementDef(
    val id: String,
    val name: String,
    val category: AchievementCategory,
    val description: String,
    val requirementText: String,
    val hidden: Boolean = false,
    val rewardText: String,
    val unlockCharacterId: CharacterId? = null,
    val rewardChallengePoints: Int = 5
)

data class EventOption(
    val id: String,
    val label: String,
    val description: String,
    val buttonCost: Int = 0
)

data class RoomEventDef(
    val id: String,
    val title: String,
    val narrative: String,
    val options: List<EventOption>
)

data class MemoryFragmentDef(
    val id: String,
    val title: String,
    val text: String
)

fun EnemyType.defaultRole(): EnemyRole = when (this) {
    EnemyType.TIN_CAPTAIN -> EnemyRole.LEADER
    EnemyType.BODYGUARD_BEAR -> EnemyRole.PROTECTOR
    EnemyType.CLOCKWORK_COURIER -> EnemyRole.COWARD
    EnemyType.THIEF_GREMLIN -> EnemyRole.COLLECTOR
    EnemyType.STITCH_MECHANIC -> EnemyRole.REPAIRER
    EnemyType.PLUSH_BEAR, EnemyType.RUSTY_TIN_KNIGHT -> EnemyRole.PROTECTOR
    else -> EnemyRole.STANDARD
}

fun EnemyType.defaultFaction(): EnemyFaction = when (this) {
    EnemyType.TIN_SOLDIER, EnemyType.TIN_CAPTAIN, EnemyType.RUSTY_TIN_KNIGHT,
    EnemyType.CLOCKWORK_SPIDER, EnemyType.WINDUP_CHOMPER, EnemyType.CLOCKWORK_COURIER -> EnemyFaction.TIN_ARMY
    EnemyType.PLUSH_BEAR, EnemyType.BODYGUARD_BEAR, EnemyType.RAG_BOMBER,
    EnemyType.BETRAYED_BUNNY, EnemyType.THIEF_GREMLIN, EnemyType.STITCH_MECHANIC -> EnemyFaction.RAG_PLUSH
    EnemyType.CREEPY_DOLL, EnemyType.MIRROR_DOLL, EnemyType.PORCELAIN_CLOWN,
    EnemyType.NEEDLE_MARIONETTE, EnemyType.WEEPING_TOY, EnemyType.JACK_IN_BOX -> EnemyFaction.PORCELAIN
    EnemyType.SHADOW_PUPPET, EnemyType.MIRROR_COPY, EnemyType.MIMIC_CHEST -> EnemyFaction.VOID_SHADOW
    else -> EnemyFaction.VOID_SHADOW
}

