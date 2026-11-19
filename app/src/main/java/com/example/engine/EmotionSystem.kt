package com.example.engine

import com.example.data.model.EmotionFusion
import com.example.data.model.EmotionState
import com.example.data.model.LightSourceType
import com.example.data.model.PatchEmotion
import com.example.data.model.RoomLightingMode
import com.example.data.model.RoomType
import kotlin.random.Random

data class EmotionModifiers(
    val damageMult: Float = 1f,
    val fireRateMult: Float = 1f,
    val moveSpeedMult: Float = 1f,
    val dodgeLengthMult: Float = 1f,
    val spreadDelta: Float = 0f,
    val batteryDrainMult: Float = 1f,
    val noiseMult: Float = 1f,
    val enemyDetectMult: Float = 1f,
    val secretSenseBonus: Float = 0f,
    val critBonus: Float = 0f,
    val bleedOnHit: Boolean = false,
    val breakCratesOnShot: Boolean = false,
    val fearResist: Float = 0f
)

object EmotionSystem {

    fun computeModifiers(e: EmotionState): EmotionModifiers {
        val lvlScale = when (e.level.coerceIn(1, 3)) {
            1 -> 1.0f
            2 -> 1.35f
            else -> 1.75f
        }
        var mod = when (e.primary) {
            PatchEmotion.NEUTRAL -> EmotionModifiers()
            PatchEmotion.SADNESS -> EmotionModifiers(
                fireRateMult = 0.88f,
                moveSpeedMult = 0.92f,
                secretSenseBonus = 0.25f * lvlScale,
                fearResist = 0.65f
            )
            PatchEmotion.ANGER -> EmotionModifiers(
                damageMult = 1f + 0.18f * lvlScale,
                spreadDelta = 0.06f * lvlScale,
                noiseMult = 1f + 0.45f * lvlScale,
                breakCratesOnShot = true
            )
            PatchEmotion.FEAR -> EmotionModifiers(
                moveSpeedMult = 1f + 0.14f * lvlScale,
                dodgeLengthMult = 1f + 0.16f * lvlScale,
                spreadDelta = 0.08f * lvlScale,
                enemyDetectMult = 0.72f
            )
            PatchEmotion.JOY -> EmotionModifiers(
                fireRateMult = 1f + 0.14f * lvlScale,
                moveSpeedMult = 1f + 0.08f * lvlScale,
                critBonus = 0.10f * lvlScale
            )
            PatchEmotion.CURIOSITY -> EmotionModifiers(
                secretSenseBonus = 0.35f * lvlScale,
                batteryDrainMult = (1f - 0.18f * lvlScale).coerceAtLeast(0.45f),
                moveSpeedMult = 1.04f
            )
            PatchEmotion.EXHAUSTION -> EmotionModifiers(
                moveSpeedMult = (1f - 0.12f * lvlScale).coerceAtLeast(0.68f),
                fireRateMult = (1f - 0.10f * lvlScale).coerceAtLeast(0.70f),
                batteryDrainMult = 0.65f
            )
        }

        // Apply contradictory dual emotion fusions on top
        mod = when (e.fusion) {
            EmotionFusion.VENGEANCE -> mod.copy(
                damageMult = mod.damageMult + 0.32f,
                fireRateMult = mod.fireRateMult * 0.85f,
                bleedOnHit = true
            )
            EmotionFusion.PARANOIA -> mod.copy(
                moveSpeedMult = mod.moveSpeedMult + 0.16f,
                secretSenseBonus = mod.secretSenseBonus + 0.40f,
                enemyDetectMult = 0.55f
            )
            EmotionFusion.EUPHORIA -> mod.copy(
                fireRateMult = mod.fireRateMult + 0.28f,
                moveSpeedMult = mod.moveSpeedMult + 0.15f,
                spreadDelta = mod.spreadDelta + 0.14f
            )
            null -> mod
        }
        return mod
    }

