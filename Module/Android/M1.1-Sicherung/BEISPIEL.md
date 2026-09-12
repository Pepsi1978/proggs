# M1.1 Sicherung — so wird es eingebaut

Aus `KompassKern`, dem ersten Konsumenten. Die Anbindung dort ist die Vorlage:
`KompassKern/src/main/java/de/frank/module/sicherung/Anbindung.kt`.

## 1. Die App erfüllt die Schnittstellen

```kotlin
// Welche Teile es gibt — das eigene Enum erfüllt einfach die Modul-Schnittstelle.
enum class SicherungsTeil(
    override val id: String,        // steht so in der Datei und darf sich nie ändern
    override val titel: String,
    override val erklaerung: String,
) : de.frank.module.sicherung.SicherungsTeil {
    NOTIZEN("notizen", "Notizen", "Alle selbst geschriebenen Notizen."),
    // …
}
```

```kotlin
// Der Inhalt: die eigene Satz-Serialisierung.
class MeinSicherungsInhalt(…) : SicherungsInhalt {
    override val teile get() = SicherungsTeil.entries
    override val produkt get() = "Meine App"
    override val datenmodellVersion get() = MeineDatenbank.VERSION

    override suspend fun schreibeNutzlast(
        schreiber: JsonWriter, umfang: Set<SicherungsTeil>, pruefsumme: Inhaltspruefsumme,
    ): Nutzlastzahlen {
        schreiber.name("notizen").beginArray()
        notizen.alle().forEach { notiz ->
            schreiber.beginObject()
            schreiber.name("id").value(notiz.id)
            schreiber.name("text").value(notiz.text)
            schreiber.endObject()
            pruefsumme.nimm(notiz.id, notiz.text)   // ← nicht vergessen
        }
        schreiber.endArray()
        return Nutzlastzahlen(mapOf("notizen" to notizen.anzahl()))
    }

    override suspend fun liesNutzlast(
        feld: String, leser: JsonReader, pruefsumme: Inhaltspruefsumme, einspielen: Boolean,
    ): Nutzlastzahlen? = when (feld) {
        "notizen" -> { /* satzweise lesen, pruefsumme.nimm(…) in DERSELBEN Reihenfolge */ }
        else -> null      // unbekanntes Feld → das Modul überspringt es
    }

    override suspend fun zaehle(umfang: Set<SicherungsTeil>) = …
    override fun schaetzeGroesse(zahlen: Nutzlastzahlen) = (zahlen.anzahl["notizen"] ?: 0) * 400L
    override fun fasseZusammen(vorschau: SicherungsVorschau) = "Sicherung vom ${vorschau.erstelltAm}: …"
}
```

> ⚠️ **Die Prüfsumme ist die häufigste Fehlerquelle.** Schreiber und Leser müssen
> dieselben Werte in derselben Reihenfolge in `pruefsumme.nimm(…)` geben. Weicht
> auch nur ein Feld ab, gilt **jede** geschriebene Sicherung beim Zurücklesen als
> beschädigt — und weil das Sichern daran scheitert, schreibt die App ab diesem
> Moment gar keine Sicherung mehr. Genau dafür gibt es den Formattest.

## 2. Zusammenstecken

```kotlin
val ruecknahme = MeineRuecknahme(repository)          // optional
val inhalt = MeinSicherungsInhalt(repository, ruecknahme)

val sicherung = SicherungsDienst(
    context = appContext,
    inhalt = inhalt,
    namen = SicherungsNamen(dateiPraefix = "meine-app"),
    ruecknahme = ruecknahme,
    protokoll = MeinProtokoll,                        // optional, Vorgabe: still
    umfangGeber = { einstellungen.sicherungsTeile() },   // der eigene Einstellungsspeicher
)
val autoSicherung = AutoSicherung(sicherung, { einstellungen.autoSicherung }, MeinProtokoll)
```

Bei einer **bestehenden** App zusätzlich die alten Namen mitgeben, sonst ist der
eingestellte Ordner verloren:

```kotlin
SicherungsNamen(
    dateiPraefix = "meine-app",
    einstellungenDatei = "…",   // der bisherige SharedPreferences-Name
    ordnerSchluessel = "…",     // der bisherige Schlüssel darin
)
```

## 3. Knöpfe und Oberfläche

Das Modul hört beim `SicherungsDienst` auf. **Was die Knöpfe tun und wie sie
aussehen, gehört der App** — genau dadurch sind die Funktionen überall dieselben,
während sich das Aussehen anpasst.

