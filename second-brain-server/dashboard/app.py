"""
sb-dashboard — privates Web-Cockpit fuer Franks "zweites Gehirn".
Erreichbar NUR ueber den WireGuard-Tunnel (10.8.0.1:8003), nicht oeffentlich. Der Tunnel IST der
Schutz (wie brain-api/agent). Das Backend traegt Gehirn-, Agent- und Server-Infos zusammen und
liefert die Oberflaeche (static/index.html). Observability-First: schlankes JSON-Log + Fehler-Faenger.
"""
from __future__ import annotations

import asyncio
import base64
import json
import logging
import os
import re
import struct
import time
import traceback
from collections import Counter
from pathlib import Path

import httpx
import psutil
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, JSONResponse, Response

VERSION = "0.24.2"  # 0.24.2: Vorlesen-Schalter Layout-Fix (Frank-Wunsch 2026-06-26) — der weisse An/Aus-Knopf lag optisch auf dem 'An/Aus'-Text (haesslich). Jetzt steht der Status-Text LINKS vom Switch (DOM-Reihenfolge state->input->track, Farbe via :has(input:checked)), klar getrennt -> der Knopf bewegt sich nur im 52px-Track, keine Ueberlappung mehr. 0.24.1: Vorlesen-Schalter optisch ueberarbeitet + Token-Sparen verifiziert (Frank-Wunsch 2026-06-26) — Schalter sass am Karten-Rand abgeschnitten/haesslich; jetzt in einer umrandeten Box (.vl-box, Border + Orange-Glow wenn an) DIREKT VOR dem Speichern-Button in EINER Zeile, An/Aus-Text faerbt sich orange(an)/grau(aus). VERIFIZIERT (Frank-Sorge Token-Verschwendung): speak() bricht in Zeile 1 per `if(!isOn()||!available) return` ab BEVOR ein /api/tts-fetch passiert -> bei AUS wird KEIN Text an Gemini-TTS gesendet, kein Token-Verbrauch (nur der manuelle "Stimme testen"-Knopf ruft TTS mit festem Mini-Satz). Beschreibung stellt das klar + Logik-Sonde console.debug in speak() macht es in der Browser-Konsole sichtbar. 0.24.0: Vorlesen An/Aus-Schalter in den Einstellungen (Frank-Wunsch 2026-06-26) — neuer Toggle-Schalter in Einstellungen->Vorlesen mit eigenem "Speichern"-Button (Muster wie Modell/Prompt): Schalter umlegen zeigt einen pending Stand, erst "Speichern" schreibt ihn dauerhaft (localStorage cortexTtsOn, gleicher Schluessel wie der Chat-Lautsprecher -> beide bleiben synchron) und zieht den orangen Lautsprecher-Knopf mit + stoppt laufendes Vorlesen sofort. Beim Oeffnen der Einstellungen wird der Schalter auf den echten Stand gesetzt. Reiner Frontend-Teil in index.html (CSS .vl-toggle, HTML in der Vorlesen-Sektion, JS in initTTS + Tab-Wechsel), bestehende Funktion (Chat-Lautsprecher, Stimmenauswahl) unveraendert. 0.23.0: VORLESEN viel schneller (Frank-Wunsch 2026-06-25, Performance) — speak() liest die Antwort jetzt Satz fuer Satz vor (ttsChunks: Schnitt an Satzgrenzen, ~220-Zeichen-Buendel) statt das komplette Audio des GANZEN Textes abzuwarten. Der ERSTE Satz wird sofort generiert + abgespielt, der naechste WAEHRENDDESSEN vorausgeladen (Pipelining); ttsSeq bricht bei neuer Antwort sauber ab. Messung: langer Text ~11s bis zum ersten Ton -> jetzt ~2s. Reiner Frontend-Fix in index.html (speak), GLEICHES Aussehen + gleiche Funktion, nur schneller. 0.22.0: STILLE Kuerzung am Speicher-Pfad endgueltig beseitigt (Frank-Bug 2026-06-25) — /api/chat kuerzte lange Eintraege bei 100000 Zeichen STILL (gleiche Fehlerklasse wie der alte 8000-Bug, nur hoehere Wand). Jetzt benannte Konstante MAX_STORE_CHARS (Default 500000, ~25x Franks groesste Datei, fastapi §8 OOM-Schutz) + LAUTE Ablehnung mit klarer Meldung statt stillem Slice -> nie wieder unbemerkter Textverlust. 0.21.0: FIX langer Eintrag wurde abgeschnitten (Frank-Bug 2026-06-25) — /api/chat cappte den Text bei 8000 Zeichen ('eine Chat-Nachricht ist nie so lang') und schnitt damit lange Eintraege (Almanache ~18k) MITTEN ab; nur die ersten 8000 Zeichen kamen im Gehirn an. Cap auf 100000 erhoeht (brain-api chunkt beliebig lange Texte selbst). Kategorie-Cap 60->120 (tiefe Pfade A/B/C/...). Rekursive Kategorie-Baeume (beliebig tief) sind rein in index.html. 0.20.0: TITEL-Feld im Sendebereich (Frank-Wunsch 2026-06-25) — neues Eingabefeld 'chatTitle' links vom Kategorie-Dropdown (bis 3x so breit via clamp, ausgegrauter Hinweis 'Titel'); fuer GANZ NEUE Eintraege gibt Frank den Titel selbst vor. /api/chat reicht 'title' an den Agenten weiter (Override, analog zur Kategorie); JS schickt den Titel mit und leert das Feld nach erfolgreichem Senden. Greift mit agent 0.24.0 (Titel-Override) + brain-api 1.10.0 (Titel im Embedding). 0.19.2: Vorlese-Stimmen im Dropdown farbig nach Geschlecht (Frank-Wunsch 2026-06-25) — weiblich rosa (#F9A8D4), maennlich hellblau (#7DD3FC), Symbol + ganze Zeile (data-gender -> cs-vf/cs-vm in syncCS; reiner Frontend-Teil in index.html). 0.19.1: Vorlese-Stimmen nach Geschlecht sortiert (weiblich oben, dann maennlich) + gender-Feld pro Stimme (Frank-Wunsch 2026-06-25; gender nach Gehoer/Community, nicht offiziell von Google) -> /api/tts/voices liefert sortiert; das Stimmen-Dropdown laeuft im Frontend durch enhanceSelect (schoenes Custom-Dropdown, Mausrad-Scroll, kein Scrollbalken) mit ♀/♂-Symbol davor. 0.19.0: Unterkategorien-UI v1 (Phasen 2-4, Frank-Wunsch 2026-06-25) — Einstellungen-Kategorien als aufklappbarer Baum (Haupt aufklappbar, Unter eingerueckt) + '+ Unter'-Knopf; Gespraech-/Drawer-Dropdowns hierarchisch (Unter eingerueckt, voller Pfad im Auswahlfeld) + Unterkategorie inline anlegbar; Uebersicht zeigt erst Hauptkategorien (Stapelbalken aggregiert, Legende als aufklappbarer Gruppenbaum), Klick filtert alles UNTER der Hauptkategorie -> neuer Proxy GET /api/by-parent (an brain /by-parent, sync def/Threadpool, graceful). Hierarchie steckt im Namen 'Haupt/Unter' -> keine Schema-Aenderung. 0.18.0: Vorlesen (Text->Sprache) im Gespraech (Frank-Wunsch 2026-06-25) — Lautsprecher-Toggle im Chat (default AN) liest Hauptagent-Antworten vor; neue Endpoints GET /api/tts/voices (30 Gemini-HD-Stimmen + Default) + POST /api/tts (Text+Stimme -> WAV, Gemini-native-TTS via GEMINI_API_KEY, PCM->WAV, x-goog-api-key-Header, to_thread). Stimmenauswahl ganz unten in Einstellungen, Praeferenz im localStorage. Hinweis: 'Chirp 3 HD' (Cloud-TTS) ist fuer das Projekt aktuell 403-gesperrt -> Gemini-native-TTS (gleichwertig) genutzt, Umstellung trivial nach Cloud-TTS-Freischaltung. Plus Einstellungen-Layout: Logbuch+Papierkorb nebeneinander (halbe Breite) unter System-Prompt. 0.17.1: Eval-Logs-Button ist jetzt ein Toggle (Frank-Wunsch 2026-06-25) — Root Cause: der "Logs"-Knopf rief stur showLogs() auf, lud die Liste neu und liess sie offen; ein zweiter Klick tat sichtbar nichts. Jetzt klappt der zweite Klick die offene Logs-Liste (bzw. eine geoeffnete Log-Ansicht) wieder zu, statt endlos scrollen zu muessen. Reiner Frontend-Fix in index.html. 0.17.0: Favicon im Browser-Tab (Frank-Wunsch 2026-06-25) — Cortex-Signet (Gehirn auf orange->rotem Marken-Verlauf) als Inline-SVG-Data-URI im <head> der index.html; vorher zeigte der Tab nur das Browser-Standard-Globus-Symbol. 0.16.0: Backup-Inhalts-Liste + Anzeige-Fixes (Frank-Wunsch 2026-06-25). (a) Neuer Endpoint GET /api/backup/contents listet GENERISCH alle Top-Level-Ordner des Z->Drive-Backups (Qdrant-Snapshots, Logbuch, Eval-Logs + kuenftige) mit Anzahl/Datum-des-letzten/Groesse -> scrollbare Box unter der Backup-Kachel im Einstellungen-Tab (immer aktueller Stand). compose: read-only Mount /srv/samba/gedanken:/gedanken:ro + DASH_BACKUP_ROOT. (b) Eval-Beschreibung 50->90 Saetze. (c) Kategorie-Tags an Eintraegen zeigen echte Schreibweise (CSS text-transform:lowercase entfernt; cap() macht nur ersten Buchstaben gross, erhaelt Bindestriche). 0.15.0: Eintrag-Bearbeitung im Drawer (Frank-Wunsch 2026-06-25). (a) PUT /api/entry reicht jetzt auch 'title' durch -> Titel im Drawer bearbeitbar (brain migriert die doc_id bei Titel-Aenderung). (b) POST /api/entry/category (Proxy an agent /categories/move-entry) verschiebt EINEN Eintrag in eine andere Kategorie fuers Drawer-Kategorie-Dropdown (neue Kategorie landet kanonisch in der Registry -> synchron mit Einstellungen+Gespraech). Frontend: Drawer-Titel im Bearbeiten-Modus editierbar; Kategorie-Dropdown (Gespraech-Stil, nach unten) neben Loeschen + 'In Kategorie speichern'-Button. 0.14.0: Anklickbare Antwort-Knoepfe im Chat (Frank-Wunsch 2026-06-25) — bei einer Speicher- ODER Kategorie-Rueckfrage (save_confirm/store_clarify) zeigt der Chat jetzt Ja/Nein-Knoepfe (chatOptions), Klick schickt die Antwort; Frank muss nicht tippen. 0.13.0: Kategorie-Verwaltung (Frank-Wunsch 2026-06-25) — Proxys /api/categories/detail (Liste mit Eintragszahl+leer-Flag), /api/categories/rename (umbenennen/mergen), /api/categories/delete (Etikett entfernen, Eintraege bleiben) an den Agenten; Frontend: Einstellungen-Abschnitt 'Kategorien' (Dropdown, Umbenennen+Speichern, Loeschen mit Warnung, Merge, Dublettenwarnung, leere anlegen); Gespraech-Dropdown synchron + deutsche Grossschreibung + leere ausgegraut. 0.12.0: Logbuch<->Gehirn-Sync (Frank-Wunsch) — wird ein 'gespraeche'-Eintrag im Gehirn geloescht, loescht das dashboard via agent auch die zugehoerige .txt auf Platte Z; beim Wiederherstellen aus dem Papierkorb wird die .txt zurueckgeschrieben. Eigene Dropdowns im Seiten-Stil; Dropdown-Scrollbalken ausgeblendet; Papierkorb-Bearbeiten mit Abbrechen-Button; Monats-Toggle. 0.11.0: Papierkorb-Bereich + Logbuch nach Monaten (Frank-Wunsch) — /api/trash (GET Liste, PUT editieren, POST /api/trash/restore wiederherstellen) als Proxy an brain; Logbuch /api/logbook/tree (Jahr/Monat-Baum) + /api/logbook?year=&month= (Lazy-Load eines Monats). Papierkorb + Logbuch teilen die Jahr/Monat-Navigation (aktueller Monat umrandet). 0.10.0: Papierkorb-Button im Eintrags-Drawer (Frank-Wunsch) — DELETE /api/entry (Proxy an brain DELETE /entry per doc_id) loescht einen Eintrag dauerhaft aus dem Gedaechtnis, mit eigenem Ja/Nein-Bestaetigungsdialog im Frontend. 0.9.0: Kategorie-Dropdown beim Senden (Frank-Wunsch) — Dropdown neben dem X-Button (Gespraech-Tab) mit allen Kategorien + 'Kategorie +' zum Anlegen; /api/chat reicht die gewaehlte 'category' an den Agenten weiter (Override). 0.8.0: Kategorie-Registry (Frank-Wunsch) — /api/categories GET/POST (Proxy an Agent); Uebersicht zeigt manuell angelegte (noch leere) Kategorien mit count 0. 0.7.0: Drei umschaltbare System-Prompts (Frank-Wunsch) — /api/prompt reicht role (haupt/speicher/abfrage) an den Agenten weiter; UI bekommt drei Umschalt-Buttons ueber dem Prompt-Textfeld. 0.6.2: Modell-pro-Rolle — drei Dropdowns (Hauptagent/Speicheragent/Abfrageagent), /api/config reicht haupt_model/speicher_model/abfrage_model weiter; System-Prompt/Logbuch-Kacheln wieder volle Breite + sauberer Abstand unter der oberen Reihe. 0.6.1: Backup-Kachel — "Mit Google verbinden"-Button + Token-Dialog (/api/backup/connect schreibt Token ins Steuer-Verzeichnis, Host stellt rclone-Verbindung her); Kacheln Bibliothekar-Agent + Backup wieder in voller Originalgroesse nebeneinander (set-row breiter); Steuer-/Status-/Trigger-Dateien jetzt im dashboard-schreibbaren /control statt auf Z (appuser-Permissions). 0.6.0: Google-Drive-Backup-Kachel (Einstellungen, neben Bibliothekar-Agent) — Status + letzter Sync-Zeitstempel, Buttons "Jetzt sichern"/"Wiederherstellen"; liest Status-Datei aus der Z-Wurzel (/gedanken) und schreibt Trigger-Flags, das eigentliche crash-sichere rclone-Backup laeuft auf dem Host (systemd). 0.5.1: Mikrofon-Hybrid-Diktat — Live-Vorschau via Web Speech API (interim) WAEHREND des Sprechens, finale Groq-Whisper-Fassung (mit Satzzeichen) ERSETZT beim Stopp die Vorschau (previewActive-Riegel verhindert, dass spaete Web-Speech-Events Groqs Endfassung ueberschreiben; Fallback auf Vorschau nur bei Groq-Ausfall, mit sichtbarem Hinweis). 0.5.0: Übersicht-Feinschliff (GEDÄCHTNIS-SPEKTRUM rechtsbündig, grosse Eintragszahl wird nicht mehr abgeschnitten + Tausenderpunkte), Browser-Navigation Zurück/Vor (History API), Kategorie gespraeche wieder als Balken/Legende/Chip sichtbar (anklickbar+bearbeitbar) — zaehlt aber NICHT in die Gesamtsumme, sichtbare Dashboard-Version im Rail-Fuss. 0.4.2: Roter X-Loeschen-Button links neben dem Mikrofon im Gespraech-Tab (leert die Eingabezeile komplett, setzt Hoehe zurueck). 0.4.1: Logbuch-Gespraeche (Kategorie gespraeche) zaehlen NICHT mehr in der Uebersicht (bleiben aber als Vektoren im Gehirn, durchsuchbar/recall). 0.4.0: Eintrags-Editor (PUT /api/entry -> brain), Mikrofon-STT (POST /api/transcribe -> Groq whisper-large-v3-turbo), Prompt-Verbesserung (POST /api/improve -> agent), Logbuch liest die .txt-Protokolle von der Samba-Platte (Z) mit Gehirn-Fallback. 0.3.0: Chat-Tab — /api/chat proxied an den Agenten (store/recall) via asyncio.to_thread (kein Event-Loop-Block, bugs/server/fastapi.md §1). 0.2.1: Einstellungen-Tab (Prompt-Editor + Modell-Wahl)

