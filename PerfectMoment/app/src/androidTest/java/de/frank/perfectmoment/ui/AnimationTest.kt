package de.frank.perfectmoment.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.frank.perfectmoment.ui.theme.BreathingBackground
import de.frank.perfectmoment.ui.theme.PerfectMomentTheme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the endless animations of the design really run — in both appearances. Renders each effect
 * at several points in time and requires the frames to differ. The breathing background used to be
 * switched off completely in the dark theme, which this test would now catch.
 */
@RunWith(AndroidJUnit4::class)
class AnimationTest {

    @get:Rule
    val rule = createComposeRule()

    private fun logFrame(bitmap: Bitmap, name: String) {
        val scale = 300f / bitmap.width
        val small = Bitmap.createScaledBitmap(
            bitmap,
            300,
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        val bytes = java.io.ByteArrayOutputStream().also {
            small.compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        encoded.chunked(2500).forEachIndexed { index, chunk ->
            android.util.Log.i("PMFX", "$name#$index#$chunk")
        }
    }

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
                if (diff > 2) changed++
            }
        }
        return changed.toFloat() / total
    }

    /** Renders [content] at two points in time and returns how much of the frame moved. */
    private fun animates(
        name: String,
        appearance: String,
        firstMs: Long,
        secondMs: Long,
        content: @Composable () -> Unit,
    ): Float {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            PerfectMomentTheme(appearance = appearance) { content() }
        }
        rule.mainClock.advanceTimeBy(firstMs)
        val first = rule.onRoot().captureToImage().asAndroidBitmap()
        logFrame(first, "${name}_frueh.png")
        rule.mainClock.advanceTimeBy(secondMs - firstMs)
        val second = rule.onRoot().captureToImage().asAndroidBitmap()
        logFrame(second, "${name}_spaet.png")
        return changedShare(first, second)
    }

    @Test
    fun hintergrundAtmetImDunkelmodus() {
        val share = animates("atmen_dunkel", "dark", 200, 9_000) {
            BreathingBackground { Box(Modifier.fillMaxSize()) }
        }
        assertTrue("Das Atmen muss auch im Dunkelmodus laufen, bewegt: ${share * 100}%", share > 0.05f)
    }

    @Test
    fun hintergrundAtmetImHellmodus() {
        val share = animates("atmen_hell", "light", 200, 9_000) {
            BreathingBackground { Box(Modifier.fillMaxSize()) }
        }
        assertTrue("Das Atmen muss im Hellmodus laufen, bewegt: ${share * 100}%", share > 0.05f)
    }

    @Test
    fun startknopfHatGoldfluss() {
        val share = animates("goldfluss", "dark", 200, 3_800) {
            Box(Modifier.size(width = 260.dp, height = 60.dp)) {
                PrimaryButton("Sitzung beginnen", onClick = {}, height = 60, textSize = 17)
            }
        }
        assertTrue("Der Goldfluss muss ueber den Knopf wandern, bewegt: ${share * 100}%", share > 0.05f)
    }

    @Test
    fun aufnahmeknopfPulsiert() {
        val share = animates("aufnahme", "dark", 200, 1_500) {
            Box(Modifier.size(200.dp)) {
                RecorderControl(state = RecordingState.IDLE, message = null, onClick = {})
            }
        }
        assertTrue("Der Aufnahmeknopf muss pulsieren, bewegt: ${share * 100}%", share > 0.01f)
    }
}
