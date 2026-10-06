package de.frank.updatestation

enum class DesignModus(val titel: String) {
    HELL("Hell"),
    AUTOMATISCH("Automatisch"),
    DUNKEL("Dunkel");

    fun istDunkel(systemDunkel: Boolean): Boolean = when (this) {
        HELL -> false
        AUTOMATISCH -> systemDunkel
        DUNKEL -> true
    }

    companion object {
        fun ausGespeichert(wert: String?): DesignModus =
            entries.firstOrNull { it.name == wert } ?: AUTOMATISCH
    }
}