VERSION = "0.40.0 (05.07.2026, 02.11 Uhr)"  # 0.40.0 (Frank-Wuensche 2026-07-05): (a) Bibliothekar-Einstellungen speichern sich jetzt AUTOMATISCH bei jeder Aenderung (Schalter/Dropdown/Feld -> 'Speichere …' -> 'Gespeichert'; Speichern-Knopf entfernt — Vergessen unmoeglich; 500ms-Debounce, kein Speichern waehrend des Befuellens). (b) Abarbeiten-Karte sagt jetzt sichtbar, WER umsetzt: '(Abgearbeitet wird alles vom eingestellten Nacht-Modell: <modell> · Thinking <stufe>)' — dynamisch aus den echten Einstellungen. (c) Einstellungen-Tab: Karte 'Bibliothekar-Agent' heisst jetzt 'Agenten' (der Nachtschicht-Bibliothekar ist ein Spezialagent mit eigenem Bereich — Namens-Kollision beseitigt); Uebersicht-Kachel entsprechend 'Agent'. Alt: 0.39.1 (05.07.2026, 01.52 Uhr)  # 0.39.1: Versions-Zeitstempel korrigiert — die naechtlichen 0.38.x/0.39.0-Stempel waren HANDGESCHAETZT und liefen der echten Uhr ~30 min voraus (Frank-Fund 2026-07-05, 'Footer zeigt 02.20, es ist 01.51'). Ab jetzt wird der Stempel IMMER per date/Get-Date von der echten Uhr geholt (steht so schon in DEPLOY.md — heute Nacht missachtet, nie wieder). Server-Uhren selbst sind NTP-korrekt (verifiziert). 0.39.0 (Frank-Wunsch 2026-07-05): Uebersicht-Kacheln (Prozessor/Arbeitsspeicher/Speicherplatz/Agent) aktualisieren sich jetzt alle 3 SEKUNDEN ueber den NEUEN leichten GET /api/vitals (nur psutil + Agent-Ping, kein Gehirn-Scan) — und ALLE Uebersicht-Polls (3s-Vitals + 20s-Komplett-Overview) laufen NUR noch, wenn der Uebersicht-Tab wirklich aktiv UND der Browser-Tab sichtbar ist (vorher pollte der 20s-Overview auch auf anderen Tabs weiter). CPU-Wert ist jetzt die echte Durchschnittslast seit dem letzten Abruf (cpu_percent interval=None, 3s-Fenster) statt einer 250-ms-Stichprobe alle 20 s, die scheinbar bei einem Wert 'einfror'. Alt: 0.38.2 (05.07.2026, 02.10 Uhr)  # 0.38.2: Bibliothekar-Einstellungen — bei aktivem 'Ohne Begrenzung durcharbeiten' werden die zwei Limit-Felder (Max. Vorschlaege je Aufgabe, KI-Budget je Nacht) AUSGEGRAUT + gesperrt (sichtbar wirkungslos, Frank-Wunsch 2026-07-05); Umschalten wirkt sofort, Speichern wie gehabt. Alt: 0.38.1 (05.07.2026, 01.55 Uhr)  # 0.38.1 (Frank-Wuensche 2026-07-05): Bibliothekar-Einstellungen ausgebaut — (a) Nacht-Modell-Liste enthaelt jetzt AUCH die verbundenen Codex/GPT-Modelle (librarian 0.2.0 zieht sie aus agent /config; gpt-* laeuft ueber den neuen Agent-Durchgriff POST /llm mit der bestehenden ChatGPT-OAuth-Anmeldung), (b) eigenes Thinking/Reasoning-Dropdown (none-xhigh) direkt NEBEN dem Modell, (c) 'Ohne Begrenzung durcharbeiten'-Schalter (Default AN): kein Vorschlags-Limit, kein KI-Budget — der Bibliothekar arbeitet nachts alles ab, nur die stille Endlosschleifen-Notbremse bleibt; Limit-Felder gelten nur bei AUS. Einstellungs-Reihe jetzt 5 Felder nebeneinander (Startzeit, Modell, Thinking, Limit, Budget). Alt: 0.38.0 (05.07.2026, 01.00 Uhr)  # 0.38.0: NACHTSCHICHT-BIBLIOTHEKAR komplett (Frank-Auftrag 2026-07-05, Plan-Bereiche 11-18 + Nachzuegler-Bonus): NEUER 5. Dienst 'librarian' (Container sb-librarian, Port 8004, eigener Sleep-Time-Agent) laeuft jede Nacht 04:10 nach dem 4-Uhr-Backup und schreibt NUR VORSCHLAEGE (nie eigenmaechtige Aenderungen; Loeschen immer via Papierkorb): Dubletten-Merge (13), Widerspruchs-Suche (12), Veraltet-Erkennung (16), Kategorien-Gaertner (15), Wissens-Luecken (17), Logbuch-Monats-Verdichtung (14), Morgen-Report (18) + Nachzuegler-Lauf (Entity-Backfill, einzige Auto-Aufgabe) + Franks EIGENE Zusatzaufgaben (per Interview definiert, Plus-Feld, an/aus, Papierkorb nur fuer eigene). Dashboard: NEUER Tab 'Bibliothekar' — Morgen-Report-Karte, Tages-Listen (mehrere Tage bleiben stehen; erledigte Funde fallen raus), pro Fund Ja/Nein/eigener-Vorschlag-Textfeld + 'Starten'-Knopf, Rueckfragen-Schleife (Bibliothekar fragt nach, Frank antwortet, nochmal Starten -> alles wird umgesetzt), Einstellungen (an/aus, Startzeit, Modellwahl, Aufgaben-Schalter, Vorschlags-Limit, LLM-Budget, 'Jetzt laufen lassen'). dashboard/app.py: whitelisted Proxy /api/lib/* -> librarian + LIBRARIAN_URL. brain-api 1.22.0 (/entities/list?with_docs=1). Alt: 0.37.3 (04.07.2026, 23.00 Uhr)  # 0.37.3: sichtbarer Bump fuer agent 0.51.1 (Eval-Nacharbeit: 'Alles ueber X' erkennt jetzt auch 'zur'/'zum'; Eval-Fall #76 akzeptiert per Frank-Urteil auch die Tagebuch-Deutung). Alt: 0.37.2 (04.07.2026, 22.05 Uhr)  # 0.37.2: sichtbarer Bump fuer den Server-Deploy agent 0.51.0 (Eval-Check + 14 Level-2-Faelle: Hybrid/BM25, Zeit-Parser, Entity-Register, Multi-Query, Confidence, Quellen) + brain-api 1.21.1 (/purge raeumt auch Test-Entitaeten auf + BM25-Cache-Invalidierung). Alt: 0.37.1 (04.07.2026, 21.40 Uhr)  # 0.37.1: FIX Browser zeigte nach dem Deploy die ALTE Oberflaeche (Frank-Bug 2026-07-04 — der neue Info-Bereich 'fehlte' im Dashboard, war aber laengst auf dem Server). Root Cause: der Root-Handler lieferte index.html OHNE Cache-Header aus -> der Browser durfte seine alte Kopie beliebig lange aus dem Cache zeigen. Jetzt Cache-Control: no-cache -> der Browser fragt bei jedem Laden beim Server nach, neue Oberflaechen sind nach JEDEM Deploy sofort sichtbar (einmaliger Hard-Reload noch noetig, danach nie wieder). Alt: 0.37.0 (04.07.2026, 19.55 Uhr)  # 0.37.0 (Level-2 Such-Intelligenz, Frank-Auftrag 2026-07-04): (a) Nr. 39 QUELLEN-DRILLDOWN — der Chat zeigt unter jeder Gedaechtnis-Antwort anklickbare Quellen-Chips (Titel + Kategorie der vom Leseagenten benutzten Eintraege, aus dem neuen 'sources'-Feld des Agenten 0.50.0); Klick oeffnet den bekannten Drawer mit dem Volltext 1:1 — jede Antwort ist bis zur Quelle nachpruefbar. Dazu ein dezentes Konfidenz-Etikett (hoch/mittel/niedrig aus 'confidence'). (b) INFO-BEREICH in den Einstellungen (Frank-Wunsch: 'was ist alles eingebaut?') — neue Sektion 'System-Info: Was Cortex kann' mit der kompletten, anklickbaren Feature-Chronik: jedes eingebaute System mit Einbau-Datum+Uhrzeit und ausfuehrlicher Erklaerung (was es ist, wie es funktioniert, in welchem Dienst es sitzt). Quelle ist die NEUE, im Repo gepflegte dashboard/features.json (GET /api/features liest sie; bei jedem kuenftigen Einbau wird dort ein Eintrag ergaenzt — Pflegepflicht in DEPLOY.md verankert). Alt: 0.36.0 (02.07.2026, 20.42 Uhr)  # 0.36.0 (Multi-Category in der Liste 2026-07-02, Frank-Wunsch): In der Kategorie-/Such-Ergebnisliste zeigt jede Eintragszeile jetzt ALLE Kategorien des Eintrags als Chips (entryTagsHtml/entryCats, Fallback auf die eine primaere) statt nur eines Tags — d.h. klickt man in der Uebersicht auf 'Persoenlich', sieht man bei einem Mehrfach-Eintrag direkt auch 'Katzen'. by-category/search liefern die 'categories'-Liste bereits mit; nur reine /list zeigt weiter die primaere. Rein Frontend (index.html: entryCats/entryTagsHtml, result-top flex-wrap). Alt: 0.35.0 (Multi-Category-UI 2026-07-02, Frank-Wunsch): Drawer zeigt jetzt ALLE Kategorien eines Eintrags als entfernbare Chips + ein Plus (bestehender Kategoriebaum-Popover) zum Hinzufuegen weiterer; sofortiges Speichern ueber den NEUEN Proxy POST /api/entry/categories (asyncio.to_thread -> brain /entry/categories, Re-Embed; validiert doc_id + nichtleere Liste, je <=120 Zeichen, dedupliziert, max 12). Das alte Einzel-Dropdown 'In Kategorie speichern' entfaellt. Backend war seit brain-api 1.11.0 bereit. Alt: 0.34.0 (Tiefen-Debugging PERFORMANCE 2026-07-02): (a) /api/overview sammelt seine fuenf UNABHAENGIGEN Reads (brain /health, /category-counts, Registry-Kategorien, agent /health, 250-ms-CPU-Messung) jetzt PARALLEL (ThreadPool) statt seriell — Latenz pro 20s-Poll = Maximum statt Summe der Teil-Latenzen, Ergebnis-JSON identisch; (b) die leeren Kategorien kommen ueber den neuen agent-Endpoint /categories/registry (nur categories.json, KEIN brain-Scan) statt ueber /categories, das pro Poll einen ZWEITEN Qdrant-Metadaten-Full-Scan ausloeste (Fallback auf /categories bei aelterem Agent — Ergebnis identisch); (c) modul-globaler httpx.Client (_HTTP, Connection-Pool + Keep-Alive, transport retries=1 nur fuer den Verbindungsaufbau) fuer ALLE ausgehenden Calls — spuerbar v.a. beim Satz-fuer-Satz-Vorlesen (Gemini-TTS: ein TLS-Handshake je Satz entfaellt) und Groq-STT; (d) index.html: der 20s-Overview-Poll pausiert bei VERSTECKTEM Browser-Tab (document.hidden) und aktualisiert beim Sichtbarwerden SOFORT — unsichtbares Polling loeste bisher weiter alle 20s die komplette Server-Kaskade aus. Verhaltensneutral: gleiche Anzeigen, gleiche Werte, gleiche 20s-Frequenz bei sichtbarem Tab. Alt: 0.33.1 (Tiefen-Debugging 2026-07-02): (a) /api/entry/category cappt die Ziel-Kategorie jetzt bei 120 statt 60 Zeichen — tiefe Pfade 'A/B/C/...' wurden beim Drawer-Verschieben STILL abgeschnitten und landeten als kaputter Teil-Pfad (Inkonsistenz zu /api/chat, das seit 0.21.0 bewusst 120 erlaubt; Agent _cat_key cappt ebenfalls 120). (b) /api/improve lehnt Texte >8000 Zeichen LAUT ab statt still bei 8000 zu kuerzen (gleiche Fehlerklasse wie der 0.21.0/0.22.0-Speicherpfad-Bug: nie wieder unbemerkter Textverlust; der Agent haette >8000 ohnehin per 422 abgelehnt — vorher wurde ein GEKUERZTER Text verbessert, ohne dass Frank es sah). (c) index.html: Kategorienamen an 3 innerHTML-Stellen (Balken-Tooltip, Legende, Chips) escaped (Injection-Flaeche: Kategorienamen koennen via Speicheragent aus untrusted Text vorgeschlagen werden); veraltete Tavily-Staffel-Texte (5/8/15) auf die echte Staffel S=8/M=12/XL=20 aktualisiert. Alt: 0.33.0: S/M/XL-Antwortlaengen-Chips im Gespraech (vor dem Titel-Feld, Standard M, Wahl wird gemerkt) — Server haengt den ZENTRALEN Prompt an (gleiche Texte wie im Handy) und staffelt die Tavily-Suchtiefe; /api/chat reicht response_size durch. Alt:  # 0.32.0: Eval-Check als Hintergrund-Lauf (Frank-Bug 2026-07-02) — /api/eval/run startet nur noch (30s-Timeout statt 600s-Wartesynchron), neues /api/eval/status + Frontend-Polling alle 5s ('laeuft… X/100 Saetze geprueft'), Ergebnis + Log-Knopf erscheinen automatisch am Ende; beim Seitenladen wird ein bereits laufender Lauf erkannt und weiter angezeigt. 0.31.0: Logbuch-Umbau (Frank-Wunsch 2026-07-02) — Logbuch + Papierkorb jetzt VOLLE Breite untereinander; jede Logbuch-Zeile zeigt vorne Datum/Uhrzeit + kurze Inhalts-Zusammenfassung (erste Frank-Zeile), das doppelte Datum hinten ist weg; stattdessen Zeilen-Papierkorb mit Ja/Nein-Dialog, der den Eintrag aus Logbuch-Datei UND Cortex loescht (bestehender DELETE /api/logbook) — die Ansicht bleibt dabei erhalten (Monat bleibt aufgeklappt, nur die Zeile verschwindet). Chat zeigt 'Kontextgrenze erreicht', wenn der Agent context_limit_reached meldet. 0.30.0: Router (Schritt 1) separat einstellbar — eigenes Modell- und Reasoning-Dropdown 'Router · Schritt 1' in den Einstellungen ('auto (wie Hauptagent)' = Default, exakt bisheriges Verhalten); /api/config reicht router_model/router_reasoning an den Agenten (0.40.0) durch. 0.29.0: Modell-Preise (Input/Output je 1 Mio Token) als Info-Block unter den Modell-Dropdowns (aus /config model_prices; minimax/gpt = Abo). 0.28.0: Reasoning/Thinking-Stufen-Auswahl jetzt auch fuer Gemini-Modelle sichtbar (nicht mehr nur Codex/GPT) — passt zum Agent 0.36.0 (Gemini-Thinking + gemini-3.5-flash/gemini-3-flash-preview). minimax bleibt ohne Auswahl (denkt nativ). 0.27.0: Sichtbarer Dashboard-Bump fuer den Agent-Deploy 0.35.0 (gpt-5.5-502-Fix: ungueltiges 'minimal' reasoning wird auf 'low' gemappt, 'minimal' nicht mehr in der /config-Auswahl, web_search VOR web_search_preview) + Android-App 0.1.43 (Dashboard-Polling ruht wenn Screen versteckt -> kein Tunnel-Stau, Chat-Timeout 60->120/180s). 0.26.0: Bibliothekar-Agent bekommt einen Tavily/Websearch-Schalter, der per /api/config mit Agent und Android-App synchron bleibt. 0.25.5: Sichtbarer Dashboard-Bump fuer Codex/Reasoning-Serverkonfiguration und aktuellen Agent-Deploy. 0.25.4: Dashboard-Footer trennt Version und Datum/Uhrzeit dauerhaft auf zwei Zeilen, damit nichts bis zum Rail-Strich gequetscht wird. 0.25.3: Dashboard-Footer zeigt Host/Privat und sichtbare Version untereinander; Versionszeit mit Doppelpunkt. 0.25.2: Web-Timestamp zeigt Aenderungszeit (updated_at) statt altem Erstellzeitpunkt; Drawer aktualisiert Zeit sofort nach Speichern. 0.25.1: Uebersicht 'Eintraege gesamt' zaehlt Gespraeche und bugfixes/* nicht mehr mit; Kategorien bleiben sichtbar. 0.25.0: Drawer-Buttons im Drawer-Header.

