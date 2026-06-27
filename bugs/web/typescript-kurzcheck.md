# TypeScript & Node.js (+ npm, Bun) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | TS-6.0-Upgrade: `process`/`Buffer`/`describe` fehlen | `types: ["node"]` explizit setzen (Default ist `[]`) | §A1 |
| 2 | TS-6.0-Upgrade: ploetzlich Massen-Typfehler | `strict` ist jetzt Default `true`; schrittweise migrieren | §A2 |
| 3 | TS-6.0: `module`/`target`/`rootDir` floaten | Alle Verhaltens-Defaults EXPLIZIT festnageln | §A3, §A4 |
| 4 | `ignoreDeprecations` gesetzt | Unterdrueckt nur Deprecations, NICHT Default-Aenderungen | §B |
| 5 | `moduleResolution: node`/`node10`/`classic` | Auf `nodenext` (Node) oder `bundler` (Bun) umstellen | §B7 |
| 6 | `ERR_REQUIRE_ASYNC_MODULE` bei `require(esm)` | Top-Level-await im Graphen → auf `await import()` | §C15 |
| 7 | ESM `ERR_MODULE_NOT_FOUND` | Relative Imports brauchen `.js`-Endung (auch bei `.ts`) | §C17 |
| 8 | `__dirname is not defined` in ESM | `import.meta.dirname` statt CJS-Globals | §C18 |
| 9 | `instanceof` false / Singleton doppelt | Dual-Package-Hazard; ESM-only oder State stateless | §C19 |
| 10 | `JSON.parse`/`.json()` weiterverwenden | Sofort als `unknown`, dann validieren — nie `any` | §D24 |
| 11 | `arr[i]`/`obj[key]` crasht zur Laufzeit | `noUncheckedIndexedAccess: true` (NICHT in `strict`) | §D27 |
| 12 | `as`/`!`/`as unknown as T` benutzt | Echten Guard/`satisfies`/Validierung statt Behauptung | §D29, §D30, §D31 |
| 13 | Prozess crasht mit exit 1 | Floating Promise; jedes `await`/`.catch`, kein stiller Schlucker | §E40 |
| 14 | `forEach` mit async-Callback | Wartet NICHT → `for...of`+`await` oder `Promise.all` | §E43 |
| 15 | `npm error ERESOLVE` | `--legacy-peer-deps` (chirurgisch), NIE `--force` | §F53, §F54 |
| 16 | Typfehler nach Node-Upgrade | `@types/node`-Major == Node-Major (24) pinnen | §F55 |
| 17 | `tsc` inkonsistent | Immer `npx tsc`/`./node_modules/.bin/tsc`, nie global | §F67 |
| 18 | Code laeuft in Bun, bricht in Node | `.ts`-Imports/`bundler` sind Bun-only; native Module gegen Node bauen | §H79, §H80, §H75 |
| 19 | Import bricht nur im Linux-CI | Datei-Casing konsistent; `forceConsistentCasingInFileNames` | §I84 |
