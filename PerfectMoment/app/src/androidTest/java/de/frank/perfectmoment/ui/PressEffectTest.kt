package de.frank.perfectmoment.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.PerfectMomentTheme
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves that the touch effects of the design actually reach the screen: pressing a card must lift
 * it, glow around it and brighten it. Renders the real components, presses them and compares the
 * pixels. Also writes both frames to the app's external files dir so they can be looked at.
 */
@RunWith(AndroidJUnit4::class)
class PressEffectTest {

    @get:Rule
    val rule = createComposeRule()

    /**
     * Streams the frame through logcat as base64 — the app's own directories are not reachable via
     * adb on this device, and this way the frames can be reassembled and looked at.
     */
    private fun save(bitmap: Bitmap, name: String) {
        val scale = 360f / bitmap.width
        val small = Bitmap.createScaledBitmap(
            bitmap,
            360,
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        val bytes = java.io.ByteArrayOutputStream().also {
            small.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        android.util.Log.i("PMFX", "BEGIN $name ${encoded.length}")
        encoded.chunked(2500).forEachIndexed { index, chunk ->
            android.util.Log.i("PMFX", "$name#$index#$chunk")
        }
        android.util.Log.i("PMFX", "END $name")
    }

    /** Share of pixels that differ noticeably between the two frames. */
    private fun changedShare(a: Bitmap, b: Bitmap): Float {
        var changed = 0
        var total = 0
        for (x in 0 until minOf(a.width, b.width) step 2) {
            for (y in 0 until minOf(a.height, b.height) step 2) {
                total++
                val pa = a.getPixel(x, y)
                val pb = b.getPixel(x, y)
                val diff = abs((pa shr 16 and 0xFF) - (pb shr 16 and 0xFF)) +
                    abs((pa shr 8 and 0xFF) - (pb shr 8 and 0xFF)) +
                    abs((pa and 0xFF) - (pb and 0xFF))
                if (diff > 6) changed++
            }
        }
        return changed.toFloat() / total
    }

    private fun captureBoth(name: String, content: @androidx.compose.runtime.Composable () -> Unit): Float {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            PerfectMomentTheme(appearance = "dark") {
                Box(
                    Modifier.size(260.dp),
                    contentAlignment = Alignment.Center,
                    content = { content() },
                )
            }
        }
        rule.mainClock.advanceTimeBy(500)
        val idle = rule.onRoot().captureToImage().asAndroidBitmap()
        save(idle, "${name}_ruhe.png")

        rule.onNodeWithTag("ziel").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(400) // press transition is 240ms
        val pressed = rule.onRoot().captureToImage().asAndroidBitmap()
        save(pressed, "${name}_gedrueckt.png")

        return changedShare(idle, pressed)
    }

    @Test
    fun karteHebtSichUndLeuchtetBeimDruecken() {
        val share = captureBoth("karte") {
            val colors = LocalPmColors.current
            Box(
                Modifier.testTag("ziel").size(168.dp)
                    .pmClickable(shape = RoundedCornerShape(32.dp), lift = true, pressBorder = colors.gold) {}
                    .pmGlassSurface(colors, 32),
            )
        }
        assertTrue(
            "Die Karte muss sich beim Druecken sichtbar veraendern, geaendert: ${share * 100}%",
            share > 0.02f,
        )
    }

    @Test
    fun startknopfWechseltBeimDruecken() {
        val share = captureBoth("startknopf") {
            Box(Modifier.testTag("ziel").size(width = 220.dp, height = 60.dp)) {
                PrimaryButton("Sitzung beginnen", onClick = {}, height = 60, textSize = 17)
            }
        }
        assertTrue(
            "Der Startknopf muss beim Druecken sichtbar wechseln, geaendert: ${share * 100}%",
            share > 0.05f,
        )
    }
}
