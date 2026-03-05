package cz.findeis.metronomeglyph

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openToysManager()
        finish()
    }

    private fun openToysManager() {
        try {
            startActivity(Intent().apply {
                component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                )
            })
        } catch (_: Exception) {
            // Správce Toys není dostupný — aplikace se tiše zavře
        }
    }
}
