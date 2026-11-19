package com.example.engine

import com.example.data.model.DarknessEnemyTrait
import com.example.data.model.EnemyType
import com.example.data.model.LightSourceType
import com.example.data.model.RoomLampProp
import com.example.data.model.RoomLightingMode
import com.example.data.model.RoomType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object LightingSystem {

    fun generateRoomLamps(room: RoomBlueprint, allocId: () -> Long): List<RoomLampProp> {
        if (room.type in setOf(RoomType.START, RoomType.SHOP, RoomType.REPAIR)) {
            return listOf(RoomLampProp(allocId(), ARENA_W * 0.5f, ARENA_H * 0.24f, lit = true, radius = 340f))
        }
        return when (room.lightingMode) {
            RoomLightingMode.LIT -> listOf(
                RoomLampProp(allocId(), ARENA_W * 0.5f, ARENA_H * 0.5f, lit = true, radius = 360f)
            )
            RoomLightingMode.DIM, RoomLightingMode.FLICKERING -> listOf(
                RoomLampProp(allocId(), ARENA_W * 0.26f, ARENA_H * 0.26f, lit = true, radius = 230f),
                RoomLampProp(allocId(), ARENA_W * 0.74f, ARENA_H * 0.74f, lit = Random.nextBoolean(), radius = 230f)
            )
            RoomLightingMode.DARK -> listOf(
                RoomLampProp(allocId(), ARENA_W * 0.76f, ARENA_H * 0.26f, lit = false, radius = 240f)
            )
            RoomLightingMode.TOTAL_DARKNESS -> emptyList()
        }
    }

    fun playerLightRadius(state: GameRunState): Float {
        if (!state.lightOn || state.lightSource == LightSourceType.NONE || state.lightSourceCharge <= 0f) {
            return 135f
        }
        val base = 245f * state.lightSource.radiusScale
        val glassBonus = if (state.equippedParts[com.example.data.model.BodyZone.EYES]?.let { !it.isDetached } == true) {
            state.equippedParts[com.example.data.model.BodyZone.EYES]?.def?.effects?.visionRangeMult ?: 1f
        } else 1f
        val mothBonus = if (state.activeCompanions.any { it.id == com.example.data.model.CompanionId.PAPER_MOTH && !it.broken }) 1.28f else 1f
        return base * glassBonus * mothBonus
    }

    fun isPointLitByPlayer(state: GameRunState, x: Float, y: Float): Boolean {
        if (!state.lightOn || state.lightSource == LightSourceType.NONE || state.lightSourceCharge <= 0f) {
            return false
        }
        val r = playerLightRadius(state)
        val dx = x - state.playerX
        val dy = y - state.playerY
        val dist = hypot(dx, dy)
        if (dist > r) return false
        if (!state.lightSource.directional || dist < 68f) return true
        val angleToTarget = atan2(dy, dx)
        var diff = angleToTarget - state.facingAngle
        while (diff > Math.PI) diff -= (2.0 * Math.PI).toFloat()
        while (diff < -Math.PI) diff += (2.0 * Math.PI).toFloat()
        return kotlin.math.abs(diff) <= 0.82f // ~94 deg cone
    }

    fun isPointIlluminated(state: GameRunState, x: Float, y: Float): Boolean {
        if (isPointLitByPlayer(state, x, y)) return true
        if (state.roomLamps.any { it.lit && hypot(x - it.x, y - it.y) <= it.radius * 0.78f }) return true
        if (state.currentRoom.lightingMode == RoomLightingMode.LIT) return true
        return false
    }

    /**
     * Veiled Specters become vulnerable ONLY when the player's direct light is OFF (or depleted)
     * and they are outside active floor lamps!
     */
    fun isVeiledSpecterVulnerable(state: GameRunState, enemy: ActiveEnemy): Boolean {
        if (enemy.type != EnemyType.VEILED_SPECTER && enemy.darknessTrait != DarknessEnemyTrait.LIGHT_INVULNERABLE) {
            return true
        }
        val inPlayerBeam = isPointLitByPlayer(state, enemy.x, enemy.y)
        val nearLitLamp = state.roomLamps.any { it.lit && hypot(enemy.x - it.x, enemy.y - it.y) <= it.radius * 0.72f }
        return !inPlayerBeam && !nearLitLamp
    }

    fun stepLighting(state: GameRunState, dt: Float): GameRunState {
        var s = state
        val mods = EmotionSystem.computeModifiers(s.emotionState)
        val source = s.lightSource

        // Battery drain / recharge
        if (source != LightSourceType.NONE) {
            if (s.lightOn && source.drainPerSec > 0f && s.lightSourceCharge > 0f) {
                val roomDrain = s.currentRoom.lightingMode.batteryDrainMult
                val nextCharge = (s.lightSourceCharge - source.drainPerSec * roomDrain * mods.batteryDrainMult * dt)
                    .coerceAtLeast(0f)
                val autoOff = nextCharge <= 0f
                s = s.copy(
                    lightSourceCharge = nextCharge,
                    lightOn = if (autoOff) false else s.lightOn,
                    activeBanner = if (autoOff) {
                        RunUiBanner("¡Luz Agotada!", "${source.displayName} se quedó sin carga. Recarga en una estación o sala iluminada.", 0xFFD98A2B, 2.8f)
                    } else s.activeBanner
                )
            } else if ((!s.lightOn || source == LightSourceType.PORTABLE_LAMP) &&
                s.currentRoom.lightingMode == RoomLightingMode.LIT &&
                s.lightSourceCharge < source.maxCharge
            ) {
                val regenRate = if (source == LightSourceType.PORTABLE_LAMP) 5.5f else 2.0f
                s = s.copy(lightSourceCharge = (s.lightSourceCharge + regenRate * dt).coerceAtMost(source.maxCharge))
            }
        }

        // LAMP_SNUFFER enemies seek lit floor lamps and extinguish them
        if (s.enemies.any { it.type == EnemyType.LAMP_SNUFFER && it.hp > 0f } && s.roomLamps.any { it.lit }) {
            val lamps = s.roomLamps.toMutableList()
            val updatedEnemies = s.enemies.map { e ->
                if (e.type != EnemyType.LAMP_SNUFFER || e.hp <= 0f) return@map e
                val targetLampIdx = lamps.indexOfFirst { it.lit }
                if (targetLampIdx == -1) return@map e
                val lamp = lamps[targetLampIdx]
                val dx = lamp.x - e.x
                val dy = lamp.y - e.y
                val d = hypot(dx, dy).coerceAtLeast(1f)
                if (d < 34f) {
                    lamps[targetLampIdx] = lamp.copy(lit = false)
                    s = s.copy(
                        activeBanner = RunUiBanner("¡Lámpara Apagada!", "Un Apagalámparas extinguió la luz de la sala.", 0xFF9E64B5, 2.2f)
                    )
                    e
                } else {
                    e.copy(
                        x = (e.x + (dx / d) * e.type.speed * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_W - WALL_THICKNESS - e.radius),
                        y = (e.y + (dy / d) * e.type.speed * dt).coerceIn(WALL_THICKNESS + e.radius, ARENA_H - WALL_THICKNESS - e.radius)
                    )
                }
            }
            s = s.copy(roomLamps = lamps, enemies = updatedEnemies)
        }

        return s
    }

    fun toggleLight(state: GameRunState): GameRunState {
        if (state.lightSource == LightSourceType.NONE) {
            return state.copy(
                activeBanner = RunUiBanner("Sin Fuente de Luz", "Consigue una vela, linterna o farol en tiendas o cofres.", 0xFFD98A2B, 2.2f)
            )
        }
        val nextOn = !state.lightOn
        if (nextOn && state.lightSourceCharge <= 0f && state.lightSource.drainPerSec > 0f) {
            return state.copy(
                lightOn = false,
                activeBanner = RunUiBanner("Sin Batería", "Recarga tu ${state.lightSource.displayName} en una Estación de Cuerda.", 0xFFD13B42, 2.2f)
            )
        }
        return state.copy(
            lightOn = nextOn,
            activeBanner = RunUiBanner(
                if (nextOn) "${state.lightSource.displayName} Encendida" else "${state.lightSource.displayName} Apagada",
                if (nextOn) "Ilumina secretos y frena amenazas fotofóbicas." else "Ahorra energía y vuelve vulnerables a los Espectros Velados.",
                0xFFE5A93B,
                1.8f
            )
        )
    }

    fun cycleLightSource(state: GameRunState): GameRunState {
        val owned = state.ownedLightSources
        if (owned.size <= 1) return toggleLight(state)
        val idx = owned.indexOf(state.lightSource)
        val next = owned[(idx + 1) % owned.size]
        return state.copy(
            lightSource = next,
            lightOn = true,
            lightSourceCharge = state.lightSourceCharge.coerceAtLeast(60f),
            activeBanner = RunUiBanner("Luz Equipada: ${next.displayName}", next.description, 0xFFE5A93B, 2.2f)
        )
    }

    fun grantLightSource(state: GameRunState, source: LightSourceType): GameRunState {
        val owned = (state.ownedLightSources + source).distinct()
        return state.copy(
            ownedLightSources = owned,
            lightSource = source,
            lightSourceCharge = source.maxCharge.coerceAtLeast(100f),
            lightOn = true,
            activeBanner = RunUiBanner("Nueva Fuente de Luz: ${source.displayName}", source.description, 0xFFE5A93B, 3.0f)
        )
    }
}
