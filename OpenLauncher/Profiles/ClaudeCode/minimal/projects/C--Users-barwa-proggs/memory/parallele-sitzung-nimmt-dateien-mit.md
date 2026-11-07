---
name: parallele-sitzung-nimmt-dateien-mit
description: "Parallele Jarvis-Sitzungen committen fremde Änderungen an gemeinsamen Dateien mit — neue Pakete zuerst committen, Aufrufstellen danach umstellen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 582de434-44ae-462b-a498-b8787dc7ad04
  modified: 2026-10-15T18:12:42.244Z
---

Beim Herauslösen von M1.4 (15.10.2026) hat eine parallele Jarvis-Sitzung meine schon umgestellte `AppViewModel.kt` in ihren eigenen Commit (1.8.78) mitgenommen und gepusht, während das neue Paket `de.frank.module.transkriptionsmodul` noch unversioniert war. `main` baute danach auf einem frischen Checkout nicht.

**Why:** In einer viel bearbeiteten App (Jarvis) laufen oft mehrere Sitzungen; sie committen ganze Dateien, nicht Hunks. Siehe [[commit-nur-mit-pfaden]].

**How to apply:** Vor dem Umstellen gemeinsamer Dateien `git status --short <App>` prüfen. Bei Umbauten erst die neuen Dateien bauen und committen, dann die Aufrufstellen ändern und sofort committen. Gelöschte Dateien beim Commit mit Pfaden über `git diff --cached --name-only --diff-filter=D` sammeln und danach `git status` prüfen, ob wirklich alles drin ist.
