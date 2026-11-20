package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Procedural PCM Tactical Audio & Voice Chat Monitor.
 * Generates zero-latency gunshot acoustics, hit-markers, Indian-inspired intro music,
 * victory fanfares, and real AudioRecord microphone voice-activity telemetry.
 */
object TacticalAudioEngine {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var musicVolume: Float = 0.7f
    @Volatile
    var sfxVolume: Float = 0.9f
    @Volatile
    var isMuted: Boolean = false

    private var introMusicJob: Job? = null
    private var voiceMonitorJob: Job? = null

    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    private val _isMicCapturing = MutableStateFlow(false)
    val isMicCapturing: StateFlow<Boolean> = _isMicCapturing.asStateFlow()

    fun playUiClick() {
        if (isMuted || sfxVolume <= 0.01f) return
        playBufferAsync(generateToneBurst(880.0, 40, 0.25f * sfxVolume, decay = 18.0))
    }

    fun playGunshot(category: String) {
        if (isMuted || sfxVolume <= 0.01f) return
        val vol = sfxVolume
        val buffer = when (category) {
            "SNIPER" -> generateGunshotBuffer(durationMs = 260, baseFreq = 110.0, noiseMix = 0.85, punch = 1.0f * vol)
            "SHOTGUN" -> generateGunshotBuffer(durationMs = 220, baseFreq = 85.0, noiseMix = 0.92, punch = 0.95f * vol)
            "SMG" -> generateGunshotBuffer(durationMs = 85, baseFreq = 210.0, noiseMix = 0.65, punch = 0.65f * vol)
            "MELEE" -> generateToneBurst(640.0, 140, 0.7f * vol, decay = 9.0)
            else -> generateGunshotBuffer(durationMs = 125, baseFreq = 155.0, noiseMix = 0.75, punch = 0.8f * vol)
        }
        playBufferAsync(buffer)
    }

    fun playHitMarker(isHeadshot: Boolean) {
        if (isMuted || sfxVolume <= 0.01f) return
        val freq = if (isHeadshot) 1680.0 else 1120.0
        playBufferAsync(generateToneBurst(freq, 65, 0.45f * sfxVolume, decay = 16.0))
    }

    fun playReload() {
        if (isMuted || sfxVolume <= 0.01f) return
        playBufferAsync(generateToneBurst(420.0, 90, 0.35f * sfxVolume, decay = 12.0))
    }

    fun playVictoryFanfare() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            // Raga-inspired heroic ascending brass/synth notes: Sa - Ga - Pa - Sa'
            val notes = listOf(261.63, 329.63, 392.00, 523.25)
            notes.forEachIndexed { idx, freq ->
                val dur = if (idx == notes.lastIndex) 420 else 160
                playBufferBlocking(generateToneBurst(freq, dur, 0.55f * sfxVolume, decay = 4.5))
            }
        }
    }

    fun startIntroTheme() {
        stopIntroTheme()
        if (isMuted || musicVolume <= 0.01f) return
        introMusicJob = scope.launch {
            // Epic Indian-inspired tactical theme loop (Bhairav / heroic pulse)
            val melody = listOf(146.83, 155.56, 196.00, 220.00, 293.66, 220.00, 196.00, 155.56)
            var step = 0
            while (isActive) {
                if (!isMuted && musicVolume > 0.01f) {
                    val freq = melody[step % melody.size]
                    val bass = freq * 0.5
                    val samples = generateIntroBeat(freq, bass, 260, 0.40f * musicVolume)
                    playBufferBlocking(samples)
                    step++
                } else {
                    kotlinx.coroutines.delay(150)
                }
            }
        }
    }

    fun stopIntroTheme() {
        introMusicJob?.cancel()
        introMusicJob = null
    }

    @SuppressLint("MissingPermission")
    fun startVoiceMicMonitor() {
        if (voiceMonitorJob?.isActive == true) return
        voiceMonitorJob = scope.launch {
            try {
                val minBuf = AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1024)

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    _isMicCapturing.value = false
                    return@launch
                }

                recorder.startRecording()
                _isMicCapturing.value = true
                val readBuf = ShortArray(minBuf / 2)
                while (isActive) {
                    val read = recorder.read(readBuf, 0, readBuf.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val norm = readBuf[i] / 32768.0
                            sumSq += norm * norm
                        }
                        val rms = sqrt(sumSq / read).toFloat()
                        _micLevel.value = (rms * 5.5f).coerceIn(0f, 1f)
                    }
                }
                recorder.stop()
                recorder.release()
            } catch (e: Exception) {
                Log.w("TacticalAudio", "AudioRecord unavailable: ${e.message}")
            } finally {
                _isMicCapturing.value = false
                _micLevel.value = 0f
            }
        }
    }

    fun stopVoiceMicMonitor() {
        voiceMonitorJob?.cancel()
        voiceMonitorJob = null
        _isMicCapturing.value = false
        _micLevel.value = 0f
    }

    private fun playBufferAsync(samples: ShortArray) {
        scope.launch { playBufferBlocking(samples) }
    }

    private fun playBufferBlocking(samples: ShortArray) {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferBytes = maxOf(minBuf, samples.size * 2)
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
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bufferBytes)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(((samples.size * 1000L) / SAMPLE_RATE) + 15L)
            track.stop()
            track.release()
        } catch (e: Exception) {
            Log.w("TacticalAudio", "Audio playback skipped: ${e.message}")
        }
    }

    private fun generateToneBurst(freq: Double, durationMs: Int, gain: Float, decay: Double): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-decay * t)
            val s = sin(2.0 * PI * freq * t) * env * gain
            (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateGunshotBuffer(durationMs: Int, baseFreq: Double, noiseMix: Double, punch: Float): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-22.0 * t)
            val pitchSweep = baseFreq * exp(-18.0 * t) + 45.0
            val thump = sin(2.0 * PI * pitchSweep * t)
            val crack = (Random.nextDouble() * 2.0 - 1.0)
            val mixed = ((1.0 - noiseMix) * thump + noiseMix * crack) * env * punch
            (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun generateIntroBeat(leadFreq: Double, bassFreq: Double, durationMs: Int, gain: Float): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val leadEnv = exp(-4.8 * t)
            val drumEnv = exp(-24.0 * t)
            val lead = (sin(2.0 * PI * leadFreq * t) + 0.35 * sin(2.0 * PI * leadFreq * 2.0 * t)) * leadEnv
            val bass = sin(2.0 * PI * bassFreq * t) * 0.65
            val dhol = sin(2.0 * PI * 75.0 * exp(-12.0 * t) * t) * drumEnv * 0.9
            val mixed = (lead * 0.45 + bass * 0.35 + dhol * 0.45) * gain
            (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
