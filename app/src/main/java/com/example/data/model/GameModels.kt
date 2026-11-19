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
    LORE("Sala de Memoria", "❦"),
    LABYRINTH("Laberinto", "⌗"),
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
    TIN_SOLDIER("Soldadito de Plomo", 34f, 75f, false, "Patrulla en guardia y dispara perdigones de latón."),
    CREEPY_DOLL("La Muñeca", 30f, 118f, false, "Permanece inmóvil como adorno hasta que te acercas."),
    PLUSH_BEAR("Osito de Felpa", 92f, 62f, false, "Resistente. Telegrafía embestidas pesadas."),
    JACK_IN_BOX("Jack Sorpresa", 48f, 48f, false, "Emerge de su caja lanzando ráfagas radiales."),
    CLOCKWORK_SPIDER("Araña de Cuerda", 22f, 138f, false, "Muy ágil; se desliza por muros y busca sombra."),
    MIRROR_DOLL("Muñeco Espejo", 54f, 95f, false, "Imita tus movimientos con inquietante precisión."),
    THIEF_GREMLIN("Duende Ladrón", 34f, 134f, false, "Recoge objetos y piezas caídas del suelo para huir con ellos."),
    MIMIC_CHEST("Cofre Imitador", 68f, 90f, false, "Se camufla entre muebles hasta despertar."),
    PORCELAIN_CLOWN("Payaso de Porcelana", 58f, 84f, false, "Ríe y asusta a juguetes cercanos; lanza cascabeles triples."),
    WINDUP_CHOMPER("Dentadura de Cuerda", 24f, 148f, false, "Mandíbula mecánica veloz de vida muy frágil."),
    NEEDLE_MARIONETTE("Marioneta de Agujas", 42f, 88f, false, "Mantiene distancia y dispara agujas cortantes."),
    SHADOW_PUPPET("Sombra Cosida", 40f, 125f, false, "Invisible en penumbra salvo por su silueta hasta ser iluminada."),
    RUSTY_TIN_KNIGHT("Caballero Oxidado", 118f, 54f, false, "Blindado y pesado; resiste balas ligeras pero teme electricidad."),
    RAG_BOMBER("Muñeco Pirotécnico", 46f, 76f, false, "Cargado de pólvora; detona en un estallido al caer."),
    BETRAYED_BUNNY("Conejito Traicionado", 74f, 134f, false, "Antiguo compañero abandonado que regresa por venganza."),
    MIRROR_COPY("Reflejo Invertido", 44f, 112f, false, "Copia nacida de los espejos que imita tu movimiento."),
    WEEPING_TOY("Estatuilla Inmóvil", 64f, 178f, false, "Se congela cuando la miras o la iluminas; ataca por la espalda."),
    TIN_CAPTAIN("Capitán de Plomo", 115f, 68f, false, "Líder táctico: coordina soldaditos; si cae, su escuadrón pierde orden."),
    BODYGUARD_BEAR("Osito Guardaespaldas", 125f, 80f, false, "Protector: intercepta disparos frente a aliados frágiles y enfurece si caen."),
    CLOCKWORK_COURIER("Conejo de Cuerda", 28f, 146f, false, "Cobarde: huye buscando refugio o la campana de alarma."),
    STITCH_MECHANIC("Costurero Reparador", 42f, 92f, false, "Reparador: cura enemigos heridos si no lo detienes."),
    VEILED_SPECTER("Espectro Velado", 76f, 96f, false, "Invulnerable bajo la luz directa; apaga tu fuente de luz para dañarlo."),
    LAMP_SNUFFER("Apagalámparas", 36f, 130f, false, "Saboteador rápido que busca apagar las lámparas de la sala."),
    CLOCKWORK_SENTINEL("Centinela de Cuerda", 175f, 64f, false, "Élite acorazado; vigila zonas clave y busca recargarse en estaciones."),
    BOSS_VENDEDOR("El Vendedor", 520f, 72f, true, "Piso 1: Oso de Cuerda acorazado; sobrecárgalo para exponer su núcleo."),
    BOSS_BOTONERO("El Botonero", 560f, 80f, true, "Piso 1: Maestro de salvas doradas con coraza de latón."),
    BOSS_COSTURERA("La Costurera", 640f, 84f, true, "Piso 2: Blindada en porcelana y acero; rompe su armadura por fases."),
    BOSS_TITIRITERO("El Titiritero", 680f, 76f, true, "Piso 2 y 5: Invulnerable mientras sus hilos estén anclados a marionetas."),
    BOSS_NINERA("La Niñera", 760f, 78f, true, "Piso 3: Vigila la oscuridad del Dormitorio con blindaje de porcelana."),
    BOSS_RELOJERO("El Relojero", 800f, 88f, true, "Piso 3: Circula en rieles horarios; acciona el freno para descarrilarlo."),
    BOSS_MAESTRO("El Maestro de Ceremonias", 860f, 92f, true, "Piso 4 y 5: Se oculta entre cajas sorpresa en el escenario."),
    BOSS_BUHONERO("El Buhonero Sombrío", 900f, 94f, true, "Piso 4: Maestro de trampas y sombras en el Depósito."),
    BOSS_COLECCIONISTA("El Coleccionista", 960f, 86f, true, "Piso 6: Custodia vitrinas del Museo bajo coraza de porcelana."),
    BOSS_SUBSUELO("El Autómata Ahogado", 1040f, 84f, true, "Piso 7: Locomotora oxidada sobre rieles electrificados del Subsuelo."),
    BOSS_MADRE("La Madre de los Juguetes", 1120f, 82f, true, "Piso 8: Despliega hilos y cambia las reglas de Wonderland."),
    BOSS_FABRICANTE("El Fabricante", 1180f, 88f, true, "Piso 8: Creador de Wonderland. Su taller responde a cada fase suya.")
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
    val altBossType: EnemyType? = null,
    val visualProfile: FloorVisualProfile? = null
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
    EnemyType.TIN_CAPTAIN, EnemyType.CLOCKWORK_SENTINEL -> EnemyRole.LEADER
    EnemyType.BODYGUARD_BEAR, EnemyType.PLUSH_BEAR, EnemyType.RUSTY_TIN_KNIGHT -> EnemyRole.PROTECTOR
    EnemyType.CLOCKWORK_COURIER -> EnemyRole.COWARD
    EnemyType.THIEF_GREMLIN -> EnemyRole.COLLECTOR
    EnemyType.STITCH_MECHANIC -> EnemyRole.REPAIRER
    EnemyType.PORCELAIN_CLOWN -> EnemyRole.RIVAL
    else -> EnemyRole.STANDARD
}

