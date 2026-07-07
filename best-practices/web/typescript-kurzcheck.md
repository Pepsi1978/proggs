# TypeScript / Node.js Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | tsconfig anlegen | `strict` + `noUncheckedIndexedAccess` + `exactOptionalPropertyTypes` explizit | §A |
| 2 | TS-6.0-Defaults | `module`/`target`/`types`/`rootDir` festnageln, nicht floaten lassen | §A |
| 3 | `moduleResolution` waehlen | `nodenext` (Node/Library), `bundler` nur bei Bundler-Emit | §A |
| 4 | ESM-Setup | `"type": "module"`, relative Imports mit `.js`-Endung | §B |
| 5 | `__dirname` gebraucht | `import.meta.dirname` statt `fileURLToPath`-Workaround | §B |
| 6 | `exports`-Map schreiben | `types` ZUERST, `default` ZULETZT — Reihenfolge load-bearing | §B, §G |
| 7 | Neues Paket publizieren | ESM-only — vermeidet Dual-Package-Hazard ganz | §B, §G |
| 8 | Externe Daten (`JSON.parse`/`fetch`) | `unknown` + Laufzeit-Validierung (Zod), Typ aus Schema | §C |
| 9 | "eins von mehreren" modellieren | Discriminated Union + `never`-Exhaustiveness im `default` | §C |
| 10 | IDs als nackte `string` | Branded Types (`unique symbol`), null Laufzeit-Overhead | §C |
| 11 | Promises | Jedes behandeln (`no-floating-promises`), nie floaten | §D |
| 12 | Mehrere Tasks, Teilfehler zaehlen | `Promise.allSettled`, nicht `Promise.all` | §D |
| 13 | Timeout/Cleanup | `AbortSignal.timeout()`, `await using` fuers Cleanup | §D |
| 14 | `unhandledRejection`-Handler | NUR geordneter Shutdown, kein Catch-all-Schlucker | §D |
| 15 | CI-Install | `package-lock.json` committen, `npm ci` statt `npm install` | §E |
| 16 | `@types/node` | Major == Node-Major (24) pinnen | §E |
| 17 | Peer-Dep-Konflikt | `overrides` statt `--force`; `save-exact` | §E |
| 18 | Einfache Calls/CLI | Built-ins: `fetch`, `AbortSignal.timeout`, `parseArgs`, `node:test` | §F |
| 19 | `node file.ts` ausfuehren | Prueft KEINE Typen → separat `tsc --noEmit` | §F |
| 20 | Build/Publish | `tsc --noEmit` als Wahrheit; `attw`+`publint`; `files`-Allowlist | §G |
