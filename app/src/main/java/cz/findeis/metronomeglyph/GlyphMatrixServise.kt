package cz.findeis.metronomeglyph

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager

abstract class GlyphMatrixService : Service() {

    protected var mGM: GlyphMatrixManager? = null
    private var mConnected = false

    protected val mCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            mConnected = true
            mGM?.register(Glyph.DEVICE_23112)
            onGlyphConnected()
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            mConnected = false
            onGlyphDisconnected()
        }
    }

    // Toy lifecycle: start on bind, stop on unbind
    override fun onBind(intent: Intent?): IBinder? {
        mGM = GlyphMatrixManager.getInstance(this)
        mGM?.init(mCallback)
        return onGlyphBind()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        onGlyphUnbind()
        if (mConnected) {
            mGM?.turnOff()
            mGM?.unInit()
        }
        mGM = null
        return false
    }

    open fun onGlyphBind(): IBinder? = null
    open fun onGlyphUnbind() {}
    open fun onGlyphConnected() {}
    open fun onGlyphDisconnected() {}

    protected fun displayFrame(pixels: IntArray) {
        val frame = GlyphMatrixFrame.Builder()
            .addLow(pixels)
            .build(this)
        mGM?.setMatrixFrame(frame.render())
    }

    protected fun clearDisplay() {
        val frame = GlyphMatrixFrame.Builder()
            .addLow(IntArray(625))
            .build(this)
        mGM?.setMatrixFrame(frame.render())
    }
}