fun EnemyType.defaultFaction(): EnemyFaction = when (this) {
    EnemyType.TIN_SOLDIER, EnemyType.TIN_CAPTAIN, EnemyType.RUSTY_TIN_KNIGHT,
    EnemyType.CLOCKWORK_SPIDER, EnemyType.WINDUP_CHOMPER, EnemyType.CLOCKWORK_COURIER,
    EnemyType.CLOCKWORK_SENTINEL -> EnemyFaction.TIN_ARMY
    EnemyType.PLUSH_BEAR, EnemyType.BODYGUARD_BEAR, EnemyType.RAG_BOMBER,
    EnemyType.BETRAYED_BUNNY, EnemyType.THIEF_GREMLIN, EnemyType.STITCH_MECHANIC -> EnemyFaction.RAG_PLUSH
    EnemyType.CREEPY_DOLL, EnemyType.MIRROR_DOLL, EnemyType.PORCELAIN_CLOWN,
    EnemyType.NEEDLE_MARIONETTE, EnemyType.WEEPING_TOY, EnemyType.JACK_IN_BOX -> EnemyFaction.PORCELAIN
    EnemyType.SHADOW_PUPPET, EnemyType.MIRROR_COPY, EnemyType.MIMIC_CHEST,
    EnemyType.VEILED_SPECTER, EnemyType.LAMP_SNUFFER -> EnemyFaction.VOID_SHADOW
    else -> EnemyFaction.VOID_SHADOW
}

