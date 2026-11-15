package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class OstMood {
    MENU,
    EXPLORE,
    SHOP,
    COMBAT,
    BOSS,
    DEATH,
    VICTORY
}

/**
 * Real-time procedural Original Soundtrack (OST) synthesizer.
 * Streams ambient dark toybox melodies, music-box arpeggios, ticking clockwork percussion,
 * and heartbeat tension dynamically tailored to the player's current situation.
 */
class OstMusicEngine {

    @Volatile var enabled: Boolean = true
    @Volatile var currentMood: OstMood = OstMood.MENU
    @Volatile var lowHealthTension: Boolean = false

    private val sampleRate = 22050
    @Volatile private var running = false
    private var workerThread: Thread? = null

    fun start() {
        if (running) return
        running = true
        workerThread = thread(start = true, isDaemon = true, name = "ToyboxOstThread") {
            runCatching { streamLoop() }
            running = false
        }
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        workerThread = null
    }

    private fun streamLoop() {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)
        val chunkSamples = (sampleRate / 12).coerceAtLeast(minBuf / 2) // ~83ms chunks

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(chunkSamples * 2 * 2)
            .build()

        track.play()
        val buffer = ShortArray(chunkSamples)

        // Musical state
        var sampleCounter = 0L
        var stepIdx = 0
        var lastStepSample = 0L

        var melodyPhase = 0.0
        var bassPhase = 0.0
        var padPhase = 0.0
        var heartPhase = 0.0

        var activeMelodyFreq = 293.66f // D4
        var activeBassFreq = 73.42f    // D2
        var noteDecayTime = 0f
        var percDecayTime = 0f
        var heartDecayTime = 0f
        var currentGain = 0f

        while (running) {
            val targetGain = if (enabled) 1f else 0f
            val mood = currentMood
            val tense = lowHealthTension

            val stepDurationSec = when (mood) {
                OstMood.MENU -> 0.42f
                OstMood.EXPLORE -> 0.48f
                OstMood.SHOP -> 0.34f
                OstMood.COMBAT -> 0.21f
                OstMood.BOSS -> 0.17f
                OstMood.DEATH -> 0.65f
                OstMood.VICTORY -> 0.40f
            }
            val stepSamples = (sampleRate * stepDurationSec).toLong()

            for (i in 0 until chunkSamples) {
                currentGain += (targetGain - currentGain) * 0.002f

                if (sampleCounter - lastStepSample >= stepSamples) {
                    lastStepSample = sampleCounter
                    stepIdx = (stepIdx + 1) % 16

                    val noteMidi = pickMelodyNote(mood, stepIdx, tense)
                    if (noteMidi > 0) {
                        val detune = if (tense) (if (stepIdx % 2 == 0) -0.18f else 0.14f) else 0f
                        activeMelodyFreq = midiToFreq(noteMidi + detune)
                        noteDecayTime = 0f
                    }
                    val bassMidi = pickBassNote(mood, stepIdx)
                    if (bassMidi > 0) {
                        activeBassFreq = midiToFreq(bassMidi.toFloat())
                    }

                    // Ticking percussion in combat/boss/shop
                    if (mood == OstMood.COMBAT || mood == OstMood.BOSS || (mood == OstMood.SHOP && stepIdx % 2 == 0)) {
                        percDecayTime = 0f
                    }
                    if (tense && stepIdx % 4 == 0) {
                        heartDecayTime = 0f
                    }
                }

                val dt = 1f / sampleRate
                noteDecayTime += dt
                percDecayTime += dt
                heartDecayTime += dt

                // 1. Music box / celesta bell voice
                melodyPhase += 2.0 * PI * activeMelodyFreq / sampleRate
                val bellEnv = when (mood) {
                    OstMood.DEATH -> exp(-noteDecayTime * 2.2f)
                    OstMood.COMBAT, OstMood.BOSS -> exp(-noteDecayTime * 6.5f)
                    else -> exp(-noteDecayTime * 3.8f)
                }
                val bellTone = (sin(melodyPhase) + 0.35 * sin(melodyPhase * 2.0) + 0.12 * sin(melodyPhase * 3.01)).toFloat() * bellEnv

                // 2. Dark organ / toyshop drone bass
                bassPhase += 2.0 * PI * activeBassFreq / sampleRate
                val bassLevel = when (mood) {
                    OstMood.BOSS -> 0.28f
                    OstMood.COMBAT -> 0.20f
                    OstMood.EXPLORE -> 0.13f
                    OstMood.MENU -> 0.12f
                    OstMood.DEATH -> 0.14f
                    else -> 0.15f
                }
                val bassTone = (sin(bassPhase) + 0.4 * sin(bassPhase * 2.0)).toFloat() * bassLevel

                // 3. Ethereal fifth pad
                padPhase += 2.0 * PI * (activeBassFreq * 1.498f) / sampleRate
                val padTone = sin(padPhase).toFloat() * 0.055f

                // 4. Clockwork tick / snare
                val tickSound = if (mood == OstMood.COMBAT || mood == OstMood.BOSS || mood == OstMood.SHOP) {
                    val isAccent = (stepIdx % 4 == 0)
                    val decayRate = if (isAccent && mood == OstMood.BOSS) 28f else 55f
                    val tickEnv = exp(-percDecayTime * decayRate)
                    val noise = (Random.nextFloat() * 2f - 1f)
                    val amp = if (mood == OstMood.BOSS && isAccent) 0.16f else 0.08f
                    noise * tickEnv * amp
                } else {
                    // Faint vinyl dust crackle in menu/explore
                    if (Random.nextFloat() < 0.004f) (Random.nextFloat() * 2f - 1f) * 0.03f else 0f
                }

                // 5. Low health heartbeat pulse
                val heartSound = if (tense) {
                    heartPhase += 2.0 * PI * 54.0 / sampleRate
                    val hEnv = exp(-heartDecayTime * 11f)
                    sin(heartPhase).toFloat() * hEnv * 0.26f
                } else 0f

                val masterVol = 0.28f * currentGain
                val mixed = ((bellTone * 0.34f + bassTone + padTone + tickSound + heartSound) * masterVol)
                    .coerceIn(-0.95f, 0.95f)
                buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                sampleCounter++
            }

            if (running) {
                track.write(buffer, 0, chunkSamples)
            }
        }