BRAIN_URL = os.getenv("BRAIN_URL", "http://brain-api:8000").rstrip("/")
AGENT_URL = os.getenv("AGENT_URL", "http://agent:8002").rstrip("/")
SB_API_KEY = os.getenv("SB_API_KEY", "")
USER_ID = os.getenv("SB_USER_ID", "frank")
# Speicher-Eingabe-Grenze (fastapi §8, OOM-Schutz). GROSSZUEGIG (~25x Franks groesste Datei) und
# bewusst KEIN stiller Slice mehr: bei Ueberschreitung lehnt /api/chat LAUT ab (klare Meldung,
# nichts gespeichert). brain-api chunkt beliebig lange Texte selbst -> diese Grenze schuetzt nur den
# Worker-Speicher, nie die Daten. Env-konfigurierbar, falls Frank mal noch groessere Almanache ablegt.
MAX_STORE_CHARS = int(os.getenv("DASH_MAX_STORE_CHARS", "500000"))
HOSTFS = os.getenv("DASH_HOSTFS", "/hostfs")
CONV_CATEGORY = os.getenv("DASH_CONV_CATEGORY", "gespräche")  # deutsche Umlaute (Frank 2026-06-24); Altbestand migriert
# Logbuch-.txt-Protokolle (Samba-Platte = Franks Z:), read-only ins dashboard gemountet.
LOGBOOK_DIR = os.getenv("DASH_LOGBOOK_DIR", os.getenv("AGENT_LOGBOOK_DIR", "/logbook"))
# Z-Wurzel (Samba "gedanken") fuer das Google-Drive-Backup: Status-Datei lesen + Trigger-Flags schreiben.
BACKUP_DIR = os.getenv("DASH_BACKUP_DIR", "/gedanken")
# Komplettes Z-Backup (read-only) fuer die Inhalts-Liste "was liegt aktuell im Drive-Backup".
BACKUP_ROOT = os.getenv("DASH_BACKUP_ROOT", "/gedanken")
# Groq Whisper (Sprache->Text) — Key fest im Server (.env). whisper-large-v3-turbo wie im Voice-Overlay.
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_STT_URL = os.getenv("GROQ_STT_URL", "https://api.groq.com/openai/v1/audio/transcriptions")
GROQ_STT_MODEL = os.getenv("GROQ_STT_MODEL", "whisper-large-v3-turbo")
MAX_AUDIO_BYTES = int(os.getenv("DASH_MAX_AUDIO_BYTES", str(24 * 1024 * 1024)))  # Groq-Limit ~24 MB (Almanach §4)
# --- Vorlesen (Text->Sprache): Gemini-TTS, Key fest im Server (.env). Die "Chirp 3 HD"-Stimmen
#     gehoeren zur Google-Cloud-TTS-API (texttospeech.googleapis.com), die fuer dieses Projekt
#     aktuell gesperrt ist (403). Gemini-native-TTS (generativelanguage) laeuft mit demselben
#     GEMINI_API_KEY sofort und liefert 30 gleichwertige HD-Stimmen. Umstellung auf Chirp3-HD ist
#     trivial, sobald die Cloud-TTS-API aktiviert ist (anderer Endpoint + Voice-Namen de-DE-Chirp3-HD-*).
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_TTS_MODEL = os.getenv("GEMINI_TTS_MODEL", "gemini-2.5-flash-preview-tts")
GEMINI_TTS_BASE = os.getenv("GEMINI_TTS_BASE", "https://generativelanguage.googleapis.com/v1beta/models")
TTS_MAX_CHARS = int(os.getenv("DASH_TTS_MAX_CHARS", "5000"))   # Eingabe-Limit (fastapi §8/§12) — eine Antwort ist nie so lang
TTS_DEFAULT_VOICE = os.getenv("DASH_TTS_VOICE", "Sulafat")
# Die 30 prebuilt Gemini-TTS-Stimmen mit deutscher Klang-Charakteristik. Sprachunabhaengig — sie sprechen
# Deutsch, wenn der Text Deutsch ist. Quelle: Gemini-API Speech-Generation-Doku (ai.google.dev).
TTS_VOICES = [
    # — weiblich (Frank-Wunsch: weiblich oben) —
    {"name": "Sulafat", "desc": "Warm", "gender": "f"},
    {"name": "Kore", "desc": "Bestimmt", "gender": "f"},
    {"name": "Aoede", "desc": "Beschwingt", "gender": "f"},
    {"name": "Leda", "desc": "Jugendlich", "gender": "f"},
    {"name": "Callirrhoe", "desc": "Locker", "gender": "f"},
    {"name": "Autonoe", "desc": "Hell", "gender": "f"},
    {"name": "Despina", "desc": "Sanft", "gender": "f"},
    {"name": "Erinome", "desc": "Klar", "gender": "f"},
    {"name": "Laomedeia", "desc": "Aufgeweckt", "gender": "f"},
    {"name": "Achernar", "desc": "Weich", "gender": "f"},
    {"name": "Gacrux", "desc": "Reif", "gender": "f"},
    {"name": "Pulcherrima", "desc": "Vorwärtstreibend", "gender": "f"},
    {"name": "Vindemiatrix", "desc": "Sanft", "gender": "f"},
    {"name": "Zephyr", "desc": "Hell", "gender": "f"},
    # — männlich —
    {"name": "Charon", "desc": "Informativ", "gender": "m"},
    {"name": "Achird", "desc": "Freundlich", "gender": "m"},
    {"name": "Puck", "desc": "Aufgeweckt", "gender": "m"},
    {"name": "Fenrir", "desc": "Lebhaft", "gender": "m"},
    {"name": "Orus", "desc": "Bestimmt", "gender": "m"},
    {"name": "Enceladus", "desc": "Behaucht", "gender": "m"},
    {"name": "Iapetus", "desc": "Klar", "gender": "m"},
    {"name": "Umbriel", "desc": "Locker", "gender": "m"},
    {"name": "Algieba", "desc": "Sanft", "gender": "m"},
    {"name": "Algenib", "desc": "Rau", "gender": "m"},
    {"name": "Rasalgethi", "desc": "Informativ", "gender": "m"},
    {"name": "Alnilam", "desc": "Bestimmt", "gender": "m"},
    {"name": "Schedar", "desc": "Ausgeglichen", "gender": "m"},
    {"name": "Zubenelgenubi", "desc": "Leger", "gender": "m"},
    {"name": "Sadachbia", "desc": "Lebhaft", "gender": "m"},
    {"name": "Sadaltager", "desc": "Sachkundig", "gender": "m"},
]
TTS_VOICE_NAMES = {v["name"] for v in TTS_VOICES}
STATIC = Path(__file__).parent / "static"
HEADERS = {"Authorization": f"Bearer {SB_API_KEY}", "Content-Type": "application/json"}

