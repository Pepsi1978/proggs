---
name: bauweise-ohne-spec-pipeline
description: "Neue Apps direkt bauen (Referenz-App + Module + Nachbesserung), nicht mehr über Spec-Grilling und Werft-Designer"
metadata:
  node_type: memory
  type: project
  originSessionId: b7db77d8-d4f1-438f-b516-b06f24346713
  modified: 2026-11-07T13:55:33.930Z
---

Am 13.09.2026 gelöscht: Skills `neue-applikation`, `spec-schmiede`, `spec-rueckimport`, `design-umsetzer` (Commit c84d5589a). Werft-Pipeline Grilling → Designer → Rückimport → Bau wird nicht mehr benutzt.

**Why:** Apps nur für den Eigengebrauch, nicht für den Play Store. Stundenlanges Grilling und der Umweg über Werft Studio kosten mehr Zeit als sie sparen; Korrekturen am fertigen Prototyp gehen heute schnell und gebündelt.

**How to apply:** Neue App = ein Satz: Zielplattform, Referenz-App für Design und Effekte, gewünschte Module aus [[modul-bibliothek]], Name und Zweck. Direkt bauen, kein Skill, kein Designer dazwischen. Dann Prototyp testen, gebündelt „fixe alle Logikfehler“ / „fixe Performance“ in Durchgängen, zuletzt nach Lücken gegenüber der ursprünglichen Zielvorgabe fragen.

Offen: GenialeIdeen soll später Design-Vorlage werden, noch in Arbeit. Design NICHT vorher als DESIGN.md oder Modul herausziehen; erst wenn Frank ausdrücklich sagt, dass er zufrieden ist, per `modul-erstellen`.
