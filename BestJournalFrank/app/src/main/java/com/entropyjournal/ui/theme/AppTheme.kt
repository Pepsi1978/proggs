package com.entropyjournal.ui.theme

/**
 * Named app themes available in Settings → Erscheinungsbild → Themes Manager dropdown.
 *
 * Hell- und Dunkel-Variante kommen aus dem aktuellen Material3 ColorScheme — der Hell/Dunkel-
 * Switch oben in der Bubble entscheidet welche Variante gerade gilt. Persistierte Keys der
 * bisherigen Themes bleiben unverändert; unbekannte oder fehlende Keys fallen auf Goldener Faden.
 *
 * Die ColorScheme-Auswahl liegt in [Theme.kt] (when-Block in EntropyJournalTheme).
 *
 * Die Reihenfolge entspricht dem Themes Manager des bestätigten Designs.
 */
enum class AppTheme(
    val storageKey: String,
    val displayName: String,
) {
    GoldenThread("golden_thread", "Goldener Faden"),
    Profile("profile", "Profilfarbe — Farbe des Dashboard-Profils"),
    Neutral("neutral", "Neutral"),
    Solarized("solarized", "Sonnenwende"),
    Dracula("dracula", "Mitternacht"),
    OneDark("one_dark", "Atelier"),
    Nord("nord", "Polarnacht"),
    Gruvbox("gruvbox", "Bernstein"),
    Cosmos("cosmos", "Cosmos"),
    Aurora("aurora", "Aurora"),
    PolarLight("polar_light", "Polarlicht"),
    Nebula("nebula", "Nebula"),
    EmeraldForest("emerald_forest", "Smaragdwald"),
    SunEmber("sun_ember", "Sonnenglut");

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: GoldenThread
    }
}
