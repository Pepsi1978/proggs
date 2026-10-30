package de.frank.updatestation

enum class DesignModus(val titel: String) {
    HELL("Hell"),
    AUTOMATISCH("Automatisch"),
    DUNKEL("Dunkel");

    /** [sonneDunkel] kommt aus der Sonnen-Automatik in Neuenhagen (rememberSonneDunkel), nicht vom System. */
    fun istDunkel(sonneDunkel: Boolean): Boolean = when (this) {
        HELL -> false
        AUTOMATISCH -> sonneDunkel
        DUNKEL -> true
    }

    companion object {
        fun ausGespeichert(wert: String?): DesignModus =
            entries.firstOrNull { it.name == wert } ?: AUTOMATISCH
    }
}
