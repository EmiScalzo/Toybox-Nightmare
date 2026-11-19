package com.example.data.model

import androidx.compose.ui.graphics.Color

// =====================================================================
// 1. ECOSISTEMA DE ENEMIGOS, ESTADOS DE ALERTA Y RESISTENCIAS
// =====================================================================

enum class EnemyAlertState(val displayName: String, val badgeGlyph: String, val colorHex: Long) {
    CALM("Calmado", "", 0xFF597354),
    SUSPICIOUS("Sospechoso", "?", 0xFFD98A2B),
    ALERT("Alerta", "!", 0xFFE5A93B),
    COMBAT("Combate", "⚔", 0xFFD13B42),
    PANIC("Pánico", "!!", 0xFF9E64B5)
}

enum class EnemyMaterialType(val displayName: String) {
    PORCELAIN("Porcelana"),
    CLOTH("Tela"),
    METAL("Metal"),
    WOOD("Madera"),
    RUBBER("Goma"),
    MECHANISM("Mecanismo")
}

enum class EnemyHpTier(val displayName: String, val minHp: Float, val maxHp: Float) {
    VERY_WEAK("Muy Débil", 10f, 25f),
    WEAK("Débil", 25f, 45f),
    NORMAL("Normal", 45f, 80f),
    RESISTANT("Resistente", 80f, 140f),
    ELITE("Élite", 140f, 230f),
    MINIBOSS("Minijefe", 250f, 450f),
    BOSS("Jefe", 500f, 1200f)
}

enum class DamageElement(val displayName: String) {
    PHYSICAL_LIGHT("Ligero"),
    BLUNT_HEAVY("Contundente"),
    SHARP("Cortante"),
    EXPLOSIVE("Explosivo"),
    ELECTRIC("Eléctrico"),
    FIRE("Calor / Fuego")
}

enum class EmergentRoomScenario(val displayName: String, val hint: String) {
    STANDARD("Patrulla", ""),
    SLEEPING_DEN("Nido Dormido", "Algunos juguetes duermen; muévete en silencio para emboscar."),
    FACTION_SKIRMISH("Riña de Bandos", "Facciones rivales de Wonderland se enfrentan entre sí."),
    GUARDING_VALUABLE("Zona Custodiada", "Un grupo protege un objetivo valioso en la sala."),
    RECHARGE_STATION("Estación de Cuerda", "Los autómatas heridos buscan recargarse en la estación."),
    TRAPPED_ALLY("Aliado Encerrado", "Un prisionero necesita ayuda entre las líneas enemigas.")
}

enum class DarknessEnemyTrait(val displayName: String) {
    NONE("Común"),
    MOVE_ONLY_IN_DARK("Acechador de Sombra"),
    FLEE_LIGHT("Fotofóbico"),
    SHADOW_INVISIBLE("Sombra Invisible"),
    FURNITURE_CAMOUFLAGE("Camuflaje de Mueblería"),
    LAMP_EXTINGUISHER("Apagalámparas"),
    LIGHT_INVULNERABLE("Espectro Velado")
}

// =====================================================================
// 2. ILUMINACIÓN, OSCURIDAD Y FUENTES DE LUZ
// =====================================================================

enum class RoomLightingMode(
    val displayName: String,
    val baseDarkAlpha: Float,
    val batteryDrainMult: Float
) {
    LIT("Iluminada", 0.22f, 0.25f),
    DIM("Tenue", 0.56f, 0.65f),
    DARK("Oscura", 0.80f, 1.0f),
    FLICKERING("Parpadeante", 0.72f, 0.90f),
    TOTAL_DARKNESS("Oscuridad Total", 0.94f, 1.25f)
}

