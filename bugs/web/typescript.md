# Bekannte Bugs: TypeScript & Node.js (+ npm, Bun)

> **PFLICHT-LESEN vor echter Arbeit an TypeScript-/Node-Code.**
> Stand: zuletzt recherchiert am **2026-06-02**, Abschnitt **J** ergaenzt am **2026-06-03** (aus dem Best-Practices-Lauf), **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax) fuer **Node v24.15.0 · TypeScript 6.0.2 · npm 11.17.0 · Bun 1.3.14**.
> **Anker:** typescript=6.0.2  <!-- maschinenlesbar fuer check-version-anchor.py -->
> Beispielprojekt im Repo: `~/proggs/mcp-code-search` (ESM + Bun + `better-sqlite3`, `moduleResolution: bundler`, `strict: true`).
> Fokus dieser Datei: **was in der Praxis schiefgeht** (Bugs/Fallen) + funktionserhaltende Loesung. Die "richtige Seite der Medaille" — *wie man es von vornherein richtig macht* — steht in `~/proggs/best-practices/web/typescript.md` (wechselseitige Bezugstabelle unten).

> **Update 2026-07-02:** Re-Recherche seit 2026-06-03 fand keine belegten neuen TypeScript-6.0.2/6.0.3/6.1-Breaking-Changes. Live-Deltas: npm **11.17.0** und Bun **1.3.14**; die bestehenden Regeln zu expliziten TS-Defaults, Node/Bun-ESM-Unterschieden und Windows/Bun-Fallen bleiben unveraendert gueltig.

---

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

---

## A. TypeScript 6.0 — geaenderte Defaults (Breaking beim Upgrade, NICHT durch `ignoreDeprecations` abschaltbar)

### 1. `types` defaultet jetzt auf `[]` (kein automatisches `@types`-Crawling)   ⭐ HAEUFIG
**Symptom:** Nach Upgrade ploetzlich `Cannot find name 'process'` / `'describe'` / `'it'` / `Buffer` — globale Deklarationen fehlen, obwohl die `@types`-Pakete installiert sind.
**Ursache:** TS 6.0 laedt nicht mehr automatisch ALLE `node_modules/@types/*` (20-50% schnellere Builds). `types` ist default leer.
**Versionen:** ab TS 6.0 (per Design).
**FIX:** Benoetigte Globals explizit listen: `"types": ["node", "bun"]` (bzw. `["node", "jest"]`). Funktionserhaltend — nur explizit machen. `["*"]` stellt das alte Verhalten her (nicht empfohlen, langsam).
**Quelle:** typescriptlang.org/docs/handbook/release-notes/typescript-6-0.html · devblogs.microsoft.com/typescript/announcing-typescript-6-0/

### 2. `strict` ist jetzt default `true`   ⭐ HAEUFIG
**Symptom:** Bisher ignorierte Typfehler (strictNullChecks, noImplicitAny, useUnknownInCatchVariables …) tauchen nach dem Upgrade massenhaft auf.
**Ursache:** In 5.x war `strict` opt-in, in 6.0 default an.
**Versionen:** ab TS 6.0 (per Design).
**FIX:** Schrittweise migrieren. Um den alten Stand zu halten: explizit `"strict": false` setzen, dann Flag fuer Flag nachziehen. Fehler NICHT mit `as any` zukleistern (siehe D-Reihe).
**Quelle:** devblogs.microsoft.com/typescript/announcing-typescript-6-0/ · socket.dev/blog/typescript-6-0-released-final-javascript-based-version

### 3. `module` defaultet auf `esnext`, `target` auf `es2025`
**Symptom:** Output-Format/Syntax aendert sich; CommonJS-Konsumenten brechen; zu neue Syntax fuer aeltere Runtime.
**Ursache:** Geaenderte Defaults in 6.0.
**Versionen:** ab TS 6.0.
**FIX:** `"module"` und `"target"` explizit setzen (z.B. `"module": "nodenext"`, passendes `"target"`). Funktion bleibt — nur explizit machen.
**Quelle:** nodejs.org/en/blog/release/v24.0.0 (Migrationskontext) · TS-6.0-Release-Notes

### 4. `rootDir` defaultet auf `.` (tsconfig-Verzeichnis)
**Symptom:** Output landet falsch, z.B. `./dist/src/index.js` statt `./dist/index.js`.
**Ursache:** Geaenderter Default.
**Versionen:** ab TS 6.0.
**FIX:** `"rootDir": "./src"` + `"include": ["./src"]` explizit setzen.
**Quelle:** TS-6.0-Release-Notes

### 5. `noUncheckedSideEffectImports` default `true`
**Symptom:** Fehler bei Side-Effect-only-Imports (`import './styles.css'`), wenn der Pfad nicht aufloesbar ist — faengt Tippfehler.
**Ursache:** Neuer Default.
**Versionen:** ab TS 6.0.
**FIX:** Import-Pfad verifizieren; nur bewusst `false`, wenn ein Plugin/Bundler solche Imports legitim macht.
**Quelle:** TS-6.0-Release-Notes

### 6. CLI-Datei-Argumente + vorhandene `tsconfig.json` = Fehler
**Symptom:** `error TS5112: tsconfig.json is present but will not be loaded if files are specified on commandline`.
**Ursache:** 6.0 verweigert das stille Ignorieren der tsconfig.
**Versionen:** ab TS 6.0.
**FIX:** Datei-Argumente weglassen (nur `tsc`) ODER `tsc --ignoreConfig foo.ts`.
**Quelle:** TS-6.0-Release-Notes

---

## B. TypeScript 6.0 — Deprecations (noch nutzbar mit `"ignoreDeprecations": "6.0"`, in TS 7.0 ENTFERNT)

> `ignoreDeprecations: "6.0"` unterdrueckt NUR diese Deprecation-Fehler, NICHT die Default-Aenderungen aus Abschnitt A. Es wirkt nur in 6.0 — in 7.0 sind die Optionen weg.