        runCatching {
            track.stop()
            track.release()
        }
    }

    private fun pickMelodyNote(mood: OstMood, step: Int, tense: Boolean): Int {
        // Notes in MIDI (D minor / harmonic minor horror toybox motifs)
        return when (mood) {
            OstMood.MENU -> {
                // Slow haunting music-box waltz in D minor (D5, F5, A5, Bb5, A5, G5, E5, C#5...)
                val seq = intArrayOf(74, 0, 77, 81, 0, 82, 81, 0, 79, 0, 76, 73, 74, 0, 69, 0)
                seq[step]
            }
            OstMood.EXPLORE -> {
                // Sparse eerie toyshop footsteps & chimes
                val seq = intArrayOf(74, 0, 0, 77, 0, 0, 76, 0, 73, 0, 0, 74, 0, 69, 0, 0)
                seq[step]
            }
            OstMood.SHOP -> {
                // Crooked playful carnival music box
                val seq = intArrayOf(72, 76, 79, 0, 75, 79, 82, 0, 74, 77, 81, 0, 73, 76, 79, 0)
                seq[step]
            }
            OstMood.COMBAT -> {
                // Urgent driving clockwork motif
                val seq = intArrayOf(74, 77, 81, 77, 82, 81, 79, 77, 76, 79, 81, 76, 73, 76, 79, 73)
                val n = seq[step]
                if (tense && step % 5 == 0) n - 1 else n
            }
            OstMood.BOSS -> {
                // Dramatic high-stakes harmonic minor ostinato
                val seq = intArrayOf(74, 86, 85, 81, 82, 81, 77, 81, 79, 82, 81, 77, 76, 73, 76, 85)
                seq[step]
            }
            OstMood.DEATH -> {
                // Winding-down broken music box
                val seq = intArrayOf(81, 0, 79, 0, 77, 0, 76, 0, 74, 0, 0, 0, 69, 0, 0, 0)
                seq[step]
            }
            OstMood.VICTORY -> {
                // Hopeful bittersweet D major / Dorian lift
                val seq = intArrayOf(74, 0, 78, 81, 0, 86, 85, 0, 81, 0, 79, 78, 74, 0, 81, 0)
                seq[step]
            }
        }
    }

    private fun pickBassNote(mood: OstMood, step: Int): Int {
        return when (mood) {
            OstMood.MENU, OstMood.EXPLORE -> when (step) {
                0 -> 38 // D2
                8 -> 34 // Bb1
                12 -> 33 // A1
                else -> -1
            }
            OstMood.SHOP -> when (step) {
                0 -> 36 // C2
                4 -> 43 // G2
                8 -> 38 // D2
                12 -> 33 // A1
                else -> -1
            }
            OstMood.COMBAT -> when (step % 4) {
                0 -> if (step < 8) 38 else if (step < 12) 36 else 33
                2 -> 45
                else -> -1
            }
            OstMood.BOSS -> when (step % 2) {
                0 -> if (step < 8) 38 else if (step < 12) 41 else 37
                else -> 50
            }
            OstMood.DEATH -> if (step == 0) 38 else if (step == 8) 33 else -1
            OstMood.VICTORY -> when (step) {
                0 -> 38
                8 -> 43
                12 -> 45
                else -> -1
            }
        }
    }

    private fun midiToFreq(midi: Float): Float {
        return (440.0 * Math.pow(2.0, (midi - 69.0) / 12.0)).toFloat()
    }
}
