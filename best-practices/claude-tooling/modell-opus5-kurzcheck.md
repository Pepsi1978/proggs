# Modell-Leitplanken Claude Opus 5 — Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT
> (`modell-opus5.md`), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Grundmuster:** Opus 5 macht **zu viel**, nicht zu wenig. Alte Regeln aus der 4.x-Zeit waren
> gegen Nachlaessigkeit gebaut — gebraucht wird jetzt Begrenzung. Und: Opus 5 nimmt Anweisungen
> **woertlicher**, unpraezise Verhaltensregeln werden dadurch gefaehrlich.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Regel aus 4.x-Zeit | Verifikations-/Doppelcheck-Pflichten streichen — Modell verifiziert selbst | §2 |
| 2 | Abgrenzung dazu | echte Aktionen (bauen, installieren, Zeit per Befehl) bleiben — kein Selbstcheck | §2 |
| 3 | zu lange Antworten/Dateien | Kuerze fuer Chat UND Dateien getrennt fordern; Effort senken wirkt NICHT | §3 |
| 4 | Aufgabe waechst ungefragt | Scope + "was bleibt unangetastet" nennen; keine neuen Dateien/Schichten ungefragt | §4 |
| 5 | Doppelung vermeiden | Scope-/Korrektur-Regeln stehen schon im Claude-Code-System-Prompt — vorher pruefen | §4 |
| 6 | Subagenten | nur grosse, echt unabhaengige Straenge; nie zur Selbstpruefung | §5 |
| 7 | Review/Audit beauftragen | kein "nur High-Severity" — erst alles finden, dann getrennt filtern | §6 |
| 8 | neue Regel formulieren | Ziel + Begruendung statt nacktem NEVER; kein "CRITICAL: You MUST" (Overtriggering) | §7 |
| 9 | Regelwerk pflegen | Loeschtest "wuerde ein starkes Modell ohne die Zeile schlechter arbeiten?"; ~halbjaehrlich ablatieren | §7 |
| 10 | Denken vertiefen | nur `ultrathink` wird erkannt; "think hard" ist wirkungsloser Fliesstext | §8 |
| 11 | Effort | Default `high`; Opus 5 BEHAELT die zuletzt gesetzte Stufe ueber Sitzungen hinweg | §8 |
| 12 | Thinking abschalten | nur bis Effort `high` erlaubt, ab `xhigh` HTTP 400; nie per "denk nicht"-Anweisung | §8 |
| 13 | 1M-Kontext | Default+Max ohne Aufpreis, aber kein Freibrief — proaktiv kompaktieren | §9 |
| 14 | Prompt-Aufbau | lange Dokumente oben, Anweisung ans Ende, XML-Struktur | §9 |
| 15 | Zeit/Version/Fakten | per Befehl holen — Halluzination ggue. 4.8 leicht GESTIEGEN, nicht gesunken | §10 |
| 16 | Behauptung ueber Repo/Code | billig pruefbare Aussagen pruefen statt erzaehlen (Issue #81168) | §10 |
| 17 | destruktive Aktionen | Commit als Rollback-Punkt, Backup ausserhalb des Baums, `permissions.deny` | §11 |

**Wirkungslos** (nicht versuchen): Effort senken gegen Geschwaetzigkeit oder Scope-Creep · lange
"Never do X"-Listen · `CRITICAL: You MUST` · Beispiel-Sammlungen im Prompt.