# Ein modul-globaler httpx.Client (Performance 2026-07-02, BP fastapi §3 Client-Reuse): Connection-
# Pool + Keep-Alive statt neuem TCP/TLS-Handshake pro Call. Spuerbar v.a. bei den TLS-Zielen
# (Gemini-TTS liest Satz fuer Satz vor -> VIELE kurze Calls; Groq-STT), aber auch bei den
# brain-/agent-Proxies. Thread-sicher (Handler laufen im Threadpool). transport retries=1:
# wiederholt NUR den VERBINDUNGSAUFBAU (nie einen gesendeten Request) — faengt die eine stale
# Keep-Alive-Verbindung nach einem Upstream-Neustart ab. Timeouts bleiben pro Aufruf explizit.
_HTTP = httpx.Client(transport=httpx.HTTPTransport(retries=1))

# Kleiner Thread-Pool NUR fuer die parallele /api/overview-Sammlung (5 unabhaengige Reads).
from concurrent.futures import ThreadPoolExecutor
_OV_POOL = ThreadPoolExecutor(max_workers=6, thread_name_prefix="ov")


def is_overview_total_excluded(category: str | None) -> bool:
    c = (category or "").strip().casefold()
    return c in {CONV_CATEGORY.casefold(), "gespraeche"} or c == "bugfixes" or c.startswith("bugfixes/")

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("sb-dashboard")


def _log(level: int, msg: str, **ctx) -> None:
    try:
        log.log(level, json.dumps({"ts": time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()),
                                   "module": "sb-dashboard", "msg": msg, "ctx": ctx}, ensure_ascii=False))
    except Exception:  # noqa: BLE001
        pass


def _bget(endpoint: str, **params):
    r = _HTTP.get(f"{BRAIN_URL}{endpoint}", params=params or None, headers=HEADERS, timeout=20.0)
    r.raise_for_status()
    return r.json()


def _bpost(endpoint: str, payload: dict):
    r = _HTTP.post(f"{BRAIN_URL}{endpoint}", json=payload, headers=HEADERS, timeout=40.0)
    r.raise_for_status()
    return r.json()


def _bput(endpoint: str, payload: dict):
    # 60s: ein Eintrags-Ersatz re-embedded den ganzen Text (mehrere Chunks moeglich).
    r = _HTTP.put(f"{BRAIN_URL}{endpoint}", json=payload, headers=HEADERS, timeout=60.0)
    r.raise_for_status()
    return r.json()


def _bdelete(endpoint: str, **params):
    r = _HTTP.delete(f"{BRAIN_URL}{endpoint}", params=params or None, headers=HEADERS, timeout=30.0)
    r.raise_for_status()
    return r.json()


def _aget(endpoint: str):
    r = _HTTP.get(f"{AGENT_URL}{endpoint}", headers=HEADERS, timeout=15.0)
    r.raise_for_status()
    return r.json()


def _aput(endpoint: str, payload: dict):
    r = _HTTP.put(f"{AGENT_URL}{endpoint}", json=payload, headers=HEADERS, timeout=20.0)
    r.raise_for_status()
    return r.json()


def _apost(endpoint: str, payload: dict):
    # 60s: ein recall macht ZWEI LLM-Aufrufe (entscheiden + antworten) — grosszuegig timen.
    r = _HTTP.post(f"{AGENT_URL}{endpoint}", json=payload, headers=HEADERS, timeout=60.0)
    r.raise_for_status()
    return r.json()


def _adelete(endpoint: str, **params):
    r = _HTTP.delete(f"{AGENT_URL}{endpoint}", params=params or None, headers=HEADERS, timeout=20.0)
    r.raise_for_status()
    return r.json()


app = FastAPI(title="Second Brain — Dashboard", version=VERSION)
_log(logging.INFO, "sb-dashboard gestartet", version=VERSION, brain_url=BRAIN_URL, agent_url=AGENT_URL)


@app.exception_handler(Exception)
async def unhandled(request: Request, exc: Exception) -> JSONResponse:
    log.error(json.dumps({"module": "sb-dashboard", "msg": "Unbehandelte Ausnahme",
                          "path": str(request.url.path), "trace": traceback.format_exc()}, ensure_ascii=False))
    return JSONResponse(status_code=500, content={"error": type(exc).__name__, "detail": str(exc)})


FEATURES_FILE = Path(__file__).parent / "features.json"   # Feature-Chronik (ins Image gebacken, Repo-gepflegt)


@app.get("/api/features")
def api_features() -> dict:
    """Feature-Chronik fuer den Info-Bereich in den Einstellungen (Frank-Wunsch 2026-07-04):
    JEDES eingebaute System mit Einbau-Datum/Uhrzeit + ausfuehrlicher Erklaerung. Quelle ist die
    im Repo gepflegte features.json — bei jedem Deploy mit neuem Feature kommt dort ein Eintrag
    dazu (Pflegepflicht in DEPLOY.md). Kaputte/fehlende Datei -> ehrliche Meldung, nie Crash."""
    try:
        with open(FEATURES_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
        feats = data.get("features") or []
        return {"ok": True, "count": len(feats), "features": feats,
                "stand": data.get("stand"), "version": VERSION}
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "features.json nicht lesbar", err=str(e))
        return {"ok": False, "count": 0, "features": [], "error": f"{type(e).__name__}"}


@app.get("/api/health")
def health() -> dict:
    return {"status": "ok", "version": VERSION}


def _registry_categories() -> list:
    """Leere (Registry-)Kategorien vom Agenten — OHNE brain-Scan (agent 0.49.0, Performance
    2026-07-02). Der alte Weg (/categories) loeste pro 20s-Poll einen ZWEITEN Qdrant-Full-Scan aus.
    Fallback auf die volle Liste, solange ein aelterer Agent laeuft — Ergebnis identisch: die
    befuellten Kategorien stehen schon in den counts und werden beim Ergaenzen uebersprungen."""
    r = _HTTP.get(f"{AGENT_URL}/categories/registry", headers=HEADERS, timeout=15.0)
    if r.status_code == 404:   # aelterer Agent ohne den Endpoint (Deploy-Reihenfolge egal)
        return _aget("/categories").get("categories", []) or []
    r.raise_for_status()
    return r.json().get("categories", []) or []


