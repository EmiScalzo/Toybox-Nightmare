package com.example.engine

import com.example.data.model.EnemyType
import com.example.data.model.RoomEventDef
import com.example.data.model.RoomType

enum class MemoryTraitId(
    val title: String,
    val triggerReason: String,
    val gameplayEffect: String,
    val isPositive: Boolean
) {
    DODGE_PREDICTION(
        title = "Paso Predecible",
        triggerReason = "Esquivaste repetidamente en combate.",
        gameplayEffect = "Los tiradores apuntan levemente por delante de tu carrera (verás su destello de mira).",
        isPositive = false
    ),
    BOMB_SCAVENGERS(
        title = "Vigilancia de Pólvora",
        triggerReason = "Usaste múltiples explosivos o bombas.",
        gameplayEffect = "Duendes Ladrones pueden aparecer para patear tus bombas activas antes de que detonen.",
        isPositive = false
    ),
    MERCY_FAVOR(
        title = "Gratitud de los Olvidados",
        triggerReason = "Ayudaste a juguetes indefensos en los eventos.",
        gameplayEffect = "Precios reducidos en puestos, diálogos amigables y pistas de salas secretas.",
        isPositive = true
    ),
    RUTHLESS_REPUTATION(
        title = "Manos de Desguace",
        triggerReason = "Desarmaste juguetes vulnerables para aprovechar sus piezas.",
        gameplayEffect = "Las tiendas suben +1 Botón y más muñecos emboscan en salas oscuras.",
        isPositive = false
    ),
    CURSE_DEFIANCE(
        title = "Desafío a la Vitrina",
        triggerReason = "Superaste salas de Maldición o Desafío.",
        gameplayEffect = "Mayor probabilidad de encontrar salas secretas y piezas inusuales.",
        isPositive = true
    ),
    MEMORY_AWAKENED(
        title = "Ecos Despiertos",
        triggerReason = "Reuniste 2 o más Recuerdos de Wonderland Toys.",
        gameplayEffect = "Los jefes y eventos reaccionan a la historia oculta que cargas contigo.",
        isPositive = true
    )
}

object ToyboxMemorySystem {

    fun activeTraits(mem: ToyboxMemoryState): List<MemoryTraitId> = buildList {
        if (mem.enemiesAdaptDodge) add(MemoryTraitId.DODGE_PREDICTION)
        if (mem.enemiesStealBombs) add(MemoryTraitId.BOMB_SCAVENGERS)
        if (mem.compassionScore >= 1) add(MemoryTraitId.MERCY_FAVOR)
        if (mem.compassionScore <= -1) add(MemoryTraitId.RUTHLESS_REPUTATION)
        if (mem.curseOrChallengeCleared >= 2) add(MemoryTraitId.CURSE_DEFIANCE)
        if (mem.memoriesCollected.size >= 2) add(MemoryTraitId.MEMORY_AWAKENED)
    }

    /**
     * Enriches room event narrative dynamically based on what the Toybox remembers!
     */
    fun contextualizeEvent(event: RoomEventDef, mem: ToyboxMemoryState): RoomEventDef {
        val extraLine = when {
            event.id == "lost_toy" && mem.compassionScore >= 1 ->
                "\n\n[La juguetería recuerda] El juguete susurra: «Sé que ayudaste a uno de los nuestros antes...»"
            event.id == "lost_toy" && mem.compassionScore <= -1 ->
                "\n\n[La juguetería recuerda] El soldadito retrocede asustado al ver tus manos manchadas de algodón ajeno."
            event.id == "part_exchange" && mem.memoriesCollected.isNotEmpty() ->
                "\n\n[La juguetería recuerda] La muñeca reconoce el recuerdo que llevas: «Eso perteneció al fundador...»"
            mem.enemiesStealBombs ->
                "\n\n[La juguetería recuerda] El aire huele a pólvora vieja; sombras inquietas vigilan tus bolsillos."
            mem.enemiesAdaptDodge ->
                "\n\n[La juguetería recuerda] Ojos de botón siguen el ritmo exacto de tus pasos."
            else -> ""
        }
        return if (extraLine.isEmpty()) event else event.copy(narrative = event.narrative + extraLine)
    }

    /**
     * Generates contextual boss entrance subtitle reflecting player actions & memories.
     */
    fun bossDialogueFor(boss: EnemyType, mem: ToyboxMemoryState): String {
        return when {
            mem.memoriesCollected.size >= 2 ->
                "${boss.displayName}: «Esos recuerdos no son tuyos, muñeco...»"
            mem.compassionScore <= -1 ->
                "${boss.displayName}: «Sabemos lo que hiciste con los juguetes perdidos.»"
            mem.compassionScore >= 1 ->
                "${boss.displayName}: «Tu compasión no abrirá la puerta de salida.»"
            mem.enemiesAdaptDodge ->
                "${boss.displayName}: «Ya aprendimos hacia dónde saltas.»"
            mem.enemiesStealBombs ->
                "${boss.displayName}: «Guarda esa pólvora; aquí no te servirá.»"
            else -> boss.description
        }
    }

    /**
     * Determines whether an extra Thief Gremlin appears in combat rooms if the player abuses bombs.
     */
    fun shouldInjectBombScavenger(roomType: RoomType, mem: ToyboxMemoryState): Boolean {
        return mem.enemiesStealBombs && (roomType == RoomType.COMBAT || roomType == RoomType.CHALLENGE)
    }
}
