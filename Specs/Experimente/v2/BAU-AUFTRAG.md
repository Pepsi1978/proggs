# Bau-Auftrag — Experimente

Stand: 09.08.2026 · Stufe: v2

---

## 1. Was gebaut wird

Eine private Android-App, die ihrem Besitzer täglich fünf persönliche Experimente vorschlägt —
Dinge, die er so noch nie gemacht hat, aus beliebigen Lebensbereichen, zugeschnitten auf seine
aktuelle Lage und auf alles, was die App über ihn weiß. Er spricht morgens seine Lage ein,
wählt aus fünf Vorschlägen (bis zu drei Experimente dürfen gleichzeitig laufen, auch
mehrtägige), arbeitet die To-Do-Liste des Tages ab, kann mit der KI über ein laufendes
Experiment sprechen, und wertet abends alle offenen Experimente der Reihe nach aus. Aus diesen
Auswertungen führt die KI selbstständig ein 15-Tage-Logbuch, ein dauerhaftes verdichtetes
Langzeit-Log und eine Erkenntnisliste, die die nächsten Vorschläge immer genauer machen.
Alles per Sprache, alles auf dem Gerät.

**Wenn beim Bauen etwas gegeneinander steht, gilt diese Reihenfolge:**
1. Die KI muss den Besitzer wirklich kennen — Kontext gewinnt.
2. Nichts darf verlorengehen.
3. Die Vorschläge müssen wirklich neu sein.

---

## 2. Zielplattform und Technik-Weg

| Plattform | Zielgerät / Auflösung | Technik-Weg | Pflicht oder später |
|-----------|----------------------|-------------|---------------------|
| **Android** | Samsung Galaxy S23 Ultra, Hochformat, 1440 × 3088 px (Standard 1080 × 2316) | **Kotlin + Jetpack Compose**, Material 3 | **Pflicht** |

`minSdk 26` · `targetSdk 36` · `compileSdk 36` · JVM-Ziel 17.

**Die Zielplattform wurde beim Herunterladen aus Werft Studio am 09.08.2026 ausdrücklich als
Android gewählt** und steht so im Stempel oben in `00-PROJEKT.md`. Das Feld `plattform: "web"`
im Messpaket ist Werfts Browser-Vorschau und **nicht** das Bauziel.

**Bezugsgröße des Designs: 412 × 915 dp bei `density: 1`.** Alle gemessenen px-Werte sind
damit unmittelbar dp-Werte und ohne Umrechnung nach Compose übertragbar.

**Vorhandener Code, der übernommen werden kann** (nicht neu bauen — abschauen):

| Baustein | Woher |
|----------|-------|
| Codex-OAuth, Modell- und Effort-Wahl, `improveWish()` | `PerfectMoment/app/src/main/java/de/frank/perfectmoment/auth/` |
| Whisper-Transkription mit Vor- und Halluzinationsfilter | `PerfectMoment/.../audio/GroqTranscriber.kt`, `SpeechAnalyzer.kt`, `WhisperHallucinationFilter.kt` |
| Vorlesen: Chirp 3 HD, eigene Stimme, Edge | `PerfectMoment/.../tts/` (`GoogleCloudTtsPlayer.kt`, `QwenTtsPlayer.kt`, `QwenVoiceEnrollment.kt`, `EdgeTtsPlayer.kt`, `TtsCatalog.kt`) |
| Verschlüsselte Einstellungen | `PerfectMoment/.../data/settings/SecureSettings.kt` |
| Gesprächsfaden mit sofortigem Vorlesen | `CortexAndroid/.../ui/chat/ChatScreen.kt`, `ChatViewModel.kt` |

---

## 3. Verbindliche Quellen

| Datei | Wofür verbindlich |
|-------|-------------------|
| `Specs/Experimente/v2/01-FUNKTIONS-SPEC.md` | **Verhalten, Daten, Regeln.** Was beim Antippen wirklich passiert, das Datenmodell (10 Room-Einheiten), die Zustandsübergänge, die Fehlerfälle, das Hintergrundverhalten |
| `Specs/Experimente/v2/02-UI-SPEC.md` | **Jede Farbe, jedes Maß, jeder Bildschirm.** Beide Erscheinungen mit je 13 Farbrollen, die Typo-Skala, Raster und Abstände, Formen, alle neun Bildschirme mit Aufbau und Zuständen, alle festen Texte wörtlich |
| `Specs/Experimente/v2/03-MOTION-SPEC.md` | **Jede Bewegung.** 75 Bewegungen mit exakter Dauer, vollständigem `cubic-bezier`, Wiederholung, Quelle — und je einem fertigen Jetpack-Compose-Ausdruck |
| `Specs/Experimente/v2/00-PROJEKT.md` | **App-Name, Zielplattform, Sprache, Berechtigungen, Abnahmekriterien** |
| `Specs/Experimente/v2/AENDERUNGEN.md` | Was gegenüber v1 anders ist und **warum** — insbesondere die vier aus v1 zurückgeholten Bewegungen |
| `Designs/Outbox/Experimente/WERFT-DESIGN/` | **Das gebaute Design als Augenschein.** `bildschirme/21dunkelstandard/*.html` und `bildschirme/22hell/*.html` zeigen jeden Bildschirm in beiden Erscheinungen; `bildschirme/design.css` enthält die Regeln; `design-tokens.json` die gemessenen Werte |