    fun triggerEmotion(
        state: GameRunState,
        emotion: PatchEmotion,
        intensityBoost: Float = 28f,
        durationSec: Float = 24f,
        reasonBanner: String? = null
    ): GameRunState {
        if (emotion == PatchEmotion.NEUTRAL) {
            return state.copy(emotionState = EmotionState())
        }
        val cur = state.emotionState
        // Glowing Toy calms Fear spikes
        val adjustedBoost = if (emotion == PatchEmotion.FEAR &&
            state.lightSource == LightSourceType.GLOWING_TOY && state.lightOn && state.lightSourceCharge > 0f
        ) {
            intensityBoost * 0.45f
        } else {
            intensityBoost
        }

        val (newPrimary, newSecondary, newScore) = if (cur.primary == emotion || cur.primary == PatchEmotion.NEUTRAL) {
            Triple(emotion, cur.secondary, (cur.intensityScore + adjustedBoost).coerceAtMost(100f))
        } else {
            // Two active emotions can merge into a fusion or shift primary
            if (adjustedBoost >= 24f) {
                Triple(emotion, cur.primary, (adjustedBoost + cur.intensityScore * 0.35f).coerceAtMost(100f))
            } else {
                Triple(cur.primary, emotion, (cur.intensityScore + adjustedBoost * 0.5f).coerceAtMost(100f))
            }
        }

        val newLevel = when {
            newScore >= 72f -> 3
            newScore >= 38f -> 2
            else -> 1
        }
        val fusion = resolveFusion(newPrimary, newSecondary)
        val nextEmotionState = EmotionState(
            primary = newPrimary,
            level = newLevel,
            secondary = newSecondary,
            fusion = fusion,
            durationSec = durationSec,
            intensityScore = newScore
        )

        var banner = state.activeBanner
        if (fusion != null && fusion != cur.fusion) {
            banner = RunUiBanner(
                "Fusión Emocional: ${fusion.displayName}",
                fusion.effectSummary,
                fusion.colorHex,
                3.2f
            )
        } else if (newPrimary != cur.primary || newLevel > cur.level) {
            val subtitle = reasonBanner ?: "${newPrimary.buffsSummary} / ${newPrimary.debuffsSummary}"
            banner = RunUiBanner(
                "Emoción: ${newPrimary.displayName} Nv.$newLevel",
                subtitle,
                newPrimary.colorHex,
                2.6f
            )
        }

        return state.copy(
            emotionState = nextEmotionState,
            activeBanner = banner
        )
    }

    private fun resolveFusion(a: PatchEmotion, b: PatchEmotion?): EmotionFusion? {
        if (b == null || a == b) return null
        return EmotionFusion.entries.firstOrNull {
            (it.first == a && it.second == b) || (it.first == b && it.second == a)
        }
    }

    fun stepEmotions(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val e = s.emotionState

        // Passive environmental emotional triggers
        val inDarkRoom = s.currentRoom.lightingMode in setOf(RoomLightingMode.DARK, RoomLightingMode.TOTAL_DARKNESS)
        val hasLightActive = s.lightOn && s.lightSource != LightSourceType.NONE && s.lightSourceCharge > 1f
        if (inDarkRoom && !hasLightActive && s.enemies.isNotEmpty() && e.primary != PatchEmotion.FEAR && Random.nextFloat() < dt * 0.14f) {
            s = triggerEmotion(s, PatchEmotion.FEAR, 26f, 20f, "La oscuridad sin luz inquieta a Patch.")
        }

        // Glowing Toy actively soothes Fear
        if (s.emotionState.primary == PatchEmotion.FEAR &&
            s.lightSource == LightSourceType.GLOWING_TOY && s.lightOn && s.lightSourceCharge > 0f
        ) {
            val reduced = (s.emotionState.durationSec - dt * 1.6f).coerceAtLeast(0f)
            s = s.copy(emotionState = s.emotionState.copy(durationSec = reduced))
        }

        // Passive Curiosity in Lore / Anomaly / Secret rooms
        if (s.currentRoom.type in setOf(RoomType.LORE, RoomType.SECRET, RoomType.LABYRINTH) &&
            s.emotionState.primary == PatchEmotion.NEUTRAL && Random.nextFloat() < dt * 0.25f
        ) {
            s = triggerEmotion(s, PatchEmotion.CURIOSITY, 26f, 25f, "Los secretos del lugar despiertan su curiosidad.")
        }

        // Decay timer back toward NEUTRAL
        val cur = s.emotionState
        if (cur.primary != PatchEmotion.NEUTRAL) {
            val nextDur = cur.durationSec - dt
            if (nextDur <= 0f) {
                s = s.copy(emotionState = EmotionState())
            } else {
                s = s.copy(
                    emotionState = cur.copy(
                        durationSec = nextDur,
                        intensityScore = (cur.intensityScore - dt * 1.1f).coerceAtLeast(10f)
                    )
                )
            }
        }
        return s
    }
}
