# Kategorie: Recht

Alles rund um rechtliche Themen, die unsere Apps und Projekte betreffen:
Datenschutz (DSGVO/GDPR), Wettbewerbsrecht (UWG), Werbung, Play-Store-Compliance,
AGB, Impressumspflicht, Lizenzen.

## Inhalts-Quellen

Die rechtlichen Inhalte zu unseren Apps liegen aus historischen Gründen verteilt
über das Repo. Diese Bibliothek **bündelt sie und verweist auf die Originale**,
damit man von einem Ort aus alles findet, ohne dass die Originale verschoben
werden müssen.

| Quelle | Was darin steht |
|--------|----------------|
| `~/proggs/rechtssicherheit.md` | Zentrale Wissensbasis: Was muss eine App rechtlich erfüllen? Detaillierte Anforderungen pro Land, Pre-Release-Checklisten |
| `~/proggs/BestJournalAndroid/docs/audit/RECHTSSICHERHEIT-AUDIT-*.md` | Iteratives Audit der BestJournal-App, Versionen v2 bis v7. Neueste Version ist immer die aktuellste Wahrheit |
| `~/proggs/BestJournalAndroid/app-roentgen-AUDIT-2026-05-01.md` | Compliance-Audit nach dem App-Roentgen-Skill: Manifest, Permissions, Architektur, Werbeaussagen |
| `~/proggs/BestJournalAndroid/app-roentgen-WERBEAUSSAGEN-FIX-2026-05-01.md` | UWG §5 Audit aller Premium-Marketing-Texte mit Korrektur-Vorschlägen |

## Cluster (Unterkategorien)

| Cluster | Worum es geht |
|---------|--------------|
| [INDEX.md](INDEX.md) | Verlinkter Index aller Rechts-Dokumente, chronologisch und thematisch |

## Wann hier reinschreiben

- Recherchen zu Datenschutzrecht, Wettbewerbsrecht, Werbeauflagen
- Audit-Dokumente zu Compliance-Status der Apps
- Notizen zu Play-Store-/App-Store-Submit-Anforderungen
- Lizenz-Recherchen (welche Library mit welcher Lizenz)
- Länder-spezifische rechtliche Anforderungen

## Wichtiger Grundsatz

**Die Bibliothek ist Lese-Index, kein Pflege-Ort für Audits.**

Aktive Audits werden weiterhin in den Projekt-Ordnern gepflegt (z.B.
`BestJournalAndroid/docs/audit/`). Diese Bibliothek **verweist** darauf, damit
ein Suchender sie findet — sie **kopiert** sie nicht. Kopien würden veralten.

Wenn ein neues Audit-Dokument entsteht, wird in der `INDEX.md` ein neuer
Eintrag mit Verweis auf das Original ergänzt.

## Was NICHT hier reinschreiben

- Eigene rechtliche Beratungen oder Empfehlungen für Dritte (das ist Anwälten vorbehalten)
- Persönliche Daten anderer Personen
- Geschäftsgeheimnisse anderer Firmen
