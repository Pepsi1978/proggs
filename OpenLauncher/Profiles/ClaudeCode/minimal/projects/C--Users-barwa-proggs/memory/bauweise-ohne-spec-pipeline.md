---
name: bauweise-ohne-spec-pipeline
description: Neue Apps werden direkt gebaut (Referenz-App + Module + Nachbesserung), nicht mehr ueber Spec-Grilling und Werft-Designer
metadata:
  type: project
---

Am 13.09.2026 wurden die Skills `neue-applikation`, `spec-schmiede`, `spec-rueckimport`
und `design-umsetzer` geloescht (Commit c84d5589a). Die Werft-Pipeline aus Grilling →
Designer → Rueckimport → Bau wird nicht mehr benutzt.

**Why:** Apps entstehen nur fuer den Eigengebrauch, nicht fuer den Play Store. Das
stundenlange Grilling und der Umweg ueber Werft Studio kosten mehr Zeit als sie sparen,
weil Korrekturen am fertigen Prototyp heute schnell und gebuendelt moeglich sind.

**How to apply:** Neue App = ein Satz — Zielplattform, Referenz-App fuer Design und
Effekte, Liste der gewuenschten Module aus [[modul-bibliothek]], Name und Zweck. Dann
direkt bauen, keinen Skill und keinen Designer dazwischenschalten. Danach Prototyp
testen, gebuendelt "fixe alle Logikfehler" / "fixe Performance" in Durchgaengen, zuletzt
nach Luecken gegenueber der urspruenglichen Zielvorgabe fragen.

Offen: GenialeIdeen soll spaeter die Design-Vorlage werden, ist aber noch in Arbeit.
Das Design NICHT vorher als DESIGN.md oder Modul herausziehen — erst wenn Frank
ausdruecklich sagt, dass er zufrieden ist, dann per `modul-erstellen`.
