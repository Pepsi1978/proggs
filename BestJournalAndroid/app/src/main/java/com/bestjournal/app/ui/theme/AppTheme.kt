package com.bestjournal.app.ui.theme

/**
 * Named app themes available in the Settings → Erscheinungsbild → Themes Manager dropdown.
 *
 * Hell- und Dunkel-Variante kommen aus dem aktuellen Material3 ColorScheme — der Hell/Dunkel-
 * Switch oben in der Bubble entscheidet welche Variante gerade gilt. Themes 3-7 sind bekannte
 * Editor-Themes (Solarized, Dracula, One Dark, Nord, Gruvbox), 1:1 mit den offiziellen Specs.
 *
 * Die ColorScheme-Auswahl liegt in [Theme.kt] (when-Block in BestJournalTheme).
 */
enum class AppTheme(val storageKey: String) {
    Profile("profile"),
    Neutral("neutral"),
    Solarized("solarized"),
    Dracula("dracula"),
    OneDark("one_dark"),
    Nord("nord"),
    Gruvbox("gruvbox");

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: Neutral
    }
}
