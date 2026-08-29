package de.frank.claudekompass.observability

/**
 * Logik-Sonde: prueft eine Erwartung und meldet ihr Scheitern, ohne den Ablauf zu stoppen.
 *
 * Sie ist gegen die STILLEN Fehler gerichtet — die, bei denen nichts abstuerzt und trotzdem
 * etwas Falsches herauskommt (leere Liste statt Inhalt, doppelter Eintrag, Zustand, den es
 * nicht geben duerfte). Rueckgabewert ist die gepruefte Bedingung, damit die Sonde direkt in
 * ein `if` gesetzt werden kann.
 */
fun probe(
    condition: Boolean,
    message: String,
    module: String,
    function: String,
    context: Map<String, Any?> = emptyMap(),
): Boolean {
    if (!condition) {
        KompassLog.warn(module, function, "SONDE verletzt: $message", context)
    }
    return condition
}

/** Meldet einen Zustandsuebergang — die zweite Haelfte der stillen Fehler lebt in Uebergaengen. */
fun probeTransition(
    module: String,
    function: String,
    from: String,
    to: String,
    context: Map<String, Any?> = emptyMap(),
) {
    KompassLog.debug(module, function, "Uebergang $from -> $to", context)
}
