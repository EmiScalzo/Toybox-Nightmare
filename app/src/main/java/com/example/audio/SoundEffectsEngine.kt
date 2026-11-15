package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class ToySfx {
    SHOOT,
    HIT_ENEMY,
    PLAYER_HURT,
    DODGE,
    PICKUP_BUTTON,
    PICKUP_ITEM,
    REPAIR_STITCH,
    DOOR_OPEN,
    EXPLOSION,
    BOSS_ROAR,
    SYNERGY,
    PART_DETACH
}

class SoundEffectsEngine {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile var enabled: Boolean = true
    private val sampleRate = 22050

    fun play(sfx: ToySfx) {
        if (!enabled) return
        executor.execute {
            runCatching {
                val samples = synthesize(sfx)
                playPcm(samples)
            }
        }
    }

    private fun synthesize(sfx: ToySfx): ShortArray {
        return when (sfx) {
            ToySfx.SHOOT -> toneSweep(0.075f, 520f, 240f, 0.25f, noiseMix = 0.15f)
            ToySfx.HIT_ENEMY -> toneSweep(0.065f, 260f, 110f, 0.28f, noiseMix = 0.22f)
            ToySfx.PLAYER_HURT -> toneSweep(0.18f, 190f, 75f, 0.4f, noiseMix = 0.35f)
            ToySfx.DODGE -> toneSweep(0.11f, 300f, 580f, 0.2f, noiseMix = 0.28f)
            ToySfx.PICKUP_BUTTON -> musicBoxChime(listOf(880f, 1174f), 0.12f)
            ToySfx.PICKUP_ITEM -> musicBoxChime(listOf(523.25f, 659.25f, 783.99f, 1046.5f), 0.28f)
            ToySfx.REPAIR_STITCH -> musicBoxChime(listOf(440f, 554.37f, 659.25f), 0.22f)
            ToySfx.DOOR_OPEN -> toneSweep(0.22f, 140f, 95f, 0.28f, noiseMix = 0.2f)
            ToySfx.EXPLOSION -> toneSweep(0.32f, 120f, 38f, 0.5f, noiseMix = 0.7f)
            ToySfx.BOSS_ROAR -> toneSweep(0.45f, 150f, 55f, 0.45f, noiseMix = 0.4f)
            ToySfx.SYNERGY -> musicBoxChime(listOf(587.33f, 739.99f, 880f, 1174.66f), 0.36f)
            ToySfx.PART_DETACH -> toneSweep(0.24f, 420f, 90f, 0.38f, noiseMix = 0.3f)
        }
    }

    private fun toneSweep(
        durationSec: Float,
        startFreq: Float,
        endFreq: Float,
        gain: Float,
        noiseMix: Float = 0f
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-t * 4.2f)
            val tone = sin(phase).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f)
            val mixed = tone * (1f - noiseMix) + noise * noiseMix
            val sample = (mixed * env * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun musicBoxChime(notes: List<Float>, totalDurationSec: Float): ShortArray {
        val count = (sampleRate * totalDurationSec).toInt().coerceAtLeast(1)
        val buffer = ShortArray(count)
        val noteLen = count / notes.size.coerceAtLeast(1)
        notes.forEachIndexed { idx, freq ->
            val start = idx * noteLen
            val end = (start + noteLen * 1.4f).toInt().coerceAtMost(count)
            for (i in start until end) {
                val localT = (i - start).toFloat() / (end - start).coerceAtLeast(1)
                val env = exp(-localT * 5.0f) * 0.32f
                val s = sin(2.0 * PI * freq * (i - start) / sampleRate).toFloat()
                val add = (s * env * Short.MAX_VALUE).toInt()
                buffer[i] = (buffer[i] + add)
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
        return buffer
    }

    private fun playPcm(samples: ShortArray) {
        val byteCount = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(byteCount)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep((samples.size * 1000L / sampleRate) + 30L)
        track.stop()
        track.release()
    }
}