enum class LightSourceType(
    val displayName: String,
    val radiusScale: Float,
    val maxCharge: Float,
    val drainPerSec: Float,
    val directional: Boolean,
    val revealsHidden: Boolean,
    val description: String
) {
    NONE("Sin Fuente", 0.42f, 0f, 0f, false, false, "Solo visión nocturna tenue."),
    CANDLE("Vela de Cera", 0.72f, 100f, 0f, false, false, "Radio corto, llama cálida de duración infinita."),
    FLASHLIGHT("Linterna de Taller", 1.38f, 100f, 2.8f, true, true, "Haz amplio y potente; consume batería al iluminar."),
    PORTABLE_LAMP("Lámpara Portátil", 1.05f, 100f, 1.1f, false, false, "Luz equilibrada; recarga un poco en salas iluminadas."),
    MECHANICAL_LANTERN("Farol Mecánico", 1.26f, 100f, 1.6f, false, true, "Alto radio circular y revela pistas ocultas en la oscuridad."),
    GLOWING_TOY("Juguete Luminoso", 0.92f, 100f, 0.5f, false, false, "Brillo suave que calma el Miedo de Patch."),
    LUMINOUS_EYE("Ojo Luminoso", 0.95f, 100f, 0f, true, true, "Luz integrada sin batería; apunta hacia donde miras.")
}

data class RoomLampProp(
    val id: Long,
    val x: Float,
    val y: Float,
    val lit: Boolean = true,
    val radius: Float = 240f
)

// =====================================================================
// 3. SISTEMA EMOCIONAL DE PATCH (NIVELES 1-3 Y FUSIONES)
// =====================================================================

enum class PatchEmotion(
    val displayName: String,
    val colorHex: Long,
    val buffsSummary: String,
    val debuffsSummary: String
) {
    NEUTRAL("Estable", 0xFFD5C3A1, "Sin alteraciones emocionales.", "Pulso regular."),
    SADNESS("Tristeza", 0xFF5D7A99, "+Resistencia al Miedo, detecta recuerdos ocultos.", "+Tiempo entre disparos, movimiento y recarga algo más lentos."),
    ANGER("Enojo", 0xFFD13B42, "+Daño, +empuje, disparos rompen cajas débiles.", "-Precisión de disparo y pasos más ruidosos."),
    FEAR("Miedo", 0xFF9E64B5, "+Velocidad, +esquiva, detección temprana de enemigos.", "-Precisión e interrupción breve ocasional al disparar."),
    JOY("Alegría", 0xFFE5A93B, "+Cadencia, +velocidad, +crítico y mejores tratos con juguetes.", "-Resistencia frente al terror intenso."),
    CURIOSITY("Curiosidad", 0xFF63805B, "+Pistas y secretos, +eficiencia con mecanismos y luz.", "Breves distracciones frente a anomalías cercanas."),
    EXHAUSTION("Agotamiento", 0xFF7D746D, "-Consumo de batería y energía mecánica.", "-Velocidad y acciones más lentas.")
}

enum class EmotionFusion(
    val displayName: String,
    val first: PatchEmotion,
    val second: PatchEmotion,
    val colorHex: Long,
    val effectSummary: String
) {
    VENGEANCE(
        "Venganza",
        PatchEmotion.ANGER,
        PatchEmotion.SADNESS,
        0xFFA8202A,
        "Enojo + Tristeza: ++Daño y sangrado de costura, mayor espera entre salvas."
    ),
    PARANOIA(
        "Paranoia",
        PatchEmotion.FEAR,
        PatchEmotion.CURIOSITY,
        0xFF845EC2,
        "Miedo + Curiosidad: ++Detección de enemigos y secretos, +velocidad, visiones falsas fugaces."
    ),
    EUPHORIA(
        "Euforia",
        PatchEmotion.JOY,
        PatchEmotion.ANGER,
        0xFFFF9F1C,
        "Alegría + Enojo: ++Cadencia y velocidad frenética, menor control de puntería."
    )
}

data class EmotionState(
    val primary: PatchEmotion = PatchEmotion.NEUTRAL,
    val level: Int = 1, // 1, 2, 3
    val secondary: PatchEmotion? = null,
    val fusion: EmotionFusion? = null,
    val durationSec: Float = 0f,
    val intensityScore: Float = 0f
)

// =====================================================================
// 4. LABERINTOS, CONTRATOS Y JEFES MULTIFASE
// =====================================================================

enum class LabyrinthType(val displayName: String, val subtitle: String) {
    FACTORY_MAZE("Laberinto de la Fábrica", "Cintas transportadoras, pistones y compuertas de engranaje."),
    DOLLS_MAZE("Laberinto de las Muñecas", "Muñecas guía indican con su mirada el camino verdadero."),
    MIRRORS_MAZE("Laberinto de Espejos", "Reflejos en la pared revelan atajos y trampas ocultas."),
    FORGOTTEN_TOYS_MAZE("Laberinto de Juguetes Olvidados", "Pasillos angostos custodiados por reliquias perdidas.")
}

