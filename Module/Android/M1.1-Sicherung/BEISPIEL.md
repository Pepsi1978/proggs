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
    umfangGeber = { speicher.teile() },
)
val autoSicherung = AutoSicherung(sicherung, { speicher.autoSicherung() }, MeinProtokoll)
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

## 3. Oberfläche

Das Modul liefert Zustand und Aktionen, **gezeichnet wird mit den eigenen
Bausteinen** — so sieht es aus wie der Rest der App:

```kotlin
val steuerung = SicherungsSteuerung(sicherung, speicher, inhalt, viewModelScope)
val zustand by steuerung.zustand.collectAsState()

MeineMehrfachauswahl(
    punkte = steuerung.teile.map { it.titel to it.erklaerung },
    aktiv = zustand.umfang,
    beiWechsel = steuerung::schalteTeil,
)
MeinSchalter("Von allein sichern", zustand.autoSicherung) {
    steuerung.schalteAutoSicherung(it, ordnerWaehlen)
}
MeinKnopf("Jetzt sichern") { steuerung.sichereJetzt(ordnerWaehlen) }
```

`KompassKern` geht hier einen eigenen Weg: Dort steuert weiterhin das bestehende
`EinstellungenViewModel`, das den `SicherungsDienst` direkt aufruft. Für eine
**neue** App ist `SicherungsSteuerung` der kürzere Weg.

## 4. Android-Rahmen

`AutoSicherung` ist ein `DefaultLifecycleObserver` und muss angemeldet werden,
damit beim Verlassen der App sofort gesichert wird:

```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(autoSicherung)
```

Den Ordner wählt Android über `ACTION_OPEN_DOCUMENT_TREE`; die Adresse kommt in
`steuerung.ordnerGewaehlt(uri)` bzw. `sicherung.merkeOrdner(uri)`.
