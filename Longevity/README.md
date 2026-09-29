# Longevity

Android-App (Kotlin, Jetpack Compose, Room), Paket `de.frank.longevity`. Zeigt alle Faktoren, die die
gesunde Lebenszeit beeinflussen – streng nach Wichtigkeit sortiert: Rang 1 bringt die meiste Lebenszeit.

## Bedienung

- **Rangliste:** Jeder Faktor als Karte (Titel max. 2 Zeilen, Lebensbereich, Evidenz, geschätzte Jahre,
  Pfeile ↑↓/NEU seit der letzten Aktualisierung). Antippen öffnet die Detailseite: Infografik (Tacho),
  Erklärung, „Warum Platz N“, **Ziel**, **Aufgabenplan** nach Wichtigkeit zum Abhaken, „Mit KI vertiefen“.
- **Plus unten rechts:** eigenen Punkt einsprechen (Groq **whisper-large-v3-turbo** mit den vier Filtern
  gegen stille Halluzinationen) oder tippen, **KI-Korrektur** (Zurück / Neue Fassung), dann
  **Auswerten & einordnen** – die KI prüft die Idee und setzt sie an die passende Stelle.
- **Oben rechts:** Aktualisieren · Design · Hell/Dunkel · Einstellungen.
- **Aktualisieren:** Forscherin Vita (Pro) und Skeptiker Kron (Contra) diskutieren die Rangliste in zwei
  Runden, Gutachterin Aeon entscheidet. Nichts wird gelöscht; neue Faktoren erscheinen als Vorschläge.
  Fortschrittsbalken 0–100 % (feste Bänder je Schritt, Denkzeit + gestreamte Zeichen), läuft als
  Vordergrund-Dienst weiter, Diskussion live im Protokoll.
- **Heute für dich:** die drei wirksamsten offenen Schritte. **Überblick:** Wirkungsbalken,
  Lebensbereiche-Ring, Evidenz-Ring (native Diagramme).
- **KI:** ChatGPT über die Codex-Anmeldung (Gerätecode 4 + 5 Zeichen, kopierbar). Modell und Effort für
  die Hauptarbeit (Standard GPT-6 Astra · Hoch) und getrennt für die Textkorrektur einstellbar.
  Optionales Kurzprofil fließt in jede Anfrage ein.
- **Vier Designs** (Morgenröte, Lebensbaum, Atem, Helix) in Hell und Dunkel, je mit eigener animierter Szene.

## Bauen

Version nur über `app/src/main/assets/versionslog.json` (neuester Eintrag unten).
Signatur: gemeinsamer Debug-Key (keine eigene signingConfig). Aufs Handy den Release-Build aus dem
Skill `apk-update` installieren (Debug-Compose ruckelt).
