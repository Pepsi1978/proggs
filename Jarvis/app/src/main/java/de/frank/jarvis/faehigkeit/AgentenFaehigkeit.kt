package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.agent.Agenten
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die Agenten von Jarvis und sein Aktivitäts-Protokoll. Steht im Plugin und im eigenen Chat (auch „Hey Jarvis“), damit
 * eine große Recherche überall an einen Agenten geht statt im kurzen Gespräch zu scheitern. Agenten selbst und die
 * Tagesauswertung bekommen diese Werkzeuge nicht (siehe JarvisAgent.versuche): Agenten starten keine Agenten.
 */
class AgentenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "agenten"
    override val name = "Agenten und Protokoll"
    override val beschreibung = "Agenten für längere Arbeiten beauftragen und verwalten; das Aktivitäts-Protokoll von Jarvis lesen (auch Fehler)."
    override val hinweise =
        "Für Recherchen, Ausarbeitungen, Berichte als PDF (auch mit Bildern) und alles, was Internet braucht oder länger als eine Minute dauert, startest du mit agent_starten " +
            "einen Agenten (meist Recherche). Er arbeitet bis zu 25 Minuten im Hintergrund, legt Bericht, Bilder und PDF in die Ablage und meldet sich per Benachrichtigung. " +
            "Gib ihm den vollständigen Auftrag mit, auch die gewünschte Form (zum Beispiel „als PDF mit Illustrationen“). Versuche so etwas nie selbst im Gespräch. " +
            "Fragt Frank, warum etwas nicht geklappt hat, oder meldet ein Werkzeug einen Fehler, lies mit protokoll_lesen nach, was Jarvis zuletzt getan hat und woran es scheiterte."

    override fun stoerung(): String? = null

    private val app = context.applicationContext

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "agenten",
                titel = "Agenten ansehen und verwalten",
                beschreibung = "Jarvis: die Agenten von Jarvis. Ohne aktion (oder aktion=liste) kommt die Liste der vorhandenen Agenten (zum Beispiel Recherche, Machbarkeit und selbst angelegte) und wer gerade arbeitet. " +
                    "Mit aktion=anlegen baust du einen neuen Agenten oder änderst einen: ein Name plus eine Rolle, also die Anweisung, wie er an Aufträge herangeht und wie sein Ergebnis aussieht – " +
                    "wenn Frank sich einen Spezialisten wünscht (zum Beispiel „Trainingsplaner“, „App-Architekt“); formuliere die Rolle selbst ausführlich. Mit aktion=loeschen entfernst du einen selbst angelegten Agenten. " +
                    "Beauftragt wird ein Agent mit agent_starten.",
                schema = schema(
                    "aktion" to text("liste (Vorgabe), anlegen oder loeschen.", listOf("liste", "anlegen", "loeschen")),
                    "name" to text("Bei anlegen und loeschen: Name des Agenten, ein bis zwei Wörter."),
                    "rolle" to text("Bei anlegen: die Rolle in fünf bis zehn Sätzen: Aufgabe, Vorgehen in Schritten, worauf zu achten ist, Form des Ergebnisses."),
                ),
                nurLesen = false,
            ) { a ->
                when (a.optString("aktion", "liste").lowercase()) {
                    "anlegen" ->
                        if (a.optString("name").isBlank() || a.optString("rolle").length < 40) Ergebnis("Name und eine ausführliche Rolle sind nötig.", fehler = true)
                        else Agenten.speichere(app, a.optString("name"), a.optString("rolle")).let { Ergebnis("Agent „${it.name}“ ist angelegt und kann mit agent_starten beauftragt werden.") }
                    "loeschen" -> if (Agenten.loesche(app, a.optString("name"))) Ergebnis("Agent „${a.optString("name")}“ gelöscht.") else Ergebnis("Kein selbst angelegter Agent mit diesem Namen.", fehler = true)
                    else -> {
                        val laufend = Agenten.laeufe.value
                        Ergebnis(buildString {
                            append("Agenten von Jarvis:\n")
                            Agenten.alle(app).forEach { append("- ").append(it.name).append(if (it.vorgegeben) " (eingebaut)" else " (selbst angelegt)").append(": ").append(it.rolle.take(220)).append('\n') }
                            if (laufend.isEmpty()) append("Gerade arbeitet kein Agent.")
                            else laufend.forEach { append("Arbeitet gerade: ").append(it.agent).append(" seit ").append((System.currentTimeMillis() - it.seit) / 60_000).append(" Minuten an „").append(it.auftrag.take(100)).append("“").append(if (it.schritt.isEmpty()) "" else ", aktueller Schritt: ${it.schritt}").append('\n') }
                        }.trim())
                    }
                }
            },
            Werkzeug(
                name = "agent_starten",
                titel = "Agenten beauftragen",
                beschreibung = "Jarvis: beauftragt einen Agenten von Jarvis mit einer längeren Arbeit, zum Beispiel einer Internet-Recherche („Jarvis, recherchiere, wie man … am besten baut“), " +
                    "einem Bericht als PDF mit Illustrationen, einer Machbarkeitsprüfung einer Idee, oder den Agenten Code-Analyse mit der Auswertung einer App in Franks Repo (er liest den Code und liefert den Stand samt fertigem Auftrag für ein Programmier-Werkzeug; er ändert nichts). " +
                    "Der Agent arbeitet bis zu 25 Minuten selbstständig im Hintergrund auf Franks Handy (eigenes Modell, Internet-Suche, Bilderzeugung, Zugriff auf Ideen, " +
                    "Aufgaben, Kalender, Biomarker), legt Bericht, Bilder und auf Wunsch ein PDF in die Ablage und meldet sich per Benachrichtigung. Dieser Aufruf kehrt sofort zurück. " +
                    "Das Ergebnis holst du später mit ablage_lesen (ohne titel die Liste, mit titel der Bericht). Mit per_mail wird der Bericht zusätzlich an Frank gemailt.",
                schema = schema(
                    "agent" to text("Name des Agenten, zum Beispiel Recherche oder Machbarkeit (siehe agenten)."),
                    "auftrag" to text("Der vollständige Auftrag mit allem Wissen aus dem Gespräch: Ziel, Rahmen, was Frank wichtig ist, gewünschte Form des Ergebnisses (zum Beispiel „als PDF mit Illustrationen“)."),
                    "per_mail" to schalter("true = den fertigen Bericht zusätzlich per E-Mail an Frank schicken."),
                    "vorlesen" to schalter("true = Jarvis liest den fertigen Bericht auf dem Handy laut vor, sobald er da ist."),
                    pflicht = listOf("agent", "auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                val plan = Agenten.finde(app, a.optString("agent"))
                when {
                    plan == null -> Ergebnis("Diesen Agenten gibt es nicht. Vorhanden: " + Agenten.alle(app).joinToString { it.name } + ". Mit agenten (aktion=anlegen) lässt sich ein neuer bauen.", fehler = true)
                    a.optString("auftrag").isBlank() -> Ergebnis("Der Auftrag ist leer.", fehler = true)
                    !CodexAuthManager(app).isConnected -> Ergebnis("Jarvis ist nicht mit seinem Modell verbunden. Frank muss sich in Jarvis unter Einstellungen bei ChatGPT anmelden.", fehler = true)
                    Agenten.laeufe.value.size >= 2 -> Ergebnis("Es arbeiten schon zwei Agenten. Bitte warten, bis einer fertig ist.", fehler = true)
                    else -> {
                        JarvisDienst.agentStarten(app, plan.name, a.optString("auftrag"), a.optBoolean("per_mail"), a.optBoolean("vorlesen"))
                        Ergebnis("Der Agent ${plan.name} arbeitet jetzt, meist fünf bis zwanzig Minuten. Das Ergebnis landet in der Ablage" + (if (a.optBoolean("per_mail")) " und kommt per E-Mail" else "") + "; Frank bekommt eine Benachrichtigung.")
                    }
                }
            },
            Werkzeug(
                name = "protokoll_lesen",
                titel = "Protokoll lesen",
                beschreibung = "Jarvis: liest das Aktivitäts-Protokoll von Jarvis (dasselbe wie in der App unter Aktivität): was Jarvis, ChatGPT und die Agenten zuletzt getan haben, " +
                    "mit Uhrzeit, Werkzeug, Ergebnis und Fehlern. Nutze es, wenn Frank fragt, warum etwas nicht geklappt hat, was ein Agent gemacht hat oder was zuletzt passiert ist.",
                schema = schema(
                    "anzahl" to zahl("Wie viele Einträge, neueste zuerst; Vorgabe 40, höchstens 200."),
                    "nur_fehler" to schalter("true = nur fehlgeschlagene Einträge."),
                    "suche" to text("Optional: nur Einträge, deren Werkzeug oder Text dieses Wort enthält (zum Beispiel „Agent“, „Bild“, „PDF“)."),
                ),
                nurLesen = true,
            ) { a ->
                val anzahl = a.optInt("anzahl", 40).coerceIn(1, 200)
                val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
                val treffer = Protokoll.eintraege.value
                    .filter { !a.optBoolean("nur_fehler") || !it.ok }
                    .filter { suche.isEmpty() || suche in it.werkzeug.lowercase(Locale.GERMAN) || suche in it.text.lowercase(Locale.GERMAN) }
                    .take(anzahl)
                if (treffer.isEmpty()) Ergebnis("Im Protokoll steht dazu nichts (es hält die letzten 200 Einträge).")
                else Ergebnis(buildString {
                    append("Protokoll von Jarvis, neueste zuerst (").append(treffer.size).append(" Einträge):\n")
                    treffer.forEach { e ->
                        append(ZEIT.format(Instant.ofEpochMilli(e.zeit).atZone(ZoneId.systemDefault()))).append(" · ").append(e.quelle.anzeige).append(" · ").append(e.werkzeug)
                            .append(if (e.ok) ": " else ": FEHLER: ").append(e.text.replace('\n', ' ')).append('\n')
                    }
                }.trim())
            },
        )
    }

    private companion object {
        val ZEIT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M. HH:mm:ss", Locale.GERMAN)
    }
}
