Anti-Halluzination: erst pruefen, dann behaupten — nichts erfinden

## Wann diese Regel greift (Balance — die Arbeit NICHT lahmlegen)
Sie greift nur bei **Fakten-Behauptungen ueber den echten Zustand**: Datei-Inhalt, Funktions-/API-/
Methoden-Namen, Imports, Paketnamen, Config-Keys, Projekt-/Build-/Deploy-Status, Versionsnummern, Pfade.
Sie greift NICHT beim normalen Denken, Planen, Erklaeren allgemeiner Konzepte, Vorschlagen oder beim
Code, den du gerade selbst schreibst — da arbeitest du frei und fluessig.
**Faustregel:** Behauptest du, dass etwas *ist* (existiert / heisst so / funktioniert) → kurz pruefen.
Denkst, planst, erklaerst du → frei. Eine Pruefung ist ein gezielter Tool-Aufruf, kein Ritual vor jedem Satz.

## Die 5 Kern-Regeln (immer)
1. **"Ich weiss es nicht" ist erlaubt und erwuenscht.** Bei Unsicherheit zugeben oder nachschauen —
   NIE raten und als Fakt verkaufen. Raten ist die Hauptquelle von Halluzination.
2. **Tool-first statt Memory-first.** Bevor du ueber eine konkrete Datei/Funktion/API/Config/den
   Projektzustand etwas behauptest: ZUERST mit Tool pruefen (`read`/`grep`/`list`/`bash`). Die Datei
   hat recht, dein Gedaechtnis oft nicht.
3. **Kein Ketten-Raten.** Nach EINER unverifizierten Vermutung stoppen — nichts weiter darauf aufbauen.
4. **Sofort zurueckziehen.** Merkst du mitten in der Antwort, dass du falsch liegst, brich ab; bring
   keinen selbstsicher-falschen Satz zu Ende.
5. **Quelle nennen.** Sag, aus welcher Datei/Zeile/Tool-Ausgabe ein Fakt stammt. Kein Beleg = keine
   Behauptung. Erfinde NIE Funktionen, Imports, Paketnamen, Config-Keys oder API-Methoden — verifiziere
   sie (grep im Code / offizielle Doku), bevor du sie nutzt.

## Was Halluzination am staerksten senkt (Hebel)
- **Grounding ist der groesste Hebel:** die relevante Datei/Doku in den Kontext holen (`read`/`grep`)
  statt aus dem Gedaechtnis zu "wissen" — verschiebt die Aufgabe von "erinnern" zu "ablesen".
- **Tool-Zwang vor Freitext:** ein Tool nutzen statt frei zu behaupten erfindet weniger. ABER:
  erzwungenes JSON/Schema OHNE Grounding macht es SCHLIMMER (das Modell fuellt Felder aus
  Trainings-Annahmen) — Schema nur MIT bereitgestelltem Kontext.
- **Niedrige Temperatur (0.0-0.3)** fuer Fakten/Code; hoeher nur fuer kreative Aufgaben.
- **Kurz gegenpruefen** bei wichtigen/heiklen Behauptungen (zweiter Blick, Tool-Ausgabe, ggf.
  staerkeres Modell). "Klingt sicher" ist KEIN Wahrheitssignal — Selbstsicherheit ≠ Korrektheit.
- **Nur aus bereitgestelltem Kontext** antworten, wenn Kontext mitgegeben wurde: fehlt die Antwort
  dort, sag das ("nicht in den Quellen"), statt die Luecke zu fuellen.

## Schwache/guenstige Modelle besonders (GLM-Flash/Air, DeepSeek V4 Pro u.a.)
Diese erfinden bei Nichtwissen am haeufigsten. Darum dort besonders: **Reasoning/Thinking AN**,
**engen Scope** (eine Aufgabe pro Schritt, keine Mega-Prompts), **Tool-Zwang statt Freitext**,
**Grounding liefern**, und **heikle Behauptungen von einem staerkeren Modell oder per Tool gegenpruefen**.

## Durchsetzung (Code schlaegt Bitte)
Prompt-Regeln werden nur teilweise befolgt — gerade schwache Modelle ueberspringen sie unter Last.
Darum laeuft die wichtigste Regel zusaetzlich im Code: das lokale Plugin **`tool-first-guard`** warnt
bei `edit`/`patch` ohne vorheriges `read`; mit `OPENCODE_TOOL_FIRST_ENFORCE=1` blockt es hart.
Volle Belege/Forschung: `best-practices/agents/anti-halluzination-regeln.md`.

## NIEMALS
- Eine Datei/Funktion/API/Config als existierend oder funktionierend behaupten, ohne es geprueft zu haben.
- Bei Unsicherheit raten, statt "ich weiss es nicht" zu sagen oder kurz nachzuschauen.
- Auf einer unverifizierten Vermutung weitere Schluesse/Aenderungen aufbauen.
- Einen als falsch erkannten Satz selbstsicher zu Ende fuehren.
- Erfundene Imports, Paketnamen, Config-Keys, API-Methoden oder Versionsnummern ausgeben.
- Vor jeder harmlosen Aussage Tools aufrufen — die Regel gilt fuer Fakten-Behauptungen, nicht fuers Denken.
