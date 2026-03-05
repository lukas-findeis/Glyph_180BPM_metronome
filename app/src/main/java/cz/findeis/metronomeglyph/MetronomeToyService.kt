package cz.findeis.metronomeglyph

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.GlyphToy
import kotlinx.coroutines.*

class MetronomeToyService : GlyphMatrixService() {

    companion object {
        private const val BPM = 180
        private const val INTERVAL_MS = 60_000L / BPM
        private const val FLASH_MS = 60L
        private const val BRIGHT = 4095
        private const val DIM = 400
        private const val OFF = 0
    }

    private var metronomeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var soundPool: SoundPool
    private var tickSoundId: Int = 0
    private var soundLoaded = false

    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val event = msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    if (event == GlyphToy.EVENT_CHANGE) {
                        if (metronomeJob?.isActive == true) {
                            stopMetronome()
                        } else {
                            startMetronome()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }
    private val serviceMessenger = Messenger(serviceHandler)

    override fun onCreate() {
        super.onCreate()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
        soundPool.setOnLoadCompleteListener { _, _, status ->
            soundLoaded = (status == 0)
        }
        try {
            val resId = resources.getIdentifier("tick", "raw", packageName)
            if (resId != 0) tickSoundId = soundPool.load(this, resId, 1)
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        stopMetronome()
        scope.cancel()
        soundPool.release()
        super.onDestroy()
    }

    override fun onGlyphBind(): IBinder = serviceMessenger.binder
    override fun onGlyphUnbind() = stopMetronome()
    override fun onGlyphConnected() = startMetronome()
    override fun onGlyphDisconnected() = stopMetronome()

    private fun startMetronome() {
        metronomeJob?.cancel()
        metronomeJob = scope.launch {
            while (isActive) {
                val tickStart = System.currentTimeMillis()
                tick()
                val elapsed = System.currentTimeMillis() - tickStart
                val remaining = INTERVAL_MS - elapsed
                if (remaining > 0) delay(remaining)
            }
        }
    }

    private fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
        clearDisplay()
    }

    private suspend fun tick() {
        displayFrame(buildTickFrame())
        delay(20L)
        withContext(Dispatchers.Main) {
            if (soundLoaded && tickSoundId != 0) {
                soundPool.play(tickSoundId, 1f, 1f, 1, 0, 1f)
            }
        }
        delay(FLASH_MS)
        clearDisplay()
    }

    private fun buildTickFrame(): IntArray {
        val pixels = IntArray(625) { OFF }
        val mid = 12
        for (d in 1..11) {
            pixels[(mid - d) * 25 + mid] = if (d <= 3) BRIGHT else DIM
            pixels[(mid + d) * 25 + mid] = if (d <= 3) BRIGHT else DIM
            pixels[mid * 25 + (mid - d)] = if (d <= 3) BRIGHT else DIM
            pixels[mid * 25 + (mid + d)] = if (d <= 3) BRIGHT else DIM
        }
        pixels[mid * 25 + mid] = BRIGHT
        return pixels
    }
}