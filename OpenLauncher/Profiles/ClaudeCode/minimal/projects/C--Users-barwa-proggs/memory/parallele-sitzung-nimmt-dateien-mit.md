---
name: parallele-sitzung-nimmt-dateien-mit
description: "Parallele Jarvis-Sitzungen committen fremde Änderungen an gemeinsamen Dateien mit — neue Pakete zuerst committen, Aufrufstellen danach umstellen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 582de434-44ae-462b-a498-b8787dc7ad04
  modified: 2026-11-12T10:24:50.689Z
---

Beim Herauslösen von M1.4 (15.10.2026) nahm eine parallele Jarvis-Sitzung meine schon umgestellte `AppViewModel.kt` in ihren Commit (1.8.78) mit und pushte, während das neue Paket `de.frank.module.transkriptionsmodul` noch unversioniert war. `main` baute danach auf frischem Checkout nicht.

**Why:** In viel bearbeiteten Apps (Jarvis) laufen oft mehrere Sitzungen; sie committen ganze Dateien, nicht Hunks. Siehe [[commit-nur-mit-pfaden]].

**How to apply:**
- Vor dem Umstellen gemeinsamer Dateien `git status --short <App>` prüfen.
- Vor dem Bauen ebenso: Stehen dort fremde Änderungen (z. B. `??` neues Paket, `M build.gradle.kts`), gleich im Worktree bauen: `git worktree add --detach <scratchpad>/wt HEAD`, `git diff -- <eigene Pfade> | git -C <wt> apply`, dort `gradlew assembleDebug`; danach `git worktree remove --force`. Spart den Fehlbau (11.11.2026: 3,5 min).
- Bei Umbauten erst die neuen Dateien bauen und committen, dann die Aufrufstellen ändern und sofort committen.
- Gelöschte Dateien beim Commit mit Pfaden über `git diff --cached --name-only --diff-filter=D` sammeln, danach per `git status` prüfen, ob wirklich alles drin ist.
- Versionseintrag im Worktree erst direkt vor dem letzten Bau: `git fetch && git rebase origin/main`, dann `eintrag.py`, bauen, committen, sofort pushen. Nie nach dem Bau bumpen: Die APK trüge die alte versionCode, und jarvis-install bricht ab, weil origin/main neuer ist (12.11.2026: drei Rebase-Konflikte im Versionslog). Scheitert der Push: Rebase, im Versionslog den fremden Stand nehmen, `eintrag.py` neu, neu bauen. Nach jedem Konflikt vor dem Commit `grep -rln '^<<<<<<<' <App>/app/src` (in `funktionen.md` blieb ein Marker stehen).