@app.get("/api/overview")
def overview() -> dict:
    out: dict = {"brain": None, "agent": None, "server": None, "categories": [], "total": None}
    # PARALLEL statt seriell (Performance 2026-07-02): brain /health, /category-counts, die
    # Registry-Kategorien, agent /health und die 250-ms-CPU-Messung sind fuenf UNABHAENGIGE Reads
    # ohne Seiteneffekte. Vorher kostete jeder 20s-Poll die SUMME der Latenzen (inkl. der
    # blockierenden CPU-Messung obendrauf) — jetzt das MAXIMUM. Ergebnis-JSON ist identisch;
    # die Fehlerbehandlung je Teil bleibt exakt wie vorher (result() wirft im jeweiligen try).
    f_health = _OV_POOL.submit(_bget, "/health")
    f_counts = _OV_POOL.submit(_bget, "/category-counts", user_id=USER_ID)
    f_registry = _OV_POOL.submit(_registry_categories)
    f_agent = _OV_POOL.submit(lambda: _HTTP.get(f"{AGENT_URL}/health", timeout=8.0).json())
    f_cpu = _OV_POOL.submit(psutil.cpu_percent, 0.25)   # misst parallel, nicht mehr additiv
    try:
        h = f_health.result()
        out["brain"] = {"status": h.get("status"), "points": h.get("points"),
                        "version": h.get("version"), "embed_model": h.get("embed_model")}
    except Exception as e:  # noqa: BLE001
        out["brain"] = {"status": "error", "detail": str(e)}
    try:
        # /category-counts ist die deduplizierte Quelle der Wahrheit: keine Volltexte, kein Listenlimit.
        # Logbuch-Gespraeche und technische Bugfix-Fallakten ERSCHEINEN als Kategorien, zaehlen aber
        # serverseitig NICHT in die Gesamtsumme "Einträge gesamt".
        cc = f_counts.result()
        c = Counter(cc.get("counts", {}) or {})
        # Manuell angelegte (noch leere) Kategorien aus der Agent-Registry mit count 0 ergaenzen,
        # damit sie in der Uebersicht erscheinen, auch wenn noch kein Eintrag drin ist (Frank-Wunsch).
        try:
            for name in f_registry.result():
                if name and name not in c and not is_overview_total_excluded(name):
                    c[name] = 0
        except Exception:  # noqa: BLE001 — Registry ist Hilfskontext; Agent-Ausfall darf die Uebersicht nicht killen
            pass
        out["categories"] = sorted(({"name": k, "count": v} for k, v in c.items()), key=lambda x: -x["count"])
        out["total"] = cc.get("total_distinct")
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorien-Abruf fehlgeschlagen", err=str(e))
        f_registry.cancel()   # haengt an keinem Ergebnis mehr
    try:
        a = f_agent.result()
        out["agent"] = {"status": a.get("status"), "version": a.get("version"),
                        "model": a.get("model"), "sessions": a.get("aktive_sitzungen")}
    except Exception:  # noqa: BLE001
        out["agent"] = {"status": "offline"}
    try:
        root = HOSTFS if os.path.exists(HOSTFS) else "/"
        du = psutil.disk_usage(root)
        vm = psutil.virtual_memory()
        # Frontend (Cortex) erwartet Speicher/Disk in MB (fmtBytes rechnet MB->GB).
        MB = 1024 * 1024
        out["server"] = {"cpu_pct": f_cpu.result(),
                         "mem_used": vm.used // MB, "mem_total": vm.total // MB, "mem_pct": vm.percent,
                         "disk_used": du.used // MB, "disk_total": du.total // MB, "disk_pct": du.percent}
    except Exception as e:  # noqa: BLE001
        out["server"] = {"detail": str(e)}
    out["dash_version"] = VERSION   # sichtbare Dashboard-Version im Cockpit (Update-Kontrolle)
    return out


@app.get("/api/vitals")
def vitals() -> dict:
    """Leichter SCHNELL-Abruf NUR fuer die Uebersicht-Kacheln (Prozessor/RAM/Disk + Agent-Status).
    Das Frontend pollt ihn alle 3 s — aber AUSSCHLIESSLICH solange der Uebersicht-Tab aktiv und
    der Browser-Tab sichtbar ist (Frank-Wunsch 2026-07-05). psutil.cpu_percent(interval=None)
    blockiert NICHT: es liefert die Durchschnittslast SEIT DEM LETZTEN Aufruf — beim 3s-Poll also
    ein echtes 3-Sekunden-Fenster (ehrlicher als die alte 250-ms-Stichprobe alle 20 s, die
    scheinbar 'eingefrorene' Werte zeigte). Sync def -> Threadpool (fastapi §1)."""
    out: dict = {}
    try:
        root = HOSTFS if os.path.exists(HOSTFS) else "/"
        du = psutil.disk_usage(root)
        vm = psutil.virtual_memory()
        MB = 1024 * 1024
        out["server"] = {"cpu_pct": psutil.cpu_percent(interval=None),
                         "mem_used": vm.used // MB, "mem_total": vm.total // MB, "mem_pct": vm.percent,
                         "disk_used": du.used // MB, "disk_total": du.total // MB, "disk_pct": du.percent}
    except Exception as e:  # noqa: BLE001
        out["server"] = {"detail": str(e)}
    try:
        a = _HTTP.get(f"{AGENT_URL}/health", timeout=5.0).json()
        out["agent"] = {"status": a.get("status"), "model": a.get("model"), "sessions": a.get("aktive_sitzungen")}
    except Exception:  # noqa: BLE001 — Agent offline darf die Kacheln nicht killen
        out["agent"] = {"status": "offline"}
    return out


@app.get("/api/entries")
def entries(q: str = "", category: str = "", limit: int = 40) -> dict:
    limit = max(1, min(limit, 200))
    if category.strip():
        d = _bget("/by-category", category=category.strip(), user_id=USER_ID)
        return {"mode": "category", "items": d.get("items", [])[:limit]}
    if q.strip():
        d = _bpost("/search", {"query": q.strip(), "user_id": USER_ID, "limit": limit})
        return {"mode": "search", "items": d.get("items", [])}
    d = _bget("/list", user_id=USER_ID, limit=limit)
    return {"mode": "list", "items": d.get("items", [])}


@app.get("/api/by-parent")
def entries_by_parent(parent: str = "", limit: int = 200) -> dict:
    """Alle Eintraege UNTER einem Kategoriepfad. brain-api filtert nur die Hauptebene; tiefere Pfade
    werden hier defensiv per Prefix gefiltert, damit die Gehirn-Drilldown-Navigation beliebig tief bleibt."""
    p = parent.strip()
    if not p:
        return {"mode": "parent", "items": []}
    limit = max(1, min(limit, 500))
    try:
        top = p.split("/", 1)[0].strip()
        d = _bget("/by-parent", parent=top, user_id=USER_ID)
        items = d.get("items", [])
        if "/" in p:
            prefix = p + "/"
            items = [it for it in items if (it.get("category") or "") == p or (it.get("category") or "").startswith(prefix)]
        return {"mode": "parent", "parent": p, "items": items[:limit]}
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "by-parent fehlgeschlagen", err=str(e), parent=p)
        return {"mode": "parent", "items": []}


@app.get("/api/entry")
def entry(title: str) -> dict:
    return _bget("/by-title", title=title, user_id=USER_ID)


def _logbook_when(stem: str, txt: str) -> str:
    """Datum/Uhrzeit eines Logbuch-Protokolls — aus der Kopfzeile 'Datum/Uhrzeit: ...', sonst der
    Dateiname (der ist bereits 'TT.MM.JJJJ - H.MM Uhr')."""
    for line in txt.splitlines()[:5]:
        low = line.lower()
        if low.startswith("datum") and ":" in line:
            return line.split(":", 1)[1].strip()
    return stem


def _logbook_files() -> list:
    """Alle Logbuch-.txt-Dateien (rekursiv), ohne sie zu lesen."""
    base = Path(LOGBOOK_DIR)
    if not base.is_dir():
        return []
    try:
        return [p for p in base.rglob("*.txt") if p.is_file()]
    except Exception:  # noqa: BLE001
        return []


def _ym_of(p) -> tuple[str, str]:
    """Jahr/Monat eines Logbuch-Files — bevorzugt aus dem Ordnerpfad JJJJ/MM, sonst aus der mtime.
    Kein datetime-Import noetig (time.localtime)."""
    parent, gp = p.parent.name, p.parent.parent.name
    if gp.isdigit() and len(gp) == 4 and parent.isdigit() and len(parent) in (1, 2):
        return gp, parent.zfill(2)
    tm = time.localtime(p.stat().st_mtime)
    return f"{tm.tm_year:04d}", f"{tm.tm_mon:02d}"


def _logbook_item(p: Path, txt: str) -> dict:
    """Ein Logbuch-Item mit sicherem rel. Pfad fuer spaeteres Loeschen ueber den Agenten."""
    base = Path(LOGBOOK_DIR).resolve()
    rel = ""
    try:
        rel = p.resolve().relative_to(base).as_posix()
    except Exception:  # noqa: BLE001 — Anzeige darf an einem komischen Pfad nicht scheitern
        pass
    return {"title": p.stem, "brain_title": f"Gespräch {p.stem}", "path": rel,
            "when": _logbook_when(p.stem, txt), "text": txt, "source": "file"}


@app.get("/api/logbook/tree")
def logbook_tree() -> dict:
    """Verfuegbare Jahre/Monate des Logbuchs (nur Zaehlung, KEINE Texte gelesen) — fuer die
    Jahr/Monat-Navigation, damit nicht alle Eintraege auf einmal geladen werden. Sync def -> Threadpool."""
    tree: dict[str, dict[str, int]] = {}
    for p in _logbook_files():
        try:
            y, m = _ym_of(p)
        except Exception:  # noqa: BLE001
            continue
        tree.setdefault(y, {}).setdefault(m, 0)
        tree[y][m] += 1
    return {"ok": True, "tree": tree}


@app.get("/api/logbook")
def logbook(year: str = "", month: str = "") -> dict:
    """Logbuch = die .txt-Gespraechsprotokolle auf der Samba-Platte (Franks Z:), gemountet unter
    LOGBOOK_DIR (JJJJ/MM/*.txt). Mit year+month: NUR die Eintraege dieses Monats (Lazy-Load, damit die
    Seite nicht endlos lang wird). Ohne Filter: die neuesten 80 (+ Gehirn-Fallback). Neueste zuerst.
    Sync def (Datei-I/O) -> Threadpool (fastapi §1)."""
    items: list[dict] = []
    filtered = bool(year and month)
    mm = month.zfill(2) if month else ""
    try:
        files = _logbook_files()
        if filtered:
            files = [p for p in files if _ym_of(p) == (year, mm)]
        files = sorted(files, key=lambda p: p.stat().st_mtime, reverse=True)
        if not filtered:
            files = files[:80]
        for p in files:
            try:
                txt = p.read_text(encoding="utf-8")
            except Exception:  # noqa: BLE001 — eine kaputte Datei darf das Logbuch nicht killen
                txt = ""
            items.append(_logbook_item(p, txt))
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Logbuch-Ordner nicht lesbar", err=str(e), dir=LOGBOOK_DIR)
    if not items and not filtered:  # Fallback NUR ohne Monatsfilter: aus dem Gehirn (Kategorie gespraeche)
        try:
            d = _bget("/by-category", category=CONV_CATEGORY, user_id=USER_ID)
            for it in d.get("items", []):
                items.append({"title": it.get("title") or "Gespraech",
                              "when": it.get("updated_at") or "", "text": it.get("text") or ""})
        except Exception as e:  # noqa: BLE001
            _log(logging.WARNING, "Logbuch-Gehirn-Fallback fehlgeschlagen", err=str(e))
    _log(logging.INFO, "Logbuch geladen", count=len(items), year=year or None, month=mm or None)
    return {"items": items}


@app.delete("/api/logbook")
async def api_delete_logbook(path: str = "", title: str = "") -> dict:
    """Loescht ein Logbuch explizit aus beiden Speichern: erst .txt via Agent (Schreibrecht), dann Cortex.
    Kein stilles Best-Effort: Fehler werden sichtbar gemeldet, damit Frank weiss, ob Datei oder Cortex noch lebt."""
    rel = (path or "").strip()
    brain_title = (title or "").strip()
    if not rel and not brain_title:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Logbuch-Pfad oder Titel erforderlich"})
    try:
        file_res = await asyncio.to_thread(_adelete, "/logbook", path=rel, title=brain_title)
        if isinstance(file_res, dict) and file_res.get("ok") is False:
            return JSONResponse(status_code=502, content={"ok": False, "detail": "Datei konnte nicht gelöscht werden"})
        if isinstance(file_res, dict) and rel and not file_res.get("deleted"):
            return JSONResponse(status_code=404, content={"ok": False, "detail": "Logbuch-Datei wurde nicht gefunden oder nicht gelöscht"})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Logbuch-Datei-Loeschen fehlgeschlagen", err=str(e), path=rel, title=brain_title)
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Datei löschen fehlgeschlagen: {type(e).__name__}"})
    brain_res = {"deleted": False}
    if brain_title:
        try:
            brain_res = await asyncio.to_thread(_bdelete, "/by-title", title=brain_title, user_id=USER_ID)
        except Exception as e:  # noqa: BLE001
            _log(logging.WARNING, "Logbuch-Cortex-Loeschen fehlgeschlagen", err=str(e), title=brain_title)
            return JSONResponse(status_code=502, content={"ok": False, "detail": f"Cortex löschen fehlgeschlagen: {type(e).__name__}",
                                                          "file_deleted": file_res.get("deleted") if isinstance(file_res, dict) else None})
    _log(logging.INFO, "Logbuch geloescht", path=rel, title=brain_title,
         file_deleted=file_res.get("deleted") if isinstance(file_res, dict) else None,
         brain_deleted=brain_res.get("deleted") if isinstance(brain_res, dict) else None)
    return {"ok": True, "file": file_res, "brain": brain_res}


# --- Einstellungen: Proxy an den Agenten (System-Prompt + Modell-Wahl) ------
@app.get("/api/prompt")
def api_get_prompt(role: str = "haupt") -> dict:
    r = role if role in ("haupt", "speicher", "abfrage") else "haupt"
    return _aget(f"/prompt?role={r}")


@app.put("/api/prompt")
async def api_put_prompt(request: Request) -> dict:
    body = await request.json()
    r = (body.get("role") or "haupt")
    r = r if r in ("haupt", "speicher", "abfrage") else "haupt"
    # sync httpx via to_thread -> blockiert den Event-Loop nicht (bugs/server/fastapi.md §1)
    return await asyncio.to_thread(_aput, "/prompt", {"instructions": body.get("instructions", ""), "role": r})


@app.get("/api/config")
def api_get_config() -> dict:
    return _aget("/config")


@app.put("/api/config")
async def api_put_config(request: Request) -> dict:
    body = await request.json()
    # Modell-pro-Rolle weiterreichen (haupt/speicher/abfrage); 'model' bleibt als Abwaertskompat dabei.
    payload = {k: body.get(k) for k in (
        "haupt_model", "speicher_model", "abfrage_model", "model",
        "haupt_reasoning", "speicher_reasoning", "abfrage_reasoning",
        "router_model", "router_reasoning", "tavily_enabled"
    ) if k in body and body.get(k) is not None}
    return await asyncio.to_thread(_aput, "/config", payload)


@app.post("/api/codex/auth/start")
async def api_codex_auth_start() -> dict:
    return await asyncio.to_thread(_apost, "/codex/auth/start", {})


@app.post("/api/codex/auth/poll")
async def api_codex_auth_poll(request: Request) -> dict:
    body = await request.json()
    return await asyncio.to_thread(_apost, "/codex/auth/poll", {"auth_id": body.get("auth_id", "")})


@app.post("/api/codex/auth/disconnect")
async def api_codex_auth_disconnect() -> dict:
    return await asyncio.to_thread(_apost, "/codex/auth/disconnect", {})


# --- Kategorien: volle Liste (mit + ohne Eintrag) abrufen / neue anlegen (Proxy an den Agenten) ----
@app.get("/api/categories")
def api_get_categories() -> dict:
    """Volle Kategorienliste fuers Dropdown (inkl. leerer, vorab angelegter). Sync def -> Threadpool."""
    try:
        return _aget("/categories")
    except Exception as e:  # noqa: BLE001 — Agent offline: leere Liste statt 500
        _log(logging.WARNING, "Kategorien-Abruf fehlgeschlagen", err=str(e))
        return {"categories": []}


@app.post("/api/categories")
async def api_post_category(request: Request) -> dict:
    body = await request.json()
    name = (body.get("name") or "").strip()
    if not name:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Kategoriename"})
    try:
        return await asyncio.to_thread(_apost, "/categories", {"name": name})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie anlegen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Anlegen fehlgeschlagen: {type(e).__name__}"})


@app.get("/api/categories/detail")
def api_get_categories_detail() -> dict:
    """Kategorien mit Eintragszahl + leer-Flag (fuer Verwaltung + Gespraech-Dropdown). Sync def -> Threadpool."""
    try:
        return _aget("/categories/detail")
    except Exception as e:  # noqa: BLE001 — Agent offline: leere Liste statt 500
        _log(logging.WARNING, "Kategorie-Detail-Abruf fehlgeschlagen", err=str(e))
        return {"ok": False, "categories": []}


@app.post("/api/categories/rename")
async def api_rename_category(request: Request) -> dict:
    """Kategorie umbenennen (Proxy an Agent -> brain set_payload, Vektor bleibt). Existiert das Ziel -> Merge."""
    body = await request.json()
    old = (body.get("old") or "").strip()
    new = (body.get("new") or "").strip()
    if not old or not new:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "old/new fehlen"})
    try:
        return await asyncio.to_thread(_apost, "/categories/rename", {"old": old, "new": new})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie umbenennen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Umbenennen fehlgeschlagen: {type(e).__name__}"})


