package de.frank.kompass.update

class DokuFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)