fun EnemyType.materialType(): EnemyMaterialType = when (this) {
    EnemyType.CREEPY_DOLL, EnemyType.PORCELAIN_CLOWN, EnemyType.MIRROR_DOLL,
    EnemyType.WEEPING_TOY, EnemyType.BOSS_COSTURERA, EnemyType.BOSS_NINERA,
    EnemyType.BOSS_COLECCIONISTA -> EnemyMaterialType.PORCELAIN
    EnemyType.PLUSH_BEAR, EnemyType.BODYGUARD_BEAR, EnemyType.RAG_BOMBER,
    EnemyType.THIEF_GREMLIN, EnemyType.STITCH_MECHANIC, EnemyType.BETRAYED_BUNNY -> EnemyMaterialType.CLOTH
    EnemyType.TIN_SOLDIER, EnemyType.TIN_CAPTAIN, EnemyType.RUSTY_TIN_KNIGHT,
    EnemyType.BOSS_BOTONERO, EnemyType.BOSS_SUBSUELO -> EnemyMaterialType.METAL
    EnemyType.JACK_IN_BOX, EnemyType.MIMIC_CHEST, EnemyType.BOSS_VENDEDOR -> EnemyMaterialType.WOOD
    EnemyType.LAMP_SNUFFER, EnemyType.SHADOW_PUPPET, EnemyType.VEILED_SPECTER -> EnemyMaterialType.RUBBER
    else -> EnemyMaterialType.MECHANISM
}

fun EnemyType.darknessTrait(): DarknessEnemyTrait = when (this) {
    EnemyType.WEEPING_TOY -> DarknessEnemyTrait.MOVE_ONLY_IN_DARK
    EnemyType.CLOCKWORK_SPIDER, EnemyType.CLOCKWORK_COURIER -> DarknessEnemyTrait.FLEE_LIGHT
    EnemyType.SHADOW_PUPPET, EnemyType.MIRROR_COPY -> DarknessEnemyTrait.SHADOW_INVISIBLE
    EnemyType.CREEPY_DOLL, EnemyType.MIMIC_CHEST -> DarknessEnemyTrait.FURNITURE_CAMOUFLAGE
    EnemyType.LAMP_SNUFFER -> DarknessEnemyTrait.LAMP_EXTINGUISHER
    EnemyType.VEILED_SPECTER -> DarknessEnemyTrait.LIGHT_INVULNERABLE
    else -> DarknessEnemyTrait.NONE
}

fun EnemyType.hpTier(): EnemyHpTier = when {
    isBoss -> EnemyHpTier.BOSS
    maxHp <= 25f -> EnemyHpTier.VERY_WEAK
    maxHp <= 45f -> EnemyHpTier.WEAK
    maxHp <= 80f -> EnemyHpTier.NORMAL
    maxHp <= 140f -> EnemyHpTier.RESISTANT
    maxHp <= 230f -> EnemyHpTier.ELITE
    else -> EnemyHpTier.MINIBOSS
}

fun EnemyType.bossMechanicKind(): BossMechanicKind = when (this) {
    EnemyType.BOSS_VENDEDOR, EnemyType.BOSS_BOTONERO -> BossMechanicKind.WINDUP_SHELL
    EnemyType.BOSS_COSTURERA, EnemyType.BOSS_NINERA, EnemyType.BOSS_COLECCIONISTA -> BossMechanicKind.PORCELAIN_ARMOR
    EnemyType.BOSS_MAESTRO, EnemyType.BOSS_BUHONERO -> BossMechanicKind.SURPRISE_BOXES
    EnemyType.BOSS_TITIRITERO, EnemyType.BOSS_MADRE, EnemyType.BOSS_FABRICANTE -> BossMechanicKind.PUPPET_STRINGS
    EnemyType.BOSS_RELOJERO, EnemyType.BOSS_SUBSUELO -> BossMechanicKind.RAIL_TRAIN
    else -> BossMechanicKind.WINDUP_SHELL
}


