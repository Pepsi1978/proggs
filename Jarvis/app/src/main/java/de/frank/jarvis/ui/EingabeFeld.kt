package de.frank.jarvis.ui

/** Die Regel, welche Meldungen des Textfelds der Eingabeblase in den Entwurf dürfen. Rein und ohne Android (EingabeFeldTest). */
object EingabeFeld {
    /**
     * Verschwindet das Feld mit Fokus, meldet Compose noch einmal seinen letzten Stand (TextFieldDelegate.onBlur); nach dem Senden
     * wäre das der schon abgeschickte Text. Deshalb gilt eine Meldung nur, solange das Feld sichtbar ist und auf dem aktuellen Entwurf
     * beruht. Hat sich der Entwurf gerade von außen geändert (Senden, Diktat), verfällt sie, bis das Feld den neuen Stand kennt.
     */
    fun meldungGilt(eingeklappt: Boolean, feldText: String, entwurf: String, gemeldet: String): Boolean =
        !eingeklappt && (feldText == entwurf || gemeldet == entwurf)
}
