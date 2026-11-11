---
name: parallele-sitzung-nimmt-dateien-mit
description: "Parallele Jarvis-Sitzungen committen fremde Änderungen an gemeinsamen Dateien mit — neue Pakete zuerst committen, Aufrufstellen danach umstellen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 582de434-44ae-462b-a498-b8787dc7ad04
  modified: 2026-11-11T13:28:29.781Z
---

Beim Herauslösen von M1.4 (15.10.2026) nahm eine parallele Jarvis-Sitzung meine schon umgestellte `AppViewModel.kt` in ihren Commit (1.8.78) mit und pushte, während das neue Paket `de.frank.module.transkriptionsmodul` noch unversioniert war. `main` baute danach auf frischem Checkout nicht.

**Why:** In viel bearbeiteten Apps (Jarvis) laufen oft mehrere Sitzungen; sie committen ganze Dateien, nicht Hunks. Siehe [[commit-nur-mit-pfaden]].

**How to apply:**
- Vor dem Umstellen gemeinsamer Dateien `git status --short <App>` prüfen.
- Vor dem Bauen ebenso: Stehen dort fremde Änderungen (z. B. `??` neues Paket, `M build.gradle.kts`), gleich im Worktree bauen: `git worktree add --detach <scratchpad>/wt HEAD`, `git diff -- <eigene Pfade> | git -C <wt> apply`, dort `gradlew assembleDebug`; danach `git worktree remove --force`. Spart den Fehlbau (11.11.2026: 3,5 min).
- Bei Umbauten erst die neuen Dateien bauen und committen, dann die Aufrufstellen ändern und sofort committen.
- Gelöschte Dateien beim Commit mit Pfaden über `git diff --cached --name-only --diff-filter=D` sammeln, danach per `git status` prüfen, ob wirklich alles drin ist.
