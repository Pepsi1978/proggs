# Werft Studio - verbindliche Implementierungsspezifikation

Version: v0.1.11 - 20.07.2026 21:17 Uhr

Die im Auftrag vom 20.07.2026 vollständig übergebene Spezifikation mit den Kapiteln 0 bis 34 ist
verbindlich. Visuelle Quelle der Wahrheit ist `../Designs/Design-App für Browser/Studio.dc.html`;
Sicherheit, Datenintegrität und Backendverhalten folgen der übergebenen Programmierspezifikation.

## Lieferumfang

Die Anwendung umfasst S-01 bis S-28, die E2E-Flows F-01 bis F-14 und die Muss-Anforderungen
FR-AUTH-001 bis FR-PORT-001. Der kanonische Zustand ist ein versioniertes `DesignDocument`.
Alle Mutationen sind autorisiert, revisions- oder idempotenzgeschützt und auditierbar. KI-Modelle
arbeiten ausschließlich über das anbieterneutrale Gateway. Preview- und Buildprozesse laufen auf
einer getrennten Origin und ohne Zugriff auf Produktionssecrets.

## Referenznachweis

| Datei | SHA-256 |
|---|---|
| `Studio.dc.html` | `209b09aeae0b7fafc1b4d86805283d6e3d9a8f0ad1ef08ea18ae41040cb3ec25` |
| `support.js` | `c60c49083997f51a592df118c0068475337afd20b8cfd8e1cd9d5eb0c7e254f6` |
| `.thumbnail` | `bb1cc9687387ca08ad1beceb4714962de3c898892d262ecd81b81306bfc7558e` |

Die vollständige Anforderungsmatrix wird in `docs/traceability.md` gegen Tests und Evidenz geführt.