**Bei Widerspruch:** Aussehen und Bewegung → das Design (v2 UI- und Motion-Spec).
Verhalten → das Funktions-Spec. Beides zusammen widersprüchlich → nachfragen, nicht raten.

---

## 4. Abhakliste

Fertig ist der Bau erst, wenn **jede** Kennung im Quellcode nachweisbar ist.

### Bildschirme — 9

`B-01` Heute (Start) · `B-02` Gespräch · `B-03` Auswertung · `B-04` Wünsche & Ziele ·
`B-05` Merkliste · `B-06` Erkenntnisse · `B-07` Logbuch · `B-08` Einstellungen ·
`B-09` Selbstbild

**Jeder in beiden Erscheinungen.** B-01 zusätzlich in allen sechs Zuständen
(`LEER` · `AUFNAHME` · `LAGE_STEHT` · `VORSCHLAEGE` · `LAEUFT` · `ABEND`) plus *lädt*,
*Fehler* und *kein Netz*.

### Funktionen — 27

`F-01` Lage einsprechen · `F-02` Text mit KI verbessern · `F-03` Fünf Vorschläge erzeugen ·
`F-04` Vorschläge aktualisieren · `F-05` Vorschlag auf die Merkliste legen ·
`F-06` Experiment auswählen und starten · `F-07` To-Do-Liste des Tages ·
`F-08` Aufgabe abhaken · `F-09` Gespräch zum Experiment · `F-10` Auswertung einsprechen ·
`F-11` KI-Auswertung erzeugen · `F-12` Auswertung vorlesen · `F-13` Experiment abschließen ·
`F-14` Logbuch fortschreiben · `F-15` Tagesverdichtung nach 15 Tagen ·
`F-16` Logbuch-Eintrag ändern oder löschen · `F-17` Erkenntnisse fortschreiben ·
`F-18` Merkliste: eigenes Experiment anlegen · `F-19` Merkliste: Eintrag löschen ·
`F-20` Wünsche & Ziele pflegen · `F-21` Selbstbild pflegen · `F-22` Modell und Effort wählen ·
`F-23` Stimme und Vorlesen einstellen · `F-24` Zugänge einrichten ·
`F-25` Erinnerungen einstellen · `F-26` Erscheinung umschalten ·
`F-27` **NEU** Zwischen den Hauptbildschirmen wischen

`F-28` bis `F-33` sind **nicht vergeben** — siehe `AENDERUNGEN.md` §2.

### Bewegungen — 75

**Aus v1, im Design nicht messbar, bewusst zurückgeholt:**
`M-01` Karte sinkt beim Drücken ein · `M-03` Vibration bei Aufnahmebeginn und -ende ·
`M-05` Vorschläge werden zweiphasig ausgetauscht · `M-06` Haken zeichnet sich

**Aus v1, im Design gemessen und bestätigt:**
`M-02` Sprechknopf atmet (3200 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos, alternate) ·
`M-04` Vorschlagskarten erscheinen gestaffelt · `M-07` Merken-Symbol füllt sich ·
`M-08` Auswertung erscheint · `M-09` Wartezustand der KI

**Aus dem Design gemessen:** `M-10` bis `M-75` — 66 Übergänge an App-Bauteilen, je mit
Selektor, Eigenschaft, Dauer, Verzögerung, Kurve, Wiederholung und Compose-Ausdruck.

Jede Bewegung nennt in `03-MOTION-SPEC.md` ihre Quelle. `werft-screen-detail` und
`werft-screen-fade` gehören zu Werfts Vorschau und sind **nicht** zu bauen.

### Abnahme — 20

`A-01` bis `A-20`, vollständig in `00-PROJEKT.md` §5. Jedes Kriterium ist am laufenden
Programm beobachtbar formuliert.

---

## 5. Offene Fragen, die vor dem Bau geklärt sein müssen

**Keine.**

Alle sieben von Werft gemeldeten Bedienelemente sind zugeordnet, alle Kennungen sind
eindeutig, beide Erscheinungen sind vollständig, und `vollstaendigkeit.nichtAufgebaut` ist
als Import-Artefakt aufgeklärt.

Ein praktischer Hinweis, keine Frage: Beim Schreiben dieses Auftrags war **kein Android-Gerät
angeschlossen** (`adb devices` leer). Vor der Installation muss das Gerät verbunden sein, und
**die sichtbare Version ist vor dem Installieren zu erhöhen** — ohne Bump wirkt eine
geglückte Installation wie eine fehlgeschlagene.
