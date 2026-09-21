package de.frank.novadrehen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        status = TextView(this).apply { textSize = 17f }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            addView(TextView(this@MainActivity).apply {
                text = "Nova Drehen"
                textSize = 26f
            })
            addView(TextView(this@MainActivity).apply {
                text = "Version ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_BUMPED_AT}\n\n" +
                    "Zugeklappt bleibt der Nova-Startbildschirm im Hochformat. " +
                    "Apps und der aufgeklappte Bildschirm drehen sich weiter frei.\n" +
                    "„Automatisch drehen“ in den Schnelleinstellungen muss an sein.\n"
                textSize = 15f
            })
            addView(status)
            addView(Button(this@MainActivity).apply {
                text = "Bedienungshilfe einschalten"
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            })
            addView(Button(this@MainActivity).apply {
                text = "Systemeinstellungen ändern erlauben"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
                }
            })
        }
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        val dienst = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains("$packageName/") == true
        val schreiben = Settings.System.canWrite(this)
        status.text = "Bedienungshilfe: ${if (dienst) "an ✓" else "aus ✗"}\n" +
            "Systemeinstellungen ändern: ${if (schreiben) "erlaubt ✓" else "nicht erlaubt ✗"}\n"
    }
}
