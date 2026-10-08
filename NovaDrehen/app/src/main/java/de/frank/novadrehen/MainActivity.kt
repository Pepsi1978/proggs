package de.frank.novadrehen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var protokoll: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        status = TextView(this).apply { textSize = 17f }
        protokoll = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
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
                    "Solange der ChatGPT-Sprachmodus (Kugel über die Seitentaste) offen ist, " +
                    "dreht sich der Bildschirm nicht, damit das Gespräch nicht abbricht. " +
                    "Alle anderen Apps drehen sich weiter frei.\n" +
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
            addView(TextView(this@MainActivity).apply {
                text = "\nProtokoll (neueste Zeile oben). Bei Fehlverhalten: App öffnen und Bildschirmfoto machen."
                textSize = 15f
            })
            addView(Button(this@MainActivity).apply {
                text = "Protokoll aktualisieren"
                setOnClickListener { protokoll.text = Protokoll.alle() }
            })
            addView(protokoll)
        }
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onResume() {
        super.onResume()
        val dienst = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains("$packageName/") == true
        val schreiben = Settings.System.canWrite(this)
        status.text = "Bedienungshilfe: ${if (dienst) "an ✓" else "aus ✗"}\n" +
            "Systemeinstellungen ändern: ${if (schreiben) "erlaubt ✓" else "nicht erlaubt ✗"}\n"
        protokoll.text = Protokoll.alle()
    }
}
