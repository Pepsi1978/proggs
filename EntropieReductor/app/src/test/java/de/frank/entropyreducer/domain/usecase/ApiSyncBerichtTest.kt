package de.frank.entropyreducer.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Alter Whoop-Fehler im gespeicherten Abgleich nach späterem Erfolg (Auftrag DDSWK, 15.10.2026). */
class ApiSyncBerichtTest {

    private val min = 60_000L
    private val elfUhr = 1_800_000_000_000L
    private val bericht = elfUhr + 5 * 60 * min // Abgleich mit "Anmeldung abgelaufen"
    private val danach = bericht + 10 * min // Abruf nach neuer Anmeldung

    private fun mitWhoop(ok: Boolean, zuletztOk: Long = elfUhr) = ApiSyncBericht(
        versuchMs = bericht,
        anlass = "Hintergrund",
        quellen = listOf(
            QuellenStand(
                id = ForegroundSyncManager.QUELLE_WHOOP, name = "Whoop", verbunden = true, ok = ok, anzahl = 0,
                fehler = if (ok) null else "Anmeldung abgelaufen", zuletztOkMs = zuletztOk, eingriffNoetig = !ok,
            ),
            QuellenStand(
                id = ForegroundSyncManager.QUELLE_OURA, name = "Oura", verbunden = true, ok = true, anzahl = 3,
                fehler = null, zuletztOkMs = bericht, eingriffNoetig = false,
            ),
        ),
    )

    private fun whoop(b: ApiSyncBericht) = b.quellen.first { it.id == ForegroundSyncManager.QUELLE_WHOOP }

    @Test
    fun `Erfolg nach neuer Anmeldung ersetzt den alten Fehler`() {
        val neu = mitWhoop(ok = false).mitSpaeteremWhoopErfolg(danach, anmeldungAbgelaufen = false, fehlversuche = 0)
        assertThat(whoop(neu).ok).isTrue()
        assertThat(whoop(neu).fehler).isNull()
        assertThat(whoop(neu).eingriffNoetig).isFalse()
        assertThat(whoop(neu).zuletztOkMs).isEqualTo(danach)
        assertThat(neu.probleme).isEmpty()
    }

    @Test
    fun `markierte abgelaufene Anmeldung bleibt sichtbar`() {
        val neu = mitWhoop(ok = false).mitSpaeteremWhoopErfolg(danach, anmeldungAbgelaufen = true, fehlversuche = 0)
        assertThat(whoop(neu).ok).isFalse()
        assertThat(neu.probleme).hasSize(1)
    }

    @Test
    fun `gescheiterter Versuch nach dem Erfolg bleibt sichtbar`() {
        val neu = mitWhoop(ok = false).mitSpaeteremWhoopErfolg(danach, anmeldungAbgelaufen = false, fehlversuche = 1)
        assertThat(whoop(neu).ok).isFalse()
    }

    @Test
    fun `Erfolg vor dem Fehlerbericht heilt nichts`() {
        val neu = mitWhoop(ok = false).mitSpaeteremWhoopErfolg(bericht - min, anmeldungAbgelaufen = false, fehlversuche = 0)
        assertThat(whoop(neu).ok).isFalse()
    }

    @Test
    fun `geglueckter Stand bekommt die neuere Erfolgszeit`() {
        val neu = mitWhoop(ok = true, zuletztOk = bericht).mitSpaeteremWhoopErfolg(danach, anmeldungAbgelaufen = false, fehlversuche = 0)
        assertThat(whoop(neu).ok).isTrue()
        assertThat(whoop(neu).zuletztOkMs).isEqualTo(danach)
    }
}
