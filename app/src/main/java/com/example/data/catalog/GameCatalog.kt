package com.example.data.catalog

import com.example.data.model.*

object GameCatalog {

    val bodyParts: List<BodyPartDef> = listOf(
        // CABEZA
        BodyPartDef(
            id = "burlap_head",
            name = "Cabeza de Arpillera",
            zone = BodyZone.HEAD,
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "repair"),
            effects = PartEffects(maxHeartsBonus = 0, accuracyBonus = 0f),
            intactDesc = "Equilibrio básico y costuras estables.",
            damagedDesc = "Pierde algodón con cada impacto.",
            criticalDesc = "Riesgo de desprendimiento al recibir daño.",
            lore = "La cabeza original cosida en la mesa tres de Wonderland."
        ),
        BodyPartDef(
            id = "porcelain_head",
            name = "Cabeza de Porcelana",
            zone = BodyZone.HEAD,
            rarity = Rarity.UNCOMMON,
            tags = listOf("porcelain", "precision"),
            effects = PartEffects(damageBonus = 2.5f, accuracyBonus = 0.08f),
            intactDesc = "+2.5 daño base y mirada imperturbable.",
            damagedDesc = "Grietas finas reducen la bonificación a la mitad.",
            criticalDesc = "Porcelana astillada; sin bono de daño.",
            lore = "Fría al tacto, conserva una sonrisa pintada a mano."
        ),
        BodyPartDef(
            id = "clown_head",
            name = "Cabeza de Payaso",
            zone = BodyZone.HEAD,
            rarity = Rarity.RARE,
            tags = listOf("explosive", "chaotic"),
            effects = PartEffects(damageBonus = 1.5f, moveSpeedMult = 1.05f),
            intactDesc = "+1.5 daño y paso burlón (+5% velocidad).",
            damagedDesc = "Cascabel roto; pierde el bono de velocidad.",
            criticalDesc = "Rostro desfigurado; puede soltar chispas erráticas.",
            lore = "Nadie recuerda si reía antes o después de cerrar la tienda."
        ),
        BodyPartDef(
            id = "clockwork_head",
            name = "Cabeza Mecánica",
            zone = BodyZone.HEAD,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "precision"),
            effects = PartEffects(fireRateMult = 1.15f, armorChance = 0.1f),
            intactDesc = "+15% cadencia y 10% probabilidad de bloquear desgaste.",
            damagedDesc = "Engranajes flojos; +5% cadencia.",
            criticalDesc = "Mecanismo trabado; pierde protección.",
            lore = "Tictaquea suavemente en lugar de respirar."
        ),

        // OJOS
        BodyPartDef(
            id = "glass_eye",
            name = "Ojo de Vidrio",
            zone = BodyZone.EYES,
            rarity = Rarity.UNCOMMON,
            tags = listOf("precision", "homing", "bounce"),
            effects = PartEffects(accuracyBonus = 0.12f, visionRangeMult = 1.15f),
            intactDesc = "+12% precisión y mayor alcance de linterna.",
            damagedDesc = "Vidrio empañado; reduce alcance de visión.",
            criticalDesc = "Grieta central; pierde la bonificación de visión.",
            lore = "Refleja cosas que no están en la habitación."
        ),
        BodyPartDef(
            id = "button_eye_part",
            name = "Ojo de Botón Cosido",
            zone = BodyZone.EYES,
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "repair"),
            effects = PartEffects(visionRangeMult = 1.0f),
            intactDesc = "Visión modesta pero costura resistente.",
            damagedDesc = "Hilo flojo; campo levemente reducido.",
            criticalDesc = "A punto de soltarse del rostro.",
            lore = "Cuatro agujeros oscuros que miran hacia adentro."
        ),
        BodyPartDef(
            id = "mechanical_eye",
            name = "Ojo Mecánico",
            zone = BodyZone.EYES,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "homing", "precision"),
            effects = PartEffects(accuracyBonus = 0.2f, fireRateMult = 1.1f),
            intactDesc = "+20% precisión y calibración rápida (+10% cadencia).",
            damagedDesc = "Lente desalineada; +8% precisión.",
            criticalDesc = "Obturador dañado; pierde calibración.",
            lore = "Un diafragma de latón que ajusta la mira automáticamente."
        ),
        BodyPartDef(
            id = "crimson_eye",
            name = "Ojo Rojo",
            zone = BodyZone.EYES,
            rarity = Rarity.CURSED,
            tags = listOf("sound", "cursed", "damage"),
            effects = PartEffects(damageBonus = 3.5f, visionRangeMult = 0.92f),
            intactDesc = "+3.5 daño a costa de una visión algo sombría.",
            damagedDesc = "El brillo carmesí parpadea (+1.5 daño).",
            criticalDesc = "Casi apagado; atrae miradas hostiles.",
            lore = "Extraído de un muñeco que jamás debía abrir los párpados."
        ),

        // BRAZOS
        BodyPartDef(
            id = "cloth_arm",
            name = "Brazo de Trapo",
            zone = BodyZone.ARMS,
            rarity = Rarity.COMMON,
            tags = listOf("projectile", "cloth"),
            effects = PartEffects(attackStyle = AttackStyle.BUTTON_SHOT),
            intactDesc = "Dispara botones afilados de forma estable.",
            damagedDesc = "Cadencia ligeramente reducida.",
            criticalDesc = "Disparo errático por costuras abiertas.",
            lore = "Un brazo un poco más largo que el otro, cosido con prisa."
        ),
        BodyPartDef(
            id = "spring_arm",
            name = "Brazo de Resorte",
            zone = BodyZone.ARMS,
            rarity = Rarity.UNCOMMON,
            tags = listOf("mechanical", "projectile", "spring", "bounce"),
            effects = PartEffects(attackStyle = AttackStyle.GEAR_PROJECTILE, fireRateMult = 1.25f, damageBonus = 1f),
            intactDesc = "Dispara engranajes rápidos (+25% cadencia).",
            damagedDesc = "Resorte fatigado; disparos más lentos.",
            criticalDesc = "Los engranajes pueden atascarse brevemente.",
            lore = "Tensa un muelle industrial en cada lanzamiento."
        ),
        BodyPartDef(
            id = "scissor_arm",
            name = "Brazo de Tijera",
            zone = BodyZone.ARMS,
            rarity = Rarity.RARE,
            tags = listOf("sharp", "projectile", "homing"),
            effects = PartEffects(attackStyle = AttackStyle.SCISSOR_SLASH, damageBonus = 4f, fireRateMult = 0.95f),
            intactDesc = "Lanza hojas cortantes de alto daño (+4 daño).",
            damagedDesc = "Filo mellado (+2 daño).",
            criticalDesc = "Tijera floja; corto alcance y menor daño.",
            lore = "Las tijeras de sastre de La Costurera."
        ),
        BodyPartDef(
            id = "explosive_arm",
            name = "Brazo Explosivo",
            zone = BodyZone.ARMS,
            rarity = Rarity.RARE,
            tags = listOf("explosive", "duplication"),
            effects = PartEffects(attackStyle = AttackStyle.EXPLOSIVE_TOY, damageBonus = 3.5f, fireRateMult = 0.72f),
            intactDesc = "Dispara juguetes detonantes en área.",
            damagedDesc = "Mecha húmeda; explosión menor.",
            criticalDesc = "Cadencia muy lenta; riesgo de fallo.",
            lore = "Lleno de pólvora húmeda de fuegos artificiales."
        ),
        BodyPartDef(
            id = "doll_arm",
            name = "Brazo de Muñeco",
            zone = BodyZone.ARMS,
            rarity = Rarity.UNCOMMON,
            tags = listOf("duplication", "projectile"),
            effects = PartEffects(attackStyle = AttackStyle.DOLL_BOLT, fireRateMult = 1.18f),
            intactDesc = "Proyectiles ligeros y articulados (+18% cadencia).",
            damagedDesc = "Articulación floja; cadencia normal.",
            criticalDesc = "Dispara proyectiles débiles.",
            lore = "Articulado con alambre fino y pintura agrietada."
        ),

        // TORSO
        BodyPartDef(
            id = "zipper_torso",
            name = "Torso con Cremallera",
            zone = BodyZone.TORSO,
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "repair"),
            effects = PartEffects(maxHeartsBonus = 0),
            intactDesc = "Almacena relleno estable y resiste el uso diario.",
            damagedDesc = "La cremallera se entreabre dejando escapar algodón.",
            criticalDesc = "Relleno expuesto; mayor riesgo de desprendimiento.",
            lore = "Una cremallera metálica recorre su pecho."
        ),
        BodyPartDef(
            id = "musical_torso",
            name = "Torso Musical",
            zone = BodyZone.TORSO,
            rarity = Rarity.RARE,
            tags = listOf("sound", "stun"),
            effects = PartEffects(periodicSoundWave = true, maxHeartsBonus = 1),
            intactDesc = "Emite una onda sonora periódica que daña y frena enemigos (+1 corazón).",
            damagedDesc = "La onda tiene menor alcance.",
            criticalDesc = "Emite notas disonantes erráticas.",
            lore = "Una caja de música de cuerda late dentro de su pecho."
        ),
        BodyPartDef(
            id = "armored_torso",
            name = "Torso Blindado",
            zone = BodyZone.TORSO,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "armor"),
            effects = PartEffects(maxHeartsBonus = 1, armorChance = 0.25f, moveSpeedMult = 0.94f),
            intactDesc = "+1 corazón y 25% de resistir desgaste corporal.",
            damagedDesc = "Hojalata abollada; 12% resistencia.",
            criticalDesc = "Placas sueltas; sin protección adicional.",
            lore = "Forjado con latas de galletas y placas de locomotora."
        ),
        BodyPartDef(
            id = "clock_torso",
            name = "Torso de Reloj",
            zone = BodyZone.TORSO,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "duplication", "clock"),
            effects = PartEffects(fireRateMult = 1.12f, dodgeCooldownMult = 0.9f),
            intactDesc = "+12% cadencia y -10% recarga de esquiva.",
            damagedDesc = "Péndulo torcido; +5% cadencia.",
            criticalDesc = "Cuerda destensada; pierde las mejoras.",
            lore = "Marca las horas muertas desde 1954."
        ),

        // PIERNAS
        BodyPartDef(
            id = "stitched_legs",
            name = "Piernas Cosidas",
            zone = BodyZone.LEGS,
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "dodge"),
            effects = PartEffects(moveSpeedMult = 1.0f),
            intactDesc = "Paso silencioso y esquiva equilibrada.",
            damagedDesc = "Cojera leve (-8% velocidad).",
            criticalDesc = "Costura abierta (-18% velocidad).",
            lore = "Un poco desiguales, pero obstinadas."
        ),
        BodyPartDef(
            id = "spring_legs",
            name = "Piernas de Resorte",
            zone = BodyZone.LEGS,
            rarity = Rarity.UNCOMMON,
            tags = listOf("spring", "dodge", "explosive"),
            effects = PartEffects(dodgeLengthMult = 1.32f, moveSpeedMult = 1.08f),
            intactDesc = "Esquiva un 32% más larga y +8% velocidad.",
            damagedDesc = "Esquiva más corta (+10%).",
            criticalDesc = "Mayor tiempo de recuperación al esquivar.",
            lore = "Muelles helicoidales que zumban al impulsarse."
        ),
        BodyPartDef(
            id = "wheel_legs",
            name = "Piernas de Ruedas",
            zone = BodyZone.LEGS,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "speed"),
            effects = PartEffects(moveSpeedMult = 1.22f, dodgeCooldownMult = 0.88f),
            intactDesc = "+22% velocidad y esquiva más ágil.",
            damagedDesc = "Eje oxidado (+10% velocidad).",
            criticalDesc = "Rueda trabada (-5% velocidad).",
            lore = "Patines de metal de un viejo autómata mensajero."
        ),
        BodyPartDef(
            id = "spider_legs",
            name = "Piernas de Araña",
            zone = BodyZone.LEGS,
            rarity = Rarity.CURSED,
            tags = listOf("sharp", "dodge", "speed"),
            effects = PartEffects(moveSpeedMult = 1.15f, dodgeCooldownMult = 0.8f),
            intactDesc = "Esquiva un 20% más rápida y gran agilidad.",
            damagedDesc = "Patas quebradas; agilidad estándar.",
            criticalDesc = "Movimiento errático y lento.",
            lore = "Seis patas articuladas que se mueven incluso cuando duermes."
        ),

        // ESPALDA
        BodyPartDef(
            id = "cotton_back",
            name = "Espalda Remendada",
            zone = BodyZone.BACK,
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "repair"),
            effects = PartEffects(armorChance = 0.05f),
            intactDesc = "Amortigua ligeramente el daño por la espalda.",
            damagedDesc = "Tela rasgada.",
            criticalDesc = "Sin amortiguación.",
            lore = "Múltiples parches cosidos uno sobre otro."
        ),
        BodyPartDef(
            id = "music_box_back",
            name = "Caja Musical",
            zone = BodyZone.BACK,
            rarity = Rarity.RARE,
            tags = listOf("sound", "stun"),
            effects = PartEffects(periodicSoundWave = true),
            intactDesc = "Cada pocos segundos emite una melodía que ralentiza enemigos.",
            damagedDesc = "Melodía intermitente.",
            criticalDesc = "Solo cruje suavemente.",
            lore = "Su manivela gira sola cuando hay peligro cerca."
        ),
        BodyPartDef(
            id = "paper_wings",
            name = "Alas de Papel",
            zone = BodyZone.BACK,
            rarity = Rarity.UNCOMMON,
            tags = listOf("dodge", "speed"),
            effects = PartEffects(moveSpeedMult = 1.12f, dodgeLengthMult = 1.18f),
            intactDesc = "+12% velocidad y esquiva más ligera.",
            damagedDesc = "Papel rasgado (+5% velocidad).",
            criticalDesc = "Alas empapadas; sin bonificación.",
            lore = "Plegadas con páginas arrancadas del catálogo de 1952."
        ),
        BodyPartDef(
            id = "windup_key_back",
            name = "Mecanismo de Cuerda",
            zone = BodyZone.BACK,
            rarity = Rarity.RARE,
            tags = listOf("mechanical", "clock"),
            effects = PartEffects(fireRateMult = 1.16f, moveSpeedMult = 1.06f),
            intactDesc = "+16% cadencia de disparo mientras gira.",
            damagedDesc = "Giro lento (+6% cadencia).",
            criticalDesc = "Llave atascada.",
            lore = "Una enorme llave dorada sobresale entre los omóplatos."
        )
    )

    val passiveItems: List<PassiveItemDef> = listOf(
        PassiveItemDef(
            id = "eyeless_bear",
            name = "Oso sin Ojos",
            rarity = Rarity.UNCOMMON,
            tags = listOf("stealth", "plush"),
            description = "Los enemigos tardan un 30% más en detectarte.",
            memoryFragmentId = "mem_nursery",
            lore = "Un oso abrazado por alguien que no quería ser encontrado.",
            effects = ItemEffects(enemyDetectionMult = 0.7f)
        ),
        PassiveItemDef(
            id = "music_box_item",
            name = "Cajita de Melodía",
            rarity = Rarity.RARE,
            tags = listOf("sound", "stun"),
            description = "Emite una onda cada 6s que aturde brevemente a los enemigos cercanos.",
            memoryFragmentId = "mem_founder",
            lore = "Toca una canción de cuna que congela los mecanismos.",
            effects = ItemEffects(periodicStunWave = true)
        ),
        PassiveItemDef(
            id = "rusty_key",
            name = "Llave Oxidada",
            rarity = Rarity.COMMON,
            tags = listOf("secret", "key"),
            description = "Aumenta la probabilidad de revelar salas secretas y tesoros.",
            lore = "Abre cerraduras que la juguetería intentó olvidar.",
            effects = ItemEffects(secretRoomChanceBonus = 0.25f)
        ),
        PassiveItemDef(
            id = "porcelain_tooth",
            name = "Diente de Porcelana",
            rarity = Rarity.UNCOMMON,
            tags = listOf("porcelain", "damage"),
            description = "+3.5 de daño por disparo, pero -8% de velocidad.",
            lore = "Arrancado de un muñeco ventrílocuo en el escaparate.",
            effects = ItemEffects(damageBonus = 3.5f, speedMult = 0.92f)
        ),
        PassiveItemDef(
            id = "handless_watch",
            name = "Reloj sin Agujas",
            rarity = Rarity.RARE,
            tags = listOf("clock", "precision"),
            description = "Ralentiza los proyectiles enemigos en un 25%.",
            memoryFragmentId = "mem_clock",
            lore = "Aquí dentro el tiempo no avanza, solo da vueltas.",
            effects = ItemEffects(slowEnemyBulletsMult = 0.75f)
        ),
        PassiveItemDef(
            id = "broken_doll",
            name = "Muñeca Rota",
            rarity = Rarity.RARE,
            tags = listOf("duplication", "decoy"),
            description = "Al recibir daño dejas un señuelo de trapo que distrae enemigos.",
            lore = "Alguien siempre ocupa tu lugar cuando golpean.",
            effects = ItemEffects(spawnDecoyOnHit = true)
        ),
        PassiveItemDef(
            id = "cursed_button",
            name = "Botón Maldito",
            rarity = Rarity.CURSED,
            tags = listOf("cursed", "economy"),
            description = "Duplica los botones obtenidos, pero vuelve más agresivos a los juguetes.",
            lore = "Brilla como oro viejo entre el polvo.",
            effects = ItemEffects(bonusDropsAndAggro = true, damageBonus = 1.5f)
        ),
        PassiveItemDef(
            id = "red_thread",
            name = "Hilo Rojo",
            rarity = Rarity.RARE,
            tags = listOf("repair", "cloth"),
            description = "Recupera automáticamente una pieza desprendida al limpiar cada sala.",
            memoryFragmentId = "mem_seamstress",
            lore = "El hilo siempre encuentra el camino de vuelta a su costura.",
            effects = ItemEffects(autoRecoverPartPerRoom = true)
        ),
        PassiveItemDef(
            id = "needle_cushion",
            name = "Alfiletero de Terciopelo",
            rarity = Rarity.UNCOMMON,
            tags = listOf("sharp", "pierce"),
            description = "Tus proyectiles atraviesan al primer enemigo (+15% cadencia).",
            lore = "Cada alfiler conserva una gota seca de barniz.",
            effects = ItemEffects(pierceBullets = true, fireRateMult = 1.15f)
        ),
        PassiveItemDef(
            id = "rubber_marble",
            name = "Canica Saltarina",
            rarity = Rarity.UNCOMMON,
            tags = listOf("bounce", "spring"),
            description = "Tus disparos rebotan una vez en los muros de madera.",
            lore = "El juguete favorito de los pasillos traseros.",
            effects = ItemEffects(bounceBullets = true)
        ),
        PassiveItemDef(
            id = "stuffed_heart",
            name = "Corazón de Felpa",
            rarity = Rarity.COMMON,
            tags = listOf("cloth", "health"),
            description = "+1 corazón máximo y +10% velocidad de disparo.",
            lore = "Tibio aún, como si alguien acabara de coserlo.",
            effects = ItemEffects(maxHeartsBonus = 1, fireRateMult = 1.1f)
        ),
        PassiveItemDef(
            id = "magnet_coil",
            name = "Bobina Imantada",
            rarity = Rarity.RARE,
            tags = listOf("homing", "mechanical"),
            description = "Los proyectiles curvan su trayectoria hacia el juguete más cercano.",
            lore = "Atrae todo lo metálico... y lo que tiene clavos dentro.",
            effects = ItemEffects(homingBullets = true)
        ),
        PassiveItemDef(
            id = "brass_thimble",
            name = "Dedal de Latón",
            rarity = Rarity.UNCOMMON,
            tags = listOf("armor", "repair"),
            description = "+20% probabilidad de evitar desgaste corporal y +1 de daño.",
            lore = "Bruñido por mil horas de costura nocturna.",
            effects = ItemEffects(armorWearReduction = 0.20f, damageBonus = 1f)
        ),
        PassiveItemDef(
            id = "velvet_ribbon",
            name = "Cinta de Terciopelo",
            rarity = Rarity.UNCOMMON,
            tags = listOf("dodge", "stealth"),
            description = "Esquiva un 20% más larga y -15% de tiempo de recarga.",
            lore = "Envuelve los tobillos sin hacer el menor ruido al rozar la madera.",
            effects = ItemEffects(dodgeLengthMult = 1.20f, dodgeCooldownMult = 0.85f)
        ),
        PassiveItemDef(
            id = "broken_music_key",
            name = "Llave de Cuerda Rota",
            rarity = Rarity.RARE,
            tags = listOf("clock", "sound", "speed"),
            description = "+22% cadencia de disparo y +10% velocidad de movimiento.",
            lore = "Gira frenéticamente cuando detecta latidos cerca.",
            effects = ItemEffects(fireRateMult = 1.22f, speedMult = 1.10f)
        ),
        PassiveItemDef(
            id = "silver_scissors",
            name = "Tijeras de Plata",
            rarity = Rarity.RARE,
            tags = listOf("sharp", "damage"),
            description = "+3.5 de daño y 15% de probabilidad de golpe crítico (x1.75).",
            lore = "Cortan el algodón y el barniz con la misma frialdad.",
            effects = ItemEffects(damageBonus = 3.5f, critChance = 0.15f)
        ),
        PassiveItemDef(
            id = "marionette_cross",
            name = "Cruz de Marioneta",
            rarity = Rarity.RARE,
            tags = listOf("decoy", "duplication", "cursed"),
            description = "Al entrar en cada sala creas un muñeco señuelo que distrae a los enemigos.",
            memoryFragmentId = "mem_theater",
            lore = "Los hilos aún recuerdan la mano del Titiritero.",
            effects = ItemEffects(spawnDecoyOnRoomEnter = true)
        ),
        PassiveItemDef(
            id = "glass_marble_bag",
            name = "Bolsa de Canicas",
            rarity = Rarity.UNCOMMON,
            tags = listOf("bounce", "porcelain"),
            description = "Tus disparos ganan +2 rebotes adicionales contra las paredes.",
            lore = "Tintinean dentro del bolso como dientes sueltos.",
            effects = ItemEffects(bounceBullets = true, extraBounces = 2)
        ),
        PassiveItemDef(
            id = "moth_lantern",
            name = "Linterna de Polilla",
            rarity = Rarity.UNCOMMON,
            tags = listOf("vision", "precision", "key"),
            description = "Amplía tu halo de luz un 25% y revela pistas de salas secretas.",
            memoryFragmentId = "mem_attic",
            lore = "Una pequeña polilla de fieltro duerme detrás del cristal ámbar.",
            effects = ItemEffects(visionBoost = 1.25f, secretRoomChanceBonus = 0.20f)
        ),
        PassiveItemDef(
            id = "bloody_pin",
            name = "Alfiler Carmesí",
            rarity = Rarity.CURSED,
            tags = listOf("cursed", "sharp"),
            description = "+4.5 de daño y +15% crítico, pero reduce -1 corazón máximo.",
            lore = "Nunca se oxida, sin importar cuántas telas atraviese.",
            effects = ItemEffects(damageBonus = 4.5f, critChance = 0.15f, maxHeartsBonus = -1)
        )
    )

    val synergies: List<SynergyDef> = listOf(
        SynergyDef(
            id = "syn_clockwork_tracker",
            name = "Engranajes Rastreadores",
            requiredTags = listOf("spring", "precision"),
            description = "Brazo de Resorte + Ojo de Vidrio/Mecánico: tus disparos persiguen enemigos y rebotan.",
            effectId = "homing_bounce"
        ),
        SynergyDef(
            id = "syn_jesters_leap",
            name = "Salto del Bufón",
            requiredTags = listOf("chaotic", "spring"),
            description = "Cabeza de Payaso + Piernas de Resorte: esquivar deja una explosión bufonesca.",
            effectId = "dodge_bomb"
        ),
        SynergyDef(
            id = "syn_hunter_blades",
            name = "Tijeras Cazadoras",
            requiredTags = listOf("sharp", "homing"),
            description = "Brazo de Tijera + Ojo Mecánico/Bobina: los cortes buscan objetivos cercanos.",
            effectId = "homing_pierce"
        ),
        SynergyDef(
            id = "syn_double_windup",
            name = "Doble Cuerda",
            requiredTags = listOf("clock", "duplication"),
            description = "Torso/Mecanismo de Reloj + Brazo de Muñeco/Explosivo: dispara proyectiles gemelos.",
            effectId = "double_shot"
        ),
        SynergyDef(
            id = "syn_crimson_requiem",
            name = "Réquiem Carmesí",
            requiredTags = listOf("sound", "cursed"),
            description = "Caja/Torso Musical + Ojo Rojo/Maldito: las ondas sonoras amplifican el daño.",
            effectId = "sonic_amp"
        ),
        SynergyDef(
            id = "syn_immortal_stitch",
            name = "Costura Eterna",
            requiredTags = listOf("cloth", "repair"),
            description = "Múltiples partes de tela e hilo: regeneras durabilidad al cambiar de sala.",
            effectId = "room_repair"
        ),
        SynergyDef(
            id = "syn_iron_automaton",
            name = "Autómata de Hierro",
            requiredTags = listOf("mechanical", "armor"),
            description = "Piezas mecánicas + Blindaje: proyectiles más grandes y resistencia extra.",
            effectId = "heavy_gear"
        ),
        SynergyDef(
            id = "syn_phantom_ribbon",
            name = "Cinta Fantasma",
            requiredTags = listOf("dodge", "stealth"),
            description = "Alas/Piernas ágiles + Sigilo: breve invisibilidad tras esquivar.",
            effectId = "phase_dodge"
        ),
        SynergyDef(
            id = "syn_crimson_tailor",
            name = "Sastre Carmesí",
            requiredTags = listOf("repair", "sharp"),
            description = "Costura + Filo: cada enemigo derrotado repara +4 de durabilidad en tus piezas.",
            effectId = "kill_repair"
        ),
        SynergyDef(
            id = "syn_clockwork_carousel",
            name = "Carrusel de Cuerda",
            requiredTags = listOf("clock", "speed"),
            description = "Cuerda + Velocidad: +22% cadencia de disparo y +12% agilidad constante.",
            effectId = "carousel_overclock"
        ),
        SynergyDef(
            id = "syn_lantern_oracle",
            name = "Oráculo de Vitrina",
            requiredTags = listOf("vision", "precision"),
            description = "Linterna + Precisión: +20% probabilidad de impacto crítico y disparos de mayor alcance.",
            effectId = "oracle_crit"
        ),
        SynergyDef(
            id = "syn_puppet_theater",
            name = "Teatro de Sombras",
            requiredTags = listOf("decoy", "cursed"),
            description = "Señuelos + Maldición: tus señuelos emiten pulsos de daño sombrío a su alrededor.",
            effectId = "decoy_pulse"
        ),
        SynergyDef(
            id = "syn_porcelain_storm",
            name = "Tormenta de Porcelana",
            requiredTags = listOf("porcelain", "bounce"),
            description = "Porcelana + Rebote: +2 rebotes extra y +2.5 de daño por esquirla.",
            effectId = "porcelain_ricochet"
        ),
        SynergyDef(
            id = "syn_button_baron",
            name = "Barón de los Botones",
            requiredTags = listOf("economy", "key"),
            description = "Botón Maldito + Llave/Linterna: obtienes daño adicional proporcional a tus botones.",
            effectId = "greed_power"
        )
    )

    val characters: List<ToyCharacterDef> = listOf(
        ToyCharacterDef(
            id = CharacterId.PATCH,
            name = "Patch",
            title = "El Muñeco Olvidado",
            description = "Muñeco de trapo equilibrado de 25 cm. Un ojo de botón, otro de vidrio.",
            lore = "Despertó en una caja de madera sin recordar las manos que lo cosieron.",
            maxHearts = 3,
            baseDamage = 9f,
            baseSpeed = 165f,
            baseFireRate = 3.2f,
            dodgeCooldownSec = 1.6f,
            abilityName = "Remiendo Rápido",
            abilityDesc = "Cose sobre la marcha: recupera +25 de durabilidad en todas tus piezas dañadas.",
            abilityCooldownSec = 16f,
            startingItemId = "stuffed_heart",
            startingParts = mapOf(
                BodyZone.HEAD to "burlap_head",
                BodyZone.EYES to "glass_eye",
                BodyZone.ARMS to "cloth_arm",
                BodyZone.TORSO to "zipper_torso",
                BodyZone.LEGS to "stitched_legs",
                BodyZone.BACK to "cotton_back"
            ),
            suggestedDefectIds = listOf("button_eye", "weak_seams"),
            unlockRequirementDesc = "Disponible desde el inicio.",
            unlockedByDefault = true,
            primaryColorHex = 0xFFB89B78,
            accentColorHex = 0xFFC89948
        ),
        ToyCharacterDef(
            id = CharacterId.TINNY,
            name = "Tinny",
            title = "Autómata de Hojalata",
            description = "Robot de hojalata. Usa placas de blindaje y energía mecánica.",
            lore = "Su corazón de latón sigue latiendo incluso cuando la cuerda se agota.",
            maxHearts = 4,
            usesEnergyShield = true,
            baseDamage = 8.5f,
            baseSpeed = 152f,
            baseFireRate = 3.8f,
            dodgeCooldownSec = 1.9f,
            abilityName = "Sobrecarga de Cuerda",
            abilityDesc = "Duplica la cadencia de disparo durante 4 segundos.",
            abilityCooldownSec = 14f,
            startingItemId = "magnet_coil",
            startingParts = mapOf(
                BodyZone.HEAD to "clockwork_head",
                BodyZone.EYES to "mechanical_eye",
                BodyZone.ARMS to "spring_arm",
                BodyZone.TORSO to "armored_torso",
                BodyZone.LEGS to "wheel_legs",
                BodyZone.BACK to "windup_key_back"
            ),
            suggestedDefectIds = listOf("stiff_joints", "wooden_feet"),
            unlockRequirementDesc = "Derrota a El Vendedor en el Piso 1.",
            unlockedByDefault = false,
            primaryColorHex = 0xFF7B8E9B,
            accentColorHex = 0xFFC89948
        ),
        ToyCharacterDef(
            id = CharacterId.DOLLY,
            name = "Dolly",
            title = "Muñeca de Porcelana",
            description = "Daño devastador y gran precisión, pero cuerpo frágil.",
            lore = "Su vestido antiguo conserva marcas de barniz y lágrimas secas.",
            maxHearts = 2,
            baseDamage = 13.5f,
            baseSpeed = 168f,
            baseFireRate = 2.9f,
            dodgeCooldownSec = 1.5f,
            abilityName = "Mirada Quebradiza",
            abilityDesc = "Dispara una ráfaga radial de esquirlas de porcelana.",
            abilityCooldownSec = 12f,
            startingItemId = "porcelain_tooth",
            startingParts = mapOf(
                BodyZone.HEAD to "porcelain_head",
                BodyZone.EYES to "crimson_eye",
                BodyZone.ARMS to "scissor_arm",
                BodyZone.TORSO to "zipper_torso",
                BodyZone.LEGS to "stitched_legs",
                BodyZone.BACK to "paper_wings"
            ),
            suggestedDefectIds = listOf("sparse_stuffing", "light_sensitivity"),
            unlockRequirementDesc = "Completa una sala de jefe sin recibir daño.",
            unlockedByDefault = false,
            primaryColorHex = 0xFFE6DDD0,
            accentColorHex = 0xFF942229
        ),
        ToyCharacterDef(
            id = CharacterId.HOPS,
            name = "Hops",
            title = "Conejo de Resortes",
            description = "Movilidad extrema y esquiva de larga distancia.",
            lore = "Saltaba en el escaparate hasta que sus muelles atravesaron la felpa.",
            maxHearts = 3,
            baseDamage = 8f,
            baseSpeed = 195f,
            baseFireRate = 3.5f,
            dodgeCooldownSec = 1.0f,
            abilityName = "Salto Aturdidor",
            abilityDesc = "Impulso instantáneo que deja una onda que aturde a enemigos.",
            abilityCooldownSec = 9f,
            startingItemId = "rubber_marble",
            startingParts = mapOf(
                BodyZone.HEAD to "burlap_head",
                BodyZone.EYES to "glass_eye",
                BodyZone.ARMS to "doll_arm",
                BodyZone.TORSO to "zipper_torso",
                BodyZone.LEGS to "spring_legs",
                BodyZone.BACK to "paper_wings"
            ),
            suggestedDefectIds = listOf("impulse_delay", "myopic"),
            unlockRequirementDesc = "Esquiva 25 ataques o completa 10 habitaciones.",
            unlockedByDefault = false,
            primaryColorHex = 0xFF9E8A78,
            accentColorHex = 0xFF597354
        ),
        ToyCharacterDef(
            id = CharacterId.JACK,
            name = "Jack",
            title = "Caja Sorpresa",
            description = "Habilidades caóticas y explosivas, impredecible en combate.",
            lore = "Cuando la manivela termina la canción, alguien sale perdiendo.",
            maxHearts = 3,
            baseDamage = 10.5f,
            baseSpeed = 160f,
            baseFireRate = 2.7f,
            dodgeCooldownSec = 1.6f,
            abilityName = "Sorpresa Mortal",
            abilityDesc = "Detona una caja sorpresa a tu alrededor dañando y empujando enemigos.",
            abilityCooldownSec = 13f,
            startingItemId = "cursed_button",
            startingParts = mapOf(
                BodyZone.HEAD to "clown_head",
                BodyZone.EYES to "crimson_eye",
                BodyZone.ARMS to "explosive_arm",
                BodyZone.TORSO to "musical_torso",
                BodyZone.LEGS to "spring_legs",
                BodyZone.BACK to "music_box_back"
            ),
            suggestedDefectIds = listOf("shaky_hand", "noise_sensitivity"),
            unlockRequirementDesc = "Activa una Sinergia o derrota 40 juguetes enemigos.",
            unlockedByDefault = false,
            primaryColorHex = 0xFF942229,
            accentColorHex = 0xFFC89948
        ),
        ToyCharacterDef(
            id = CharacterId.STITCH,
            name = "Stitch",
            title = "Muñeco Costurero",
            description = "Especialista en reparar partes corporales y sobrevivir al desgaste.",
            lore = "Fue el ayudante silencioso del taller antes de que el hilo cobrara vida.",
            maxHearts = 3,
            baseDamage = 9.5f,
            baseSpeed = 162f,
            baseFireRate = 3.3f,
            dodgeCooldownSec = 1.5f,
            abilityName = "Aguja Maestra",
            abilityDesc = "Repara +40 durabilidad y recupera cualquier pieza caída en el suelo.",
            abilityCooldownSec = 15f,
            startingItemId = "red_thread",
            startingParts = mapOf(
                BodyZone.HEAD to "burlap_head",
                BodyZone.EYES to "button_eye_part",
                BodyZone.ARMS to "scissor_arm",
                BodyZone.TORSO to "zipper_torso",
                BodyZone.LEGS to "spider_legs",
                BodyZone.BACK to "cotton_back"
            ),
            suggestedDefectIds = listOf("weak_seams", "sticky_fingers"),
            unlockRequirementDesc = "Completa una partida o sobrevive con 2+ defectos.",
            unlockedByDefault = false,
            primaryColorHex = 0xFF597354,
            accentColorHex = 0xFFD13B42
        )
    )

    val floors: List<FloorTheme> = listOf(
        FloorTheme(1, "Sala de Exhibición", "Vitrinas rotas y cajas abiertas", EnemyType.BOSS_VENDEDOR, 0xFF231B18, 0xFF140F0E, 0x22C89948, altBossType = EnemyType.BOSS_BOTONERO),
        FloorTheme(2, "Taller de Reparaciones", "Mesas de sastre y agujas oxidadas", EnemyType.BOSS_COSTURERA, 0xFF20181A, 0xFF120C0E, 0x22942229, altBossType = EnemyType.BOSS_TITIRITERO),
        FloorTheme(3, "Guardería", "Cunas silenciosas y dibujos en la pared", EnemyType.BOSS_NINERA, 0xFF1B201E, 0xFF0F1312, 0x22597354, altBossType = EnemyType.BOSS_RELOJERO),
        FloorTheme(4, "Parque de Diversiones", "Carruseles detenidos en la oscuridad", EnemyType.BOSS_MAESTRO, 0xFF221C16, 0xFF130F0B, 0x22D98A2B, altBossType = EnemyType.BOSS_BUHONERO),
        FloorTheme(5, "Depósito de los Olvidados", "Montañas de juguetes descartados", EnemyType.BOSS_COLECCIONISTA, 0xFF1A1B20, 0xFF0E0F13, 0x224C5F6E),
        FloorTheme(6, "Corazón de Wonderland", "El taller donde todo comenzó", EnemyType.BOSS_MADRE, 0xFF1D1014, 0xFF0D0709, 0x33D13B42)
    )

    val events: List<RoomEventDef> = listOf(
        RoomEventDef(
            id = "prize_machine",
            title = "Máquina de Premios",
            narrative = "Una vieja expendedora de latón zumba sola en la esquina. En su cristal empañado brillan piezas y sombras.",
            options = listOf(
                EventOption("insert_coin", "Introducir 3 Botones", "Puede entregar un objeto pasivo, consumibles o despertar una trampa.", buttonCost = 3),
                EventOption("kick_machine", "Golpear el cristal", "Arriesga durabilidad del brazo para intentar sacar algo gratis."),
                EventOption("leave", "Alejarse en silencio", "No tentar a la suerte.")
            )
        ),
        RoomEventDef(
            id = "part_exchange",
            title = "La Muñeca del Intercambio",
            narrative = "Una muñeca sentada sobre un baúl sostiene una caja de terciopelo. «¿Me das una de tus piezas por un secreto?»",
            options = listOf(
                EventOption("trade_part", "Intercambiar pieza dañada", "Sustituye tu pieza más desgastada por una nueva aleatoria."),
                EventOption("repair_trade", "Pagar 4 Botones para coser", "Repara todas tus piezas (+35 durabilidad). ", buttonCost = 4),
                EventOption("leave", "Rechazar el trato", "Conservar tus piezas actuales.")
            )
        ),
        RoomEventDef(
            id = "mystery_box",
            title = "Caja Misteriosa",
            narrative = "Una caja atada con cinta roja tiembla levemente sobre el suelo de madera.",
            options = listOf(
                EventOption("open_box", "Desatar la cinta", "Recompensa completamente incierta: reliquia, botones o emboscada."),
                EventOption("leave", "Dejarla cerrada", "Seguir explorando el piso.")
            )
        ),
        RoomEventDef(
            id = "lost_toy",
            title = "El Juguete Perdido",
            narrative = "Un pequeño soldadito sin piernas se arrastra hacia ti pidiendo un poco de hilo para no apagarse.",
            options = listOf(
                EventOption("help_toy", "Compartir 2 Botones", "Ayudas al juguete. La juguetería recordará tu compasión.", buttonCost = 2),
                EventOption("salvage_toy", "Tomar sus piezas", "Obtienes una pieza corporal o consumible, pero la tienda no olvida."),
                EventOption("leave", "Pasar de largo", "Cada muñeco sobrevive como puede.")
            )
        ),
        RoomEventDef(
            id = "invisible_child",
            title = "Pasos Invisibles",
            narrative = "Escuchas risas infantiles recorriendo las estanterías vacías. Algo invisible observa tus costuras.",
            options = listOf(
                EventOption("offer_button", "Dejar 1 Botón en el suelo", "Calmas al niño invisible y obtienes pistas del piso.", buttonCost = 1),
                EventOption("run_away", "Mantenerte alerta", "Ganas velocidad temporal al salir de la sala.")
            )
        ),
        RoomEventDef(
            id = "mirror_room",
            title = "Habitación de Espejos",
            narrative = "Un espejo agrietado te devuelve un reflejo que parpadea un segundo después que tú.",
            options = listOf(
                EventOption("touch_mirror", "Tocar el cristal", "Obtienes +2 daño o un recuerdo, a cambio de desgaste leve."),
                EventOption("leave", "Apartar la mirada", "No mirar demasiado tiempo al cristal.")
            )
        )
    )

    val memories: List<MemoryFragmentDef> = listOf(
        MemoryFragmentDef("mem_nursery", "Recuerdo I: La Guardería", "«No mires al oso cuando apaguen la vitrina. Solo escucha cómo respira el algodón.»"),
        MemoryFragmentDef("mem_founder", "Recuerdo II: El Fundador", "«El dueño de Wonderland Toys no desapareció. Solo decidió quedarse con sus mejores creaciones.»"),
        MemoryFragmentDef("mem_clock", "Recuerdo III: Medianoche Perpetua", "«Todos los relojes de la tienda se detuvieron a las 11:58, justo antes del último cierre.»"),
        MemoryFragmentDef("mem_seamstress", "Recuerdo IV: Hilo Carmesí", "«La Costurera juró que ningún muñeco volvería a quedar abandonado en una caja.»"),
        MemoryFragmentDef("mem_theater", "Recuerdo V: El Teatrillo", "«Los hilos invisibles del escenario siguieron moviéndose mucho después de caer el telón.»"),
        MemoryFragmentDef("mem_attic", "Recuerdo VI: Luz de Desván", "«Quien dejó encendida la última linterna sabía que algún juguete despertaría en la oscuridad.»")
    )

    val achievements: List<AchievementDef> = listOf(
        // Supervivencia
        AchievementDef("ach_first_floor", "Primera Salida", AchievementCategory.SURVIVAL, "Derrota a El Vendedor en la Sala de Exhibición.", "Derrota al jefe del Piso 1", rewardText = "Desbloquea a Tinny", unlockCharacterId = CharacterId.TINNY, rewardChallengePoints = 10),
        AchievementDef("ach_win_run", "Fuera de la Caja", AchievementCategory.SURVIVAL, "Completa una partida escapando de Wonderland Toys.", "Vence al jefe final o completa la ruta", rewardText = "Desbloquea a Stitch + 25 pts", unlockCharacterId = CharacterId.STITCH, rewardChallengePoints = 25),
        AchievementDef("ach_10_rooms", "Explorador de Pasillos", AchievementCategory.SURVIVAL, "Completa 10 habitaciones en total.", "Limpia 10 salas", rewardText = "Desbloquea a Hops", unlockCharacterId = CharacterId.HOPS, rewardChallengePoints = 10),
        AchievementDef("ach_no_repair", "Costuras al Límite", AchievementCategory.SURVIVAL, "Completa un piso sin usar mesas ni kits de reparación.", "Supera un piso sin reparar", rewardText = "+15 pts de desafío", rewardChallengePoints = 15),

        // Combate
        AchievementDef("ach_40_kills", "Coleccionista de Chatarra", AchievementCategory.COMBAT, "Derrota 40 juguetes hostiles.", "40 enemigos derrotados", rewardText = "Desbloquea a Jack", unlockCharacterId = CharacterId.JACK, rewardChallengePoints = 10),
        AchievementDef("ach_flawless_boss", "Sin Un Rasguño", AchievementCategory.COMBAT, "Derrota a un jefe de piso sin recibir daño.", "Jefe perfecto", rewardText = "Desbloquea a Dolly", unlockCharacterId = CharacterId.DOLLY, rewardChallengePoints = 20),
        AchievementDef("ach_bomb_multikill", "Pólvora en la Vitrina", AchievementCategory.COMBAT, "Elimina 2 o más enemigos con una sola explosión.", "Doble baja explosiva", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),
        AchievementDef("ach_boss_no_consumable", "Manos Desnudas", AchievementCategory.COMBAT, "Derrota a un jefe sin usar consumibles en esa sala.", "Jefe sin consumibles", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),

        // Coleccionismo
        AchievementDef("ach_first_synergy", "Combinación Prohibida", AchievementCategory.COLLECTION, "Activa tu primera sinergia entre partes u objetos.", "Activa 1 sinergia", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),
        AchievementDef("ach_5_parts", "Cuerpo Remendado", AchievementCategory.COLLECTION, "Descubre 8 partes corporales distintas.", "8 partes en el Álbum", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),
        AchievementDef("ach_5_items", "Bolsillos Llenos", AchievementCategory.COLLECTION, "Descubre 6 objetos pasivos distintos.", "6 objetos en el Álbum", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),

        // Defectos
        AchievementDef("ach_two_defects", "Fabricación Imperfecta", AchievementCategory.DEFECTS, "Limpia un piso jugando con al menos 2 defectos activos.", "Piso con 2+ defectos", rewardText = "+15 pts de desafío", rewardChallengePoints = 15),
        AchievementDef("ach_three_defects", "Pesadilla de Fábrica", AchievementCategory.DEFECTS, "Juega y supera un jefe con 3 defectos de fabricación.", "Jefe con 3 defectos", rewardText = "+25 pts de desafío", rewardChallengePoints = 25),
        AchievementDef("ach_severe_defect", "Costura Frágil", AchievementCategory.DEFECTS, "Supera un piso con un defecto severo (Costuras débiles o Relleno escaso).", "Piso con defecto severo", rewardText = "+15 pts de desafío", rewardChallengePoints = 15),

        // Personajes
        AchievementDef("ach_patch_win", "El Regreso de Patch", AchievementCategory.CHARACTERS, "Completa una partida jugando con Patch.", "Victoria con Patch", rewardText = "+15 pts de desafío", rewardChallengePoints = 15),
        AchievementDef("ach_recover_part", "Recuperar lo Perdido", AchievementCategory.CHARACTERS, "Recoge del suelo una pieza corporal desprendida durante el combate.", "Recoge 1 pieza caída", rewardText = "+5 pts de desafío", rewardChallengePoints = 5),

        // Secretos
        AchievementDef("ach_secret_room", "Detrás del Estante", AchievementCategory.SECRETS, "Descubre una habitación secreta en Wonderland Toys.", "Entra a 1 sala secreta", hidden = true, rewardText = "+15 pts de desafío", rewardChallengePoints = 15),
        AchievementDef("ach_memory_2", "Ecos de la Tienda", AchievementCategory.SECRETS, "Reúne 2 recuerdos de Wonderland Toys.", "2 recuerdos hallados", hidden = true, rewardText = "+15 pts de desafío", rewardChallengePoints = 15),
        AchievementDef("ach_toybox_remembers", "La Juguetería Te Observa", AchievementCategory.SECRETS, "Activa la adaptación enemiga tras repetir una táctica.", "Despierta la memoria adaptativa", hidden = true, rewardText = "+10 pts de desafío", rewardChallengePoints = 10),

        // Desafíos
        AchievementDef("ach_nightmare_floor", "Modo Pesadilla", AchievementCategory.CHALLENGES, "Completa al menos un piso en dificultad Pesadilla.", "Piso en Pesadilla", rewardText = "+20 pts de desafío", rewardChallengePoints = 20),
        AchievementDef("ach_rich_buttons", "Tesorero de Latón", AchievementCategory.CHALLENGES, "Acumula 25 botones en una sola partida.", "25 botones en partida", rewardText = "+10 pts de desafío", rewardChallengePoints = 10),
        AchievementDef("ach_lore_puzzle_5", "Curiosidad de Taller", AchievementCategory.SECRETS, "Resuelve 5 puzles en Habitaciones de Lore sin recibir daño.", "5 puzles de lore resueltos", rewardText = "+20 pts de desafío", rewardChallengePoints = 20),
        AchievementDef("ach_archive_10", "Archivista de Wonderland", AchievementCategory.COLLECTION, "Reúne 10 entradas en el Archivo de Wonderland.", "10 archivos desbloqueados", rewardText = "+20 pts de desafío", rewardChallengePoints = 20),
        AchievementDef("ach_forbidden_doc", "Documento Prohibido", AchievementCategory.SECRETS, "Descubre un registro prohibido sobre El Fabricante.", "1 documento prohibido", hidden = true, rewardText = "+25 pts de desafío", rewardChallengePoints = 25),
        AchievementDef("ach_backpack_craft", "Costurero de Expedición", AchievementCategory.COLLECTION, "Fabrica o equipa una mochila mejorada en la Mesa de Costura.", "1 mochila fabricada", rewardText = "+15 pts de desafío", rewardChallengePoints = 15)
    )

    fun partById(id: String): BodyPartDef = bodyParts.find { it.id == id } ?: bodyParts.first()
    fun itemById(id: String): PassiveItemDef? = passiveItems.find { it.id == id }
    fun characterById(id: CharacterId): ToyCharacterDef = characters.find { it.id == id } ?: characters.first()

    fun computeActiveSynergies(
        parts: Map<BodyZone, EquippedBodyPart>,
        items: List<PassiveItemDef>
    ): List<SynergyDef> {
        val activeTags = mutableSetOf<String>()
        parts.values.filter { !it.isDetached }.forEach { activeTags.addAll(it.def.tags) }
        items.forEach { activeTags.addAll(it.tags) }
        return synergies.filter { syn -> syn.requiredTags.all { it in activeTags } }
    }
}
