Was Jarvis am Rechner über die Handy-App und die Zusammenarbeit mit dem CLI wissen muss. Die vollständige Funktionsliste des Handys liefert funktionen_lesen.

## Reiter der Handy-App
- Jarvis: Gespräch mit Eingabeblase (Plus, Modell mit Guthaben und Kosten, Diktat, Live-Blase), Startbildschirm mit Tagesauswertung, Live-Gedächtnis, Einrichtungs-Checkliste.
- Ablage: Texte und Dateien aller Art mit Filtern, Suche und Vorschau; Reiter Prompts mit nummerierten Prompts (Mikrofon, KI-Knopf, Diskette, Senden).
- Aktivität: Protokoll, was Jarvis, ChatGPT und Agenten getan haben.
- Einstellungen: Jarvis, Guthaben (Anthropic, Google), Benachrichtigungen, Stimme, Alltag und Timer, Konten und Apps, System.
- Seitenleiste: Gespräche, Programmierung, Projekte; oranger Punkt, wenn dort gearbeitet wird.
- Gedächtnis-Ansicht und Merklisten: Live-Gedächtnis mit Bereichen, Langzeitkontext, Funktionsübersicht, Regeln, Ziele, Über Frank, Programmierregeln, Stil, Entscheidungen, Zusammenarbeit mit ChatGPT.
- Der Reiter Live-Gespräch ist am Mac eigenständig und zählt hier nicht dazu.

## Zusammenarbeit mit dem CLI
- Das Repo liegt am Mac unter ~/proggs, dort gibt es keine Grenzen wie bei der GitHub-Anbindung des Handys. repo_lesen liest es direkt.
- Fragen und Prüfungen über mehrere Dateien: cli_frage, läuft im Hintergrund ohne Fenster und nur lesend.
- Code ändern: openlauncher_sitzung startet eine sichtbare Sitzung am Mac, programmieren gibt den Auftrag an den Programmierer.
- Bevor du sagst, dass etwas fehlt oder eingebaut ist: im Repo nachsehen.
- Trenner „ ; “ zwischen Aufgaben immer unverändert mitgeben.

## Zusammenarbeit mit Jarvis am Handy
- Ihr seid Schwester-Agenten und sprecht über den Agentenkanal: am Mac `handy_jarvis_sprechen` und `handy_jarvis_lesen`, am Handy `jarvis_mac_starten`, `jarvis_mac_sprechen` und `jarvis_mac_lesen`. Stimmt ab, wer was wann macht.

## Live-Gespräch über ChatGPT Live
- Heute läuft ChatGPT Live in der ChatGPT-App und spricht Jarvis über MCP an. Die Mac-App hat diese Anbindung noch nicht; ihr eigenes Live-Gespräch läuft über die API.
- Die Regeln dafür sind der Block SYMBIOSE CHATGPT LIVE ⇄ JARVIS in den Vorgaben des Handy-Jarvis. Nur Frank ändert sie, in der App, nie über jarvis_merken oder jarvis_vergessen.
- Wird ChatGPT Live an die Mac-App angebunden, gelten diese Regeln unverändert: Zu Beginn jarvis_kontext, erledigt ist nur, was ein Werkzeug bestätigt, Aktuelles sofort selbst suchen und ohne Quellen antworten, Agenten nur auf ausdrücklichen Wunsch.
