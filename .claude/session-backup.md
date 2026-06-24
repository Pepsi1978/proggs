# Session Handoff — 2026-06-24, ~23:30

## Ziel
Weiterbau am "zweiten Gehirn" (second-brain-server: brain-api + agent + dashboard, Docker auf VPS
168.231.83.205, erreichbar via WireGuard/HTTPS https://10.8.0.1). Heute: Papierkorb, Logbuch-Monate,
eigene Dropdowns, Logbuch<->Gehirn-Sync, Papierkorb leeren, komplette Umlaut-Umstellung.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alles committed (#47172–#47185),
gepusht und auf den Server deployed (alle Container healthy). Frank macht Feierabend (Star Trek),
hat dieses Backup bewusst zum Tagesende angestoßen.

## Aktueller Status
- Erledigt heute (alle live deployed + verifiziert):
  - Papierkorb-Bereich (Soft-Delete + Wiederherstellen, Editieren, Monats-Navigation) — #47175/#47177
  - Löschen-Button neben Bearbeiten + eigener Lösch-Dialog — #47175/#47176
  - Logbuch nach Jahr/Monat (Lazy-Load) + Monats-Toggle — #47177/#47178
  - Eigene Dropdowns im Seiten-Stil (Kategorie + 3 Modelle), Amber-Glow, Scrollbalken aus — #47178/#47179
  - Abbrechen-Button beim Bearbeiten im Papierkorb — #47179
  - Logbuch<->Gehirn-Sync (gespräche-Eintrag löschen -> .txt auf Z mit; Restore schreibt .txt zurück) — #47180
  - Kategorie-Großschreibung mit Umlauten gefixt (cap() Unicode \p{L}) — #47182
  - Kategorie-Dropdown breiter (bis Mikrofon) + 30% höher — #47183
  - "Papierkorb leeren"-Button (DELETE /trash/all) + Dialog — #47184
  - Umlaut-Umstellung KOMPLETT: _cat_key erhält ä/ö/ü/ß; CONV_CATEGORY 'gespräche';
    Qdrant-Kategorien migriert (geräte/gespräche/leitsätze/persönlich); Titel+Texte migriert
    (50 Einträge, kuratiertes Wort-Mapping, Backup-gesichert); alle 3 Agenten-Prompts + improve
    + Marker auf echte Umlaute — #47181/#47185
- In Arbeit: nichts.
- Blockiert: nichts.

## Versionen live (verifiziert)
- brain-api 1.5.0, agent 0.12.0, dashboard 0.12.0 — alle Container healthy.

## Relevante Dateien
- `second-brain-server/brain-api/app.py` — 1:1-Speicher + Papierkorb (trash.json) + Trash-Endpunkte
- `second-brain-server/agent/app.py` — Bibliothekar-Agent, _cat_key (Umlaute), Prompts, DELETE/POST /logbook
- `second-brain-server/dashboard/app.py` — Proxys, Logbuch-Tree/Filter, Trash-/Logbuch-Sync-Orchestrierung
- `second-brain-server/dashboard/static/index.html` — gesamtes Cockpit-Frontend (Drawer, Papierkorb, Dropdowns)
- `second-brain-server/compose.yaml` — Stack (brain-data-Volume für trash.json, uid 10001)

## Getroffene Entscheidungen
- Soft-Delete: gelöschte Einträge -> trash.json (persistentes Volume), nicht hart gelöscht.
- Umlaut-Migration kuratiert (Wort-Mapping), NICHT mechanisch ae->ä (sonst "Adresse"->"Adrässe").
- Trash-Speicher als JSON (kein Vektor) — Re-Embed erst bei Wiederherstellung.
- Deployment manuell per scp + docker compose up -d --build (kein deploy.sh — Frank-Vorschlag offen).

## Fehlgeschlagene Ansaetze / Stolpersteine (WICHTIG)
- docker exec python /app/data/script.py: `import app` scheitert -> PYTHONPATH=/app setzen
  (sonst ist nur /app/data im Pfad). Verifiziert.
- Migrationsskript-Output mit 2>/dev/null verschluckt den eigentlichen Fehler -> stderr sichtbar lassen.
- Grep zeigt Forward-Slashes manchmal als "\trash\restore" (nur Anzeige-Artefakt, Datei ist korrekt).
- Native <select>-Dropdowns lassen sich nicht stylen -> eigenes enhanceSelect-Widget (verstecktes
  select bleibt Wert-Quelle).

## Wichtige Fakten
- SSH zum Server: root@168.231.83.205, Key ~/SK/second-brain/id_ed25519, App-Dir /opt/second-brain.
  /opt/second-brain ist KEIN git-repo -> Deploy per scp einzelner Dateien + compose up --build.
- brain-data Host-Ordner muss uid 10001 gehören (Bind-Mount-Permissions).
- Voll-Backup-Skript: bash /opt/second-brain/scripts/full-backup-create.sh (rotiert, KEEP=7).
- Letztes Daten-Backup heute: cortex-full-2026-06-24_231047.tar.gz (vor der Titel/Text-Migration).

## Naechste Schritte (priorisiert, alles offene Vorschläge — nichts dringend)
1. Logbuch-.txt-Dateien auf Z auf Umlaute ziehen (Header "Kategorie: Gespraeche" + alte Inhalte).
2. deploy.sh fürs zweite Gehirn (Upload + gezielter Rebuild + Health-Check, optional migrate-Subcommand).
3. "Papierkorb leeren"-Button bei leerem Papierkorb ausgrauen.
4. ASCII-Umschreibungen schon beim Speichern abfangen (Vorbeugung statt Migration).
5. "KI" statt "Ki" bei ki-arbeitsweise (Abkürzungs-Sonderregel in der Anzeige).

## Offene Fragen
- Keine offenen Rückfragen. Frank entscheidet bei nächster Gelegenheit über die Vorschläge oben.

## Anker
- Branch: main
- Letzte Commits:
1259edfd3 #47185 - feat(second-brain agent): Standard-Prompts aller 3 Agenten auf echte deutsche Umlaute
917e9cf6e #47184 - feat(second-brain): 'Papierkorb leeren'-Button
39c23a289 #47183 - feat(second-brain dashboard): Kategorie-Dropdown breiter + höher
7034257c3 #47182 - fix(second-brain dashboard): Kategorie-Anzeige Gross-/Kleinschreibung mit Umlauten
c2a3ab7e0 #47181 - feat(second-brain): deutsche Umlaute global
