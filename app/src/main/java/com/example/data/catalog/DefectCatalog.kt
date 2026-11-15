package com.example.data.catalog

import com.example.data.model.DefectCategory
import com.example.data.model.DefectDef
import com.example.data.model.DefectEffects
import com.example.data.model.Difficulty
import kotlin.random.Random

object DefectCatalog {

    val allDefects: List<DefectDef> = listOf(
        // Percepción (1-5)
        DefectDef(
            id = "button_eye",
            name = "Ojo de botón",
            category = DefectCategory.PERCEPTION,
            severity = 2,
            challengePoints = 2,
            description = "Campo visual reducido hacia el lado izquierdo; los enemigos allí tardan en revelarse.",
            upsideText = "Mayor calma frente a destellos directos.",
            incompatibleIds = listOf("one_eye_only"),
            effects = DefectEffects(leftBlindness = true)
        ),
        DefectDef(
            id = "myopic",
            name = "Miope",
            category = DefectCategory.PERCEPTION,
            severity = 1,
            challengePoints = 1,
            description = "Los elementos lejanos se ven envueltos en penumbra.",
            upsideText = "Mayor nitidez en amenazas cercanas.",
            incompatibleIds = listOf("blurry_vision"),
            effects = DefectEffects(visionScale = 0.75f)
        ),
        DefectDef(
            id = "blurry_vision",
            name = "Visión borrosa",
            category = DefectCategory.PERCEPTION,
            severity = 2,
            challengePoints = 2,
            description = "Pulsos de distorsión visual nublan el borde de la sala.",
            upsideText = "Los proyectiles conservan su brillo para una lectura justa.",
            incompatibleIds = listOf("myopic"),
            effects = DefectEffects(blurPulses = true)
        ),
        DefectDef(
            id = "one_eye_only",
            name = "Un solo ojo funcional",
            category = DefectCategory.PERCEPTION,
            severity = 2,
            challengePoints = 2,
            description = "Menor percepción periférica en los costados.",
            upsideText = "+18% de precisión frontal al disparar.",
            incompatibleIds = listOf("button_eye"),
            effects = DefectEffects(visionScale = 0.85f, frontalPrecisionBonus = 0.18f)
        ),
        DefectDef(
            id = "light_sensitivity",
            name = "Sensibilidad a la luz",
            category = DefectCategory.PERCEPTION,
            severity = 1,
            challengePoints = 1,
            description = "Las salas iluminadas producen destellos breves al entrar.",
            upsideText = "En zonas oscuras percibes un 15% mejor.",
            effects = DefectEffects(lightSensitivity = true)
        ),

        // Audición y estímulos (6-9)
        DefectDef(
            id = "deaf",
            name = "Sordo",
            category = DefectCategory.HEARING,
            severity = 2,
            challengePoints = 2,
            description = "No recibes señales sonoras de enemigos fuera de tu foco.",
            upsideText = "Inmune a la desorientación por ruido fuerte.",
            incompatibleIds = listOf("noise_sensitivity", "selective_hearing"),
            effects = DefectEffects(deafOffscreen = true)
        ),
        DefectDef(
            id = "selective_hearing",
            name = "Audición selectiva",
            category = DefectCategory.HEARING,
            severity = 1,
            challengePoints = 1,
            description = "Algunos sonidos ambientales ocultan alertas sonoras.",
            upsideText = "Las señales visuales de peligro resaltan más.",
            incompatibleIds = listOf("deaf"),
            effects = DefectEffects(selectiveAudio = true)
        ),
        DefectDef(
            id = "noise_sensitivity",
            name = "Sensibilidad al ruido",
            category = DefectCategory.HEARING,
            severity = 2,
            challengePoints = 2,
            description = "Las explosiones causan breve desorientación.",
            upsideText = "Los ataques silenciosos nunca te aturden.",
            incompatibleIds = listOf("deaf"),
            effects = DefectEffects(noiseDisorient = true)
        ),
        DefectDef(
            id = "late_reaction",
            name = "Reacción tardía",
            category = DefectCategory.HEARING,
            severity = 2,
            challengePoints = 2,
            description = "Las alertas de ataque aparecen con un ligero retraso.",
            upsideText = "Aprender las animaciones enemigas otorga esquiva crítica.",
            effects = DefectEffects(delayedTelegraph = true)
        ),

        // Movimiento (10-14)
        DefectDef(
            id = "limping",
            name = "Cojo",
            category = DefectCategory.MOVEMENT,
            severity = 2,
            challengePoints = 2,
            description = "Velocidad de desplazamiento reducida en un 16%.",
            upsideText = "Pisadas más firmes ante retrocesos.",
            incompatibleIds = listOf("uneven_leg", "sparse_stuffing"),
            effects = DefectEffects(speedMult = 0.84f)
        ),
        DefectDef(
            id = "uneven_leg",
            name = "Una pierna más corta",
            category = DefectCategory.MOVEMENT,
            severity = 2,
            challengePoints = 2,
            description = "Desplazamiento ligeramente oscilante y esquiva más corta.",
            upsideText = "Perfil bajo al desplazarse.",
            incompatibleIds = listOf("limping", "faulty_spring"),
            effects = DefectEffects(irregularWalk = true, dodgeDistanceMult = 0.78f)
        ),
        DefectDef(
            id = "stiff_joints",
            name = "Articulaciones rígidas",
            category = DefectCategory.MOVEMENT,
            severity = 1,
            challengePoints = 1,
            description = "El arranque inicial al moverte es más lento.",
            upsideText = "Conservas la velocidad máxima intacta.",
            effects = DefectEffects(stiffAccel = true)
        ),
        DefectDef(
            id = "wooden_feet",
            name = "Pies de madera",
            category = DefectCategory.MOVEMENT,
            severity = 1,
            challengePoints = 1,
            description = "Tus pasos resuenan y alertan antes a los juguetes.",
            upsideText = "Las trampas de suelo causan menos desgaste.",
            effects = DefectEffects(noisySteps = true)
        ),
        DefectDef(
            id = "faulty_spring",
            name = "Resorte defectuoso",
            category = DefectCategory.MOVEMENT,
            severity = 2,
            challengePoints = 2,
            description = "La esquiva tarda un 35% más en recargarse.",
            upsideText = "Al esquivar liberas una pequeña onda de impacto.",
            incompatibleIds = listOf("uneven_leg"),
            effects = DefectEffects(dodgeCooldownMult = 1.35f, dodgeShockwave = true)
        ),

        // Manipulación y combate (15-18)
        DefectDef(
            id = "short_arm",
            name = "Brazo corto",
            category = DefectCategory.COMBAT,
            severity = 1,
            challengePoints = 1,
            description = "Menor alcance efectivo de tus disparos.",
            upsideText = "+22% de daño a corta distancia.",
            effects = DefectEffects(shortRangeBonusDamage = true)
        ),
        DefectDef(
            id = "shaky_hand",
            name = "Mano temblorosa",
            category = DefectCategory.COMBAT,
            severity = 2,
            challengePoints = 2,
            description = "El apuntado manual sufre una ligera dispersión.",
            upsideText = "Más fácil impactar objetivos en movimiento rápido.",
            effects = DefectEffects(spreadPenalty = 0.14f)
        ),
        DefectDef(
            id = "sticky_fingers",
            name = "Dedos pegados",
            category = DefectCategory.COMBAT,
            severity = 1,
            challengePoints = 1,
            description = "Activar consumibles tiene una pequeña recarga extra.",
            upsideText = "Nunca sueltas accidentalmente piezas recuperadas.",
            effects = DefectEffects(slowConsumable = true)
        ),
        DefectDef(
            id = "weak_arm",
            name = "Brazo débil",
            category = DefectCategory.COMBAT,
            severity = 2,
            challengePoints = 2,
            description = "Los proyectiles pesados pierden un 18% de fuerza.",
            upsideText = "+15% de cadencia en disparos rápidos.",
            effects = DefectEffects(weakHeavyFastLight = true)
        ),

        // Personalidad y comportamiento (19-22)
        DefectDef(
            id = "rigid_routines",
            name = "Rutinas rígidas",
            category = DefectCategory.PERSONALITY,
            severity = 1,
            challengePoints = 1,
            description = "Alternar disparos sin ritmo reduce levemente el daño.",
            upsideText = "Mantener cadencia constante otorga +12% de daño.",
            effects = DefectEffects(patternBonus = true)
        ),
        DefectDef(
            id = "sensory_hypersensitivity",
            name = "Hipersensibilidad sensorial",
            category = DefectCategory.PERSONALITY,
            severity = 2,
            challengePoints = 2,
            description = "Los golpes cercanos causan un destello breve de distracción.",
            upsideText = "Detectas anomalías y salas secretas con más facilidad.",
            effects = DefectEffects(sensoryDistract = true)
        ),
        DefectDef(
            id = "mechanical_impulsivity",
            name = "Impulsividad mecánica",
            category = DefectCategory.PERSONALITY,
            severity = 2,
            challengePoints = 2,
            description = "Tras pausar el fuego, el primer disparo demora un instante.",
            upsideText = "Luego ganas ráfaga rápida durante 2 segundos.",
            effects = DefectEffects(impulseDelayThenBurst = true)
        ),
        DefectDef(
            id = "faulty_memory",
            name = "Memoria defectuosa",
            category = DefectCategory.PERSONALITY,
            severity = 1,
            challengePoints = 1,
            description = "El minimapa oculta el tipo de las salas ya visitadas.",
            upsideText = "Todos los descubrimientos siguen guardándose en tu Álbum.",
            effects = DefectEffects(fogForgetfulness = true)
        ),

        // Estructura y fabricación (23-24)
        DefectDef(
            id = "weak_seams",
            name = "Costuras débiles",
            category = DefectCategory.STRUCTURE,
            severity = 3,
            challengePoints = 3,
            description = "El desgaste corporal avanza un 40% más rápido al recibir daño.",
            upsideText = "Las reparaciones recuperan un 35% más de durabilidad.",
            incompatibleIds = listOf("sparse_stuffing"),
            effects = DefectEffects(wearRateMult = 1.4f, repairBonusMult = 1.35f)
        ),
        DefectDef(
            id = "sparse_stuffing",
            name = "Relleno escaso",
            category = DefectCategory.STRUCTURE,
            severity = 3,
            challengePoints = 3,
            description = "Comienzas con 1 corazón máximo menos (mínimo 2).",
            upsideText = "+12% de velocidad de desplazamiento por ligereza.",
            incompatibleIds = listOf("weak_seams", "limping"),
            effects = DefectEffects(fragileBody = true, speedMult = 1.12f)
        )
    )