@app.post("/api/categories/delete")
async def api_delete_category(request: Request) -> dict:
    """Kategorie loeschen: Etikett von allen Eintraegen entfernen (Eintraege BLEIBEN). Proxy an Agent."""
    body = await request.json()
    name = (body.get("name") or "").strip()
    if not name:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Name"})
    try:
        return await asyncio.to_thread(_apost, "/categories/delete", {"name": name})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie loeschen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Loeschen fehlgeschlagen: {type(e).__name__}"})


# --- Eval-Check: Selbsttest aller 3 Agenten (Proxy an den Agenten) -----------
@app.post("/api/eval/run")
async def api_eval_run() -> dict:
    """Eval-Check STARTEN — der Agent laeuft im Hintergrund und kehrt sofort zurueck (Frank-Bug
    2026-07-02: der fruehere synchrone 600s-Wait timte bei langsamen Modellen aus). Fortschritt
    kommt ueber /api/eval/status. Sync httpx via asyncio.to_thread (fastapi §1)."""
    def _run():
        r = _HTTP.post(f"{AGENT_URL}/eval-run", headers=HEADERS, timeout=30.0)
        r.raise_for_status()
        return r.json()
    try:
        return await asyncio.to_thread(_run)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Lauf fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Eval-Lauf fehlgeschlagen: {type(e).__name__}"})


@app.get("/api/eval/status")
def api_eval_status() -> dict:
    """Live-Status des Eval-Laufs (running/done/total/passed/log/error) fuers Frontend-Polling."""
    try:
        return _aget("/eval-status")
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Status fehlgeschlagen", err=str(e))
        return {"ok": False, "running": False, "error": type(e).__name__}


@app.get("/api/eval/logs")
def api_eval_logs() -> dict:
    """Liste der Eval-Log-Dateien (neueste zuerst). Sync def -> Threadpool."""
    try:
        return _aget("/eval-logs")
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Logs-Liste fehlgeschlagen", err=str(e))
        return {"ok": False, "logs": []}


@app.get("/api/eval/log")
def api_eval_log(name: str) -> dict:
    """Eine Eval-Log-Datei (Markdown) lesen. Sync def -> Threadpool."""
    try:
        r = _HTTP.get(f"{AGENT_URL}/eval-log", params={"name": name}, headers=HEADERS, timeout=20.0)
        r.raise_for_status()
        return r.json()
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eval-Log lesen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Log lesen fehlgeschlagen: {type(e).__name__}"})


# --- Chat: Proxy an den Agenten (ablegen ODER nachschlagen) ------------------
@app.post("/api/chat")
async def api_chat(request: Request) -> dict:
    """Ein Eingang zum Bibliothekar-Agenten: store/ask/recall/smalltalk entscheidet der Agent.
    Der synchrone httpx-Call laeuft via asyncio.to_thread, damit ein langer recall (zwei
    LLM-Aufrufe, bis ~60s) den Event-Loop NICHT blockiert (bugs/server/fastapi.md §1)."""
    body = await request.json()
    text = (body.get("text") or "").strip()
    if not text:
        return {"ok": False, "reply": "Leere Nachricht — schreib mir was zum Ablegen oder eine Frage."}
    # Eingabe-Limit (fastapi §8) gegen OOM — GROSSZUEGIG und NIE still kuerzen: bei Ueberschreitung
    # LAUT ablehnen (klare Meldung, NICHTS gespeichert), damit nie wieder Text unbemerkt verloren geht.
    # (Frank-Bug 2026-06-25: erst 8000-, dann 100000-Slice schnitt lange Almanache STILL mitten ab.)
    # brain-api chunkt beliebig lange Texte selbst -> diese Grenze schuetzt nur den Worker, nicht die Daten.
    if len(text) > MAX_STORE_CHARS:
        return {"ok": False, "reply": f"Der Text ist mit {len(text)} Zeichen zu lang "
                f"(Maximum {MAX_STORE_CHARS}). Es wurde NICHTS gespeichert — bitte teile ihn in zwei Eintraege auf."}
    payload = {"text": text, "user_id": USER_ID, "store_timestamp": True}
    sid = (body.get("session_id") or "").strip()
    if sid:
        payload["session_id"] = sid
    cat = (body.get("category") or "").strip()
    if cat:                       # im Dropdown gewaehlte Kategorie (Override) an den Agenten durchreichen
        payload["category"] = cat[:120]   # tiefe Pfade A/B/C/... brauchen mehr als 60 (Agent _cat_key cappt bei 120)
    ttl = (body.get("title") or "").strip()
    if ttl:                       # im Sendefeld eingetippter Titel (Override) an den Agenten durchreichen
        payload["title"] = ttl[:200]
    rs = (body.get("response_size") or "").strip().lower()
    if rs in ("s", "m", "xl"):    # S/M/XL-Profil (Frank 2026-07-02): Agent haengt den zentralen Prompt an + staffelt Tavily
        payload["response_size"] = rs
    try:
        return await asyncio.to_thread(_apost, "/chat", payload)
    except Exception as e:  # noqa: BLE001 — Agent offline/Fehler: sauberer Fehler statt 500
        _log(logging.WARNING, "Chat-Proxy fehlgeschlagen", err=str(e))
        return {"ok": False, "reply": "Der Agent ist gerade nicht erreichbar — versuch es bitte gleich nochmal."}


# --- Eintrag editieren: alten Vektor 1:1 durch neuen Text ersetzen (Proxy an brain PUT /entry) ----
@app.put("/api/entry")
async def api_put_entry(request: Request) -> dict:
    """Ersetzt einen Gehirn-Eintrag (per doc_id) durch neuen Text. Der sync httpx-Call laeuft via
    asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    text = (body.get("text") or "").strip()
    if not doc_id or not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und text erforderlich"})
    payload = {"doc_id": doc_id, "text": text, "user_id": USER_ID}
    # Optionaler neuer Titel (Frank-Wunsch: Titel im Drawer bearbeiten). Nur durchreichen, wenn das
    # Frontend ihn mitschickt -> brain migriert dann die doc_id; ohne title bleibt der alte erhalten.
    if "title" in body and body.get("title") is not None:
        payload["title"] = str(body.get("title")).strip()
    try:
        return await asyncio.to_thread(_bput, "/entry", payload)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Ersatz fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


# --- Eintrag dauerhaft loeschen (Papierkorb-Button im Drawer): Proxy an brain DELETE /entry ---------
@app.delete("/api/entry")
async def api_delete_entry(doc_id: str = "") -> dict:
    """Loescht einen Gehirn-Eintrag dauerhaft (per doc_id). Frank-Wunsch: Papierkorb neben Bearbeiten.
    Sync httpx-Call via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    doc_id = (doc_id or "").strip()
    if not doc_id:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id erforderlich"})
    try:
        res = await asyncio.to_thread(_bdelete, "/entry", doc_id=doc_id, user_id=USER_ID)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Loeschen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Loeschen fehlgeschlagen: {type(e).__name__}"})
    # Gespraech geloescht -> zugehoerige Logbuch-.txt auf Platte Z mitloeschen (Logbuch == Gehirn synchron)
    if isinstance(res, dict) and res.get("category") == CONV_CATEGORY and res.get("title"):
        try:
            await asyncio.to_thread(_adelete, "/logbook", title=res["title"])
        except Exception as e:  # noqa: BLE001 — .txt-Sync best-effort; die Gehirn-Loeschung steht schon
            _log(logging.WARNING, "Logbuch-.txt-Sync (loeschen) fehlgeschlagen", err=str(e), title=res.get("title"))
    return res


