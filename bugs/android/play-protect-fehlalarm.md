# Play Protect: eigene App als „schädlich“ markiert (Fehlalarm)

Stand: 01.10.2026 · Anlass: Longevity (`de.frank.longevity`, Version 1.0.12) am Galaxy Fold 6 als
„Diese App könnte schädlich sein“ gemeldet, alle anderen selbst gebauten Apps nicht.

## Was Play Protect sagt und was nicht

- „Diese App könnte schädlich sein“ ist das Urteil „Potentially Harmful App“ (PHA) aus Googles
  automatischer Erkennung (maschinelles Lernen, Cloud-Abgleich). Das ist nicht der harmlosere Hinweis
  „Play Protect hat diese App noch nie gesehen“.
- Google nennt den Auslöser **nie**. Jede Ursache ist deshalb eine Hypothese mit Belegen.
- Offizielle Hinweise: <https://developers.google.com/android/play-protect/warning-dev-guidance>.
  Harte Auslöser dort: SMS-Rechte, Benachrichtigungs-Listener, Bedienungshilfen (Accessibility),
  `isAccessibilityTool`, `targetSdk` zu alt, dynamisch nachgeladener Code, fremde SDKs.

## Befund Longevity (01.10.2026)

Ausgeschlossen, weil in anderen, nicht markierten Apps genauso vorhanden:
- Codex-Anmeldung (Geräte-Code bei `auth.openai.com`, Aufrufe an `chatgpt.com/backend-api/codex` mit
  `originator: codex_cli_rs`): steckt in 15+ Apps.
- Websuche der KI-Agenten: läuft auf dem OpenAI-Server (`"tools":[{"type":"web_search"}]`), die APK
  schickt nur einen Schalter. Gleiches in NewsKompass, Gedankenspeicher, KarteikartenLernen.
- Rechte (Mikrofon, Vordergrund-Dienst `dataSync`, WakeLock): gibt es auch in Gedankenspeicher.
- Signatur: alle Versionen mit demselben geteilten Debug-Key (`apk-update.ps1` signiert unsignierte
  Release-APKs mit `SK/Android/debug-shared.keystore`), kein Schlüsselwechsel.
- Keine harten Auslöser aus der Google-Liste (kein SMS, kein Accessibility, kein Nachladen von Code).

Was nur Longevity hat (wahrscheinlichste Gründe, zusammen):
1. **Ganz neues Paket ohne Ruf**: erste Version 29.09.2026, 13 Versionen in rund 30 Stunden.
2. **Debug-Build** (`debuggable=true`) seit 1.0.9, selbst signiert, außerhalb des Play Store installiert.
3. **Einzige App mit `WifiLock`** (`KiDienst.kt`, seit 1.0.8) plus WakeLock bis 2 Stunden im
   Hintergrund-Dienst: Hintergrund-Datenverkehr bei ausgeschaltetem Bildschirm ähnelt Mustern von
   Schadsoftware.

## Vorgehen

1. **Nicht auf Verdacht Code entfernen.** Jede neue APK ist wieder ein unbekannter Build, und der
   WifiLock ist absichtlich (Abbrüche bei ausgeschaltetem Bildschirm). `User-Agent`/`originator` nie
   ändern, sonst lehnt das Codex-Backend ab.
2. Am Handy: Play Protect → Aktualisieren-Knopf (Neu-Scan). Urteile für neue Pakete kippen oft
   nach einigen Tagen von selbst. App nicht deinstallieren, wenn man ihr vertraut (Daten gingen verloren).
3. Einspruch (nur Frank, nicht aus der Cloud): <https://support.google.com/googleplay/android-developer/contact/protectappeals>,
   auf Englisch, mit Paketname und aktueller APK (Google Drive `Dokumente/Updates/<Projekt>/`).
4. Bleibt die Warnung: als Test eine Version ohne `WifiLock` bauen und neu scannen lassen.