data class LabyrinthState(
    val type: LabyrinthType,
    val chestsTotal: Int = 2,
    val chestsOpened: Int = 0,
    val hiddenPocketFound: Boolean = false,
    val lostToysTotal: Int = 3,
    val lostToysFound: Int = 0,
    val trapsTriggered: Int = 0,
    val guardianDefeated: Boolean = false,
    val perfectRewardClaimed: Boolean = false,
    val conveyorDirX: Float = 1f,
    val mirrorGateOpened: Boolean = false
) {
    val isPerfectExploration: Boolean
        get() = chestsOpened >= chestsTotal &&
            hiddenPocketFound &&
            lostToysFound >= lostToysTotal &&
            trapsTriggered == 0 &&
            guardianDefeated
}

enum class ContractType(val displayName: String) {
    HUNTER("Cazador"),
    COLLECTOR("Recolector"),
    EXPLORER("Explorador"),
    PROTECTOR("Protector"),
    SURVIVOR("Superviviente"),
    REPAIRER("Reparador"),
    RELIC_SEEKER("Buscador de Reliquias"),
    ELITE_HUNTER("Caza Élites"),
    LAST_COMPANION("El Último Compañero")
}

enum class ContractRestriction(val label: String) {
    NONE("Sin restricción adicional"),
    NO_BOMBS_3_ROOMS("No usar bombas durante 3 salas"),
    NO_DAMAGE_2_COMBATS("Supera 2 combates sin recibir daño"),
    NO_HEART_PICKUPS("No recoger corazones mientras dure el encargo"),
    NO_REPAIR_ON_FLOOR("No reparar en la mesa durante este piso")
}

data class ContractDef(
    val id: String,
    val type: ContractType,
    val title: String,
    val description: String,
    val targetCount: Int,
    val restriction: ContractRestriction = ContractRestriction.NONE,
    val incompatibleIds: List<String> = emptyList(),
    val rewardButtons: Int = 8,
    val rewardDesc: String,
    val isStorySecret: Boolean = false
)

data class ActiveContract(
    val def: ContractDef,
    val progress: Int = 0,
    val restrictionCounter: Int = 0,
    val completed: Boolean = false,
    val failed: Boolean = false
)

enum class BossMechanicKind(val displayName: String, val hint: String) {
    WINDUP_SHELL("Coraza de Cuerda", "Armadura resistente: hazlo sobrecalentar tras su embestida o usa la palanca."),
    PORCELAIN_ARMOR("Porcelana Blindada", "Disparos rápidos rebotan: usa bombas, carga pesada o derriba estanterías."),
    SURPRISE_BOXES("Cajas Sorpresa", "Se oculta entre 3 cajas: observa cuál tiembla y evita las trampas."),
    PUPPET_STRINGS("Hilos del Titiritero", "Inmune mientras los anclajes de hilo estén activos: córtalos primero."),
    RAIL_TRAIN("Locomotora de Carga", "Circula sobre rieles blindado: acciona el freno de vía para descarrilarlo.")
}

data class BossArenaProp(
    val id: Long,
    val kind: String, // "surprise_box", "puppet_anchor", "rail_switch", "overheat_vent"
    val x: Float,
    val y: Float,
    val isReal: Boolean = false,
    val hp: Float = 30f,
    val active: Boolean = true,
    val shakePhase: Float = 0f
)

enum class FloorArchitecturalStyle(val displayName: String) {
    TOY_SHOP("Sala de Exhibición"),
    FACTORY("Fábrica Industrial"),
    BEDROOM("Dormitorio Gigante"),
    DEPOT("Depósito y Estantes"),
    THEATER("Teatro de Marionetas"),
    MUSEUM("Museo de Vitrinas"),
    SUBSOIL("Subsuelo Inundado"),
    WORKSHOP("Taller del Fabricante")
}

data class FloorVisualProfile(
    val style: FloorArchitecturalStyle,
    val floorPaletteHexes: List<Long>,
    val wallColorHex: Long,
    val trimColorHex: Long,
    val subRoomNames: List<String>,
    val darknessBias: Float,
    val exclusiveMechanicHint: String
)
