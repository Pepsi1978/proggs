# Recht — Index aller Dokumente im Repo

Verlinkter Index aller Rechts-Dokumente die irgendwo im Repo liegen.
Die **aktuelle** Version eines iterativen Audits steht oben, ältere Versionen darunter.

## Zentrale Wissensbasis (immer als Erstes lesen)

### `rechtssicherheit.md` — Repo-Root

[../../rechtssicherheit.md](../../rechtssicherheit.md)

Die zentrale Wissensbasis für rechtliche Anforderungen an unsere Apps.
Enthält länderspezifische Vorgaben, Pre-Release-Checklisten, Anforderungs-Cluster
nach Themen (Datenschutz, Werbung, Inhalte, etc.).

**Wenn ein neues Land/Markt erschlossen werden soll oder eine neue rechtliche
Frage auftritt: hier zuerst nachschauen.**

---

## BestJournal Android — Audit-Dokumente

Iterative Compliance-Audits der BestJournal-Android-App. Jede Version ergänzt
die vorherige — die jeweils neueste ist die aktuell gültige Wahrheit.

### Aktuell (v7, 2026-04-28)

[../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v7.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v7.md)

Aktueller Compliance-Stand für die Play-Console-Submission. Frank's TODO-Liste
zur finalen Submit-Vorbereitung steht hier.

### Historische Versionen (chronologisch)

| Datum | Version | Pfad |
|-------|---------|------|
| 2026-04-28 | v6 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v6.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-28-v6.md) |
| 2026-04-23 | v5 (revised) | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-23-v5.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-23-v5.md) |
| 2026-04-22 | v5 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-22-v5.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-22-v5.md) |
| 2026-04-21 | v4 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v4.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v4.md) |
| 2026-04-21 | v3 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v3.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-21-v3.md) |
| 2026-04-20 | v2 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20-v2.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20-v2.md) |
| 2026-04-20 | v1 | [../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20.md](../../BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-2026-04-20.md) |

Diese Vorgänger-Versionen sind als historische Referenz erhalten. Sie zeigen
wie sich der Audit-Prozess entwickelt hat und welche Themen wann erkannt wurden.

---

## App-Röntgen-Audits (BestJournal Android)

Audits nach dem App-Röntgen-Skill — systematische Analyse der App auf
versteckte Features, Architektur, Compliance.

### Compliance-Audit 2026-05-01

[../../BestJournalAndroid/app-roentgen-AUDIT-2026-05-01.md](../../BestJournalAndroid/app-roentgen-AUDIT-2026-05-01.md)

Vollständiger Compliance-Audit der BestJournal-Android-App: Manifest,
Permissions, Subscription-State-Machine, Paywall-Marketing-Claims, versteckte
Features, Architektur-Schichten 1-7.

### Werbeaussagen-Fix 2026-05-01

[../../BestJournalAndroid/app-roentgen-WERBEAUSSAGEN-FIX-2026-05-01.md](../../BestJournalAndroid/app-roentgen-WERBEAUSSAGEN-FIX-2026-05-01.md)

Detaillierter UWG §5 Audit aller Premium-Marketing-Texte mit Korrektur-Vorschlägen
und Zeichenlängen-Analyse für UI-Compatibility. Daraus entstand der Commit #1950
mit Korrekturen in 27 Sprachen.

---

## Themen-Cluster (für die Suche)

Wer zu einem bestimmten Thema sucht, findet die wichtigsten Quellen hier:

### Datenschutz / DSGVO / GDPR

- `rechtssicherheit.md` (Hauptkapitel)
- Audit v7 (Privacy-Policy-Status, Account-Deletion-URL)

### Wettbewerbsrecht / UWG / Werbeaussagen

- `app-roentgen-WERBEAUSSAGEN-FIX-2026-05-01.md` (UWG §5)
- Audit v7 (Werbeaussagen-Status nach Korrektur)

### Play-Store-Submission

- Audit v7 (Health Declaration, Standort-Begründung, Country-Distribution)
- `rechtssicherheit.md` (Länder-Ausschluss-Empfehlungen)

### Länder-spezifische Themen

- Quebec/Kanada: Memory-Eintrag `project_quebec_canada_future_update.md` und
  Audit v6 enthalten den Stand. Kanada beim Initial-Release ausgeschlossen.
- UK: Memory-Eintrag `feedback_uk_always_option_b.md` — UK ausschließen wenn
  personenbezogene Daten von UK-Bürgern verarbeitet werden.

---

## Was hier NICHT steht

- Konkrete Korrespondenz mit Anwälten (vertraulich, nicht im Repo)
- Persönliche Daten von Frank oder anderen
- Privatdokumente zu Markenrecht/Patenten falls vorhanden