### 7. `moduleResolution: node`/`node10` und `classic` deprecated/entfernt   ⭐ HAEUFIG
**Symptom:** Stiller Wechsel auf `bundler` wenn `moduleResolution` fehlte; `--moduleResolution node`/`node10` wirft ab 6.0 Fehler; `classic` ganz entfernt; falsche Aufloesung moderner `exports`/`imports`-Felder.
**Ursache:** Aufraeumen veralteter Resolution-Strategien. `node10` bildet Node 10 ab und ignoriert alle seither neuen Regeln.
**Versionen:** deprecated/Fehler ab TS 6.0; Entfernung in TS 7.0. (Issue #62200 CLOSED/COMPLETED 2025-08-27 — Deprecation umgesetzt.)
**FIX:** Explizit `"moduleResolution": "nodenext"` (direkt auf Node) oder `"bundler"` (Bun/Bundler) setzen.
**Quelle:** github.com/microsoft/TypeScript/issues/62200 · TS-6.0-Release-Notes

### 8. `baseUrl` deprecated (nicht mehr Modul-Lookup-Root)
**Symptom:** Bare-Imports, die sich auf `baseUrl` verliessen, werden nicht mehr aufgeloest.
**Ursache:** `baseUrl` ist in 6.0 deprecated und dient nicht mehr als Lookup-Root.
**Versionen:** deprecated ab TS 6.0.
**FIX:** Prefix in `paths` verschieben — `"paths": { "@app/*": ["./src/app/*"] }`. Aliase bleiben, nur ohne `baseUrl`.
**Quelle:** devbolt.dev/blog/typescript-6-migration-guide

### 9. `target: es5` deprecated (Minimum jetzt ES2015)
**Symptom:** `error TS5111: Option 'target' must be 'es2015' or newer`.
**Ursache:** Niedrigstes Target jetzt ES2015.
**Versionen:** deprecated ab TS 6.0.
**FIX:** Auf `es2020`+ migrieren. Fuer echten ES5-Output externen Bundler/Compiler nutzen (Feature bleibt, anderer Weg). `--downlevelIteration` faellt damit ebenfalls weg.
**Quelle:** TS-6.0-Release-Notes

### 10. `outFile` deprecated
**Symptom:** `error TS5111: outFile is not supported`.
**Ursache:** Single-File-Concat-Emit deprecated.
**Versionen:** ab TS 6.0.
**FIX:** Echten Bundler (esbuild/Rollup/Vite) nutzen — schneller, mehr Features. (`out` ist ohnehin entfernt → `outFile`/Bundler.)
**Quelle:** TS-6.0-Release-Notes

### 11. `esModuleInterop`/`allowSyntheticDefaultImports`/`alwaysStrict` muessen `true` sein
**Symptom:** Mit `esModuleInterop:false` bricht `import express from "express"`; reservierte Woerter (`await`, `static`) als Identifier werfen Syntaxfehler (alwaysStrict).
**Ursache:** Diese drei `false`-Werte sind in 6.0 deprecated.
**Versionen:** ab TS 6.0.
**FIX:** Auf `true` lassen; Default-Imports nutzen; reservierte Identifier umbenennen.
**Quelle:** TS-6.0-Release-Notes

### 12. `module Foo {}`-Keyword deprecated → `namespace Foo {}`
**Symptom:** `error TS1208`. (Achtung: `declare module "x"` fuer Ambient-Module bleibt erlaubt.)
**Ursache:** `module`-Keyword fuer interne Namespaces deprecated.
**Versionen:** ab TS 6.0.
**FIX:** `module Foo {}` → `namespace Foo {}`.
**Quelle:** TS-6.0-Release-Notes

### 13. Import-`assert` deprecated → `with`
**Symptom:** `error TS1452`; zur Laufzeit (Node 22+) `SyntaxError: Unexpected identifier 'assert'`.
**Ursache:** Import-Assertions (`assert {type:'json'}`) durch Import-Attributes (`with {type:'json'}`) ersetzt.
**Versionen:** `assert` Node 17.1-21, entfernt ab Node 22; TS deprecated ab 6.0.
**FIX:** `import data from "./data.json" with { type: "json" };` (braucht `module: esnext`/`nodenext`). Versionsunabhaengig: `createRequire` oder `fs.readFileSync`+`JSON.parse`.
**Quelle:** v8.dev/features/import-attributes · github.com/microsoft/TypeScript/issues/51783

### 14. `out`, `suppressImplicitAnyIndexErrors`, `/// <amd-module>`, `module: amd/umd/system/none`, `/// <reference no-default-lib>` deprecated/entfernt
**Symptom:** Build bricht ab wegen entfernter/deprecateter Optionen.
**Ursache:** 6.0-Aufraeumen der ueber 5.x deprecateten Optionen.
**Versionen:** ab TS 6.0.
**FIX:** `out`→`outFile`/Bundler; Index-Fehler explizit behandeln (`noUncheckedIndexedAccess`); Legacy-Modulformate via Bundler aus ESM; `no-default-lib` → `noLib`/`libReplacement`.
**Quelle:** TS-6.0-Release-Notes

> ## C-Vorschau: TS 7.0 "Corsa" (Go-Port, `tsgo`) — was 6.0 ankuendigt
> Reine Speed (10x), Semantik gleich (6.0-Defaults matchen schon 7.0-Verhalten). ABER: die **interne TS-API (Strada) entfaellt** → Plugins, Linter, Formatter, IDE-Extensions, die sie nutzen, brechen bis zur Anpassung. JSDoc-Typchecking neu (JS-Projekte sehen mehr Fehler). `.d.ts`-Emit aus `.js` bei Fehlern/`@ts-ignore` weicht ab. — github.com/microsoft/typescript-go/blob/main/CHANGES.md

---

## C. ESM vs CommonJS (der groesste Schmerzbereich)

### 15. `ERR_REQUIRE_ASYNC_MODULE` — `require(esm)` scheitert bei Top-Level-await   ⭐ HAEUFIG
**Symptom:** `Error [ERR_REQUIRE_ASYNC_MODULE]: require() of an ES Module that uses top-level await is not supported`. Tritt kaskadierend auf, wenn ESM-Deps mit TLA transitiv ueber CJS required werden (jsdom/vitest/prettier/firebase-tools).
**Ursache:** Synchrones `require()` kann eine asynchrone TLA-Auswertung nicht "pausieren" — strukturell unmoeglich. Node 24 erlaubt `require(esm)` NUR fuer komplett synchrone ESM-Graphen.
**Versionen:** `require(esm)` stabil ab Node 20.19/22.12, unflagged ab 23/24. TLA-Ausschluss ist **per Design** (bleibt).
**FIX:** Auf `const mod = await import('paket')` umstellen (async-Kontext). Verursacher finden mit `node --experimental-print-required-tla`. Eigenes TLA in eine async-Init-Funktion verschieben.
**Quelle:** github.com/nodejs/node/issues/52697 (CLOSED/COMPLETED 2026-01-12, Feature stabil) · joyeecheung.github.io/blog/2025/12/30/require-esm-in-node-js-from-experiment-to-stability/

### 16. ESM→CJS→ESM-Zyklen mit `require(esm)` verboten
**Symptom:** `require(esm)` schlaegt fehl, wenn der Graph einen Zyklus ESM→CJS→ESM enthaelt.
**Ursache:** Solche Zyklen sind synchron nicht aufloesbar.
**Versionen:** per Design (Issue #52145 CLOSED/COMPLETED 2024-04-08 — Einschraenkung absichtlich eingebaut).
**FIX:** Zyklus auftrennen — gemeinsamen Code in ein drittes, zyklusfreies Modul auslagern (funktionserhaltend) oder an der Zyklus-Kante dynamisches `import()` statt statischem Import.
**Quelle:** github.com/nodejs/node/issues/52145

### 17. `ERR_MODULE_NOT_FOUND` — fehlende `.js`-Endung in ESM-Imports   ⭐ HAEUFIG
**Symptom:** Kompilierter ESM-Output wirft `ERR_MODULE_NOT_FOUND`, weil `import './foo'` keine Endung hat; Node-ESM verlangt `import './foo.js'`. Zur Compile-Zeit `TS2835`.
**Ursache:** Bei `moduleResolution: NodeNext`/`Node16` haelt sich TS strikt an den Node-Spec: relative ESM-Imports brauchen explizite Endungen. TS rewritet sie historisch NICHT.
**Versionen:** NodeNext/Node16 seit jeher; `rewriteRelativeImportExtensions` ab TS 5.7 (in 6.0.2 vorhanden). (Issue #60926 OPEN — die Fehlermeldung schlaegt teils falsch `.js` vor.)
**FIX (3 funktionserhaltende Optionen):** (a) In TS-Quelle direkt `.js` schreiben (`import './foo.js'`, auch wenn Datei `foo.ts` heisst — Node loest korrekt auf). (b) `"rewriteRelativeImportExtensions": true` (TS 5.7+) → in Quelle `.ts` schreiben, TS rewritet beim Compile zu `.js`. (c) `allowImportingTsExtensions: true` + `.ts`-Endungen NUR mit `noEmit` oder Bun-Runtime.
**Quelle:** totaltypescript.com/relative-import-paths-need-explicit-file-extensions-in-ecmascript-imports · github.com/microsoft/TypeScript/issues/60926

### 18. `__dirname`/`__filename`/`require` nicht definiert in ESM   ⭐ HAEUFIG
**Symptom:** `ReferenceError: __dirname is not defined in ES module scope`.
**Ursache:** Das sind CommonJS-Globals, in ESM nicht vorhanden.
**Versionen:** `import.meta.dirname`/`filename` ab Node 20.11/21.2.
**FIX:** Modern (Node 20.11+): `const __dirname = import.meta.dirname;`. Universell: `import { fileURLToPath } from 'node:url'; const __filename = fileURLToPath(import.meta.url); const __dirname = path.dirname(__filename);`. Fuer `require` in ESM: `import { createRequire } from 'node:module'; const require = createRequire(import.meta.url);`.
**Quelle:** nodejs.org/api/esm.html · sonarsource.com/blog/dirname-node-js-es-modules

### 19. Dual-Package-Hazard — `instanceof` schlaegt fehl, doppelter Modul-State   ⭐ HAEUFIG
**Symptom:** `instanceof` gibt `false`, obwohl es `true` sein sollte; Singleton/Modul-State existiert doppelt; stille Logikfehler. **Verschaerft in Node 24**, weil `require` jetzt die `import`-Condition treffen kann.
**Ursache:** CJS- (`Module._cache` keyed by Pfad) und ESM-Loader (keyed by URL) haben getrennte Caches. Conditional `exports` zeigen `import` und `require` auf verschiedene Dateien → zwei Eval-Instanzen.
**Versionen:** strukturell, per Design der Dual-Package-Architektur.
**FIX:** Diagnose: Modul-Pfad von beiden Seiten loggen (unterschiedlich ⇒ zwei Instanzen). Fix A: `import`+`require` auf denselben Code zeigen lassen (ESM-Wrapper re-exportiert CJS). Fix B: Modul stateless halten (nur pure Funktionen/Konstanten) → Hazard irrelevant.
**Quelle:** github.com/GeoffreyBooth/dual-package-hazard · thenodebook.com/modules/cjs-esm-interop

### 20. `package.json` `"type":"module"` vs `exports`/`main` Fehlkonfiguration
**Symptom:** `ERR_PACKAGE_PATH_NOT_EXPORTED` — ein vorher importierbarer interner Pfad (`paket/lib/x`) geht ploetzlich nicht mehr.
**Ursache:** Sobald `exports` gesetzt ist, kapselt es das Paket — nur explizit gelistete Pfade sind importierbar (`main` wird ignoriert).
**Versionen:** seit Node 12.7+, unveraendert.
**FIX:** Nur offiziell exportierte Pfade importieren. Beim eigenen Paket `exports` vollstaendig anlegen: `{".":{"import":"./dist/index.js","require":"./dist/index.cjs"},"./feature":"./dist/feature.js"}`. `.cjs`=immer CJS, `.mjs`=immer ESM (unabhaengig von `"type"`).
**Quelle:** github.com/axios/axios/issues/5072 · github.com/date-fns/date-fns/issues/3612

### 21. `__esModule`/Default-Export-Interop: "x is not a function"
**Symptom:** `import x from 'cjs-pkg'; x()` → `TypeError: x is not a function` bzw. `(0, import_..default) is not a function`.
**Ursache:** CJS-Paket hat `module.exports = fn` (kein echter ESM-default). Ohne Interop verpackt TS den default falsch.
**Versionen:** klassisches TS-Verhalten.
**FIX:** `"esModuleInterop": true` (setzt implizit `allowSyntheticDefaultImports`; in TS 6.0 ohnehin Pflicht, siehe #11). Alternativ `import * as x from 'cjs-pkg'` oder `import x = require('cjs-pkg')`.
**Quelle:** iifx.dev/en/articles/326420424 · github.com/evanw/esbuild/issues/2480

### 22. JSON-Import: `assert {type:'json'}` → `with {type:'json'}`
**Symptom:** `SyntaxError: Unexpected identifier 'assert'` beim JSON-Import nach Node-22-Upgrade; ohne Attribut `ERR_IMPORT_ASSERTION_TYPE_MISSING`.
**Ursache:** Import-Assertions durch Import-Attributes ersetzt; `assert` ab Node 22 entfernt.
**Versionen:** `assert` Node 17.1-21; `with` ab Node 22+.
**FIX:** `import data from './data.json' with { type: 'json' };` (braucht `module: esnext`/`nodenext`). Versionsunabhaengig: `createRequire`/`fs.readFileSync`+`JSON.parse`.
**Quelle:** v8.dev/features/import-attributes · dev-solve.com/posts/4b39013

### 23. ESM-only-Pakete in CJS-Projekt (chalk 5, node-fetch 3, nanoid 4, execa, p-limit, got)
**Symptom:** `require('chalk')` etc. → frueher hartes `ERR_REQUIRE_ESM`. Ab Node 24 oft OK (require(esm)), AUSSER das Paket nutzt Top-Level-await (dann #15).
**Ursache:** Diese Pakete sind ab bestimmter Major-Version ESM-only.
**Versionen:** chalk ESM-only ab v5, node-fetch ab v3, nanoid ab v4; require(esm)-Erleichterung ab Node 20.19+.
**FIX:** (a) Node ≥20.19/22.12/24 + synchrones Paket → `require()` geht jetzt. (b) Sonst `await import('chalk')`. (c) Bewusst auf letzter CJS-Major bleiben (chalk 4, node-fetch 2) — funktionserhaltend, wenn die API genuegt.
**Quelle:** fullstacknotes.dev/blog/2026/2026-01/2026-01-24-nodejs-require-esm/ · codingdunia.com/blog/nodejs-20-to-24-migration/

---

## D. TypeScript — Typ-Fallen (Soundness, `any`-Leaks, tsconfig-Luecken)

### 24. `JSON.parse` liefert stilles `any`   ⭐ HAEUFIG
**Symptom:** `const data = JSON.parse(str)` ist `any` — jeder Zugriff danach ungeprueft, Tippfehler in Property-Namen werden nicht erkannt.
**Ursache:** Rueckgabetyp ist `any`. `noImplicitAny` verhindert nur *implizites* any, nicht von APIs geliefertes.
**Versionen:** per Design, alle Versionen.
**FIX:** Sofort in `unknown` casten: `const data: unknown = JSON.parse(str)`, dann mit Schema-Validierung (zod) oder Type-Guard pruefen. NICHT direkt nutzen.
**Quelle:** typescript-eslint.io/blog/avoiding-anys/

### 25. `catch (e)` ist `unknown` (nicht mehr `any`) — aber `.catch(cb)` nicht
**Symptom:** `e.message` greift ungeprueft und crasht, wenn der geworfene Wert kein `Error` ist; bei Promise-`.catch(cb)` gibt es KEIN Compiler-Aequivalent.
**Ursache:** `useUnknownInCatchVariables` (in `strict`, also TS 6.0 default) macht `catch (e)` zu `unknown`. Promise-`.catch`-Callbacks bleiben `any`.
**Versionen:** `unknown` ab 4.4 mit Flag; default ab 6.0.
**FIX:** `if (e instanceof Error) …`-Guard. Fuer `.catch(cb)`: ESLint-Regel `@typescript-eslint/use-unknown-in-catch-callback-variable`.
**Quelle:** typescriptlang.org/tsconfig/useUnknownInCatchVariables.html

### 26. `any`-Returns vergiften nachgelagerten Code (ansteckend)
**Symptom:** Eine Funktion (oft Drittanbieter ohne/mit schlechten `@types`) gibt `any` zurueck → zugewiesene Variablen werden `any`, Operationen bleiben `any`. Vergiftung breitet sich still aus.
**Ursache:** `any` ist ansteckend; `noImplicitAny` greift nicht, weil das `any` explizit vom Typ kommt.
**Versionen:** per Design.
**FIX:** Rueckgabe explizit typisieren oder in `unknown` casten + validieren. `@typescript-eslint/no-unsafe-*`-Regeln aktivieren (fangen diese Leaks). Merksatz: `strict` allein reicht NICHT — nur Defense-in-Depth mit typescript-eslint.
**Quelle:** typescript-eslint.io/blog/avoiding-anys/

### 27. `noUncheckedIndexedAccess` fehlt — Index-Zugriff laeuft ins `undefined`   ⭐ HAEUFIG
**Symptom:** `arr[i]` / `obj[key]` ist als Wert typisiert (nicht `| undefined`), Code type-checkt, crasht aber zur Laufzeit mit "Cannot read property X of undefined".
**Ursache:** Ohne das Flag nimmt TS an, jeder indizierte Zugriff liefert den deklarierten Wert. **NICHT in `strict` enthalten** (auch in TS 6.0 nicht).
**Versionen:** ab 4.1, opt-in.
**FIX:** `"noUncheckedIndexedAccess": true` → `arr[i]` wird `T | undefined`, erzwingt `?.`/Guard. (Das Beispielprojekt `mcp-code-search` hat es bereits an.)
**Quelle:** typescriptlang.org/tsconfig/noUncheckedIndexedAccess.html

### 28. `exactOptionalPropertyTypes` fehlt — `undefined` vs. fehlender Key verwischt
**Symptom:** `{ x?: number }` akzeptiert auch `{ x: undefined }`; Bug bei JSON-Serialisierung, Object-Spread, DB-Updates wo "Key fehlt" ≠ "Key ist undefined".
**Ursache:** Ohne Flag behandelt TS optionale Props und explizites `undefined` gleich. Ebenfalls **NICHT in `strict`**.
**Versionen:** ab 4.4, opt-in.
**FIX:** `"exactOptionalPropertyTypes": true`. Zuletzt aktivieren (braucht Verstaendnis des Datenmodells). Hinweis: Issue #63232 (Warnung wenn `strict` undefiniert) ist gefixt (2026-03-11) → `strict` explizit setzen.
**Quelle:** dev.to/jtorchia/typescript-strict-mode-the-6-tsconfig-options-that-actually-matter-in-production-and-when-to-enable-them-446d

### 29. `as`-Assertion umgeht Checks (inkl. Excess-Property)
**Symptom:** `{...} as User` akzeptiert falsche/fehlende Props ohne Fehler.
**Ursache:** `as` ist eine Behauptung, kein Cast — kein Runtime-Check, keine Excess-Property-Pruefung.
**Versionen:** per Design (`satisfies` ab 4.9).
**FIX:** `satisfies` statt `as` fuer Objekt-Literale → behaelt engen Inferenz-Typ UND erzwingt Property-Matching.
**Quelle:** talent500.com/blog/typescript-satisfies-keyword-structural-typing/

### 30. `as unknown as T` (Doppel-Assertion) → garantierter Runtime-Crash
**Symptom:** `dog as unknown as Cat` kompiliert, crasht zur Laufzeit.
**Ursache:** Umgeht jede Typsicherheit (kein Runtime-Check).
**Versionen:** per Design.
**FIX:** Extrem selten verwenden — signalisiert Design-Problem. Stattdessen Type-Guard/Validierung.
**Quelle:** betterstack.com/community/guides/scaling-nodejs/type-assertions/

### 31. Non-null `!` luegt — Crash wenn doch null/undefined
**Symptom:** `value!.foo` type-checkt, aber `value` ist zur Laufzeit `null`/`undefined` → "Cannot read property of undefined".
**Ursache:** `!` wird im Emit entfernt, kein Runtime-Schutz.
**Versionen:** per Design.
**FIX:** Echten Guard (`if (value) …`) oder `?.` statt `!`. ESLint `no-non-null-assertion`.
**Quelle:** sentry.io/answers/how-to-fix-the-forbidden-non-null-assertion-in-typescript-and-react/

### 32. Excess-Property-Check umgehbar via Zwischenvariable
**Symptom:** `const u: User = {name, age, email}` direkt → Fehler bei `email`. Aber `const temp = {...}; const u: User = temp;` → durchgewunken, Tippfehler schluepfen durch.
**Ursache:** Excess-Property-Check greift NUR bei direkten Objekt-Literalen, nicht bei Variablen-Zuweisung (strukturelles Typing). Per Design (Issue #48852 CLOSED/COMPLETED, working as intended).
**Versionen:** alle Versionen.
**FIX:** `satisfies` am Literal (`const temp = {...} satisfies User`) oder Literal direkt zuweisen.
**Quelle:** github.com/microsoft/TypeScript/issues/48852

### 33. `satisfies` nicht direkt mit `as const` verkettbar
**Symptom:** `value satisfies T as const` wirft "'const' assertions can only be applied to references to enum members, or string, number, boolean, array, or object literals."
**Ursache:** Grammatik-/Parser-Einschraenkung. Issue #51173 CLOSED/COMPLETED 2024-02-28 — als Design-Grenze geschlossen.
**Versionen:** Workaround bleibt gueltig.
**FIX:** Trennen: `const x = {...} as const; x satisfies T;` oder Helper-Funktion mit `const`-Type-Parameter.
**Quelle:** github.com/microsoft/TypeScript/issues/51173

### 34. TS2589 "Type instantiation is excessively deep and possibly infinite"   ⭐ HAEUFIG
**Symptom:** Compiler bricht bei tief verschachtelten/rekursiven Typen oder grossen Unions ab; IntelliSense extrem langsam.
**Ursache:** Rekursionslimit (50 Ebenen) bzw. `instantiationCount` (5e6) erreicht; grosse Unions × Conditional/Intersection-Types expandieren exponentiell.
**Versionen:** TS 4.1+ bis aktuell — Langlaeufer (#34933 **OPEN**; die TS-5-Regression #53514 ist gefixt 2025-08-27).
**FIX:** Typen abflachen, Unions reduzieren, Conditional-Types vereinfachen, rekursive Generics begrenzen; bei Libraries (Kysely, Reselect) deren dokumentierte Recipes nutzen. NICHT die TS-Source patchen.
**Quelle:** github.com/microsoft/TypeScript/issues/34933

### 35. Generic-Inferenz kollabiert zu `unknown`/`{}`
**Symptom:** Generischer Aufruf liefert `unknown`/`{}` statt erwartetem Typ; Folgecode wird ungeprueft.
**Ursache:** TS kann den Typ-Parameter nicht inferieren und faellt auf Constraint/`unknown` zurueck.
**Versionen:** teils per Design, teils Bugs (#43371 gefixt 2022-09-26; #36124 constraint-loss gefixt 2024-02-25).
**FIX:** Typ-Argument explizit angeben (`fn<MyType>(…)`) statt auf Inferenz zu hoffen; Constraints praezisieren.
**Quelle:** github.com/microsoft/TypeScript/issues/43371

### 36. Mapped-Type prueft Constraint nicht bei Funktions-Typ-Parametern (Unsoundness)
**Symptom:** Ein Typ-Parameter aus einer Funktion, durch einen Mapped-Type geschickt, wird NICHT gegen den Constraint geprueft → `null`/falscher Typ schluepft durch.
**Ursache:** TS prueft den Parameter in diesem Kontext nicht.
**Versionen:** bekannter Bug — #49302 **OPEN**; verwandte #47191/#47385 gefixt 2022-01-15.
**FIX:** Bei komplexen Funktions-Inferenz-Mustern Typen explizit annotieren statt auf Inferenz zu vertrauen.
**Quelle:** github.com/microsoft/TypeScript/issues/49302

### 37. `const enum` import → Runtime-Crash mit `verbatimModuleSyntax`/`isolatedModules`
**Symptom:** Import eines `const enum` kompiliert, ist zur Laufzeit `undefined`/Crash — der const enum existiert nicht im emittierten JS.
**Ursache:** `verbatimModuleSyntax` laesst Imports verbatim stehen; `const enum` hat keine Runtime-Repraesentation ueber Modulgrenzen. Single-File-Transpiler (esbuild/swc/babel/Bun) koennen const enums nicht inlinen → `isolatedModules` verbietet sie. (#52669/#48040 gefixt — Fehler wird jetzt gemeldet; Verhalten bleibt.)
**Versionen:** ab TS 5.0.
**FIX:** Exportiertes/extern konsumiertes `const enum` zu normalem `enum` machen (gleiche Werte, jetzt mit Runtime-Objekt). `isolatedModules: true` setzen, um es zur Compile-Zeit zu fangen.
**Quelle:** github.com/microsoft/TypeScript/issues/52669 · typescriptlang.org/tsconfig/isolatedModules.html

### 38. type-only Symbol ohne `import type` bricht beim Bundler
**Symptom:** Mit `verbatimModuleSyntax` bleibt ein nur-als-Typ genutzter Import im Output stehen → toter Import / Side-Effect / Bundler-Fehler.
**Ursache:** Ohne `type`-Modifier wird der Import nicht entfernt. (#55741 NOT_PLANNED: Enum nur-als-Typ zeigt faelschlich KEINEN Fehler — wachsam sein.)
**Versionen:** ab TS 5.0.
**FIX:** `import type { X }` / `export type` konsequent nutzen.
**Quelle:** typescriptlang.org/tsconfig/verbatimModuleSyntax.html · github.com/microsoft/TypeScript/issues/55741

### 39. `skipLibCheck: true` versteckt echte Fehler in `.d.ts`
**Symptom:** Compiler ist gruen, aber fehlerhafte/konfligierende Typen in Dependencies (oder eigenen ambient declarations) bleiben unentdeckt — Fehler erst zur Laufzeit.
**Ursache:** `skipLibCheck` ueberspringt Typpruefung ALLER `.d.ts` — auch eigener. (#41883 gefixt: wurde ignoriert wenn `types` auf eine `.ts` zeigt.)
**Versionen:** per Design.
**FIX:** `skipLibCheck` nur gezielt fuer Performance; bei Verdacht temporaer `false` setzen und voll typpruefen.
**Quelle:** dd.engineering/blog/typescript-the-skiplibcheck-option-explained · github.com/microsoft/TypeScript/issues/41883

---

## E. Node.js — Async / Promises / Event-Loop / Runtime

### 40. Unhandled Promise Rejection crasht den Prozess (Default seit Node 15)   ⭐ HAEUFIG
**Symptom:** Prozess beendet sich mit exit 1, oft ohne klare Meldung; Process-Manager melden Failure.
**Ursache:** Eine Promise rejectet ohne `await`/`.catch`. Seit Node 15 ist `--unhandled-rejections=throw` Default.
**Versionen:** ab Node 15, gilt fuer 16-24. Per Design.
**FIX:** In jeder `async`-Funktion `try/catch` um awaited Calls; Floating Promises bewusst mit `.catch()`. `process.on('unhandledRejection', …)` NUR als Sicherheitsnetz zum **Loggen + geordneten Shutdown**, NIEMALS als stiller Schlucker (sonst laeuft der Prozess korrupt weiter). Praevention: ESLint `@typescript-eslint/no-floating-promises` + `no-misused-promises`.
**Quelle:** maximorlov.com/node-js-15-is-out-what-does-it-mean-for-you/ · nodejs.org/api/process.html

### 41. `ERR_UNHANDLED_REJECTION` False-Positive trotz `.catch()` (Microtask-Timing)
**Symptom:** App crasht mit `ERR_UNHANDLED_REJECTION`, obwohl ein `.catch()`/try-catch existiert; `processTicksAndRejections` im Stack.
**Ursache:** Der `.catch()` wird erst in einem SPAETEREN Microtask attached (z.B. nach `setTimeout` oder `await` in separatem Tick). Node zaehlt die Rejection schon vorher als "unhandled".
**Versionen:** Node 14/16+, weiter **OPEN** (#43326); Microtask-Semantik per Design.
**FIX:** Rejection-Handler synchron im selben Microtask attachen — `.catch()` direkt an die Promise haengen, NICHT nach `await`/Timeout. IIFE: `(async()=>{…})().catch(e=>…)` sofort dranhaengen.
**Quelle:** github.com/nodejs/node/issues/43326 · jakearchibald.com/2023/unhandled-rejections/

### 42. `uncaughtException` — Prozess MUSS beendet werden
**Symptom:** Versuchung, nach `process.on('uncaughtException')` einfach weiterzulaufen.
**Ursache:** Eine uncaught synchrone Exception laesst die App in undefiniertem State (halb ausgefuehrte Operationen, korrupte Variablen). Weiterlaufen = "Stecker beim Update ziehen".
**Versionen:** alle. Per Design.
**FIX:** Im Handler NUR synchrones Cleanup (FDs/Handles schliessen), dann **geordnet beenden** (`process.exitCode=1`). Unterschied zu `unhandledRejection`: Letzteres ist async und theoretisch spaeter behandelbar — `uncaughtException` nicht.
**Quelle:** hemantasundaray.com/blog/nodejs-uncaughtexcption-unhandledrejection · nodejs.org/api/process.html

### 43. `forEach` mit async-Callback wartet NICHT   ⭐ HAEUFIG
**Symptom:** Code nach der Schleife laeuft, bevor die async-Operationen fertig sind; Race Conditions, unvollstaendige Daten, Reihenfolge nicht eingehalten.
**Ursache:** `Array.prototype.forEach` ist synchron, gibt keine Promise zurueck und ignoriert den Callback-Rueckgabewert — alle Iterationen feuern parallel ohne `await`.
**Versionen:** alle. Per Design (Spec).
**FIX:** Sequenziell → `for...of` + `await`. Parallel-mit-Warten → `await Promise.all(arr.map(async x => …))`. `forEach` nie mit async.
**Quelle:** coreycleary.me/why-does-async-await-in-a-foreach-not-actually-await

### 44. Vergessenes `await` → Promise statt Wert
**Symptom:** Variable enthaelt `Promise { <pending> }` statt Ergebnis; Bedingungen immer truthy (eine Promise ist truthy); spaetere Rejection wird unhandled (#40).
**Ursache:** `await` vor einem Promise-liefernden Aufruf vergessen.
**Versionen:** alle.
**FIX:** `await` ergaenzen. Praevention: `@typescript-eslint/no-floating-promises` + `no-misused-promises` zur Compile-Zeit.
**Quelle:** jakearchibald.com/2023/unhandled-rejections/

### 45. `Promise.all` — ein Reject killt alle, Rest wird unhandled
**Symptom:** Beim ersten Fehler rejectet `Promise.all` sofort; erfolgreiche Ergebnisse gehen verloren; die noch laufenden, spaeter rejectenden Promises erzeugen unhandled rejections (#40).
**Ursache:** `Promise.all` ist fail-fast und cancelt die uebrigen Promises nicht.
**Versionen:** alle (`allSettled` ab Node 12.9/ES2020).
**FIX:** Wenn jedes Einzelergebnis zaehlt → `Promise.allSettled` (wartet auf alle, `{status, value|reason}`, nichts unhandled). `Promise.all` nur, wenn ein Fehler wirklich alles abbrechen soll und Inputs eigene `.catch` haben.
**Quelle:** coreycleary.me/better-handling-of-rejections-using-promise-allsettled

### 46. Top-Level-await blockiert Modul-Graph / Deadlock bei zyklischen Imports
**Symptom:** Importierende Module starten ihre Auswertung erst nach Abschluss des TLA (Startverzoegerung); bei gegenseitig awaitenden Modulen Deadlock.
**Ursache:** Ein Modul mit Top-Level-`await` verhaelt sich wie eine grosse async-Funktion; alle Importeure warten.
**Versionen:** TLA ab Node 14.8 (nur ESM). Per Design.
**FIX:** TLA sparsam und nur an Modul-Wurzeln; Zyklen zwischen TLA-Modulen aufbrechen (gemeinsame Abhaengigkeit auslagern, dynamisches `import()` an der Kante).
**Quelle:** v8.dev/features/top-level-await

### 47. Verlorener async Stack bei `return promise` ohne `await`
**Symptom:** Stack-Trace endet abrupt nach der ersten `await`-Grenze; aufrufende Frames fehlen.
**Ursache:** Zero-Cost Async Stack Traces (V8 ≥ 7.3) rekonstruieren Frames NUR an `await`-Stellen. Bei `return throwSomething()` (Promise direkt zurueck, ohne await) fehlt der Frame.
**Versionen:** V8 ≥ 7.3 / Node ≥ 12, weiter in Node 24. Per Design.
**FIX:** `return await throwSomething()` statt `return throwSomething()` — voller Stack, gleiches Laufzeitverhalten.
**Quelle:** v8.dev/docs/stack-trace-api · draconianoverlord.com/2025/04/17/fixing-async-stack-traces.html

### 48. Timer-Leak: `setTimeout` + AbortController ohne `clearTimeout`
**Symptom:** Haengende Timer halten den Event-Loop wach (Prozess beendet nicht); bei vielen Aufrufen Memory-/Handle-Anstieg.
**Ursache:** Klassisches Timeout-Pattern braucht `clearTimeout` im Erfolgs- UND Fehlerpfad; Listener auf dem AbortSignal werden nicht entfernt.
**Versionen:** `AbortSignal.timeout` ab Node 17.3, `AbortSignal.any` ab 20.3 (in 24.15 vorhanden).
**FIX:** `AbortSignal.timeout(ms)` nutzen (raeumt selbst auf). Listener mit `{ signal }`/`{ once: true }` → automatische Entfernung. Eigenes Keep-Alive-`setTimeout` mit `.unref()`.
**Quelle:** blog.appsignal.com/2025/02/12/managing-asynchronous-operations-in-nodejs-with-abortcontroller.html

### 49. `Promise.race` cancelt den Verlierer NICHT
**Symptom:** Nach `Promise.race([task, timeout])` laeuft die langsame Operation im Hintergrund weiter — Ressourcen/Requests nicht gestoppt, evtl. spaetere unhandled rejection.
**Ursache:** `Promise.race` settled nur das Ergebnis, unterbricht die anderen nicht.
**Versionen:** alle. Per Design.
**FIX:** AbortSignal an die eigentliche Operation durchreichen und beim Timeout `abort()` aufrufen. Verlierer-Promise mit `.catch()` absichern.
**Quelle:** blog.appsignal.com/2025/02/12/managing-asynchronous-operations-in-nodejs-with-abortcontroller.html

### 50. EventEmitter `'error'`-Event ohne Listener crasht den Prozess
**Symptom:** Prozess stuerzt mit uncaught exception ab, sobald ein EventEmitter `'error'` emittiert und kein Listener registriert ist.
**Ursache:** `'error'` ist ein Sonderfall — ohne Listener wirft Node den Fehler.
**Versionen:** alle. Per Design.
**FIX:** IMMER `emitter.on('error', handler)` registrieren (loggen + geordnet behandeln, nicht schlucken). `events.errorMonitor` nur zum Monitoring (verhindert den Crash NICHT). `node:domain` ist deprecated — nicht verwenden.
**Quelle:** nodejs.org/api/events.html

### 51. `MaxListenersExceededWarning` (Memory-Leak-Hinweis)
**Symptom:** `MaxListenersExceededWarning: Possible EventEmitter memory leak detected. 11 X listeners added.`
**Ursache:** Standard-Limit 10 Listener pro Emitter/Event; meist Listener wiederholt hinzugefuegt, nie entfernt (Schleife/Request-Handler).
**Versionen:** alle. Per Design (Warnung, Prozess laeuft weiter).
**FIX:** Root Cause mit `--trace-warnings` finden; Listener mit `removeListener`/`off`/`{ once: true }` aufraeumen. `setMaxListeners(n)` NUR erhoehen, wenn die Zahl legitim ist (sonst maskiert es das Leak).
**Quelle:** stefanjudis.com/today-i-learned/nodejs-sends-warnings-when-you-add-too-many-listeners-to-an-event-emitter/

### 52. `process.exit()` schneidet pending async I/O ab (Logs/Writes verloren)
**Symptom:** Letzte `console.log`/Datei-Writes fehlen; Logs abgeschnitten; Daten nicht geschrieben.
**Ursache:** `process.exit()` beendet sofort, auch wenn Writes zu stdout/Files noch ueber mehrere Ticks pending sind.
**Versionen:** alle. Per Design.
**FIX:** Statt `process.exit(code)` lieber `process.exitCode = code` setzen und keine neue Arbeit einplanen — Node beendet sich, sobald der Event-Loop leer ist (pending I/O wird vorher geflusht).
**Quelle:** kostasbariotis.com/why-you-should-not-use-process-exit/

---

## F. npm / Dependencies

### 53. `npm error ERESOLVE unable to resolve dependency tree`   ⭐ HAEUFIG
**Symptom:** Install bricht ab; Paket A braucht `dep@1`, Paket B `dep@2`.
**Ursache:** Ab **npm 7** werden peer-dependencies STRIKT durchgesetzt (npm 6 nur Warnung).
**Versionen:** npm 7+ (per Design, gilt in npm 11).
**FIX (best→notfalls):** (1) Konfliktpakete auf kompatible Versionen bringen. (2) `npm install --legacy-peer-deps` — ignoriert NUR peer-dep-Konflikte, laesst andere Checks intakt. (3) NUR letztes Mittel `--force`.
**Quelle:** blog.openreplay.com/fix-npm-err-eresolve-dependency/

### 54. `--force` zerstoert den Dependency-Tree
**Symptom:** Install laeuft durch, App crasht zur Laufzeit (z.B. "Invalid hook call" bei doppeltem React).
**Ursache:** `--force` ignoriert fast alles — peer-deps, invalide Trees, Cache → installiert z.B. zwei Versionen parallel.
**Versionen:** per Design, npm 7+.
**FIX:** Immer `--legacy-peer-deps` bevorzugen (ignoriert nur peer-deps, respektiert Tree). `--force` nur mit vollem Verstaendnis der Folgen.
**Quelle:** dev.to/just_ritik/why-legacy-peer-deps-is-better-than-force-in-npm-p44

### 55. `@types/node`-Major passt nicht zur Node-Version   ⭐ HAEUFIG
**Symptom:** Falsche/fehlende globale Typen, `unmet peer dependency @types/node@*`, oder Compile-Fehler nach Node-Upgrade.
**Ursache:** `@types/node`-Major MUSS dem Node-Major entsprechen (Node 24 → `@types/node@24`); npm haelt das NICHT automatisch synchron (Node kommt nicht ueber npm).
**Versionen:** per Design (DefinitelyTyped-Konvention; #42640 bestaetigt).
**FIX:** `@types/node`-Major exakt auf Node-Major pinnen (Node 24.15 → `@types/node@^24`), dann `npm update`.
**Quelle:** github.com/DefinitelyTyped/DefinitelyTyped/issues/42640

### 56. `@types/X` bricht semver durch TypeScript-Mindestversion
**Symptom:** Nach `@types/X`-Minor-Bump ploetzlich `error TS1144: '{' or ';' expected` o.ae.
**Ursache:** `@types`-Pakete droppen Support fuer alte TS-Versionen in MINOR-Releases (verletzt semver). Relevant fuer den `mcp-code-search`-Fall: `peerDependencies: typescript ^5`, aber global `typescript 6` installiert → Build kann die globale 6 ziehen, waehrend `@types` evtl. nur fuer ^5 getypt wurde.
**Versionen:** wiederkehrendes Muster.
**FIX:** TypeScript lokal pinnen (in `devDependencies` + Lockfile), `@types`-Version darauf abstimmen; bei peer-dep-Konflikt via `overrides` fixieren.
**Quelle:** github.com/DefinitelyTyped/DefinitelyTyped/discussions/55429

### 57. Gemischte Lockfiles im selben Repo (`package-lock.json` + `bun.lock` + `pnpm-lock.yaml`)   ⭐ HAEUFIG
**Symptom:** Verschiedene Entwickler/CI bekommen unterschiedliche Versionen; jemand aktualisiert das falsche Lockfile. (Genau die Lage in `mcp-code-search`: `bun.lock` UND `package-lock.json` liegen beide vor.)
**Ursache:** Jeder Paketmanager pflegt nur SEIN Format. Bun migriert beim ersten `bun install` ein vorhandenes Lockfile, laesst das Original aber liegen.
**Versionen:** per Design.
**FIX:** EINEN Paketmanager waehlen, NUR dessen Lockfile committen, die anderen loeschen + in `.gitignore`.
**Quelle:** bun.com/docs/pm/lockfile

### 58. Phantom Dependency: Import funktioniert lokal, crasht in CI/Prod
**Symptom:** Paket wird importiert, ohne in `package.json` zu stehen; lokal OK, in CI "Module not found".
**Ursache:** npm hoistet transitive Deps ins Top-Level-`node_modules` → zufaellig erreichbar. Hoisting-Reihenfolge variiert je Umgebung; verschwindet, wenn der eigentliche Bringer das Paket entfernt.
**Versionen:** per Design (npm flat hoisting).
**FIX:** Jedes tatsaechlich importierte Paket EXPLIZIT in `package.json` aufnehmen. Aufdecken via Probe-Install mit pnpm (strikt isoliert, meldet undeklarierte Imports).
**Quelle:** mergify.com/blog/npm-to-pnpm-phantom-dependencies

### 59. npm 11: `--production` deprecated → `--omit=dev`
**Symptom:** `npm install --production` laeuft noch mit Deprecation-Warnung; in strikten Docker-Builds Bruch.
**Ursache:** npm 11 hat `--production` abgekuendigt.
**Versionen:** ab npm 11 (betrifft 11.12.0).
**FIX:** `npm install --omit=dev` bzw. `npm prune --omit=dev`.
**Quelle:** medium.com/@quicksilversel/i-upgraded-node-js-from-20-to-24…

### 60. npm 11: `--ignore-scripts` blockiert jetzt auch `prepare`-Scripts von git-Dependencies
**Symptom:** Build mit `--ignore-scripts` schlaegt fehl, weil git-Dependencies nicht mehr gebaut werden (prepare laeuft nicht).
**Ursache:** npm 11 blockt mit `--ignore-scripts` nun ALLES inkl. `prepare` (v.a. Docker/CI).
**Versionen:** ab npm 11 (Breaking Change).
**FIX:** git-Deps als gebaute Tarballs/veroeffentlichte Versionen referenzieren, oder `--ignore-scripts` nur wo keine prepare-Scripts noetig sind.
**Quelle:** medium.com/@quicksilversel/i-upgraded-node-js-from-20-to-24…

### 61. `^0.x` pinnt anders: `^0.10.5` zieht KEIN `0.11.0`
**Symptom:** Erwartetes Minor-Update kommt nie; `npm outdated` zeigt neuere 0.x, die nicht installiert wird.
**Ursache:** Caret erlaubt keine Aenderung des linksten Nicht-Null-Elements. Fuer `0.x` (>=0.1.0) nur Patch-Updates; `^0.0.x` = gar keine.
**Versionen:** per Design (node-semver).
**FIX:** Bei gewolltem Pre-1.0-Minor-Update explizit `>=0.10.5 <0.12.0` o.ae. setzen. Verhalten ist korrekt nach semver.
**Quelle:** michaelsoolee.com/npm-pre-1-caret-rules/

### 62. Kompromittierte Pakete via `postinstall` (Supply-Chain)   ⭐ HAEUFIG
**Symptom:** Stiller Schadcode beim Install (Krypto-Wallet-Hijack / RAT).
**Ursache:** Maintainer-Accounts gephisht → boesartige Versionen. Beispiele: Sep 2025 chalk/debug/ansi-styles (2,6 Mrd Downloads/Woche); "Shai-Hulud" selbst-verbreitender npm-Wurm (500+ Pakete, TruffleHog-Credential-Harvesting); 31.03.2026 zwei axios-Versionen mit Cross-Platform-RAT.
**Versionen:** laufend (2025-2026).
**FIX (funktionserhaltend):** In CI `npm ci --ignore-scripts` fuer Repos ohne Script-Bedarf; global `npm config set ignore-scripts true` + Allowlist. Bei Befall: auf sichere Version downgraden, `node_modules` des Pakets loeschen, mit `--ignore-scripts` neu installieren. Lockfile pinnen.
**Quelle:** wiz.io/blog/widespread-npm-supply-chain-attack… · microsoft.com/en-us/security/blog/2026/04/01/mitigating-the-axios-npm-supply-chain-compromise/

### 63. `npm audit`-Rauschen (False Positives aus dev-deps)
**Symptom:** Hunderte "Vulnerabilities", viele aus dev-deps, die Prod nie erreichen.
**Ursache:** `npm audit` prueft direct/dev/bundled/optional Deps ohne Prod-Kontext; erfasst nur bekannte Issues (verfehlt Zero-Days/Malware).
**Versionen:** per Design.
**FIX:** `npm audit --omit=dev` fuer nur Prod-relevante Findings. `npm audit fix` NICHT blind (kann breaking Major-Bumps ziehen) — jedes Update pruefen.
**Quelle:** docs.npmjs.com/cli/audit/

### 64. Transitive Vulnerability nicht direkt fixbar → `overrides`
**Symptom:** `npm audit` meldet ein tief verschachteltes Paket, das man nicht direkt steuert.
**Ursache:** Verwundbares Paket kommt nur transitiv rein.
**Versionen:** `overrides` ab npm 8.3.0.
**FIX:** `overrides`-Feld in `package.json` erzwingt eine Version fuer alle Instanzen; danach Lockfile loeschen, `npm install`, mit `npm audit` + Tests verifizieren. Als temporaere Massnahme behandeln.
**Quelle:** herodevs.com/blog-posts/a-guide-to-npm-overrides…

### 65. Workspace-Version-Drift & Hoisting zieht falsche Version
**Symptom:** Workspace nutzt ungewollt eine andere als die deklarierte Version; bricht beim isolierten Publish/Deploy.
**Ursache:** npm hoistet gemeinsame Deps nach Top-Level; bei inkompatiblen Versionen gewinnt eine. Workspaces erreichen gehoistete Deps anderer Workspaces (Phantom).
**Versionen:** per Design (npm workspaces).
**FIX:** ALLE Deps in jedem Workspace EXPLIZIT deklarieren; Drittanbieter-Deps konsistent (zentral) versionieren.
**Quelle:** jonathancreamer.com/inside-the-pain-of-monorepos-and-hoisting/

### 66. `npx` fuehrt veraltete gecachte Version aus
**Symptom:** `npx tool` laeuft mit alter Version trotz neuerem Release.
**Ursache:** `npx` nimmt zuerst die im Central-Cache vorhandene Version (#4108).
**Versionen:** per Design.
**FIX:** `npx tool@latest` erzwingen, oder Cache leeren: `npm cache clean --force` bzw. `rm -rf ~/.npm/_npx` (Win: `%LocalAppData%/npm-cache/_npx`).
**Quelle:** github.com/npm/cli/issues/4108

### 67. Globales `tsc` statt lokalem → Versions-Mismatch   ⭐ HAEUFIG
**Symptom:** `tsc` kompiliert mit globaler TS-Version (hier 6.0.2), nicht der vom Projekt erwarteten (`mcp-code-search`: peer `typescript ^5`) → Inkonsistenzen/Fehler.
**Ursache:** Nacktes `tsc` nimmt die PATH-Version statt `node_modules`.
**Versionen:** per Design.
**FIX:** Immer `npx tsc` oder `./node_modules/.bin/tsc` bzw. via npm-Script; TypeScript lokal pinnen + Lockfile committen. Pruefen: `./node_modules/.bin/tsc --version` vs `tsc --version`.
**Quelle:** jsdev.space/howto/tsc-not-found/

### 68. npm 11.12.x: `peerOptional`-Dependencies werden als `extraneous` markiert und entfernt
**Symptom:** Inkonsistenter `node_modules`-Baum (frischer Install vs. zweiter Install), Runtime-Failures, Duplikate.
**Ursache:** npm 11 waehlt optionale Peer-Deps anders zum Hoisten/Installieren.
**Versionen:** ab npm 11.12.1 gemeldet — **OPEN** (#9249, betrifft die installierte 11.12.0-Linie).
**FIX (Workaround):** Nach `npm install` einen zweiten `npm install` oder `npm ci` gegen committetes Lockfile laufen lassen, bis ein Fix da ist; Lockfile committen.
**Quelle:** github.com/npm/cli/issues/9249

---

## G. Node.js 24 — entfernte / geaenderte APIs (Breaking beim Upgrade)

### 69. `require(esm)` jetzt default aktiv (Verhaltensaenderung)
**Symptom:** `require()` eines ESM klappt nun (frueher hartes `ERR_REQUIRE_ESM`) — ABER `require` kann jetzt die `import`-Condition treffen → andere Datei geladen (Dual-Package-Risiko #19), und Top-Level-await wirft `ERR_REQUIRE_ASYNC_MODULE` (#15). Zusaetzlich `ExperimentalWarning` ausserhalb `node_modules`.
**Ursache:** require(esm) unflagged ab Node 23/24.
**Versionen:** Node 23/24. (#55417 "Warnung unterdruecken" CLOSED/COMPLETED 2024-12-11.)
**FIX:** Nach Node-Upgrade Conditional-Exports-Pfade pruefen; CI auf Ziel-Node-Version; Module mit State auf doppeltes Laden testen (#19-Diagnose).
**Quelle:** github.com/nodejs/node/issues/55417 · socket.dev/blog/node-js-delivers-first-lts-with-require-esm-enabled

### 70. OpenSSL 3.5, Security Level 2 default
**Symptom:** `SSLV3_ALERT_HANDSHAKE_FAILURE` / Key-Rejection.
**Ursache:** RSA/DSA/DH < 2048 bit und ECC < 224 bit verboten, RC4 aus.
**Versionen:** ab Node 24.
**FIX:** Staerkere Keys/Ciphers verwenden (Funktion bleibt, sichere Parameter).
**Quelle:** nodejs.org/en/blog/migrations/v22-to-v24

### 71. Entfernte/deprecateted Node-APIs (Codemods verfuegbar)
**Symptom:** `is not a function`/Deprecation-Warnungen nach Node-24-Upgrade.
**Ursache:** Reihe entfernter APIs.
**Versionen:** ab Node 24.
**FIX (funktionserhaltend, je Ersatz):** `fs.F_OK/R_OK/W_OK/X_OK`-Getter → `fs.constants.*` (DEP0176) · `fs.truncate(fd,…)` → `fs.ftruncate(fd,…)` (DEP0081) · `dirent.path` → `dirent.parentPath` (DEP0178) · HTTP/2-`priority` entfernen (DEP0194) · `process.assert()` → `node:assert` (DEP0100) · `tls.createSecurePair()` → `new tls.TLSSocket(...)` (DEP0064) · `crypto.generateKeyPair("rsa-pss")` `hash`/`mgf1Hash` → `hashAlgorithm`/`mgf1HashAlgorithm` (DEP0154) · `url.parse()` → `new URL()` · `SlowBuffer` → `Buffer.alloc()` · `util.is*()` → `typeof`/eigene Checks. Viele haben offizielle Codemods (`@nodejs/...`).
**Quelle:** nodejs.org/en/blog/migrations/v22-to-v24

### 72. Node 24 Test-Runner: `test()`/`t.test()` geben keine Promise mehr zurueck
**Symptom:** `await t.test(…)` verhaelt sich anders; Subtests werden automatisch abgewartet.
**Ursache:** Verhaltensaenderung im node:test-Runner.
**Versionen:** ab Node 24.
**FIX:** `await` vor `test()/t.test()` entfernen.
**Quelle:** nodejs.org/en/blog/migrations/v22-to-v24

### 73. `--experimental-permission` → `--permission` umbenannt
**Symptom:** Alte Startskripte erkennen das Flag nicht.
**Ursache:** Permission-Modell stabilisiert, Flag umbenannt.
**Versionen:** ab Node 24.
**FIX:** Startskripte umstellen (Modell bleibt, nur stabiler Flag-Name).
**Quelle:** nodejs.org/en/blog/migrations/v22-to-v24

### 74. CVE-2025-55130 — Permission-Model FS-Bypass via Symlink (historisch, in 24.15 gefixt)
**Symptom:** Mit `--permission` und FS-Read/Write konnte ueber lokale Symlinks aus dem erlaubten Ordner ausgebrochen werden (Path-Traversal).
**Ursache:** Symlink-Aufloesung umging die Pfad-Pruefung.
**Versionen:** betroffen <20.20.0, <22.22.0, <24.13.0, <25.3.0 — **gefixt ab 24.13.0** (die installierte 24.15.0 ist sicher; Wissen fuer aeltere Builds).
**FIX:** Node patchen (≥24.13.0). Bis dahin keine schreibbaren Symlink-Verzeichnisse im erlaubten Scope; Pfade vorab realpath-aufloesen.
**Quelle:** research.jfrog.com/vulnerabilities/nodejs-fs-permissions-bypass-cve-2025-55130/

---

## H. Bun-Kompatibilitaet (Bun 1.3.11)

### 75. `better-sqlite3` ABI-Mismatch unter Bun   ⭐ HAEUFIG (betrifft mcp-code-search)
**Symptom:** `The module 'better_sqlite3' was compiled against a different Node.js ABI version` (NODE_MODULE_VERSION …) beim Laden, teils Bun-Crash.
**Ursache:** `bun install` kompiliert das N-API-Addon gegen Buns internen ABI; das Binary passt dann nicht zu Node (und umgekehrt). Brisant, wenn derselbe Code mit Bun UND Node laufen soll.
**Versionen:** durchgehend bis Bun 1.3 (per Design der nativen ABI; #19328/#16050 als DUPLICATE geschlossen — Kernproblem bleibt).
**FIX (funktionserhaltend):** Erste Wahl `bun:sqlite` (built-in, kein node-gyp, schneller) — aber NICHT 1:1-API zu better-sqlite3, Adapter pruefen. Wenn `better-sqlite3` zwingend: per `npm install` gegen System-Node bauen, NICHT mit `bun install`. Im Zweifel das Modul unter Node ausfuehren.
**Quelle:** github.com/oven-sh/bun/issues/19328 · bun.com/docs/runtime/sqlite

### 76. `napi_register_module_v1 not found` — natives Addon laedt nicht
**Symptom:** `symbol 'napi_register_module_v1' not found in native module. Is this a Node API (napi) module?`.
**Ursache:** Addons, die nicht ueber N-API gebaut sind oder gegen interne Node-Symbole linken, fehlen in Bun; Bun nutzt nicht libuv → Addons muessen `napi_get_uv_event_loop` statt `uv_default_loop` verwenden.
**Versionen:** #23136 CLOSED/COMPLETED 2025-12-16 (Bun-seitig verbessert) — Klasse bleibt fuer Alt-Addons relevant.
**FIX:** Auf N-API-konforme Addon-Version wechseln; im Zweifel das Modul unter Node statt Bun ausfuehren.
**Quelle:** github.com/oven-sh/bun/issues/23136

### 77. Bun 1.3 Breaking Changes (Runtime-Verhalten)
**Symptom:** Nach Upgrade auf 1.3: `Bun.serve()`-WebSocket-Typen brechen; SQL-Client wirft, wenn als Funktion statt Tagged-Template aufgerufen (`sql(...)` statt `` sql`...` ``); `module` default jetzt `Preserve`.
**Ursache:** Bewusste API-Schaerfung in 1.3.0.
**Versionen:** ab Bun 1.3.0 (per Design; #20292/#23517 CLOSED/COMPLETED).
**FIX:** `Bun.serve`-WebSocket-`data`-Typ anpassen; SQL ausschliesslich als Tagged-Template; tsconfig-`module`-Annahme pruefen.
**Quelle:** github.com/oven-sh/bun/issues/20292 · github.com/oven-sh/bun/issues/23517

### 78. `worker_threads`/`cluster`/`process.binding` nur teilweise in Bun
**Symptom:** `new Worker(..., { stdin, stdout, resourceLimits, … })` ignoriert/wirft; `markAsUntransferable`/`getHeapSnapshot` fehlen; Pakete mit `cluster` oder internem `process.binding` brechen.
**Ursache:** Bun-eigene Implementierung, nicht 1:1 zu Node; `process.binding` ist interne Node-API ohne Vertrag.
**Versionen:** laufend verbessert bis Bun 1.3; nie als stabil annehmen.
**FIX:** Worker ohne diese Optionen nutzen; Daten via `getEnvironmentData`/`setEnvironmentData`; Multiprocessing via `Bun.spawn`. Wenn ein Paket die Optionen zwingend braucht: unter Node laufen lassen.
**Quelle:** bun.com/docs/runtime/nodejs-compat

### 79. `allowImportingTsExtensions` nur mit `noEmit` — und `.ts`-Imports brechen unter Node
**Symptom:** `Option 'allowImportingTsExtensions' can only be used when either 'noEmit' or 'emitDeclarationOnly' is set`, sobald man `import './x.ts'` (Bun-Stil) schreibt und tsc emittieren soll. Spaeter unter Node: `ERR_MODULE_NOT_FOUND`.
**Ursache:** `.ts`-Imports waeren im JS-Output nicht aufloesbar. Bun fuehrt `.ts` direkt aus, tsc/Node nicht.
**Versionen:** per Design (TS).
**FIX:** Fuer reine Bun-Projekte `noEmit: true` (Bun transpiliert selbst). Wenn der Code spaeter mit Node laufen soll: KEINE `.ts`-Extensions in Imports — sonst bricht Node-Aufloesung.
**Quelle:** typescriptlang.org/tsconfig/allowImportingTsExtensions.html

### 80. `module: Preserve` / `moduleResolution: bundler` ≠ tsc/Node-Striktheit
**Symptom:** Code laeuft in Bun (extensionlose Imports, `exports`-Maps), bricht unter Node-ESM (`ERR_MODULE_NOT_FOUND`).
**Ursache:** `bundler`/`Preserve` (Bun-Default ab 1.3) erlauben extensionlose Imports und kennen die strenge Node-ESM-Aufloesung nicht. (Das Beispielprojekt `mcp-code-search` nutzt genau `moduleResolution: bundler` + `module: Preserve` → Bun-only, nicht Node-portabel.)
**Versionen:** TS 5.0+ / Bun-Default ab 1.3.
**FIX:** Wenn Node-Kompatibilitaet Ziel ist: `module: NodeNext` + Datei-Extensions in Imports. Fuer reines Bun: `Preserve`/`bundler` OK, aber bewusst als Bun-only dokumentieren.
**Quelle:** typescriptlang.org/tsconfig/module

### 81. `@types/bun` + `@types/node` gleichzeitig = globale Typkonflikte   ⭐ HAEUFIG
**Symptom:** `tsc` bricht: doppeltes `fetch`, `Subsequent property declarations must have the same type` (`Symbol.toStringTag`), `setTimeout` liefert `NodeJS.Timeout` statt `number` (Node-Globals leaken, oft weil eine Dependency `@types/node` faelschlich unter `dependencies` statt `devDependencies` hat).
**Ursache:** Beide Pakete deklarieren ueberlappende globale Typen.
**Versionen:** wiederkehrende Regression (#21701/#15481/#8761, bis 2026 nicht dauerhaft geloest).
**FIX (funktionserhaltend):** In tsconfig `"types": ["bun"]` (bzw. `[]`) setzen, damit nicht alle `@types/*` gecrawlt werden — dann gewinnt EIN Typ-Set; `@types/node` nur als devDependency. Fuer reine Bun-Projekte `@types/node` ganz weglassen. (Passt zu A1: TS 6.0 crawlt ohnehin nicht mehr automatisch.)
**Quelle:** github.com/oven-sh/bun/issues/21701 · donatstudios.com/TypeScriptTimeoutTrouble

### 82. `bun.lock`/`bun.lockb` + `package-lock.json` im selben Repo = Drift (+ Crash)
**Symptom:** Unterschiedlicher Dependency-Tree je Tool; `bun install --frozen-lockfile` erzeugt trotzdem `bun.lock`; `overrides` aus npm-Lock verschwinden bei Migration; in einem Fall Bun-Crash bei `bun install` mit `bun.lockb` neben `package.json`.
**Ursache:** Zwei Package-Manager, zwei Wahrheiten; Migration uebernimmt nicht alle Felder.
**Versionen:** #16646 (frozen-lockfile erzeugt trotzdem bun.lock) **OPEN**; #23980 (Crash) **OPEN**; #7233 (overrides) gefixt 2024-07-23.
**FIX:** Genau EIN Lockfile committen. Bei Migration npm→bun: nach erstem `bun install` `package-lock.json` loeschen; `overrides` manuell in Bun verifizieren. (Siehe auch #57.)
**Quelle:** github.com/oven-sh/bun/issues/16646 · github.com/oven-sh/bun/issues/23980 · bun.com/docs/pm/lockfile

### 83. Bun `fetch` kennt keinen undici-`dispatcher` (Self-Signed/Cert-Pinning bricht)
**Symptom:** `fetch(url, { dispatcher })` aus undici wird ignoriert; `rejectUnauthorized: false` im custom Dispatcher wirkungslos — in einer `bun build --compile`-Exe komplett ignoriert. Kein `https.Agent`-Aequivalent fuer Per-Request-Cert-Pinning.
**Ursache:** Bun-`fetch` ist nicht undici-basiert; kennt die `dispatcher`-API nicht.
**Versionen:** durchgehend bis Bun 1.3; #10642 **OPEN**, Standalone-Exe-Bug #24376 **OPEN**.
**FIX (funktionserhaltend):** Buns eigene `tls`-Option pro Request (`fetch(url, { tls: { rejectUnauthorized: false } })`) bzw. CA via `tls.ca`; `process.env.NODE_TLS_REJECT_UNAUTHORIZED` ist in neueren Bun-Versionen zur Laufzeit setzbar. Kein undici-`dispatcher` annehmen.
**Quelle:** github.com/oven-sh/bun/issues/10642 · github.com/oven-sh/bun/issues/24376

---

## I. Plattform-Fallen (Windows · macOS/Linux)

### 84. Import-Casing bricht auf Linux/CI, nicht auf macOS/Windows   ⭐ HAEUFIG
**Symptom:** `import './FileManager'` bei Datei `fileManager.ts` laeuft lokal (macOS/Windows case-insensitive), faellt erst im Linux-CI/Container/Deploy mit `Module not found` durch.
**Ursache:** macOS/Windows-FS case-insensitive, Linux case-sensitive. TS folgt dem FS.
**Versionen:** per Design.
**FIX (Poka-Yoke):** `"forceConsistentCasingInFileNames": true` (in TS-Strict ohnehin Default) → tsc meldet Casing-Mismatch lokal; CI auf Linux laufen lassen; `eslint-plugin-import` ergaenzend.
**Quelle:** typescriptlang.org/tsconfig/forceConsistentCasingInFileNames.html

### 85. Windows-Konsole cp1252 statt UTF-8 (Umlaute/Emoji zerstoert)
**Symptom:** Umlaute/Emoji im stdout verstuemmelt; nach `chcp 65001` teils immer noch falsch; Datei-Reads von Windows-1252-Dateien als Muell.
**Ursache:** Windows-Default-Codepage CP1252 ≠ Node-Default UTF-8.
**Versionen:** durchgehend (offener Node-Bug #57780 auch nach chcp).
**FIX:** `chcp 65001` (temporaer) oder dauerhaft per Registry; Legacy-Dateien mit `iconv-lite` (`win1252`); fuer Datei-IO mit Unicode immer explizit `encoding: 'utf-8'`.
**Quelle:** github.com/nodejs/node/issues/57780

### 86. Pfad-Trenner `\` vs `/` (Split auf `'/'` bricht auf Windows)
**Symptom:** Pfad-Logik funktioniert auf macOS/Linux, liefert auf Windows falsche/leere Pfade.
**Ursache:** `path.join` liefert `data\test.txt` auf Windows; eigenes Splitten auf `/` ignoriert das.
**Versionen:** per Design.
**FIX:** Immer `node:path` (`join`/`resolve`/`sep`) statt String-Manipulation; fuer URL-aehnliche Pfade gezielt `path.posix`.
**Quelle:** gist.github.com/domenic/2790533

### 87. CRLF-Shebang `/bin/bash^M: bad interpreter`
**Symptom:** Shell-/Node-Skript bricht auf Linux/macOS mit `bad interpreter: No such file or directory`; `^M` unsichtbar.
**Ursache:** Datei mit CRLF committet → Kernel sucht Interpreter `bash\r`.
**Versionen:** per Design.
**FIX (Poka-Yoke):** `.gitattributes` mit `*.sh text eol=lf`. Akut: `sed -i 's/\r$//' script.sh` bzw. Editor auf LF.
**Quelle:** iotools.cloud/journal/crlf-vs-lf-the-line-ending-bug-that-breaks-ci/

### 88. Windows: `EPERM` bei Directory-Symlinks (`fs.symlink ... 'dir'`)
**Symptom:** `fs.symlinkSync(target, path, 'dir')` → `EPERM`, obwohl `mklink` als Tool geht. File-Symlinks gehen, Dir-Symlinks nicht.
**Ursache:** Windows erlaubt Dir-Symlinks nur mit Developer Mode oder Admin/SeCreateSymbolicLinkPrivilege.
**Versionen:** per Design (Windows).
**FIX:** Developer Mode aktivieren; oder Junctions (`fs.symlink(..., 'junction')`) fuer Verzeichnisse statt `'dir'` — funktionserhaltend ohne Adminrechte.
**Quelle:** github.com/nodejs/node/issues/18518

### 89. Linux: `ENOSPC: System limit for number of file watchers reached`
**Symptom:** Watch-/Dev-Mode (`bun --watch`, nodemon, Vite, Jest) bricht mit ENOSPC — trotz freier Platte.
**Ursache:** Linux inotify-Limit (`fs.inotify.max_user_watches`, oft ~8192) durch viele beobachtete Dateien (inkl. `node_modules`) erschoepft.
**Versionen:** per Design (Linux-Kernel).
**FIX (funktionserhaltend):** Limit erhoehen: `echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf && sudo sysctl -p`; `node_modules`/`.git`/`dist` aus dem Watch ausschliessen.
**Quelle:** bobbyhadz.com/blog/system-limit-for-number-of-file-watchers-reached

---

## J. Aus dem Best-Practices-Lauf ergaenzt (2026-06-03)

> Diese Bugs hat der Best-Practices-Lauf (`best-practices/web/typescript.md`) zutage gefoerdert
> und waren in A-I noch nicht abgedeckt. Quelle pro Eintrag, gegen A-I dedupliziert.

### 90. `exports`-Condition-Reihenfolge falsch — `types`/`import` werden ueberschattet   ⭐ HAEUFIG
**Symptom:** Consumer bekommt `any`/"Cannot find module"-Typen oder laedt die falsche Datei, obwohl alle Pfade in `exports` stehen. `attw` meldet "masquerading"/missing types.
**Ursache:** `exports`-Conditions werden der Reihe nach geprueft; die ERSTE passende gewinnt. Steht `default` (oder `require`/`import`) VOR `types`, findet TS nie die Deklarationen. Haeufigster Publishing-Fehler.
**Versionen:** Node 12.7+ exports, aktuell 24. Per Design.
**FIX:** Conditions spezifisch->allgemein ordnen: `types` IMMER zuerst, `default` IMMER zuletzt (`types -> node-addons -> node -> import/require -> module-sync -> default`). Mit `attw --pack .` verifizieren.
**Quelle:** nodejs.org/api/packages.html · esmodules.com/publishing/ (Best-Practices A/G)

### 91. Switch ueber Discriminated Union ohne `never`-Default — neue Member still vergessen
**Symptom:** Beim Hinzufuegen eines Union-Members wird der zugehoerige `case` vergessen; kein Compile-Fehler, falsches Laufzeitverhalten.
**Ursache:** Ohne Exhaustiveness-Check faellt der unbehandelte Member stillschweigend durch.
**Versionen:** per Design, alle.
**FIX:** Im `default` `const _exhaustive: never = shape;` — TS wirft dann einen Compile-Fehler, sobald ein Member fehlt. (Best-Practice C, Kern-Pattern.)
**Quelle:** typescriptlang.org/docs/handbook/2/narrowing.html

### 92. `typeof x === "object"` laesst `null` durch -> `null.foo`-Crash
**Symptom:** Naives Object-Guard akzeptiert `null` -> "Cannot read properties of null".
**Ursache:** `typeof null === "object"` (historischer JS-Bug, per Spec).
**Versionen:** alle.
**FIX:** `x && typeof x === "object"` schreiben (Truthiness-Check zuerst).
**Quelle:** typescriptlang.org/docs/handbook/2/narrowing.html

### 93. Strukturelle ID-Verwechslung (`UserId`/`OrderId` als nackte `string`)
**Symptom:** Eine `OrderId` wird an eine Funktion uebergeben, die eine `UserId` erwartet — kein Compile-Fehler, subtiler Logik-Bug (kein Crash).
**Ursache:** TS ist strukturell; zwei `string`-Aliasse sind austauschbar.
**Versionen:** per Design.
**FIX:** Branded/Nominal Types per `unique symbol`: `type UserId = string & { readonly [brand]: "UserId" }`. Null Laufzeit-Overhead, Cast nur im Constructor. (Best-Practice C.)
**Quelle:** github.com/microsoft/TypeScript/pull/33038 · oneuptime.com (Branded Types)

### 94. `await using` / `Symbol.asyncDispose` braucht Runtime-Support
**Symptom:** Code kompiliert (TS 5.2+), wirft aber zur Laufzeit in aelteren Runtimes — `Symbol.asyncDispose` ist `undefined`.
**Ursache:** Explicit Resource Management ist nativ erst ab Node 24; aeltere Runtimes brauchen einen Polyfill.
**Versionen:** TS 5.2+ (Syntax), nativ ab Node 24.
**FIX:** Node >=24 sicherstellen oder Polyfill laden. Bei `tsc`-Downlevel den Helper einbinden (TS emittiert ihn). (Best-Practice D.)
**Quelle:** typescriptlang.org/docs/handbook/release-notes/typescript-5-2.html · tc39/proposal-async-explicit-resource-management

### 95. `node file.ts` (Type-Stripping) prueft KEINE Typen
**Symptom:** `node app.ts` laeuft durch, obwohl Typfehler im Code sind — der Fehler kommt erst zur Laufzeit. Falsche Sicherheit ("ist ja getypt").
**Ursache:** Node-Type-Stripping entfernt nur Annotations und fuehrt das uebrige JS aus; es ruft KEINEN Typechecker.
**Versionen:** Type-Stripping stable ab Node 24.12.0.
**FIX:** Typecheck IMMER separat: `tsc --noEmit` im CI / pre-commit. Type-Stripping ersetzt nur den Transpile-Step, nicht den Check.
**Quelle:** nodejs.org/api/typescript.html · nodejs.org/learn/typescript/run-natively (Best-Practice F)

### 96. `enum`/`namespace`/Parameter-Properties crashen beim reinen Type-Stripping
**Symptom:** `node file.ts` wirft `SyntaxError` bei `enum`, `namespace` mit Runtime-Code oder `constructor(private x)`.
**Ursache:** Diese TS-Features erzeugen Runtime-Code; reines Stripping kann sie nicht entfernen — sie brauchen Code-Generierung (`--experimental-transform-types`).
**Versionen:** ab Node 24 (Type-Stripping).
**FIX:** Erasable-only TS schreiben (`type`, `interface`, `import type`, keine `enum`/`namespace`) ODER `--experimental-transform-types` setzen. Empfehlung: erasable-only.
**Quelle:** nodejs.org/api/typescript.html

### 97. `node --watch` startet bei `.env`-Aenderung NICHT neu
**Symptom:** `node --watch --env-file=.env app.ts` reagiert nicht, wenn man die `.env` speichert — alte Werte bleiben.
**Ursache:** `--watch` beobachtet die JS/TS-Modulgraphen, nicht die per `--env-file` geladene Datei.
**Versionen:** Node 24 — **OPEN** (#54001).
**FIX:** `.env` manuell in eine Watch-Liste aufnehmen oder den Prozess bei `.env`-Aenderung selbst neu starten (z.B. via separatem Watcher).
**Quelle:** github.com/nodejs/node/issues/54001

### 98. `node:sqlite` in Node 24 nur mit `--experimental-sqlite`
**Symptom:** `import { DatabaseSync } from 'node:sqlite'` wirft `ERR_UNKNOWN_BUILTIN_MODULE` / Flag-Fehler ohne Flag.
**Ursache:** Built-in SQLite ist in Node 24 noch experimentell; RC-Status (1.2) erst ab v25.7.0.
**Versionen:** Node 22.5+ (Flag), in 24 weiter `--experimental-sqlite`.
**FIX:** Fuer Prototyping `--experimental-sqlite` setzen; fuer Produktion `better-sqlite3` (erprobt, schneller) verwenden.
**Quelle:** zenn.dev/.../node-builtin-sqlite · sqg.dev/blog/sqlite-driver-benchmark/

### 99. `--enable-source-maps` in Produktion: Performance-Overhead + bricht `Error.prepareStackTrace`
**Symptom:** (a) Spuerbare Verlangsamung bei vielen/grossen Source-Maps und hoher Error-Rate (kleiner Service kann bei ~10 failenden Requests/s einbrechen). (b) Custom-Stack-Trace-Formatter (Sentry-Wrapper etc.) werden still ignoriert.
**Ursache:** `--enable-source-maps` remappt jeden Stack-Trace (teuer) und ueberschreibt/ignoriert `Error.prepareStackTrace`.
**Versionen:** durchgehend — Perf-Issue **OPEN** (node#41541).
**FIX:** In Staging/CI an, in Hot-Path-Produktion abwaegen. Wenn ein Custom-Formatter gebraucht wird: `--enable-source-maps` dort NICHT setzen oder Source-Maps anders aufloesen.
**Quelle:** github.com/nodejs/node/issues/41541 · nodejs.org CLI Docs

### 100. `.npmignore` ueberschreibt `.gitignore`-Verhalten komplett -> Sources/Secrets im Tarball
**Symptom:** Veroeffentlichtes Paket enthaelt Quellen, Tests, `.env` o.ae. — obwohl `.gitignore` sie ausschliesst.
**Ursache:** Sobald eine `.npmignore` existiert, ignoriert npm die `.gitignore` KOMPLETT; vergessene Eintraege lecken in das Tarball.
**Versionen:** per Design.
**FIX:** Statt `.npmignore`-Denylist die `files`-Allowlist in package.json nutzen (`"files": ["dist"]`) + `npm pack --dry-run` als Pflicht-Gate vor jedem Publish.
**Quelle:** docs.npmjs.com/cli/v11/using-npm/developers (Best-Practice G)

### 101. `isolatedDeclarations` failt bei nicht vollstaendig annotierten exports
**Symptom:** Compiler-Fehler an Public-API-Funktionen mit inferiertem Rueckgabetyp, sobald `isolatedDeclarations: true` aktiv ist.
**Ursache:** `isolatedDeclarations` verlangt explizite Typ-Annotationen an ALLEN exports (damit `.d.ts` ohne Typechecker generierbar ist).
**Versionen:** TS 5.5+.
**FIX:** Public-API-Rueckgabetypen explizit annotieren. Migration grosser Bestands-Libraries ist nicht "frei" — case-by-case einfuehren.
**Quelle:** typescriptlang.org/docs/handbook/release-notes/typescript-5-5.html

### 102. `composite`-Projekt: alle Dateien muessen per `include`/`files` erfasst sein
**Symptom:** `error TS6307: File ... is not listed within the file list of project` bei `composite: true`.
**Ursache:** Project-References-faehige Projekte (`composite: true`) verlangen, dass JEDE Implementierungsdatei ueber `include`/`files` erfasst ist.
**Versionen:** per Design.
**FIX:** `include` vollstaendig setzen (z.B. `"include": ["src"]`), keine impliziten Dateien ausserhalb.
**Quelle:** typescriptlang.org/docs/handbook/project-references.html

### 103. `tsup` honoriert `declarationMap` aus tsconfig nicht zuverlaessig
**Symptom:** Erwartete `.d.ts.map` fehlen trotz `declarationMap: true` in tsconfig.
**Ursache:** `tsup` liest die Option nicht zuverlaessig aus tsconfig; braucht explizite Config.
**Versionen:** **OPEN** (tsup #488/#885).
**FIX:** `declarationMap` explizit in der tsup-Config setzen; `.d.ts.map` ohnehin NICHT ins npm-Tarball publishen (nur Monorepo-DX).
**Quelle:** github.com/egoist/tsup/issues/488 · github.com/egoist/tsup/issues/885

### 104. `npm publish --provenance` erzeugt Attestation auch bei fehlgeschlagenem Publish
**Symptom:** Eine Provenance-Attestation entsteht, obwohl der eigentliche Publish scheitert -> potenziell verwaiste Attestation.
**Ursache:** Reihenfolge-Problem im Publish-Flow.
**Versionen:** **OPEN** (npm/cli #7654).
**FIX:** Publish-Erfolg in CI explizit verifizieren (Exit-Code + `npm view <pkg>@<version>`), bevor man sich auf die Attestation verlaesst.
**Quelle:** github.com/npm/cli/issues/7654

### 105. Native C++-Addons brauchen ggf. C++20 (V8 13.6 in Node 24)
**Symptom:** Recompile-Fehler bei nativen Modulen, die mit C++17 gebaut wurden, nach Node-24-Upgrade.
**Ursache:** V8 13.6 in Node 24 verlangt teils C++20 fuer Addon-Builds.
**Versionen:** ab Node 24.
**FIX:** Addon-Toolchain auf C++20 heben bzw. aktualisierte Addon-Version nutzen; im Zweifel das Modul gegen die Ziel-Node-Version neu bauen.
**Quelle:** nodejs.org/en/blog/migrations/v22-to-v24

---

## 🔗 Kopplung zur Best-Practices-Datei (wechselseitige Bezugstabelle)

Bug-Almanach (diese Datei) <-> Best-Practices `~/proggs/best-practices/web/typescript.md`.
Die identische Tabelle steht auch dort.

| Best-Practice-Abschnitt (`best-practices/web/typescript.md`) | Zugehoeriger Bug-Almanach-Abschnitt (hier) |
|------------------------------------------------------------|--------------------------------------------|
| **A — Strikte tsconfig & Compiler-Strenge** | **A** (TS-6.0 Defaults), **B** (Deprecations), **D24-D39** (Typ-Fallen / fehlende Strict-Flags / skipLibCheck) |
| **B — Sauberes ESM-Setup + package.json** | **C** (ESM vs CommonJS #15-23), **G69** (require(esm) default), **J90** (exports-Reihenfolge) |
| **C — Typsichere Patterns ohne any** | **D** (Typ-Fallen #24-39), **J91** (never-Exhaustiveness), **J92** (typeof null), **J93** (Branded IDs) |
| **D — Robuste async-Fehlerbehandlung** | **E** (Async/Promises #40-52), **J94** (await using Runtime) |
| **E — Dependency-Hygiene & npm/Toolchain** | **F** (npm/Dependencies #53-68), **J104** (--provenance bei fail) |
| **F — Node 24 APIs + Bun-Interop** | **G** (Node-24-APIs #69-74), **H** (Bun #75-83), **J95-J98, J105** (Type-Stripping, --watch/.env, node:sqlite, C++20) |
| **G — Build, Publishing & Projektstruktur** | **A/B** (tsconfig), **C20** (exports-Kapselung), **J99-J103** (source-maps, .npmignore, isolatedDeclarations, composite, tsup) |

---

## Fix-Status (was ist seit Meldung schon behoben?)

> Hart per `gh issue view` geprueft am 2026-06-02 (gh authentifiziert). Trennt **belegt gefixt** von **noch offen / per Design**. Im Zweifel: Workaround bleibt aktiv.

### Belegt GEFIXT (Workaround historisch / Eintrag dient dem Verstaendnis)
| Frueherer Bug | Issue | gefixt | Bezug |
|---------------|-------|--------|-------|
| require(esm) Top-Level-Error vergiftet `await import` | nodejs/node#58945 | CLOSED 2025-07-09 | — (war Researcher-"offen", real gefixt) |
| require(esm) Tracking (Feature stabil) | nodejs/node#52697 | CLOSED 2026-01-12 | #15 |
| require(esm) ExperimentalWarning unterdrueckbar | nodejs/node#55417 | CLOSED 2024-12-11 | #69 |
| JSON-Import-Regression Node16-Resolution | microsoft/TypeScript#60589 | CLOSED 2024-12-04 | C-Reihe |
| `satisfies` + `as const` (als Design geschlossen) | microsoft/TypeScript#51173 | CLOSED 2024-02-28 | #33 (Workaround bleibt) |
| const type param in Mapped Types | microsoft/TypeScript#54537 | CLOSED 2023-06-06 | #35 |
| TS5-Regression "deep instantiation" | microsoft/TypeScript#53514 | CLOSED 2025-08-27 | #34 |
| `verbatimModuleSyntax` + const enum (Fehler jetzt gemeldet) | microsoft/TypeScript#52669/#48040 | CLOSED 2023/2024 | #37 |
| Excess-Property-Bypass (als Design geschlossen) | microsoft/TypeScript#48852 | CLOSED 2022-04-26 | #32 (Verhalten bleibt) |
| skipLibCheck ignoriert bei `.ts`-types | microsoft/TypeScript#41883 | CLOSED 2020-12-11 | #39 |
| exactOptionalPropertyTypes-Warnung (TS 6.0) | microsoft/TypeScript#63232 | CLOSED 2026-03-11 | #28 |
| Generic-Inferenz/Constraint-Loss | microsoft/TypeScript#43371/#36124/#47191/#47385 | CLOSED 2022-2024 | #35/#36 |
| npm ci out-of-sync Lockfile | npm/cli#8726/#8693 | CLOSED 2026-02/2025-10 | F-Reihe |
| npm 11.6.1 nondeterministisches Lockfile | npm/cli#8725 | CLOSED 2025-11-26 | F-Reihe (in 11.12 gefixt) |
| npx stale cache | npm/cli#4108 | CLOSED 2022-01-27 | #66 (Verhalten per Design, @latest bleibt) |
| Bun `napi_register_module_v1` | oven-sh/bun#23136 | CLOSED 2025-12-16 | #76 |
| Bun 1.3 Breaking (SQL/WS) | oven-sh/bun#20292/#23517 | CLOSED 2025/2026 | #77 |
| Bun overrides-Migration | oven-sh/bun#7233 | CLOSED 2024-07-23 | #82 |
| @types/bun-Konflikt (eine Runde) | oven-sh/bun#21701/#8761 | CLOSED 2025 | #81 (Regression kehrt wieder) |
| CVE-2025-55130 Permission-Bypass | — | gefixt ab Node 24.13.0 | #74 (24.15 sicher) |

### Noch NICHT gefixt — Workaround bleibt aktiv
| Bug | Issue | Status | Eintrag |
|-----|-------|--------|---------|
| TS2589 "deep instantiation" (Kern) | microsoft/TypeScript#34933 | **OPEN** | #34 |
| TS2835 schlaegt falsch `.js` vor | microsoft/TypeScript#60926 | **OPEN** | #17 |
| Mapped-Type prueft Constraint nicht | microsoft/TypeScript#49302 | **OPEN** | #36 |
| Enum nur-als-Typ: kein Fehler unter verbatimModuleSyntax | microsoft/TypeScript#55741 | **NOT_PLANNED** | #38 |
| ERR_UNHANDLED_REJECTION False-Positive (Microtask) | nodejs/node#43326 | **OPEN** | #41 |
| npm 11.12.x peerOptional → extraneous/entfernt | npm/cli#9249 | **OPEN** | #68 |
| Bun `--frozen-lockfile` erzeugt trotzdem bun.lock | oven-sh/bun#16646 | **OPEN** | #82 |
| Bun crasht bei `bun.lockb` + `package.json` | oven-sh/bun#23980 | **OPEN** | #82 |
| Bun fetch ignoriert undici-`dispatcher` | oven-sh/bun#10642 | **OPEN** | #83 |
| Bun-Exe ignoriert `rejectUnauthorized:false` | oven-sh/bun#24376 | **OPEN** | #83 |

**Ehrlichkeits-Hinweis zur Methodik:** GitHub-Issues wurden per `gh issue view` mit hartem `state`/`stateReason` geprueft (verlaesslich). Viele Eintraege sind **per Design** (ESM/CJS-Semantik, strukturelles Typing, Bun-vs-Node-Architektur, npm-Hoisting) — die werden NIE "gefixt", der Workaround bleibt dauerhaft gueltig. `CLOSED/COMPLETED` heisst nicht immer "Bug behoben": bei Design-Fragen (`#51173`, `#48852`) bedeutet es "als beabsichtigtes Verhalten geschlossen" — das Symptom bleibt, nur der Workaround zaehlt.

---

## Pflicht-Checkliste vor TypeScript-/Node-Arbeit

- [ ] **tsconfig geprueft?** `strict` + `noUncheckedIndexedAccess` + `exactOptionalPropertyTypes` bewusst gesetzt? `moduleResolution` = `nodenext` (Node) oder `bundler` (Bun)? `types`-Feld explizit (TS 6.0 crawlt nicht mehr)?
- [ ] **ESM oder CJS?** `"type"` im package.json bewusst? ESM-Imports mit `.js`-Endung (oder `rewriteRelativeImportExtensions`)? Kein `__dirname` ohne `import.meta`?
- [ ] **Async sauber?** Kein vergessenes `await`? Floating Promises mit `.catch`? `forEach`+async vermieden (→ `for...of`/`Promise.all`)? `Promise.allSettled` wo Einzelergebnisse zaehlen?
- [ ] **Versionen passen?** `@types/node`-Major = Node-Major (24)? Lokales `tsc` (nicht global 6.0.2) via `npx tsc`? Nur EIN Lockfile im Repo?
- [ ] **Bun vs Node klar?** Wenn der Code auch unter Node laufen soll: keine `.ts`-Imports, `module: NodeNext`, native Module gegen Node bauen?
- [ ] **Plattform?** Datei-Casing konsistent (Linux-CI!)? `node:path` statt String-Split? `.gitattributes eol=lf` fuer Skripte? Datei-IO mit `encoding: 'utf-8'`?
- [ ] **Nach Node-/TS-Upgrade:** Deprecation-/Breaking-Changes-Liste (Abschnitt A/B/G) durchgegangen? `node --experimental-print-required-tla` bei `require(esm)`-Fehlern?

---

*Neue selbst erlebte TS-/Node-Bugs hier als Eintrag ergaenzen (Bug + funktionserhaltende Loesung + Versionen) und den Stand-Header aktualisieren. Bei Versionssprung (Node 26, TS 7.0 "Corsa") kurzer Re-Check von Abschnitt A/B/G.*