Die Vorlage dafür ist kein ausgedachtes Muster, sondern laufender Code in drei
Apps: der Sicherungsanteil von
`KompassKern/src/main/java/de/frank/kompass/vm/EinstellungenViewModel.kt`
(Zeilen ~550–900) und der zugehörige Abschnitt in
`ui/screens/EinstellungenScreen.kt`. Abschreiben und die Texte anpassen.

Was dort drinsteht und beim Nachbauen leicht vergessen wird:

| | |
|---|---|
| Kein Haken entfernbar, wenn es der letzte ist | sonst entsteht eine leere Datei, die aussieht wie eine Sicherung |
| Ohne gemerkten Ordner erst fragen, dann sichern | ein Schalter auf „an" ohne Ordner schreibt nie etwas |
| Ordnerwahl abgebrochen → Schalter zurückstellen | sonst wartet man auf Sicherungen, die nicht kommen |
| Auswahlliste selbst zeichnen, nicht den Dateiwähler öffnen | aus dem führt die Zurück-Geste Ordner für Ordner heraus |
| Vor dem Einspielen immer erst die Vorschau | nie ungefragt in die Datenbank |
| `laeuft`-Wächter an jedem Knopf | zwei gleichzeitige Läufe schreiben sich gegenseitig kaputt |
| `CancellationException` weiterwerfen | sonst verschluckt `runCatching` den Abbruch des Bereichs |

### Das Gerüst der Bedienung

So ist der Bereich in der Vorlage aufgebaut, von oben nach unten. **Diese
Reihenfolge wird übernommen**, gezeichnet mit den Bausteinen der jeweiligen App
— deren Knöpfe, deren Schrift, deren Abstände. Zusätzliches der App darf
dahinter, nicht dazwischen.

| # | Was | Anmerkung |
|---|---|---|
| 1 | Mehrfachauswahl „Was gesichert wird" | je Punkt Titel **und** Erklärung aus `SicherungsTeil` |
| 2 | Hinweiszeile, was **nicht** in der Sicherung steckt | app-eigener Text |
| 3 | Schalter „Von allein sichern" mit Erklärung darunter | die Erklärung wechselt, solange kein Ordner gewählt ist |
| 4 | Ordnername | gedämpft, solange keiner gewählt ist |
| 5 | Zeile: Haken (nur wenn geprüft) + Stand der letzten Sicherung | der Haken steht für „zurückgelesen", nicht für „geschrieben" |
| 6 | Zeile „Nächste Sicherung: …" | steht **von allein** da, kein Knopf |
| 7 | Vorschau-Streifen + „Jetzt einspielen" · „Abbrechen" | nur, solange eine Vorschau offen ist |
| 8 | Knopfzeile, umbrechend: **Jetzt sichern · Ordner wählen · Ordner vergessen** | der letzte nur, wenn ein Ordner gemerkt ist |
| 9 | Knopfzeile, umbrechend: **Wiederherstellen · Neueste wiederherstellen** | |
| 10 | Rückgängig-Zeile + Text daneben | nur nach einem Einspielen |
| 11 | Auswahlliste „Welche Sicherung?" + „Abbrechen" | **eingebettet** im selben Bereich, kein Dialog und kein Dateiwähler |

Drei Dinge, die beim Nachbauen regelmäßig danebengehen:

- Die Knöpfe stehen **nebeneinander in einer umbrechenden Zeile**, nicht
  untereinander. Untereinander wird der Bereich doppelt so lang, und man sieht
  den Stand nicht mehr, ohne zu scrollen.
- Punkt 6 ist eine **Zeile, kein Knopf.** Wer daraus „Umfang zeigen" macht,
  versteckt die einzige Angabe, die vor dem Sichern interessiert.
- **Kein einleitender Absatz.** Was der Schalter tut, steht am Schalter. Ein
  Absatz darüber wiederholt es nur und bläht den Bereich auf.

Der Aufruf selbst ist kurz:

```kotlin
val zahlen = dienst.voraussichtlich()          // „12 Einträge, 3 Fragen — etwa 40 KB"
val stand  = dienst.sichere()                  // schreibt und prüft
val liste  = dienst.sicherungen()              // für die eigene Auswahlliste
val sicht  = dienst.vorschauVon(quelle)        // ansehen
val erg    = dienst.stelleWiederHerAus(quelle) // einspielen, erg.spur für das Zurücknehmen
dienst.nimmZurueck(erg.spur!!)
```

## 4. Android-Rahmen

`AutoSicherung` ist ein `DefaultLifecycleObserver` und muss angemeldet werden,
damit beim Verlassen der App sofort gesichert wird:

```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(autoSicherung)
```

Den Ordner wählt Android über `ACTION_OPEN_DOCUMENT_TREE`; die Adresse kommt in
`sicherung.merkeOrdner(uri)`.
