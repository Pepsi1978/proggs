# Anforderungsnachweis

Version: v0.8.0 - 26.07.2026 21:23 Uhr

| Bereich | Implementierung | Evidenz |
|---|---|---|
| Visuelle Shell | `apps/web/src`, `packages/ui` | Komponenten- und visuelle Tests |
| Verträge | `packages/contracts` | Schema- und Contracttests |
| Designmodell | `packages/design-model` | Operations- und Migrationstests |
| Persistenz | `packages/database` | Integrationstests gegen PostgreSQL |
| Authz/Audit | `packages/authz`, `apps/api` | Cross-Tenant- und Policytests |
| Realtime | `apps/realtime` | Sequenz- und Reconnecttests |
| KI | `packages/ai-gateway`, `apps/worker-ai` | Provider- und Schema-Evals |
| Preview/Export | Worker-Apps | Sandbox- und Exporttests |
| Projektimport | `apps/web`, `apps/api`, `project_imports`, `jobs`, MinIO | Streaming-Ordnerimport, vollständige UI-Quellchunkung, Framework-/Pfadguard-Tests und echter Fortschrittsstatus |
| OpenAI OAuth | `apps/web`, `apps/api`, `provider_connections` | AES-GCM-Tests und echter Gerätecode-Start gegen OpenAI |
| GPT-5.6 Routing | `apps/web`, `apps/api`, `codex-auth` | Sol/Terra/Luna-, vollständige Effort- und Live-Verbindungstests |
| Leinwand-Navigation | `apps/web`, `apps/api/preview-canvas-bridge.ts` | Zoom-Anker-, Zoomgrenzen- und Bridge-Injektionstests |
| Leinwand-Vollbild | `apps/web/src/App.tsx` | Browser-Vollbild mit fokussiertem Canvas und getrennten Panel-Schaltern |
| Import-Fidelity | `apps/api/src/import-reconstruction.ts`, `preview-canvas-bridge.ts`, `apps/web/src/App.tsx` | Plattform-/Quellviewport, unverändertes iframe-DOM, Assetbasis-, Dateivollständigkeits- und Fidelity-Prüfrunde |
| Formatabdeckung Import | `apps/api/src/import-project.ts` | Regressionstest: jede vom Filter akzeptierte Erweiterung hat einen bekannten MIME-Typ; Interface-Builder-Dateien (`.xib`) werden importiert |
| Design Board | `apps/web/src/App.tsx`, `styles.css` | Bildschirmliste aus der Preview-Bridge links, aktiver Bildschirm hervorgehoben, Klick navigiert die Bühne und füllt den Zurück-Verlauf |
| Markieren & Kommentieren | `apps/api/src/preview-canvas-bridge.ts`, `apps/api/src/server.ts`, `apps/web/src/App.tsx` | Element im Vorschau-Dokument umranden, Rechteck/CSS-Pfad/wörtlichen Ausschnitt melden, Rahmen mit zoomunabhängigem Eingabefenster, Ziel im Chat-Endpunkt (striktes Schema); Test hält das Bruecken-Skript parsebar |
| Projektbezogene Rückfragen | `apps/api/src/server.ts` | Offene Punkte aus dem echten Import statt fester Fragen; an drei Projekten belegt: reines HTML → keine Frage, natives Projekt → keine Frage (die Leinwand baut selbst auf), Web-Projekt mit Flutter-Quelle → Frage mit beiden Optionen |
| Vorschaugeometrie | `apps/api/src/extract-windows.ts`, `extract-apple.ts` | Hauptfenster bzw. Hauptstoryboard bestimmt die Größe, sonst die größte Fläche; Regressionstests mit den echten Maßen aus OpenLauncher (680 gegen 1360) und LaunchScreen gegen Main |
| Durchklickbarkeit | `apps/api/src/extract-android.ts` | Sprechende Öffner (`openHookEditor(hook)`) zählen als Navigationsziel; an PerfectMoment von 6 auf 9 Bildschirme mit Klickzielen |
| Theme-Umschaltung | `apps/api/src/preview-canvas-bridge.ts`, `apps/web/src/App.tsx` | Die Vorschau meldet, ob ein zweites Theme vorliegt, und schaltet `data-theme` im Design um |
| Claude-Design-Optik | `packages/ui/src/tokens.css`, `apps/web/src/styles.css` | Leinwand `#f0eee6`/`#2e2c26`, Akzent `#d97757`, Weichton `#f7e1d3` und die `sc-shine`-Ladeanimation, gelesen aus der Claude-Design-Laufzeit der Exporte unter `Designs` |
| Kommentaransicht | `apps/web/src/App.tsx` | Jeder abgeschickte Kommentar mit Element, Bildschirm, Anweisung und Ergebnis; „angewendet“ nur bei wirklich geschriebenen Dateien |
| Live-Farbregler | `apps/api/src/preview-canvas-bridge.ts`, `apps/web/src/App.tsx` | Die aus den Projektquellen gemessenen Farbtoken werden gemeldet und direkt in der Vorschau überschrieben — ohne KI-Lauf, auf allen Bildschirmen |
| Direkte Textänderung | `apps/api/src/server.ts`, `preview-canvas-bridge.ts`, `apps/web/src/App.tsx` | Text im Design an Ort und Stelle ändern, deterministisch ohne KI; nur bei genau einem Vorkommen, sonst gemeldete Mehrdeutigkeit. Live geprüft: eindeutiger Wortlaut angewendet und in der ausgelieferten Vorschau sichtbar (revision 1), fehlender Wortlaut und ein dreifach vorkommender abgelehnt |
| Modellauswahl im Ablauf | `apps/web/src/App.tsx` | Modell und Effort werden am Eingabefeld gewählt und atomar an den nächsten Lauf übergeben |

Offene oder fehlgeschlagene Gates dürfen nicht als bestanden markiert werden.

## Offen

- Die KI-gestützte Änderung über einen markierten Bereich braucht eine verbundene OpenAI-Codex-
  Verbindung; ohne sie endet der Lauf mit `OPENAI_NOT_CONNECTED`. Bis dorthin ist die Kette geprüft.
  Die direkte Textänderung läuft dagegen ohne Provider und ist end-to-end nachgewiesen.
- Alle im Auftrag genannten Bedienelemente sind vorhanden und funktionieren (Startseite,
  Projektansicht, Plus-Schaltfläche, Import, Modellauswahl, Rückfragen, Lade- und Fehlerzustände,
  Design Board, Vorschau, Bearbeitung, Speicherung, erneutes Öffnen). Offen bleiben die
  pixelgenauen Abstände und das Icon-Set der Startseite: `claude.ai/design` antwortet ohne
  Anmeldung mit HTTP 403, in dieser Umgebung steht kein Browser-Werkzeug bereit, und die
  öffentliche Dokumentation nennt keine Maße. Übernommen wurde daher nur Belegbares — aus der
  Claude-Design-Laufzeit der Exporte (Farben, Ladeanimation) und aus der offiziellen Doku
  (zwei Bereiche mit Gespräch links und Leinwand rechts, Kommentarmodus, Export oben rechts).
- Die Rekonstruktion nativer Projekte braucht eine verbundene OpenAI-Codex-Verbindung. Import,
  Faktenmessung, Bildschirm- und Navigationserkennung sind ohne sie geprüft, der KI-Aufbau nicht.
