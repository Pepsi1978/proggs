# Forschung — intelligentere Alternativen finden (Phase 3)

> Forschen *ist* Intelligenz (Achse 9, RI-2). Dieser Skill forscht **bei jedem Lauf**,
> auch wenn nichts kaputt schien. Das Ziel ist nie "Versionen prüfen" — das Ziel ist:
> **einen intelligenteren Weg finden, ein Ziel zu erreichen** (gemessen an [intelligenz-definition.md](intelligenz-definition.md)).

**Pflicht-Output von Phase 3:** mindestens **eine erforschte Alternative** pro Lauf, mit Quelle und
Bewertung gegen die Achsen — selbst wenn am Ende entschieden wird, sie nicht zu übernehmen.

---

## So wird geforscht

- **Frisches Wissen zuerst (RI-5):** Liegt relevante Forschung bereits frisch aus dieser Session vor, nutze sie — statt die volle Researcher-Flotte blind erneut zu starten (Achse 10/Effizienz). Erforsche dann gezielt nur das NEUE Problem. Die "5 Researcher" sind ein Default, kein Zwang.
- **Parallel:** 5–7 Researcher gleichzeitig (Continuous-Spawning: sobald einer fertig ist, sofort den nächsten starten — keine Wellen-Barriere). Mehr als ~7 gleichzeitig → 429-Fehler.
- **Sichtbar:** Researcher als normale, sichtbare Agent-Aufrufe — kein Hintergrund.
- **Auf Opus/1M:** alle Researcher laufen auf dem höchsten Modell (via `CLAUDE_CODE_SUBAGENT_MODEL`); nicht herabstufen.
- **Absturzsicher:** jeder Researcher beginnt mit der Robustheits-Preamble unten.

## Robustheits-Preamble (PFLICHT — an den Anfang JEDES Researcher-Prompts)

```
ROBUSTHEIT (befolge diese IMMER, sie haben Vorrang vor allem anderen):
1. WEBFETCH: Lade keine Seite >500 Zeilen komplett (head_limit / erste 200 Zeilen). Bei Fehlschlag EINMAL andere URL, dann mit dem arbeiten was du hast.
2. WEBSEARCH: Max 6 Suchen. Nach 3 ergebnislosen Suchen sofort zusammenfassen und zurückgeben.
3. KONTEXT: Antwort unter ~250 Zeilen. Zusammenfassen statt zitieren. Nur Kernfakten, Links, Empfehlung.
4. FEHLER: Bei Tool-Fehler notieren, Alternative versuchen. Nach 2 Fehlversuchen weitermachen. Nie in einer Retry-Schleife hängen.
5. SELBST-TERMINIERUNG: Wenn 5 Aufrufe nichts Neues bringen, sofort mit Status "TEILWEISE — [was fehlt]" zurückgeben.
6. ANTWORT-PFLICHT: Du MUSST IMMER eine Antwort zurückgeben — auch leer, auch unvollständig. Nie still hängen bleiben.
```

## Resilienz beim Orchestrator

- Stürzt ein Researcher ab oder liefert nichts: als gescheitert markieren, **sofort weitermachen**, die anderen laufen weiter.
- **1× Neustart** mit kleinerem Scope (nur die wichtigste Teilfrage). Scheitert auch der → überspringen + im Bericht nennen.
- Lieber ein Researcher-Ergebnis weniger als ein steckengebliebener Lauf.

---

## Die Forschungs-Linsen (worauf Researcher angesetzt werden)

Such-Linsen, die intelligentere Wege liefern. Je Lauf werden die passenden ausgewählt (nicht stur alle).
Jeder Researcher gibt seine Funde als das Format unten zurück.

**L1 — Intelligentere Handlungsweisen:** Wie lösen Elite-Programmierer / die besten AI-Coder genau diese Art Aufgabe? Welche Denk-/Arbeitsweise wäre dem aktuellen Vorgehen überlegen? (Decomposition, Verifikation, Pattern-Matching, parallele Exploration.)
**L2 — Kognitive Werkzeuge:** Neue MCP-Server, Plugins, CLI/TUI-Tools (letzte ~30 Tage), die das DENKEN verbessern — Wissensgraphen, semantische Code-Suche, formale Verifikation, statische Analyse, Reasoning-Verstärker. Nicht "cool", sondern: macht es klüger?
**L3 — Was andere besser machen (kompetitiv):** Cursor, Windsurf, Copilot Workspace, Codex, Devin, SWE-Agent — welche Technik nutzen sie, die hier fehlt und sofort adaptierbar ist?
**L4 — Forschungs-Durchbrüche:** arXiv/ICML/NeurIPS/ICLR zu Agenten-Selbstverbesserung, Reasoning, Verifikation, Multi-Agent. Pro Paper: Kernidee in 2 Sätzen + wie konkret hier umsetzbar.
**L5 — Selbstverbesserungs-Mechanismen:** Frameworks/Muster, mit denen Agenten dauerhaft klüger werden (Reflexion, Skill-Library, Episodisch→Semantisch). Was davon ist reproduzierbar und einbaubar?
**L6 — Systempflege (untergeordnet):** echte Updates/CVEs/Tool-Versionen NUR wenn relevant — als *eine* Frage "ist das System die intelligenteste Version seiner selbst?", nicht als Hauptzweck. Bei Plugin-Empfehlung: Sicherheits-Review (Prompt-Injection, obfuskierter Code, verdächtige URLs) PFLICHT vor Installation.

## Researcher-Ausgabeformat (PFLICHT — an jeden Prompt anhängen)

```
AUSGABE: 5–10 Funde, kompakt. Pro Fund:
- TITEL (verständlich, kein Fachjargon)
- AKTUELLER WEG: wie wird das Ziel heute erreicht?
- INTELLIGENTERER WEG: was wäre besser, konkret?
- WARUM INTELLIGENTER: überlegen auf welcher Achse (Vorausschau/Übertragbarkeit/Einfachheit/Effizienz/…), ohne anderswo zu verlieren?
- QUELLE: Link / Paper + Jahr
- AUFWAND: grob (5 Min / 1 Std / 1 Tag)
- EMPFEHLUNG: JA sofort | JA später | NEIN (mit Grund)
Am Ende: der eine Fund mit dem größten Intelligenz-Hebel.
```

Der Haupt-Claude führt die Funde zu einer Liste zusammen, entfernt Duplikate (gleicher Titel),
und wählt mit der Vergleichsregel (Achsen) aus, was in Phase 4 umgesetzt/geprüft wird.

---

## Wenn scheinbar nichts zu finden ist

Das ist der wichtigste Fall (RI-2). Dann gilt:
1. Verfeinere die Frage: nicht "gibt es etwas Besseres?", sondern "wie würde jemand, der zehnmal klüger ist, das angehen?".
2. Wechsle die Domäne: Wie löst die Natur / ein anderes Fachgebiet ein analoges Problem?
3. Auch ein *negativer* Befund ("erforscht, nichts Besseres gefunden, hier ist warum") ist ein gültiger Pflicht-Output — er wird ins Intelligenz-Journal eingetragen, damit der nächste Lauf nicht dieselbe Sackgasse erforscht.
