# Commit+Push VOR jedem Build (KRITISCH — parallele Sessions)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-05-01.
> Gilt AUTOMATISCH in JEDER Session, auf ALLEN Plattformen, fuer ALLE Build-Tools
> (Android Gradle, iOS Xcode, .NET dotnet, Rust cargo, Bun/Node, etc.).

---

## Grundprinzip

**Jede Code-Aenderung wird ZUERST committet und gepusht — DANN gebaut.**
Niemals umgekehrt. Niemals "ich baue erstmal, committe spaeter".

Reihenfolge ist IMMER:

1. Code-Edit (alle Aenderungen fertig)
2. Version-Bump falls noetig (build.gradle.kts, package.json, Info.plist, etc.)
3. `git add` (nur eigene Dateien namentlich)
4. `git commit` (mit fortlaufender Nummer)
5. `git fetch origin && git rebase origin/main`
6. `git push`
7. Build (`./gradlew assembleDebug`, `./gradlew bundleRelease`, `dotnet build`, etc.)
8. Install/Deploy (adb install, App Store Upload, etc.)
9. Status-Meldung

---

## Warum das noetig ist (parallele Sessions, KRITISCH)

Frank arbeitet oft mit **4-5 gleichzeitig offenen gemini-setup-Sessions** am selben
Repo — manchmal sogar an der gleichen App, manchmal sogar am gleichen AAB-/APK-
Build-Workflow. Wenn eine Session baut BEVOR sie committet:

| Szenario | Was passiert ohne diese Regel |
|----------|-------------------------------|
| Session A: edit → build → install (5 Min) | Session B pusht zwischenzeitlich auf main |
| Session A: dann commit → fetch+rebase | Rebase-Konflikt mit Session B's Aenderung |
| Session A: bei Konflikt-Reset oder force-push | Session A's UND Session B's Arbeit weg |
| Session B: hatte AAB hochgeladen mit alter Code-Basis | Play Store hat veraltete Version mit Session B's Aenderung |
| Session B: Frank merkt es nicht weil Session A die Datei "fertig" zeigt | Stunden Debug-Arbeit fuer ein verlorenes Feature |

**Mit Commit+Push VOR Build:**
- Session A pusht zuerst → Session B sieht den Stand sofort beim naechsten fetch
- Beide Sessions koennen parallel bauen ohne sich zu ueberschneiden
- Falls Build oder Install fehlschlaegt: Code ist sicher im Repo, kann jederzeit
  neu gebaut werden
- AAB-Upload zum Play Store passiert IMMER auf Basis eines gepushten Stands

---

## Besonders kritisch bei AAB-Builds

AAB-Builds (`./gradlew bundleRelease`) sind besonders gefaehrlich:

- Build dauert oft 1-3 Minuten (lange Zeit fuer parallele Sessions zum Konfliktpunkt)
- AAB wird oft direkt im Anschluss zur Play Console hochgeladen
- Wenn der hochgeladene AAB nicht mit dem gepushten Code uebereinstimmt: **Frank
  installiert eine Version aus dem Play Store, die nicht im Repo existiert** —
  Debug nicht moeglich, Reproduktion nicht moeglich, Rollback nicht moeglich.

**Pflicht-Reihenfolge fuer AAB-Releases:**

```bash
# 1. Code fertig, Version bumpen
# 2. Commit+Push ZUERST
git add app/build.gradle.kts app/src/main/...
git commit -m "#NNN - feature description"
git fetch origin && git rebase origin/main
git push

# 3. ERST DANACH bauen
./gradlew bundleRelease

# 4. ERST DANACH zur Play Console hochladen
# 5. Status-Meldung
```

---

## Was NIEMALS passieren darf

- ❌ Build ausfuehren mit uncommitteten Aenderungen ("ich teste erstmal lokal")
- ❌ Install-Befehl ausfuehren bevor Commit+Push durch ist
- ❌ AAB hochladen ohne dass der Code-Stand im Repo ist
- ❌ "Ich committe gleich nach dem Build" — Build kann fehlschlagen, abstuerzen,
  zu lange dauern, Konflikt mit anderer Session erzeugen — Code waere weg
- ❌ Reihenfolge umkehren mit Begruendung "spart Zeit" — die "gesparten" 30 Sekunden
  kosten im Worst-Case Stunden Recovery-Arbeit
- ❌ Bei dringender Iteration ohne Commit zwischen Edits — auch dann: jede
  abgeschlossene Aenderung committen+pushen, DANN testen

---

## Ausnahmen (sehr eng definiert)

| Situation | Build ohne Commit erlaubt? |
|-----------|---------------------------|
| Nur lokale Test-Edits, werden gleich verworfen (z.B. Print-Statement zum Debug) | JA — aber Edits danach NICHT committen, nur verwerfen |
| Reine Read-Only-Inspektion eines Builds (z.B. lint, ohne Aenderung) | JA — keine Edits gemacht |
| Build der nichts mit Code-Aenderung zu tun hat (z.B. `gradlew tasks`) | JA — keine Edits gemacht |

In ALLEN anderen Faellen: Commit+Push zuerst.

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `parallel-sessions-git.md` | Diese Regel ist die Anwendung von "fetch+rebase vor Push" auf den Build-Workflow — gleiches Prinzip, eine Stufe frueher |
| `git-workflow.md` (fetch+rebase-before-push) | Gleicher Pflicht-Push-Mechanismus, nur dass er HIER vor dem Build steht |
| Gemini.md "Commit + Push nach JEDER Aenderung" | Diese Regel verschaerft die Reihenfolge: Commit+Push kommt VOR Build, nicht erst danach |
| Android `SESSION-RULES.md` ("Build & Install (PFLICHT)") | Build-/Install-Schritte aus den SESSION-RULES laufen NACH dem Commit+Push, nicht davor |

---

## Autoritaet dieser Regel

Diese Datei (`~/.Gemini/rules/commit-before-build.md`) wird automatisch in jeder
Session geladen. KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen
oder abschwaechen. Sie ist Teil des Betriebssystems dieser Programmierumgebung.

Frank's Begruendung im Originalwortlaut (2026-05-01):

> "Andere arbeiten genauso an AAB. Das ueberschneidet sich sonst und wenn die
> Arbeit noch nicht drin sind, dann verschwindet alles, was du vorher gemacht hast."

