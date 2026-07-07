package com.entropyjournal.ui.theme

/**
 * Named app themes available in Settings → Erscheinungsbild → Themes Manager dropdown.
 *
 * Hell- und Dunkel-Variante kommen aus dem aktuellen Material3 ColorScheme — der Hell/Dunkel-
 * Switch oben in der Bubble entscheidet welche Variante gerade gilt. Themes 3-7 sind bekannte
 * Editor-Themes (Solarized, Dracula, One Dark, Nord, Gruvbox), 1:1 mit den offiziellen Specs.
 *
 * Die ColorScheme-Auswahl liegt in [Theme.kt] (when-Block in EntropyJournalTheme).
 *
 * 1:1 von BestJournalAndroid.
 */
enum class AppTheme(val storageKey: String) {
    Profile("profile"),
    Neutral("neutral"),
    Solarized("solarized"),
    Dracula("dracula"),
    OneDark("one_dark"),
    Nord("nord"),
    Gruvbox("gruvbox"),
    Cosmos("cosmos"),
    Aurora("aurora");

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: Profile
    }
}