# --- Eintrag in andere Kategorie verschieben (Kategorie-Dropdown im Drawer): Proxy an agent ---------
@app.post("/api/entry/category")
async def api_move_entry_category(request: Request) -> dict:
    """Verschiebt einen Eintrag (doc_id) in eine andere Kategorie. Geht ueber den AGENTEN, damit eine
    NEUE Ziel-Kategorie kanonisch in der Registry landet (sofort synchron mit Einstellungen->Kategorien
    + Gespraech-Dropdown). Sync httpx-Call via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    category = (body.get("category") or "").strip()
    if not doc_id or not category:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und category erforderlich"})
    try:
        # 120 statt 60 (Tiefen-Debugging 2026-07-02): tiefe Pfade 'A/B/C/...' brauchen mehr als 60 —
        # sonst wird die Ziel-Kategorie beim Drawer-Verschieben STILL abgeschnitten (kaputter Teil-Pfad).
        # Konsistent mit /api/chat (cappt 120) und dem Agent (_cat_key cappt 120, MoveEntryReq max_length=120).
        return await asyncio.to_thread(_apost, "/categories/move-entry", {"doc_id": doc_id, "category": category[:120]})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Eintrag-Verschieben fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Verschieben fehlgeschlagen: {type(e).__name__}"})


# --- Multi-Category: komplette Kategorie-LISTE eines Eintrags setzen (Drawer-Plus) -> Proxy an brain -----
@app.post("/api/entry/categories")
async def api_set_entry_categories(request: Request) -> dict:
    """Setzt die VOLLSTAENDIGE Kategorie-Liste eines Eintrags (Multi-Category, hinter dem Drawer-Plus).
    Geht DIREKT an brain /entry/categories (Re-Embed, da die Kategorien den Vektor mitpraegen). Die
    Kategorien sind schon kanonisch (aus dem Baum gewaehlt bzw. ueber den bestehenden 'Neue Kategorie'-
    Flow angelegt). Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    raw = body.get("categories")
    if not isinstance(raw, list):
        return JSONResponse(status_code=400, content={"ok": False, "detail": "categories muss eine Liste sein"})
    # saeubern: trimmen, je <=120 Zeichen (tiefe Pfade 'A/B/C/...'), Leere raus, dedupliziert (Reihenfolge
    # erhalten -> erste = primaer), max 12 (brain EntryCategoriesReq begrenzt ebenfalls auf 12).
    seen: set[str] = set()
    cats: list[str] = []
    for c in raw:
        if not isinstance(c, str):
            continue
        c = c.strip()[:120]
        if c and c.lower() not in seen:
            seen.add(c.lower())
            cats.append(c)
        if len(cats) >= 12:
            break
    if not doc_id or not cats:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und mindestens eine Kategorie erforderlich"})
    try:
        return await asyncio.to_thread(_bpost, "/entry/categories", {"doc_id": doc_id, "categories": cats, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Kategorie-Liste setzen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


# --- Papierkorb (Soft-Delete): Liste / im Papierkorb editieren / wiederherstellen (Proxy an brain) ---
@app.get("/api/trash")
def api_trash() -> dict:
    """Papierkorb-Liste (neueste Loeschung zuerst). Sync def -> Threadpool (fastapi §1)."""
    try:
        return _bget("/trash", user_id=USER_ID)
    except Exception as e:  # noqa: BLE001 — brain offline: leerer Papierkorb statt 500
        _log(logging.WARNING, "Papierkorb-Abruf fehlgeschlagen", err=str(e))
        return {"ok": False, "items": [], "count": 0}


@app.put("/api/trash")
async def api_trash_edit(request: Request) -> dict:
    """Aendert den Text eines Eintrags IM Papierkorb (kein Re-Embed). Proxy an brain PUT /trash."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    text = (body.get("text") or "").strip()
    if not doc_id or not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id und text erforderlich"})
    try:
        return await asyncio.to_thread(_bput, "/trash", {"doc_id": doc_id, "text": text, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Papierkorb-Edit fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Speichern fehlgeschlagen: {type(e).__name__}"})


@app.post("/api/trash/restore")
async def api_trash_restore(request: Request) -> dict:
    """Stellt einen Papierkorb-Eintrag wieder her (re-embed + zurueck ins Gehirn). Proxy an brain
    POST /trash/restore. Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    body = await request.json()
    doc_id = (body.get("doc_id") or "").strip()
    if not doc_id:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "doc_id erforderlich"})
    try:
        res = await asyncio.to_thread(_bpost, "/trash/restore", {"doc_id": doc_id, "user_id": USER_ID})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Wiederherstellen fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Wiederherstellen fehlgeschlagen: {type(e).__name__}"})
    # Gespraech wiederhergestellt -> Logbuch-.txt auf Platte Z zurueckschreiben (Logbuch == Gehirn synchron)
    if isinstance(res, dict) and res.get("category") == CONV_CATEGORY and res.get("title") and res.get("text"):
        try:
            await asyncio.to_thread(_apost, "/logbook", {"title": res["title"], "content": res["text"]})
        except Exception as e:  # noqa: BLE001 — .txt-Sync best-effort; der Gehirn-Eintrag ist schon zurueck
            _log(logging.WARNING, "Logbuch-.txt-Sync (schreiben) fehlgeschlagen", err=str(e), title=res.get("title"))
    res.pop("text", None)   # grossen Volltext nicht an den Browser zurueckgeben
    return res


@app.delete("/api/trash/all")
async def api_trash_empty() -> dict:
    """Leert den Papierkorb komplett (unwiderruflich). Proxy an brain DELETE /trash/all.
    Sync httpx via asyncio.to_thread (kein Event-Loop-Block, fastapi §1)."""
    try:
        return await asyncio.to_thread(_bdelete, "/trash/all", user_id=USER_ID)
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Papierkorb-Leeren fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Leeren fehlgeschlagen: {type(e).__name__}"})


# --- Prompt verbessern (G-Button): Text sprachlich verbessern (Proxy an agent /improve) ------------
@app.post("/api/improve")
async def api_improve(request: Request) -> dict:
    body = await request.json()
    text = (body.get("text") or "").strip()
    if not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Text"})
    # LAUT ablehnen statt still kuerzen (Tiefen-Debugging 2026-07-02) — gleiche Fehlerklasse wie der
    # 0.21.0/0.22.0-Speicherpfad-Bug: der stille text[:8000]-Slice verbesserte einen ABGESCHNITTENEN
    # Text, ohne dass Frank es sah (Rest ging verloren). Der Agent (ImproveReq max_length=8000)
    # haette >8000 ohnehin via 422 abgelehnt. 8000 deckt jeden eingesprochenen Text ab.
    # HTTP 200 + ok:false (wie /api/chat bei zu langem Text): so zeigt das Frontend die klare
    # detail-Meldung an, statt in seinem generischen catch zu landen.
    if len(text) > 8000:
        return {"ok": False, "detail":
                f"Der Text ist mit {len(text)} Zeichen zu lang für die Verbesserung (Maximum 8000). "
                "Es wurde NICHTS verändert — bitte in kleineren Abschnitten verbessern."}
    try:
        return await asyncio.to_thread(_apost, "/improve", {"text": text})
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Verbesserung fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Verbesserung fehlgeschlagen: {type(e).__name__}"})


# --- Sprache -> Text (Mikrofon): Audio-Body -> Groq Whisper. Browser schickt das Audio-Blob als ROHEN
#     Body (kein python-multipart noetig); wir bauen das multipart erst hier fuer Groq. --------------
def _groq_transcribe(audio: bytes, content_type: str) -> dict:
    files = {"file": ("audio.webm", audio, content_type or "audio/webm")}
    data = {"model": GROQ_STT_MODEL, "language": "de", "response_format": "json"}
    r = _HTTP.post(GROQ_STT_URL, files=files, data=data,
                   headers={"Authorization": f"Bearer {GROQ_API_KEY}"}, timeout=90.0)
    r.raise_for_status()
    return r.json()


@app.post("/api/transcribe")
async def api_transcribe(request: Request) -> dict:
    """Audio (roher Request-Body, z.B. audio/webm) -> Groq whisper-large-v3-turbo -> deutscher Text.
    GROQ_API_KEY fest im Server (.env). Body-Limit gegen OOM (fastapi §8); sync httpx via to_thread."""
    if not GROQ_API_KEY:
        return JSONResponse(status_code=503, content={"ok": False, "detail": "GROQ_API_KEY fehlt im Server"})
    audio = await request.body()
    if not audio:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Audio empfangen"})
    if len(audio) > MAX_AUDIO_BYTES:
        return JSONResponse(status_code=413, content={"ok": False, "detail": "Audio zu gross (>24 MB)"})
    ctype = request.headers.get("content-type", "audio/webm")
    try:
        out = await asyncio.to_thread(_groq_transcribe, audio, ctype)
        return {"ok": True, "text": (out.get("text") or "").strip()}
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Transkription fehlgeschlagen", err=str(e))
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Transkription fehlgeschlagen: {type(e).__name__}"})


# --- Text -> Sprache (Vorlesen): Hauptagent-Antwort -> Gemini-TTS -> WAV. Key fest im Server. ------
def _pcm_to_wav(pcm: bytes, rate: int = 24000, channels: int = 1, bits: int = 16) -> bytes:
    """Roh-PCM (Gemini liefert L16/PCM mono) in eine abspielbare WAV-Datei verpacken (44-Byte-Header).
    Der Browser kann rohes PCM nicht via <audio> abspielen, WAV dagegen direkt."""
    byte_rate = rate * channels * bits // 8
    block_align = channels * bits // 8
    header = (b"RIFF" + struct.pack("<I", 36 + len(pcm)) + b"WAVE"
              + b"fmt " + struct.pack("<IHHIIHH", 16, 1, channels, rate, byte_rate, block_align, bits)
              + b"data" + struct.pack("<I", len(pcm)))
    return header + pcm


def _gemini_tts(text: str, voice: str) -> bytes:
    """Synchroner Gemini-TTS-Call (im Threadpool via to_thread aufgerufen -> blockiert den Loop nicht,
    fastapi §1). Key per x-goog-api-key-Header (nicht ?key=, Almanach google-gemini-api §C8).
    Rueckgabe: WAV-Bytes. Wirft bei fehlenden Audio-Daten (mit finishReason fuers Log, §B4/§D10)."""
    payload = {
        "contents": [{"parts": [{"text": text}]}],
        "generationConfig": {
            "responseModalities": ["AUDIO"],
            "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": voice}}},
        },
    }
    url = f"{GEMINI_TTS_BASE}/{GEMINI_TTS_MODEL}:generateContent"
    r = _HTTP.post(url, json=payload,
                   headers={"x-goog-api-key": GEMINI_API_KEY, "Content-Type": "application/json"},
                   timeout=60.0)   # expliziter Timeout (fastapi §3); nie None
    r.raise_for_status()
    data = r.json()
    cand = (data.get("candidates") or [{}])[0]
    parts = ((cand.get("content") or {}).get("parts") or [])
    inline = next((p["inlineData"] for p in parts if p.get("inlineData") and p["inlineData"].get("data")), None)
    if not inline:
        raise RuntimeError(f"keine Audio-Daten (finishReason={cand.get('finishReason')})")
    pcm = base64.b64decode(inline["data"])
    m = re.search(r"rate=(\d+)", inline.get("mimeType", ""))   # Sample-Rate aus mimeType (Default 24000)
    return _pcm_to_wav(pcm, rate=int(m.group(1)) if m else 24000)