    fun byId(id: String): DefectDef? = allDefects.find { it.id == id }

    fun isCompatible(selected: List<DefectDef>, candidate: DefectDef): Boolean {
        if (selected.any { it.id == candidate.id }) return false
        return selected.none { existing ->
            existing.incompatibleIds.contains(candidate.id) ||
                candidate.incompatibleIds.contains(existing.id)
        }
    }

    fun generateRandomForDifficulty(
        difficulty: Difficulty,
        preferredIds: List<String> = emptyList(),
        rng: Random = Random.Default
    ): List<DefectDef> {
        val count = if (difficulty.minDefects == difficulty.maxDefects) {
            difficulty.minDefects
        } else {
            rng.nextInt(difficulty.minDefects, difficulty.maxDefects + 1)
        }
        if (count <= 0) return emptyList()

        val result = mutableListOf<DefectDef>()
        val pool = allDefects.shuffled(rng).toMutableList()
        // Prioritize one suggested defect occasionally for flavor
        val preferred = preferredIds.mapNotNull { byId(it) }.shuffled(rng)
        if (preferred.isNotEmpty() && rng.nextFloat() < 0.45f) {
            val pick = preferred.first()
            if (pick.severity <= if (difficulty == Difficulty.SUAVE) 2 else 3) {
                result.add(pick)
            }
        }
        for (candidate in pool) {
            if (result.size >= count) break
            if (difficulty == Difficulty.SUAVE && candidate.severity >= 3) continue
            if (isCompatible(result, candidate)) {
                result.add(candidate)
            }
        }
        return result
    }
}
