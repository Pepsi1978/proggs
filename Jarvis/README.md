# Jarvis

Persönlicher Assistent auf dem Handy (Android, Kotlin, Jetpack Compose). Paket `de.frank.jarvis`.
Zielbild und Gespräche dazu: `Datenbank/JARVIS/`.

Version 1 kann zwei Dinge:

1. **ChatGPT-Plugin (MCP).** Jarvis ist ein MCP-Server, der auf dem Handy läuft. In ChatGPT wird er als eigener
   Konnektor „Jarvis“ eingetragen. Danach genügt im Chat oder im Sprachmodus „Jarvis, speichere …“.
2. **Eigener Chat.** Jarvis hat ein eigenes Modell aus der ChatGPT-Anmeldung (Codex-Gerätecode), Modell und
   Denkstufe sind wählbar. Es bedient dieselben Werkzeuge selbstständig.

Erste angebundene App: **Geniale Aufgaben** (`de.frank.aufgaben`).

## Aufbau

```
ChatGPT (Cloud) ──HTTPS──> ngrok-Adresse ──Tunnel──> Jarvis-App ──ContentProvider──> Geniale Aufgaben
                                                     └─ MCP-Server (127.0.0.1:8765)
```

- **Tunnel** (`tunnel/Tunnel.kt`): Das ngrok-SDK läuft in der App und baut die Verbindung vom Handy nach außen auf.
  Kein eigener Server, kein offener Port. Braucht den Authtoken eines (kostenlosen) ngrok-Kontos; mit der festen
  Adresse des Kontos bleibt die Plugin-Adresse dauerhaft gleich.
- **MCP-Server** (`mcp/`): Streamable HTTP, zustandslos, nur JSON-Antworten (kein Server-Stream, keine Sitzungen).
  Erreichbar nur unter `/j/<Geheimnis>/mcp`; alles andere ist 404. Das Geheimnis (40 Zeichen) ist der Zugangsschutz,
  weil ChatGPT-Konnektoren ohne Anmeldung arbeiten. Es lässt sich in den Einstellungen erneuern.
- **Dienst** (`dienst/JarvisDienst.kt`): Vordergrunddienst (Typ `specialUse`), hält Server und Tunnel auch bei
  gesperrtem Handy am Leben, startet nach dem Einschalten und nach Updates von selbst.
- **Werkzeuge** (`faehigkeit/`): Jede angebundene App ist eine `Faehigkeit` mit einer Liste von `Werkzeug`en.
  Plugin, eigener Chat und Oberfläche lesen alle dasselbe `Register`.
- **Agent** (`agent/JarvisAgent.kt`): Schleife Modell → Werkzeug → Ergebnis, höchstens 8 Schritte. Werkzeugaufrufe
  laufen über ein festes JSON-Format im Antworttext, damit jedes Text-Modell Jarvis antreiben kann.

## Werkzeuge im Plugin

| Werkzeug | Zweck |
|---|---|
| `aufgaben_lesen` | Aufgaben und Termine lesen (heute, morgen, Datum, demnächst, Eingang, offen, erledigt, alle; Suche) |
| `aufgabe_anlegen` | Neue Aufgabe; `text` und `datum` sind Pflicht, Erinnerung mit Vorlesen ist Vorgabe |
| `aufgabe_aendern` | Ändern und verschieben, per `id` oder Suchwort |
| `aufgabe_erledigen` | Abhaken oder wieder öffnen, per `id` oder Suchwort |
| `aufgabe_loeschen` | Löschen, per `id` oder Suchwort |
| `jarvis_status` | Erreichbarkeit, Datum und Uhrzeit auf dem Handy, angebundene Apps |
| `jarvis_auftrag` | Freier Auftrag an den Agenten für Mehrschritt-Aufgaben (Zeitfenster 45 s) |

Fehlt beim Anlegen der Tag, obwohl eine Uhrzeit genannt wurde, fragt ChatGPT nach; ruft es trotzdem auf, lehnt die
Aufgaben-App mit einem Hinweis ab. Mehrere Treffer bei einem Suchwort führen zu einer Rückfrage statt zu einer
geratenen Änderung. Ein wiederholter identischer Anlege-Aufruf innerhalb von 90 Sekunden legt nichts doppelt an.

## Einrichten

1. Auf ngrok.com anmelden, Authtoken und feste Adresse (Domains) in Jarvis → Einstellungen eintragen.
2. In Jarvis mit ChatGPT verbinden (Gerätecode) und Modell wählen.
3. Plugin-Adresse kopieren. Am PC auf chatgpt.com: Einstellungen → Apps und Konnektoren → Erweitert →
   Entwicklermodus → Erstellen: Name „Jarvis“, MCP-Server-URL = Plugin-Adresse, Authentifizierung „Keine“.
4. Akku-Sparen für Jarvis ausschalten (Knopf in der App); bei Samsung Jarvis zusätzlich unter
   „Nie in Standby versetzen“ eintragen.

## Eine weitere App anbinden

1. In der Ziel-App einen ContentProvider nach dem Muster `Aufgaben/.../bruecke/JarvisBruecke.kt` anlegen, geschützt
   mit der Signatur-Erlaubnis.
2. In Jarvis eine Klasse `XyFaehigkeit : Faehigkeit` schreiben und in `Register.alle` eintragen, dazu den
   `<queries>`-Eintrag im Manifest.

## Signierung

Jarvis und die angebundenen Apps müssen mit demselben Schlüssel signiert sein (gemeinsamer Debug-Key aus
`~/SK/Android/`), sonst verweigert Android den Zugriff auf die Brücke. Beide Apps erklären die Erlaubnis
`de.frank.aufgaben.permission.JARVIS`, deshalb ist die Installationsreihenfolge egal.

## Noch offen

- Ob der ChatGPT-Sprachmodus eigene Konnektoren aufruft und ob er Schreibaktionen ohne Bestätigungsdialog ausführt,
  muss am Gerät geprüft werden. Falls nicht: GPT Live direkt in Jarvis einbauen (Werkzeuge und Agent stehen bereit).
- Bei tiefem Stromsparmodus ohne Ladekabel kann Android die Verbindung trotz Dienst kappen.