@app.get("/api/tts/voices")
def api_tts_voices() -> dict:
    """Liste der waehlbaren Stimmen + Default + ob TTS ueberhaupt verfuegbar ist (Key vorhanden)."""
    return {"ok": True, "voices": TTS_VOICES, "default": TTS_DEFAULT_VOICE, "enabled": bool(GEMINI_API_KEY)}


@app.post("/api/tts")
async def api_tts(request: Request):
    """Text (Hauptagent-Antwort) + Stimme -> WAV-Audio. GEMINI_API_KEY fest im Server (.env).
    Eingabe-Limit gegen OOM (fastapi §8); sync httpx via to_thread (kein Event-Loop-Block, §1)."""
    if not GEMINI_API_KEY:
        return JSONResponse(status_code=503, content={"ok": False, "detail": "GEMINI_API_KEY fehlt im Server"})
    body = await request.json()
    text = (body.get("text") or "").strip()
    if not text:
        return JSONResponse(status_code=400, content={"ok": False, "detail": "Kein Text"})
    if len(text) > TTS_MAX_CHARS:
        text = text[:TTS_MAX_CHARS]
    voice = (body.get("voice") or TTS_DEFAULT_VOICE).strip()
    if voice not in TTS_VOICE_NAMES:   # nur bekannte Stimmen -> sonst Default (verhindert API-Fehler)
        voice = TTS_DEFAULT_VOICE
    try:
        wav = await asyncio.to_thread(_gemini_tts, text, voice)
        _log(logging.INFO, "TTS erzeugt", voice=voice, chars=len(text), bytes=len(wav))
        return Response(content=wav, media_type="audio/wav")
    except Exception as e:  # noqa: BLE001 — sauberer Fehler statt 500; nie str(exc) roh an den Client (§7)
        _log(logging.WARNING, "TTS fehlgeschlagen", err=str(e), voice=voice)
        return JSONResponse(status_code=502, content={"ok": False, "detail": f"Vorlesen fehlgeschlagen: {type(e).__name__}"})


# --- Google-Drive-Backup (Z -> Drive): Status lesen + Buttons. Das eigentliche Backup laeuft auf dem
#     HOST (rclone + systemd, crash-sicher). Das Dashboard liest nur die Status-Datei und schreibt
#     Trigger-Flags in die Z-Wurzel, die von Host-systemd-.path-Units ausgefuehrt werden. -----------
@app.get("/api/backup/status")
def backup_status() -> dict:
    """Liest die Status-Datei, die das Host-Backup-Skript in die Z-Wurzel schreibt. Fehlt sie -> 'unbekannt'."""
    p = Path(BACKUP_DIR) / ".gdrive-backup-status.json"
    try:
        if p.is_file():
            return json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Backup-Status nicht lesbar", err=str(e))
    return {"state": "unbekannt", "detail": "Noch kein Backup gelaufen oder Status nicht verfuegbar."}


def _touch_trigger(name: str) -> bool:
    """Schreibt eine Trigger-Flag-Datei in die Z-Wurzel; die Host-systemd-.path-Unit fuehrt die Aktion aus."""
    try:
        (Path(BACKUP_DIR) / name).write_text(time.strftime("%Y-%m-%dT%H:%M:%S", time.gmtime()), encoding="utf-8")
        return True
    except Exception as e:  # noqa: BLE001
        _log(logging.WARNING, "Trigger schreiben fehlgeschlagen", name=name, err=str(e))
        return False


@app.post("/api/backup/run")
def backup_run() -> dict:
    """'Jetzt sichern' — stoesst das Host-Backup an (Spiegel Z -> Drive, crash-sicher)."""
    ok = _touch_trigger(".backup-trigger")
    return {"ok": ok, "detail": "Sicherung angestossen." if ok else "Konnte Sicherung nicht anstossen (Schreibrecht?)."}


@app.post("/api/backup/restore")
def backup_restore() -> dict:
    """'Wiederherstellen' (Notfall) — holt das Drive-Backup additiv zurueck auf Z (loescht lokal nichts)."""
    ok = _touch_trigger(".restore-trigger")
    return {"ok": ok, "detail": "Wiederherstellung angestossen." if ok else "Konnte Wiederherstellung nicht anstossen (Schreibrecht?)."}


@app.post("/api/backup/connect")
async def backup_connect(request: Request) -> dict:
    """'Mit Google verbinden' — nimmt das rclone-OAuth-Token (Frank erzeugt es einmalig per
    `rclone authorize "drive"` auf seinem PC) und legt es im Steuer-Verzeichnis ab; die Host-systemd-
    Unit baut daraus die rclone-Verbindung. Das Token wird NICHT geloggt (Secret)."""
    body = await request.json()
    token = (body.get("token") or "").strip()
    if "access_token" not in token:
        return JSONResponse(status_code=400, content={"ok": False,
            "detail": "Bitte die komplette Ausgabe von 'rclone authorize \"drive\"' einfuegen (ein JSON mit access_token)."})
    try:
        (Path(BACKUP_DIR) / ".rclone-token").write_text(token, encoding="utf-8")
        return {"ok": True, "detail": "Token erhalten — Verbindung wird hergestellt..."}
    except Exception as e:  # noqa: BLE001 — Token NIE loggen, nur Fehlertyp
        _log(logging.WARNING, "Token ablegen fehlgeschlagen", err=type(e).__name__)
        return JSONResponse(status_code=500, content={"ok": False, "detail": "Konnte Token nicht ablegen (Schreibrecht?)."})


# --- Backup-Inhalts-Liste: was liegt AKTUELL im Z->Drive-Backup? GENERISCH ueber alle Top-Level-
#     Ordner, damit auch kuenftig dazukommende Komponenten automatisch mit erscheinen (Frank-Wunsch). -
_BACKUP_LABELS = {                        # Ordnername -> (Anzeigename, Einheit, Sortier-Rang)
    "qdrant-snapshot": ("Qdrant-Snapshots (Gehirn)", "Snapshots", 0),
    "Logbuch":         ("Logbuch-Protokolle",        "Protokolle", 1),
    "Eval-Logs":       ("Eval-Check-Logs",           "Logs",       2),
}


@app.get("/api/backup/contents")
def backup_contents() -> dict:
    """Komplettes Abbild des Z->Drive-Backups: pro Top-Level-Ordner Anzahl Dateien, Datum der
    neuesten Datei und Groesse. GENERISCH — neue Ordner erscheinen automatisch. Sync def ->
    Threadpool (fastapi §1: blockierendes Datei-I/O nie in einem async-Handler)."""
    root = Path(BACKUP_ROOT)
    try:
        dirs = [p for p in root.iterdir() if p.is_dir() and not p.name.startswith(".")]
    except Exception as e:  # noqa: BLE001 — Backup nicht gemountet/lesbar -> leere Liste statt 500
        _log(logging.WARNING, "Backup-Inhalt nicht lesbar", err=str(e), root=str(root))
        return {"ok": False, "items": [], "detail": "Backup-Verzeichnis nicht lesbar."}
    items: list[dict] = []
    total_files = total_bytes = 0
    for d in dirs:
        count = 0
        newest = 0.0
        size = 0
        try:
            for f in d.rglob("*"):
                if f.is_file() and not f.name.startswith("."):
                    try:
                        st = f.stat()
                    except OSError:
                        continue          # einzelne unlesbare Datei ueberspringen, nicht die Liste killen
                    count += 1
                    size += st.st_size
                    newest = max(newest, st.st_mtime)
        except Exception as e:  # noqa: BLE001 — ein kaputter Unterordner darf die Liste nicht killen
            _log(logging.WARNING, "Backup-Komponente nicht vollstaendig lesbar", comp=d.name, err=str(e))
        label, unit, rank = _BACKUP_LABELS.get(d.name, (d.name, "Dateien", 99))
        items.append({
            "key": d.name, "label": label, "unit": unit, "count": count,
            "last": (time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(newest)) if newest else None),
            "size_mb": round(size / 1048576, 2), "_rank": rank,
        })
        total_files += count
        total_bytes += size
    items.sort(key=lambda it: (it["_rank"], it["label"].lower()))
    for it in items:
        it.pop("_rank", None)
    return {"ok": True, "items": items,
            "total_files": total_files, "total_mb": round(total_bytes / 1048576, 1)}


# --- Nachtschicht-Bibliothekar (librarian-Dienst): schlanker Proxy /api/lib/* ------------------
# Whitelisted Passthrough statt 15 Einzel-Proxys: das Frontend spricht /api/lib/<pfad>, das
# Dashboard reicht Methode, Query und JSON-Body 1:1 an den librarian weiter (mit SB_API_KEY).
# Sync-Aufruf via asyncio.to_thread (fastapi §1: der modul-globale _HTTP ist ein SYNC-Client).
LIBRARIAN_URL = os.getenv("LIBRARIAN_URL", "http://librarian:8004").rstrip("/")
_LIB_ALLOWED = ("status", "run-now", "reports", "report", "process", "process-status",
                "settings", "custom-tasks", "health")


@app.api_route("/api/lib/{path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def lib_proxy(path: str, request: Request):
    if not any(path == p or path.startswith(p + "/") for p in _LIB_ALLOWED):
        return JSONResponse(status_code=404, content={"ok": False, "detail": "Unbekannter Bibliothekar-Endpunkt"})
    body = await request.body()
    if len(body) > 200_000:   # OOM-Backstop (fastapi §8) — Entscheidungs-Listen sind nie so gross
        return JSONResponse(status_code=413, content={"ok": False, "detail": "Anfrage zu gross"})

    def _do() -> httpx.Response:
        return _HTTP.request(request.method, f"{LIBRARIAN_URL}/{path}",
                             params=dict(request.query_params),
                             content=body if body else None,
                             headers=HEADERS, timeout=180.0)

    try:
        r = await asyncio.to_thread(_do)
    except Exception as e:  # noqa: BLE001 — librarian evtl. (noch) nicht erreichbar: ehrlich melden
        _log(logging.WARNING, "librarian-Proxy fehlgeschlagen", path=path, err=type(e).__name__)
        return JSONResponse(status_code=502, content={"ok": False, "detail": "Bibliothekar nicht erreichbar."})
    try:
        payload = r.json()
    except Exception:  # noqa: BLE001
        payload = {"ok": False, "detail": (r.text or "")[:500]}
    return JSONResponse(status_code=r.status_code, content=payload)


@app.get("/", response_class=HTMLResponse)
def index() -> HTMLResponse:
    # Cache-Control: no-cache (Frank-Bug 2026-07-04, Root Cause): vorher kam die Seite OHNE
    # Cache-Header -> der Browser durfte seine alte Kopie beliebig lange zeigen; nach einem Deploy
    # fehlten neue Bereiche (Info-Bereich/Feature-Chronik) bis zum manuellen Hard-Reload (Strg+F5).
    # no-cache = Browser MUSS bei jedem Laden beim Server rueckfragen -> neue Oberflaeche ist nach
    # jedem Deploy sofort sichtbar. (no-cache, nicht no-store: Revalidierung genuegt, kein Verbot.)
    return HTMLResponse((STATIC / "index.html").read_text(encoding="utf-8"),
                        headers={"Cache-Control": "no-cache"})
