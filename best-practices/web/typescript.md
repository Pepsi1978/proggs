# TypeScript / Node.js — Best Practices (Stand 2026-07-02, Node v24.15.0 · TypeScript 6.0.2 · npm 11.17.0 · Bun 1.3.14)

> **Die "richtige Seite der Medaille" zum Bug-Almanach `~/proggs/bugs/web/typescript.md`.**
> Dort steht *was schiefgeht und wie man es umgeht* — hier steht *wie man es von vornherein
> richtig macht, damit der Bug gar nicht erst entsteht*. Die wechselseitige Abschnitts-
> Bezugstabelle steht unten ("Kopplung zum Bug-Almanach").
>
> **Anker (live ermittelt):** Node **v24.15.0**, TypeScript **6.0.2**, npm **11.17.0**, Bun **1.3.14**.
> TS 6.0 ist die LETZTE JavaScript-basierte Version vor dem Go-Rewrite (TS 7.0 "Corsa") — viele
> Defaults wurden in 6.0 modernisiert, deshalb 2026 alles Verhaltens-Relevante EXPLIZIT setzen.
> Node 24 erlaubt `require(esm)` synchron und kann `.ts` nativ ausfuehren (Type-Stripping stable
> seit 24.12).
>
> **Projekt-Anker:** `~/proggs/mcp-code-search` (ESM + Bun + `better-sqlite3`, `moduleResolution: bundler`).
>
> **Quellen-Rangordnung:** offizielle Quellen (typescriptlang.org, nodejs.org/api, docs.npmjs.com) =
> Grundwahrheit; Community/Library-Docs als `extern` gelabelt (fuer ihre eigene Lib aber autoritativ).
> Jeder Eintrag traegt Quelle + Datum + `offiziell`/`extern`-Flag.

> **Update 2026-07-02:** Re-Recherche seit 2026-06-03 fand keine belegten neuen TypeScript-Breaking-Changes; die Best Practices bleiben fachlich unveraendert. Aktualisiert wurden nur die Live-Anker fuer npm/Bun.

---

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

---

## A — Strikte tsconfig & Compiler-Strenge

> Versions-Anker: Node v24.15.0, TypeScript 6.0.2. TS 6.0 ist die LETZTE JavaScript-basierte Version vor dem Go-Rewrite (TS 7.0) — viele Defaults wurden in 6.0 modernisiert, deshalb 2026 alles Verhaltens-Relevante EXPLIZIT setzen, nicht auf Defaults vertrauen.

### Do's

- **`strict: true` IMMER setzen — auch wenn es in TS 6.0 jetzt Default ist.** Explizit hinschreiben macht die Absicht klar und schuetzt vor versehentlichem Abschalten durch ein geerbtes Base-Config. `strict` aktiviert 8 Flags: `noImplicitAny`, `strictNullChecks`, `strictFunctionTypes`, `strictBindCallApply`, `strictPropertyInitialization`, `noImplicitThis`, `useUnknownInCatchVariables`, `alwaysStrict` (offiziell, typescriptlang.org/tsconfig/strict.html, 2026-06-03).
- **`useUnknownInCatchVariables` ist bereits in `strict` enthalten** — `catch (e)` typt `e` als `unknown` statt `any`. Nicht separat noetig, aber wissen dass `strict:true` es mitbringt (offiziell, typescriptlang.org/tsconfig/strict.html, 2026-06-03).
- **Die NICHT in `strict` enthaltenen Strenge-Flags zusaetzlich aktivieren** — das offizielle TS-Team und `@tsconfig/strictest` empfehlen mindestens diese sechs:
  - `noUncheckedIndexedAccess: true` — haengt `| undefined` an jeden Index-/dynamischen Key-Zugriff (`arr[0]`, `obj[key]`). Wichtigste fehlende Strenge; verhindert die haeufigste Laufzeit-`undefined`-Falle (offiziell, typescriptlang.org/tsconfig/, 2026-06-03; extern, totaltypescript.com/tsconfig-cheat-sheet, 2026-06-03).
  - `exactOptionalPropertyTypes: true` — trennt "Property fehlt" von "Property = undefined". Relevant bei Serialisierung, Prisma, externen Daten. ACHTUNG: kann bei bestehenden Codebasen viele Fehler erzeugen — bei Neuprojekten an, bei Migration ggf. zuletzt (extern, dev.to strict-mode-Artikel, 2026-06-03; offiziell, typescriptlang.org/tsconfig/, 2026-06-03).
  - `noImplicitOverride: true` — erzwingt `override`-Keyword bei ueberschriebenen Methoden (offiziell, @tsconfig/strictest npm, 2026-06-03).
  - `noFallthroughCasesInSwitch: true` — verhindert vergessenes `break` (offiziell, @tsconfig/strictest npm, 2026-06-03).
  - `noPropertyAccessFromIndexSignature: true` — erzwingt `obj["key"]` statt `obj.key` bei Index-Signaturen (offiziell, @tsconfig/strictest npm, 2026-06-03).
  - `noImplicitReturns: true` — alle Code-Pfade muessen returnen (offiziell, @tsconfig/strictest npm, 2026-06-03).
  - Optional zusaetzlich (Codehygiene, nicht Typsicherheit): `noUnusedLocals`, `noUnusedParameters`, `allowUnreachableCode:false`, `allowUnusedLabels:false` — alle in `@tsconfig/strictest` (offiziell, @tsconfig/strictest npm, 2026-06-03).
- **In TS 6.0 geaenderte Defaults bewusst EXPLIZIT setzen** (sie "floaten" sonst mit der TS-Version mit):

  | Option | Alter Default | TS-6.0-Default | 2026-Empfehlung Node-24 |
  |--------|---------------|----------------|--------------------------|
  | `strict` | `false` | `true` | `true` (explizit) |
  | `module` | `commonjs` | `esnext` | `nodenext` |
  | `target` | `es5` | `es2025` (floatet pro Jahr) | `es2024` fix anheften |
  | `rootDir` | inferiert | `.` | `./src` |
  | `types` | alle `@types` | `[]` | `["node"]` |
  | `noUncheckedSideEffectImports` | `false` | `true` | `true` lassen |
  | `libReplacement` | `true` | `false` | `false` lassen (Perf) |

  (offiziell, typescriptlang.org/docs/handbook/release-notes/typescript-6-0.html, 2026-06-03)
- **`target` fix anheften (z.B. `es2024`) statt den floatenden 6.0-Default `es2025` zu nehmen** — ein per-Jahr floatender Target macht Builds nicht reproduzierbar zwischen TS-Patch-Versionen. Fuer Node 24 ist `es2024` sicher unterstuetzt (offiziell, typescriptlang.org release-notes 6.0, 2026-06-03).
- **`types: ["node"]` setzen** — der neue `[]`-Default in 6.0 bedeutet, dass `@types/node`-Globals (`process`, `Buffer`, …) sonst NICHT geladen werden. Haeufigster Migrations-Bruch (offiziell, typescriptlang.org release-notes 6.0, 2026-06-03).
- **`moduleResolution`: fuer Node-24-Apps und Libraries `nodenext`** — erzwingt korrekte ESM/CJS-Dual-Format-Semantik, `package.json` `exports`/`imports`, und verlangt Datei-Endungen in relativen Imports (so wie Node sie braucht). `nodenext` setzt `moduleResolution` automatisch passend, wenn `module: nodenext` (offiziell, typescriptlang.org/tsconfig/moduleResolution.html, 2026-06-03).
- **`bundler` NUR wenn ein Bundler (Vite/esbuild/webpack) emittiert, nicht tsc** — `bundler` erlaubt endungslose Imports und externalisierte Deps, die in echtem Node CRASHEN wuerden. Fuer Libraries ist `nodenext` Pflicht, weil es genau diese unsicheren Specifier verhindert (extern, blog.andrewbran.ch, 2026-06-03; offiziell, typescriptlang.org/tsconfig/moduleResolution.html, 2026-06-03).
- **`verbatimModuleSyntax: true`** — verbietet, dass TS `import`/`export` still in `require` umschreibt; erzwingt bewusstes `import type`. Paart perfekt mit `module: nodenext` und ist Pflicht bei Isolated-Compilation-Tools (Vite/esbuild/Babel/Bun) (offiziell, typescriptlang.org/tsconfig/verbatimModuleSyntax.html, 2026-06-03).
- **`isolatedModules: true`** — garantiert, dass jede Datei einzeln transpilierbar ist (Voraussetzung fuer esbuild/Bun/swc). `moduleDetection: "force"` dazu, damit jede Datei als ESM-Modul gilt (extern, totaltypescript.com/tsconfig-cheat-sheet, 2026-06-03).
- **`forceConsistentCasingInFileNames`** — schon laenger Default `true`; nicht entfernen. Schuetzt vor Casing-Bugs zwischen macOS/Windows (case-insensitive) und Linux-CI (case-sensitive) (offiziell, typescriptlang.org/tsconfig/, 2026-06-03).
- **`skipLibCheck: true` pragmatisch setzen** — spart bis ~50% Typecheck-Zeit, umgeht inkompatible/zu strikt kompilierte Dependency-`.d.ts`. Dein eigener Code bleibt voll geprueft (offiziell, typescriptlang.org/tsconfig/skipLibCheck.html, 2026-06-03; extern, totaltypescript.com, 2026-06-03).
- **`isolatedDeclarations: true` fuer Libraries** — erzwingt explizite Rueckgabetypen an Public-API-Grenzen, sodass `.d.ts` ohne vollen Typechecker generiert werden kann (Voraussetzung fuer schnelle parallele Declaration-Emission, relevant fuer TS-7-Vorbereitung) (offiziell, typescriptlang.org/tsconfig/, 2026-06-03).
- **Project References / `composite: true` ab Monorepo / grossem mehrteiligem Build** — teilt das Projekt in unabhaengig baubare Sub-Projekte mit `.d.ts`-Grenzen, ermoeglicht echte inkrementelle Builds. Der KORREKTE Skalierungsweg fuer Monorepos — besser als `skipLibCheck` (das in Monorepos auch eigene Workspace-`.d.ts` ueberspringt und Fehler verdeckt) (offiziell, typescriptlang.org Handbook Project References, 2026-06-03).

### Don'ts

- **NICHT auf die floatenden 6.0-Defaults `target:es2025`/`module:esnext` vertrauen** — sie aendern Verhalten still zwischen TS-Versionen und brechen reproduzierbare Builds (offiziell, release-notes 6.0, 2026-06-03).
- **NICHT `moduleResolution: bundler` fuer eine echte Node-App oder Library verwenden** — fuehrt zu Imports, die im Bundler laufen, in Node aber crashen (extern, blog.andrewbran.ch, 2026-06-03).
- **NICHT `exactOptionalPropertyTypes` blind in eine grosse Bestands-Codebase werfen** — kann hunderte Fehler erzeugen; bei Migration als letzten Strenge-Schritt einfuehren (extern, dev.to, 2026-06-03).
- **NICHT `skipLibCheck` als Allheilmittel in Monorepos** — es ueberspringt auch eigene Workspace-Declarations; dort Project References nutzen (extern, totaltypescript.com / moonrepo, 2026-06-03).
- **NICHT `noUncheckedIndexedAccess` weglassen, nur weil es `strict` nicht enthaelt** — es ist die wichtigste fehlende Laufzeitsicherung (offiziell, typescriptlang.org/tsconfig/, 2026-06-03).

### Vollstaendiges Basis-tsconfig — (a) Node-24-App (mit tsc transpiliert)

```jsonc
{
  "compilerOptions": {
    // Module & Target (explizit, nicht TS-6.0-Defaults floaten lassen)
    "module": "nodenext",
    "moduleResolution": "nodenext",    // implizit durch module, der Klarheit halber explizit
    "target": "es2024",
    "lib": ["es2024"],
    "types": ["node"],                  // TS 6.0 Default ist [] -> sonst fehlt process/Buffer
    "moduleDetection": "force",

    // Strenge
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "noImplicitOverride": true,
    "noFallthroughCasesInSwitch": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,

    // Modul-Hygiene
    "verbatimModuleSyntax": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,

    // Build
    "rootDir": "./src",                 // TS 6.0 Default ist "." -> explizit setzen
    "outDir": "./dist",
    "sourceMap": true,
    "skipLibCheck": true
  },
  "include": ["src/**/*"]
}
```
(offiziell, release-notes 6.0 + tsconfig-Referenz, 2026-06-03; extern, totaltypescript.com/tsconfig-cheat-sheet, 2026-06-03)

### Vollstaendiges Basis-tsconfig — (b) Library

```jsonc
{
  "compilerOptions": {
    "module": "nodenext",
    "moduleResolution": "nodenext",
    "target": "es2024",
    "lib": ["es2024"],
    "moduleDetection": "force",

    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "noImplicitOverride": true,
    "noFallthroughCasesInSwitch": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,

    "verbatimModuleSyntax": true,
    "isolatedModules": true,
    "isolatedDeclarations": true,       // Library: explizite API-Rueckgabetypen, schnelle .d.ts
    "esModuleInterop": true,
    "forceConsistentCasingInFileNames": true,

    "rootDir": "./src",
    "outDir": "./dist",
    "declaration": true,                // .d.ts ausliefern (Pflicht fuer Library)
    "declarationMap": true,
    "sourceMap": true,
    "composite": true,                  // Project-References-faehig (Monorepo)
    "skipLibCheck": true
  },
  "include": ["src/**/*"]
}
```
Library-Unterschiede zur App: `declaration` + `declarationMap` (Konsumenten brauchen `.d.ts`), `composite` (Project References), `isolatedDeclarations` (erzwingt explizite Public-API-Typen). `nodenext` ist hier Pflicht, damit emittierte Module-Specifier auch in echtem Node funktionieren (offiziell, tsconfig-Referenz + Project References Handbook, 2026-06-03; extern, totaltypescript.com/tsconfig-cheat-sheet, 2026-06-03).

---

## B — Sauberes ESM-Setup fuer Node 24 + package.json

### Grundsatz: ESM-first 2026

- **DO** `"type": "module"` setzen — alle `.js`-Dateien laden dann als ESM. `.mjs` ist immer ESM, `.cjs` immer CommonJS (unabhaengig vom `type`-Feld). (offiziell, nodejs.org/api/packages.html, 2026-06-03)
- **DO** fuer NEUE Pakete ESM-only publizieren — das ist der einzige zuverlaessige Weg, den Dual-Package-Hazard ganz zu vermeiden (keine doppelten Modul-Instanzen, kein State-Split). (offiziell, packages.html "Dual CommonJS/ES module packages", 2026-06-03)
- **DON'T** ohne Not gleichzeitig `import`- und `require`-Eintrittspunkte mit eigenem Code anbieten — genau das erzeugt den Hazard. Wenn Dual noetig: gemeinsamen State in ein internes CJS-Modul auslagern, das beide Wrapper teilen. (offiziell, packages.html, 2026-06-03)

### `node:`-Prefix fuer Built-ins

- **DO** immer `import { resolve } from 'node:path'` statt `'path'` — macht explizit, dass ein Built-in gemeint ist (kein npm-Paket gleichen Namens), und ist die empfohlene Form. (offiziell, packages.html, 2026-06-03)

### `import.meta.dirname` / `import.meta.filename` (der moderne `__dirname`-Ersatz)

- **DO** `import.meta.dirname` und `import.meta.filename` verwenden (Node 20.11+ / 21.2+) statt des alten `fileURLToPath(import.meta.url)`-Workarounds. Sie entsprechen `path.dirname(...)` bzw. dem aufgeloesten Pfad mit Symlinks. (offiziell, nodejs.org/api/esm.html, 2026-06-03)
- **Caveat:** Nur fuer `file:`-Module vorhanden — bei Modulen ueber andere Protokolle (z.B. `https:`) sind sie `undefined`. (offiziell, esm.html, 2026-06-03)
- **Legacy-Fallback** (nur fuer Node <20.11): `const __dirname = dirname(fileURLToPath(import.meta.url))`. (offiziell, esm.html, 2026-06-03)

### JSON-Imports mit Import Attributes (Node 24 stabil)

- **DO** `import data from './data.json' with { type: 'json' }` — das Attribut `with { type: 'json' }` ist **Pflicht**. Es gibt nur einen `default`-Export, **keine** named exports. (offiziell, esm.html, 2026-06-03)
- **DON'T** named imports aus JSON erwarten (`import { foo } from './x.json'` schlaegt fehl). (offiziell, esm.html, 2026-06-03)

### Top-Level await — wann es Konsumenten bricht

- **DO** TLA in App-Code/ESM-only frei nutzen.
- **DON'T** TLA in Library-Code, der via `require()` aus CommonJS geladen werden koennen soll: `require(esm)` (stabil seit Node 24.15.0) laedt nur **synchrone** Modul-Graphen. Ein TLA irgendwo im Graphen macht das Modul fuer CJS-Konsumenten un-`require`-bar (`ERR_REQUIRE_ASYNC_MODULE`). (offiziell, v24.15.0 Release / esm.html, 2026-06-03)
- **Tipp:** Wer Dual-faehig bleiben will, kann die `"module-sync"`-Condition anbieten — sie deckt `import` UND `require()` ab, **verbietet aber top-level await**. (offiziell, packages.html, 2026-06-03)

### `.js`-Endungen in Imports (TypeScript `moduleResolution: nodenext`)

- **DO** relative Imports mit expliziter `.js`-Endung schreiben — auch wenn die Quelle `./utils.ts` heisst: `import { x } from './utils.js'`. ESM in Node loest fehlende Endungen NICHT auf; TypeScript spiegelt das mit `nodenext`/`node16` und verlangt die Runtime-Endung. (extern, typescriptlang.org moduleResolution, 2026-06-03)
- **DON'T** `from './utils'` ohne Endung — bricht zur Laufzeit (`ERR_MODULE_NOT_FOUND`). (offiziell, esm.html, 2026-06-03)

### `exports`-Map richtig (Reihenfolge ist signifikant!)

- **DO** Conditions von **spezifisch -> allgemein** ordnen. `"types"` IMMER zuerst, `"default"` IMMER zuletzt. Reihenfolge wird der Reihe nach geprueft — falsche Reihenfolge liefert die falsche Datei. (offiziell, packages.html, 2026-06-03)
- Kern-Conditions (offiziell unterstuetzt): `types` -> `node-addons` -> `node` -> `import` / `require` (gegenseitig exklusiv) -> `module-sync` -> `default`. (offiziell, packages.html, 2026-06-03)
- **DO** `"exports"` schliesst das Paket ab: nur explizit gelistete Subpaths sind erreichbar. `"./package.json": "./package.json"` exportieren, falls Tools sie brauchen. Subpath-Pattern via `*` (String-Replace). Mit `null` gezielt sperren. (offiziell, packages.html, 2026-06-03)
- **DON'T** sich auf `"main"`/`"module"` verlassen, wenn `"exports"` da ist — `"exports"` hat Vorrang; `"main"` ist nur Fallback fuer sehr alte Node-Versionen, `"module"` ist KEIN Node-Standard (nur Bundler-Konvention). (offiziell, packages.html, 2026-06-03)

### `engines`, `files`, `sideEffects`

- **DO** `"engines": { "node": ">=24" }` setzen, damit Konsumenten/Installer die Mindestversion kennen (der konkrete Wert ist eine Projekt-Entscheidung; Bedeutung des Felds offiziell). (offiziell, packages.html, 2026-06-03)
- **DO** `"files": ["dist/"]` — kontrolliert, was ins npm-Tarball wandert (kleinere Pakete, keine Quellen-Leaks). (offiziell, packages.html, 2026-06-03)
- **DO** `"sideEffects": false` (oder Datei-Liste), wenn das Paket nebenwirkungsfrei ist -> erlaubt Bundlern Tree-Shaking. (offiziell, packages.html, 2026-06-03)

### Vollstaendiges Beispiel — ESM-only (empfohlen)

```json
{
  "name": "my-package",
  "version": "1.0.0",
  "type": "module",
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "default": "./dist/index.js"
    },
    "./feature": {
      "types": "./dist/feature.d.ts",
      "default": "./dist/feature.js"
    },
    "./package.json": "./package.json"
  },
  "engines": { "node": ">=24" },
  "files": ["dist/"],
  "sideEffects": false
}
```

### Beispiel — Dual (nur wenn unvermeidbar)

```json
{
  "name": "my-package",
  "version": "1.0.0",
  "type": "module",
  "main": "./dist/index.cjs",
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs",
      "default": "./dist/index.mjs"
    }
  },
  "engines": { "node": ">=24" },
  "files": ["dist/"],
  "sideEffects": false
}
```

### Wann (noch) CJS sinnvoll ist — und wie man sauber bleibt

- Wenn Konsumenten zwingend `require()` ohne `require(esm)`-Support nutzen (Node <22) ODER ein Tool/Plugin-System nur CJS akzeptiert. (offiziell, v22-to-v24 Migration, 2026-06-03)
- **Sauber-Regeln dann:** explizite `.cjs`/`.mjs`-Endungen, gemeinsamen State in EIN internes Modul (Hazard-Schutz), `"module-sync"` statt getrennter `import`/`require`-Wrapper anbieten (ein Code-Pfad, sofern kein TLA noetig). (offiziell, packages.html, 2026-06-03)

---

## C — Typsichere Patterns ohne `any`

> Versions-Anker: Node v24.15.0, TypeScript 6.0.2. Fundament fuer alles: `"strict": true`, `"noUncheckedIndexedAccess": true`, `"exactOptionalPropertyTypes": true`. Ohne `strict` greifen die meisten Patterns hier nur halb.

### Do's / Don'ts (kompakt)

| Do | Don't |
|----|-------|
| `unknown` an allen Aussengrenzen (`JSON.parse`, `fetch().json()`, Drittanbieter) und dann narrowen/validieren | `any` als "geht-schon"-Typ — schaltet Typpruefung komplett ab und verbreitet sich (ein `any` infiziert die ganze Kette) |
| Discriminated Union als Kern-Pattern fuer "eins von mehreren" | Boolean-Flags + optionale Felder, die ungueltige Kombinationen erlauben |
| Exhaustiveness-Check mit `never` im `default` | Switch ohne `never` — neue Union-Member werden still vergessen |
| `satisfies` zur Validierung ohne Verbreiterung (Literale bleiben eng) | Type-Annotation `: T`, die `as const`-Literale wieder zu `string`/`number` verbreitert |
| Laufzeit-Validierung externer Daten (Zod/Valibot/ArkType) + Typ aus Schema ableiten | Glauben, ein TS-`interface` garantiere die Form zur Laufzeit (Typen sind zur Laufzeit weg) |
| `as`/`!` nur an einer einzigen, gekapselten Stelle (z. B. Validator-Constructor) | `as`/`!` streuen, um Compiler-Fehler "wegzudruecken" |
| User-defined Guards (`x is T`) / Assertion-Funktionen (`asserts x is T`) zentral definieren | Inline-Casts `(x as T).foo` an jeder Verwendungsstelle |

### `unknown` statt `any` an Aussengrenzen

`JSON.parse` und `Response.json()` liefern beide `any` — das ist die Haupt-Eintrittsstelle fuer Bugs. Den Wert sofort als `unknown` behandeln und vor der Nutzung narrowen oder validieren. `unknown` ist der typsichere Gegenpart zu `any`: alles ist `unknown` zuweisbar, aber `unknown` ist nichts zuweisbar ausser sich selbst — der Compiler **zwingt** zum Pruefen. (offiziell, TS Handbook — Narrowing, 2026-06-03)

```ts
async function loadUser(url: string): Promise<User> {
  const data: unknown = await (await fetch(url)).json(); // nicht any annehmen
  return UserSchema.parse(data); // narrowen/validieren bevor genutzt
}
```

### Type Narrowing / Type Guards

`typeof`, `in`, `instanceof`, Truthiness und Equality narrowen automatisch. Fuer eigene Formen: **User-defined Guard** (`pet is Fish`). Achtung: `typeof null === "object"`. (offiziell, TS Handbook — Narrowing, 2026-06-03)

```ts
type Fish = { swim: () => void };
type Bird = { fly: () => void };

function isFish(pet: Fish | Bird): pet is Fish {
  return "swim" in pet; // 'in' ist robuster als (pet as Fish).swim !== undefined
}

// Bonus: als Array-Filter typt es korrekt zu Fish[]
const onlyFish: Fish[] = zoo.filter(isFish);
```

**Assertion-Funktion** (`asserts`) — narrowt fuer den Rest des Scopes, statt einen Boolean zurueckzugeben. Ideal fuer "wirf, wenn falsch": (offiziell, TS Handbook — Assertion Signatures, TS 3.7+, 2026-06-03)

```ts
function assertIsString(v: unknown): asserts v is string {
  if (typeof v !== "string") throw new Error("Not a string");
}
assertIsString(x);
x.toUpperCase(); // x ist ab hier string
```

### Discriminated Union + Exhaustiveness mit `never` (Kern-Pattern)

Gemeinsame Literal-Property (`kind`) als Diskriminante -> TS narrowt jeden Branch automatisch. Der `never`-Default macht das Uebersehen neuer Member zu einem **Compile-Fehler**. (offiziell, TS Handbook — Narrowing / Discriminated Unions / Exhaustiveness, 2026-06-03)

```ts
interface Circle { kind: "circle"; radius: number; }
interface Square { kind: "square"; side: number; }
type Shape = Circle | Square;

function area(shape: Shape): number {
  switch (shape.kind) {
    case "circle": return Math.PI * shape.radius ** 2;
    case "square": return shape.side ** 2;
    default: {
      const _exhaustive: never = shape; // Fehler, wenn ein Member fehlt
      return _exhaustive;
    }
  }
}
```

### `satisfies` + `as const`

`satisfies` prueft, ob ein Ausdruck einen Typ erfuellt, **ohne** den abgeleiteten (engeren) Typ zu verbreitern. Eine Annotation `: T` wuerde Literale verlieren. Mit `as const` bleiben Werte als Literale erhalten. (offiziell, TS Handbook — 4.9 Release Notes `satisfies`, 2026-06-03)

```ts
type Color = "red" | "green" | "blue";
const palette = {
  primary: "red",
  accent:  "blue",
} satisfies Record<string, Color>;
// Vorteil: palette.primary ist Typ "red" (nicht string) UND auf Color geprueft
```

### Generics richtig

- **Constraints** (`<T extends ...>`) statt `any`-Parameter, damit man auf Eigenschaften zugreifen kann.
- **`infer`** in Conditional Types, um Teiltypen zu extrahieren (Basis von `ReturnType`, `Awaited`).
- **Generic statt Overload**, wenn der Rueckgabetyp vom Eingabetyp abhaengt (eine Signatur reicht).
- **`NoInfer<T>`** (TS 5.4+): blockiert, dass ein Parameter zur Inferenz herangezogen wird — erzwingt strengere Argumentpruefung. (offiziell, TS 5.4 Release Notes, 2026-06-03)

```ts
function createOption<C extends string>(
  colors: C[],
  fallback: NoInfer<C>, // fallback wird nicht zur Inferenz von C genutzt
) { /* ... */ }
createOption(["red", "green"], "blue"); // Fehler: "blue" nicht in C
```
Anti-Pattern: `<T>` deklarieren, aber nie in der Signatur verwenden (reine Deko) — oder Generics nutzen, wo eine Union/Overload klarer waere.

### Utility Types praktisch

`Pick` / `Omit` / `Partial` / `Required` / `Record` / `Readonly` / `ReturnType` / `Awaited` (TS 4.5+) decken die meisten Faelle ab — selbst-gebaute Mapped Types nur, wenn keiner passt (z. B. tiefes `DeepReadonly`, key-remapping per `as`). (offiziell, TS Handbook — Utility Types, 2026-06-03)

```ts
type User = { id: string; name: string; pw: string };
type PublicUser = Omit<User, "pw">;           // ohne sensibles Feld
type UserPatch  = Partial<Pick<User, "name">>; // optionales Update-DTO
type Unwrapped  = Awaited<ReturnType<typeof loadUser>>; // User
```

### Branded / Nominal Types fuer IDs

TS ist strukturell — `UserId` und `OrderId` als nackte `string` sind austauschbar und werden verwechselt. Ein Brand (per `unique symbol`, am robustesten gegen Kollisionen) macht sie nominal, mit **null Laufzeit-Overhead** (Brand existiert nur zur Compile-Zeit). (extern, oneuptime/nanamanu — Branded Types, 2026-06-03; offiziell, Microsoft/TypeScript PR #33038 als Hintergrund)

```ts
declare const brand: unique symbol;
type Brand<T, B extends string> = T & { readonly [brand]: B };

type UserId  = Brand<string, "UserId">;
type OrderId = Brand<string, "OrderId">;

const toUserId = (s: string) => s as UserId; // einziger legitimer Cast: im Constructor
function getUser(id: UserId) { /* ... */ }
// getUser(orderId) -> Compile-Fehler, obwohl beide string sind
```

### Schema-Validierung externer Daten zur Laufzeit (Empfehlung 2026)

**Wichtig:** TS-Typen sind zur Laufzeit geloescht — ein `interface` prueft eingehende API-/JSON-Daten **nicht**. An jeder Aussengrenze braucht es einen Laufzeit-Validator, und der Statik-Typ wird **aus dem Schema abgeleitet** (Single Source of Truth), nicht doppelt gepflegt.

Empfehlung 2026:
- **Zod 4** — Default fuer Node-APIs, tRPC, grosses Oekosystem (~20M weekly downloads), beste DX/Docs. (extern, zod.dev/basics, 2026-06-03 — autoritativ fuer Zod)
- **Valibot** — fuer Edge/Bundle-kritisch: bis ~90 % kleinerer Bundle (modulare Funktionen, tree-shakebar). (extern, valibot.dev/guides/comparison, 2026-06-03)
- **ArkType** — wenn rohe Validierungs-Performance entscheidend ist (oft mehrfach schneller als Zod), TS-aehnliche Syntax. (extern, pkgpulse.com/guides/zod-vs-arktype-2026, 2026-06-03)

Zod-Inferenz + sicheres Parsen (`safeParse` gibt eine Discriminated Union zurueck — kein try/catch noetig): (extern, zod.dev/basics, 2026-06-03)

```ts
import { z } from "zod";

const UserSchema = z.object({ id: z.string(), name: z.string(), xp: z.number() });
type User = z.infer<typeof UserSchema>; // Typ AUS dem Schema, nicht separat

const res = UserSchema.safeParse(unknownData);
if (!res.success) {
  console.error(res.error); // ZodError
} else {
  res.data.name; // vollstaendig typsicher
}
```

### `as`-Casts & non-null `!` minimieren

- **Legitim:** im Brand-Constructor, nach erfolgter Laufzeit-Validierung, bei `as const`, beim Verengen (nicht Erweitern) eines bereits passenden Typs.
- **Gefahr:** `as` erweitert/erzwingt einen Typ ohne Pruefung — der Compiler glaubt blind. `foo!` behauptet "nicht null", ohne es zu garantieren -> klassische `Cannot read properties of undefined`-Crashes zur Laufzeit. Stattdessen narrowen (`if (foo)`), `??`, oder optional chaining `?.`. (offiziell, TS Handbook — Everyday Types / Narrowing, 2026-06-03)

```ts
// Schlecht: erzwungen, ungeprueft
const u = JSON.parse(s) as User;        // Laufzeit-Form ungeprueft -> Luege
const name = config.user!.name;         // crasht, wenn user undefined

// Gut: geprueft / narrowed
const u = UserSchema.parse(JSON.parse(s));
const name = config.user?.name ?? "anon";
```

---

## D — Robuste async-Fehlerbehandlung & Promises

> Versions-Anker: Node v24.15.0, TypeScript 6.0.2. Strict-Mode vorausgesetzt (`"strict": true`).

### Do's

- **Jedes Promise behandeln**: `await`en, `return`en, mit `.catch()`/zweiarg-`.then()` versehen — oder bewusst mit `void` markieren. ESLint-Regel `no-floating-promises` (type-checked) erzwingt das. (extern, typescript-eslint.io/rules/no-floating-promises, 2025)
- **`process.on('unhandledRejection')` NUR fuer geordneten Shutdown** (Logging + `process.exitCode = 1` + Cleanup), nicht als Catch-all-Schlucker. Seit Node 15 beendet eine unbehandelte Rejection den Prozess per Default. (offiziell, nodejs.org/api/process, 2026-06-03)
- **`catch (e: unknown)`** + `instanceof`-Narrowing bevor auf `.message` zugegriffen wird. `useUnknownInCatchVariables` ist Teil von `--strict` (seit TS 4.4). (offiziell, typescriptlang.org/tsconfig/useUnknownInCatchVariables, 2026-06-03)
- **`Error.cause` (ES2022)** fuer Fehlerketten: `throw new Error("...", { cause: original })`. Custom-Error-Klassen + `instanceof` zur Klassifikation. (offiziell, developer.mozilla.org/.../Error/cause, 2026-06-03)
- **Result-/Either-Typ statt Exceptions** an API-/Domain-Grenzen, wo Fehler erwartbar sind (Validierung, IO mit bekannten Fehlern). Macht Fehler im Typ sichtbar und erzwingt Behandlung. Exceptions weiter fuer wirklich aussergewoehnliche/unerwartete Faelle. (extern, typescript.tv/best-practices/error-handling-with-result-types, 2025)
- **Richtigen Promise-Kombinator waehlen** (Entscheidungsbaum unten). (offiziell, developer.mozilla.org/.../Promise/allSettled, 2026-06-03)
- **Cancellation per AbortController/AbortSignal**: `AbortSignal.timeout(ms)` fuer Timeouts, `AbortSignal.any([...])` zum Kombinieren (z.B. User-Abbruch + Timeout). (offiziell, developer.mozilla.org/.../AbortSignal/timeout_static, 2026-06-03)
- **`await using` / `Symbol.asyncDispose`** (TS 5.2+, nativ in Node 24) fuer deterministisches async-Cleanup (DB-Connections, File-Handles, Locks) — ersetzt fehleranfaellige `try/finally`-Ketten. (offiziell, typescriptlang.org release-notes 5.2, 2026-06-03)
- **`AsyncLocalStorage`** (`node:async_hooks`) fuer Request-Kontext (z.B. Request-ID, User) ohne Prop-Drilling. (offiziell, nodejs.org/api/async_context, 2026-06-03)
- **Unabhaengige async-Calls parallelisieren** mit `Promise.all`, nicht sequenziell `await` im `for`-Loop.

### Don'ts

- Promise erstellen und nicht behandeln (floating) — fuehrt zu falscher Reihenfolge + verschluckten Rejections.
- `process.on('unhandledRejection')` als globalen Catch-all, der den Fehler nur loggt und weiterlaufen laesst — der Prozess ist danach in unbekanntem Zustand.
- `catch (e: any)` oder ungetyptes `e.message` ohne Narrowing.
- `Promise.all` fuer voneinander unabhaengige Tasks, bei denen Teil-Erfolge zaehlen — der erste Reject bricht alles ab (short-circuit). Dann `allSettled`.
- Eigene Timeout-Implementierung mit `setTimeout` + Flag, wenn `AbortSignal.timeout()` reicht.
- `await` in jeder Loop-Iteration, wenn die Calls parallel laufen koennten.

### Promise-Kombinator — Entscheidungsbaum

- **Alle muessen erfolgreich sein, alle Ergebnisse gebraucht** -> `Promise.all` (bricht beim ersten Reject ab, short-circuit).
- **Jedes Ergebnis einzeln gebraucht, egal ob ok/fehlgeschlagen** -> `Promise.allSettled` (rejected NIE, liefert `{status:'fulfilled',value}` / `{status:'rejected',reason}`).
- **Erstes settle (egal ob ok oder Fehler) zaehlt** -> `Promise.race` (z.B. "echte Operation vs. Timeout").
- **Erstes erfolgreiche reicht** -> `Promise.any` (rejected mit `AggregateError`, wenn ALLE fehlschlagen).

(offiziell, developer.mozilla.org/.../Promise/allSettled, 2026-06-03)

### Code: typsicherer Result-Typ

```ts
// Diskriminierte Union — Fehler ist im Typ sichtbar, kein throw
type Result<T, E = Error> =
  | { ok: true; value: T }
  | { ok: false; error: E };

const ok  = <T>(value: T): Result<T, never> => ({ ok: true, value });
const err = <E>(error: E): Result<never, E> => ({ ok: false, error });

async function parseConfig(raw: string): Promise<Result<Config, ConfigError>> {
  try {
    return ok(schema.parse(JSON.parse(raw)));
  } catch (e: unknown) {                      // useUnknownInCatchVariables
    const cause = e instanceof Error ? e : new Error(String(e));
    return err(new ConfigError("config invalid", { cause })); // Error.cause
  }
}

const r = await parseConfig(raw);
if (!r.ok) {                                  // Compiler erzwingt die Pruefung
  log.warn(r.error);                          // r.error: ConfigError
  return;
}
use(r.value);                                 // r.value: Config (narrowed)
```

### Code: fetch mit Timeout + kombiniertem Abbruch-Signal

```ts
async function getJson(url: string, userSignal?: AbortSignal): Promise<unknown> {
  // AbortSignal.timeout -> bricht nach 5s mit TimeoutError ab
  // AbortSignal.any     -> bricht ab, sobald EIN Signal feuert (Timeout ODER User)
  const signal = userSignal
    ? AbortSignal.any([userSignal, AbortSignal.timeout(5_000)])
    : AbortSignal.timeout(5_000);

  try {
    const res = await fetch(url, { signal });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (e: unknown) {
    if (e instanceof DOMException && e.name === "TimeoutError") {
      throw new Error(`timeout: ${url}`, { cause: e });
    }
    throw e;
  }
}
```
(offiziell, developer.mozilla.org/.../AbortSignal, 2026-06-03)

### Code: `await using` fuer deterministisches async-Cleanup

```ts
class DbConnection implements AsyncDisposable {
  static async open(): Promise<DbConnection> { /* ... */ return new DbConnection(); }
  async query(sql: string) { /* ... */ }
  async [Symbol.asyncDispose]() {            // wird automatisch awaited
    await this.close();
  }
  private async close() { /* ... */ }
}

async function run() {
  await using db = await DbConnection.open();
  await db.query("SELECT 1");
  // db wird am Block-Ende automatisch + awaited geschlossen — auch bei throw
}
```
(offiziell, typescriptlang.org release-notes 5.2, 2026-06-03; extern, TC39 proposal-async-explicit-resource-management Stage 3)

### Code: unhandledRejection NUR fuer geordneten Shutdown

```ts
// Last-Resort-Failsafe — NICHT die primaere Fehlerbehandlung.
process.on("unhandledRejection", (reason) => {
  log.fatal({ reason }, "unhandled rejection — shutting down");
  process.exitCode = 1;          // exitCode statt process.exit(): laufende IO darf auslaufen
  void gracefulShutdown();       // Server schliessen, Connections drainen
});
```
(offiziell, nodejs.org/api/process, 2026-06-03)

### Code: parallel statt sequenziell

```ts
// langsam: sequenziell
for (const id of ids) results.push(await fetchOne(id));

// parallel (alle muessen klappen)
const results = await Promise.all(ids.map(fetchOne));

// parallel, Teilfehler toleriert
const settled = await Promise.allSettled(ids.map(fetchOne));
const okValues = settled.filter(s => s.status === "fulfilled").map(s => s.value);
```

---

## E — Dependency-Hygiene & npm/Toolchain

> Versions-Anker: Node v24.15.0, npm 11.12.0, TypeScript 6.0.2, Bun 1.3.11.

### E1 — Lockfile-Disziplin & reproduzierbare Builds

**Do's**
- `package-lock.json` IMMER committen (auch bei Libraries). Quelle der Wahrheit fuer reproduzierbare Installs. (offiziell, npm-ci v11, 2026)
- In CI/Deploy/reproduzierbaren Builds `npm ci` statt `npm install`. `npm ci` garantiert: (1) erfordert vorhandenes Lockfile, (2) loescht `node_modules` vorab komplett, (3) **bricht mit Fehler ab**, wenn Lockfile und `package.json` divergieren (statt das Lockfile still zu aendern), (4) schreibt nie in `package.json`/Lockfile. (offiziell, npm-ci v11, 2026)

**Don'ts**
- Kein `npm install` in CI — es kann das Lockfile mutieren und Build-Drift erzeugen. (offiziell, npm-ci v11, 2026)
- `npm ci` NICHT zum Hinzufuegen einzelner Pakete nutzen — installiert nur ganze Projekte. (offiziell, npm-ci v11, 2026)

```bash
# CI / Docker / reproduzierbar:
npm ci
# Lokal Paket hinzufuegen (mutiert Lockfile bewusst):
npm install <pkg>
```

### E2 — Dependency-Felder sauber trennen

- `dependencies`: zur Laufzeit noetig. `devDependencies`: nur Build/Test/Dev (Consumer laden sie nicht mit). (offiziell, package-json v11, 2026)
- `@types/*` (DefinitelyTyped) gehoeren per Konvention in **`devDependencies`** — nur zur Compile-/Typecheck-Zeit gebraucht. (offiziell/extern, npm Docs / DefinitelyTyped-Konvention, 2026)
- `peerDependencies` fuer Plugins: Kompatibilitaet mit einem Host ausdruecken, ohne ihn selbst zu `require`-n. Seit npm v7 werden Peers automatisch mitinstalliert. (offiziell, package-json v11, 2026)
- `optionalDependencies`: darf fehlen ohne Install-Bruch — der eigene Code MUSS das Fehlen via try/catch abfangen. (offiziell, package-json v11, 2026)

**Sonderfall `@types/node`**: Der **Major MUSS dem Node-Major folgen** (Node 24 -> `@types/node@24`). Falscher Major erzeugt verwirrende Typfehler. (extern, DefinitelyTyped Discussion #69418, 2026)

```jsonc
"devDependencies": {
  "@types/node": "^24",   // == Node 24
  "typescript": "^6.0.2"
}
```

### E3 — Peer-Deps-Konflikte (ERESOLVE) richtig loesen

In dieser Reihenfolge:
1. Pakete auf kompatible Versionen heben — die stabilste Loesung. (extern, oneuptime/bobbyhadz, 2026)
2. Gezielt `overrides` in der Root-`package.json` setzen (nur Root wird ausgewertet). (offiziell, package-json v11, 2026)
3. `--legacy-peer-deps` nur als **temporaeres, chirurgisches** Unblock-Mittel fuer Dev-Only-Tools — danach via `overrides` sauber loesen. (extern, Plain-English/oneuptime, 2026)

```jsonc
"overrides": { "some-lib": { "react": "^18.2.0" } }
```

**Don'ts**
- `--force` ist "boese": ignoriert ALLE Konflikte/Warnungen, nicht nur Peers. Letztes Mittel. (extern, Khazi/Medium, 2026)
- NIEMALS `legacy-peer-deps=true` global in `.npmrc` — versteckt echte Inkompatibilitaeten, die spaeter als Runtime-Fehler auftauchen. (extern, Plain-English, 2026)

### E4 — Supply-Chain-Sicherheit 2026

- `npm audit` fuer bekannte Schwachstellen; `--audit-level=moderate` um nur ab gewuenschter Schwere zu failen. (offiziell, npm-audit v11, 2026)
- `npm audit signatures` verifiziert **Registry-Signaturen (ECDSA) und Provenance-Attestations** (Sigstore). (offiziell, npm-audit v11, 2026)
- Beim Publishen `npm publish --provenance` aus CI (GitHub Actions/OIDC): npm signiert via Sigstore, loggt ins Transparency-Ledger, bindet Source-Repo-URI + Commit-Hash. (offiziell/extern, npm Docs / GitHub Blog, 2026)
- Untrusted Installs mit `--ignore-scripts` (verhindert Lifecycle-Scripts). (offiziell, npm-audit v11, 2026)
- Minimale Abhaengigkeiten; Production-Deps eher exakt pinnen (`save-exact=true`). (offiziell/extern, npm .npmrc / Mondoo, 2026)

**Don't**: `npm audit fix --force` nicht blind — kann Major-Range-Bumps mit Breaking Changes einziehen. (offiziell, npm-audit v11, 2026)

```bash
npm audit signatures        # Provenance/Signaturen pruefen
npm ci --ignore-scripts     # untrusted Install ohne Lifecycle-Scripts
```

### E5 — Update-Strategie (kurz)

`npm outdated` fuer veraltete Pakete. Automatisierung via **Renovate** oder **Dependabot** (PR-basierte Bumps). (extern, npm Docs, 2026)

### E6 — Toolchain-Empfehlung 2026

- **Typecheck**: `tsc --noEmit` bleibt Goldstandard — esbuild/SWC/tsup typchecken bewusst NICHT (nur Transpile). Typecheck und Build trennen. (extern, PkgPulse/Leapcell, 2026)
- **Build**: Library -> `tsup` (esbuild-basiert, Zero-Config). App/Prod-Bundle -> `esbuild`/`tsup`; bessere Tree-Shaking -> `unbuild` (Rollup). (extern, PkgPulse, 2026)
- **Dev-Ausfuehrung**: `tsx` (esbuild) statt globalem `ts-node` — oder direkt `node file.ts` (Node 24, Abschnitt F). (extern, PkgPulse, 2026)
- **Test-Runner**: `node:test` (built-in, dependency-frei) wenn Minimal-Footprint zaehlt; sonst **Vitest** als Default (HMR-Watch, beste TS-Integration). Jest nur in bestehenden Setups. (offiziell/extern, Vitest Docs / PkgPulse, 2026)
- **Linter/Formatter**: **Biome** (Rust, ~25x schneller, ein Tool fuer Lint+Format) deckt ~80% der ESLint-Regeln. **ESLint+Prettier** weiter noetig fuer type-aware Regeln / Framework-Plugins / custom Rules. (extern, Biome / Better Stack, 2026)

### E7 — npx/tsc PATH-Falle

**Immer lokales `npx tsc`** (bzw. npm-Script) statt eines global installierten `tsc`. Global pinnt eine versionsfremde TS-Version, die nicht zum Projekt-`typescript` passt -> inkonsistente Compile-Ergebnisse. `npx` loest die lokale `node_modules/.bin`-Version auf. (extern, Konvention, 2026)

```jsonc
"scripts": { "typecheck": "tsc --noEmit", "build": "tsup" }
```

### E8 — `.npmrc`-Empfehlungen

```ini
# .npmrc (Projekt-Root)
save-exact=true       # exakte Versionen (keine ^-Ranges) — supply-chain-sicherer
engine-strict=true    # bricht ab, wenn Node/npm nicht zu "engines" passt
# NIEMALS: legacy-peer-deps=true (versteckt echte Konflikte)
```
(offiziell, npm .npmrc, 2026 — save-exact/engine-strict dokumentiert)

---

## F — Node 24 moderne APIs richtig nutzen + Bun-Interop

> Leitidee: **Built-ins statt Dependencies**, wo gut genug. Weniger Pakete = weniger ABI-Brueche, schnellerer Start, einfacheres Deployment.

### Native TypeScript-Ausfuehrung (Type-Stripping)

**Do:**
- `node file.ts` direkt ausfuehren — Node erkennt `.ts` automatisch, **kein Flag noetig**. Type-Stripping ist **stable seit v24.12.0**. (offiziell, nodejs.org Modules: TypeScript, Stand v24.14.1)
- Fuer Dev/Tests/Scripts nutzen — spart den Build-Step.
- Typecheck **separat**: `tsc --noEmit` im CI / pre-commit. Node prueft Typen NICHT.

**Don't:**
- Nicht annehmen `node file.ts` validiert Typen — es entfernt nur Annotations und fuehrt das uebrige JS aus. (offiziell, nodejs.org/learn/typescript/run-natively)
- Kein `enum`, keine `namespace` mit Runtime-Code, keine Parameter-Properties (`constructor(private x)`) bei reinem Stripping — die brauchen `--experimental-transform-types`. Besser: erasable-only TS (`type`, `interface`, `import type`). (offiziell, nodejs.org Modules: TypeScript)

### Built-in Test-Runner (`node:test` + `node:assert`)

**Do:** Tests mit `node --test` (Glob `*.test.ts`). `--watch` fuer Auto-Rerun, Coverage mit `--experimental-test-coverage`. Fuer kleine/mittlere Projekte, Libraries, CLIs: null Dependencies. (offiziell, nodejs.org Test runner v24)

**Don't:** Nicht zu `node:test` wechseln, wenn man **Snapshot-Tests oder reiches Mocking** braucht — dann Vitest/Jest. (extern, betterstack, 2025)

```ts
import { test } from 'node:test';
import assert from 'node:assert/strict';

test('add', () => {
  assert.equal(1 + 1, 2);
});
// Lauf:  node --test --experimental-test-coverage
```

### Globale Web-APIs statt Pakete

**Do:** Diese Globals direkt nutzen — keine Imports, keine Dependencies:
- `fetch` (stable, Undici 7 in Node 24) statt `node-fetch`/`axios` fuer einfache Calls.
- `AbortSignal.timeout(ms)` statt manuellem `setTimeout`+`AbortController`.
- `structuredClone(obj)` statt `lodash.clonedeep`.
- `crypto.randomUUID()` statt `uuid`; `crypto.subtle` fuer Hashing/HMAC.
- `URL`, `URLPattern` (jetzt global in Node 24), `Blob`, Web Streams. (offiziell, Node 24.0.0 Release)

```ts
const res = await fetch('https://api.example.com', {
  signal: AbortSignal.timeout(5000),
});
```

### CLIs: `node:util` `parseArgs`

**Do:** Fuer einfache CLIs `parseArgs` statt commander/yargs — null Dependencies, Typvalidierung beim Parsen. (offiziell, nodejs.org util parseArgs; extern, lirantal.com 2025-09-13)

```ts
import { parseArgs } from 'node:util';
const { values, positionals } = parseArgs({
  options: {
    file: { type: 'string', short: 'f' },
    verbose: { type: 'boolean', short: 'v' },
  },
  allowPositionals: true,
});
```

**Don't:** Bei komplexen Subcommand-Baeumen + Auto-Help bleibt commander sinnvoller — `parseArgs` ist absichtlich minimal.

### `--env-file`, `--watch`, `--permission`

**Do:**
- `node --env-file=.env app.ts` statt `dotenv` — nicht mehr experimentell. Mehrere `--env-file` moeglich. (offiziell, Command-line API; extern, LogRocket)
- `node --watch app.ts` statt nodemon.
- `node --permission --allow-fs-read=./data app.ts` fuer Sandboxing (FS, Child-Process via `--allow-child-process`, Worker via `--allow-worker`). (offiziell, nodejs.org Permissions v24)

**Don't:**
- `--watch` startet **nicht** neu, wenn nur die `.env`-Datei sich aendert (Limitierung). (offiziell, nodejs/node#54001)
- `--permission` ist scharf: Default-deny — alle Pfade explizit erlauben, sonst Crash.

### `node:sqlite` vs better-sqlite3 (Node 24)

**Do:**
- In Node 24 ist `node:sqlite` **noch experimentell** (`--experimental-sqlite`). RC-Status (1.2) erst ab v25.7.0. Fuer **Prototyping/leichte Apps** ok. (extern, zenn.dev/LogRocket + sqg.dev Benchmark)
- Fuer **Produktion** weiterhin `better-sqlite3` (schneller, erprobt) — wie im Beispielprojekt `mcp-code-search`.

### Bun-Interop (Beispielprojekt nutzt Bun)

**Do (portabel bleiben):**
- Web-Globals (`fetch`, `crypto`, `structuredClone`) laufen in **beiden** identisch — bevorzugen.
- `node:test` laeuft auch in Bun (Bun mappt `node:test`) -> portable Tests. (extern, bun.com/reference/node/test — autoritativ)
- SQLite hinter einem **eigenen Adapter** kapseln: `bun:sqlite` (Bun, schneller) bzw. `better-sqlite3`/`node:sqlite` (Node) je Runtime laden. APIs sind **nicht 1:1** kompatibel. (extern, oneuptime + dev.to Bun 1.2)

**Don't (bricht je Runtime):**
- `bun:sqlite` direkt importieren, wenn der Code auch unter Node laufen soll — Modul existiert in Node nicht.
- `tsconfig` mit `module: "Preserve"` / `moduleResolution: "bundler"` + extension-lose `.ts`-Imports erwarten, dass es unter `node file.ts` laeuft — das ist **Bun-Verhalten**, Node verlangt korrekte ESM-Spezifizierer/`.js`-Endungen. Entweder bewusst Bun-only oder Node-kompatible Imports. (extern, betterstack Bun-vs-ts-node + Bun docs)
- Native N-API-Module (`better-sqlite3`) blind in Bun erwarten — ABI-/Recompile-Probleme moeglich. (extern, oven-sh/bun#16050)

---

## G — Build, Publishing & Projektstruktur

> Versions-Anker: Node v24.15.0, TypeScript 6.0.2, npm 11.12.0. ESM-only Library, Stand Juni 2026.

### Do's

- **Trenne Type-Check vom Emit.** `tsc --noEmit` ist die EINZIGE Quelle der Wahrheit fuers Typechecking — kein Bundler ersetzt das. Build (JS + `.d.ts`) separat, in CI immer beide Schritte. (extern, pkgpulse, 2026)
- **App vs. Library entscheidet die Build-Strategie:**
  - **Library** -> `tsc --emitDeclarationOnly` fuer Typen + Bundler (tsup/tsdown/unbuild) fuer JS, ODER reines `tsc` bei kleinem Scope. (extern, tsup-Docs, 2026)
  - **App** -> den Framework-eigenen Bundler (Vite/esbuild/Next), kein Library-Tooling. (extern, pkgpulse, 2026)
- **ESM-only ist 2026 der Default fuer neue Libraries.** Node 22.12+/23+ kann `require()` von ESM nativ — CJS-Consumer brauchen kein dual build mehr. Spart den dual-package-hazard. (extern, 2ality Feb 2025 / esmodules.com 2026)
- **`"types"` MUSS die ERSTE Condition in jedem exports-Eintrag sein.** TS resolved Conditions der Reihe nach — steht `import`/`default` davor, findet TS keine Typen. (extern, esmodules.com, 2026)
- **`"declaration": true` + `"declarationMap": true`** fuer publizierte Libraries (declarationMap = "Go to Definition" landet im TS-Source). (offiziell, typescriptlang.org/tsconfig/declarationMap.html)
- **Veroeffentlichte Typen IMMER mit `attw` + `publint` pruefen — beide.** `publint` prueft package.json-Metadaten (exports/main/types gegen reale Dateien); `attw` prueft Consumer-Erfahrung unter node10/node16/bundler-Resolution (FalseESM/FalseCJS/masquerading types). Sie ueberschneiden sich nicht. (offiziell-Tool, publint Comparisons / attw README)
- **`isolatedDeclarations: true`** (TS 5.5+) fuer schnelle/parallele `.d.ts`-Emit in Monorepos — Deklarations-Generierung wird zum reinen Syntax-Stripping (3–15x, teils 145x). Erfordert explizite Typ-Annotationen an allen exports. (offiziell, TS 5.5 Release Notes; extern, void.ma 2026)
- **Monorepo: npm workspaces + TS project references.** Jedes Paket `"composite": true`; Root-tsconfig listet alle Pakete unter `references`; gebaut mit `tsc --build` (orchestriert Reihenfolge, skipped unveraenderte Pakete). (offiziell, typescriptlang.org Project References)
- **`files`-Feld in package.json (allowlist)** statt `.npmignore` (denylist) — nur `dist` publishen. **Immer `npm pack --dry-run` vor dem Publish** und die Dateiliste pruefen. (offiziell, npm-publish v11 / npm developers Docs)
- **`"type": "module"`, `"engines": { "node": ">=22.12" }`, `"sideEffects": false`** — `sideEffects: false` aktiviert aggressives Tree-Shaking bei Consumern. (extern, esmodules.com, 2026)
- **Provenance beim Publish:** `npm publish --provenance` in CI (cloud-hosted runner) — etabliert oeffentlich, woher das Paket gebaut wurde. (offiziell, Generating provenance — npm Docs)
- **Versionierung:** semver + `npm version patch|minor|major` (taggt + bumpt atomar). (offiziell, npm developers Docs)
- **Node Source-Maps:** `sourceMap: true` im Build lassen, aber `--enable-source-maps` in Produktion **bewusst** einsetzen — besseres Error-Reporting, aber spuerbarer Performance-Overhead bei grossen/vielen Maps. (offiziell, Node CLI Docs)

### Don'ts

- `moduleResolution: "node"` (classic) — fuer ESM `"NodeNext"`/`"node16"` (bzw. `"bundler"` nur bei App-Builds). (extern, esmodules.com, 2026)
- Extension-lose relative Imports in ESM (`from './utils'`) — bricht zur Laufzeit; immer `'./utils.js'`. (extern, 2ality, 2025)
- `.d.ts.map` (declarationMaps) ins npm-Tarball publishen — nur fuer Monorepo-DX, NICHT fuer Consumer. (extern, tsup #885)
- Sources/Tests/`tsconfig.json` ins Tarball — `files: ["dist"]` + `npm pack --dry-run` verhindert das. (offiziell, npm developers Docs)
- Auf "es kompiliert ja" vertrauen — ohne `attw`/`publint` bleiben FalseCJS/masquerading-Type-Fehler unentdeckt bis der Consumer sie meldet. (offiziell-Tool, attw README)

### Beispiel — exports-Map (ESM-only Library)

```json
{
  "name": "my-lib",
  "version": "1.0.0",
  "type": "module",
  "sideEffects": false,
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "default": "./dist/index.js"
    },
    "./package.json": "./package.json"
  },
  "files": ["dist"],
  "engines": { "node": ">=22.12" }
}
```
`types` zuerst, dann `default`. Reihenfolge ist load-bearing. (extern, esmodules.com, 2026)

### Beispiel — attw + publint als CI-Schritt

```jsonc
// package.json scripts
{
  "scripts": {
    "build": "tsc --noEmit && tsup src/index.ts --format esm --dts",
    "check:exports": "publint --strict && attw --pack ."
  }
}
```
```yaml
# GitHub Actions
- run: npm run build
- run: npx publint --strict
- run: npx @arethetypeswrong/cli --pack .   # exit != 0 bei Type-Resolution-Problemen
- run: npm publish --provenance --access public
```
`attw --pack .` packt mit `npm pack` und prueft das echte Tarball gegen alle Resolution-Modi; non-zero exit failed die CI. (offiziell-Tool, attw README / publint Rules)

### Beispiel — project references (Monorepo)

```jsonc
// packages/core/tsconfig.json
{
  "compilerOptions": {
    "composite": true,
    "declaration": true,
    "declarationMap": true,
    "isolatedDeclarations": true,
    "outDir": "./dist",
    "rootDir": "./src"
  },
  "include": ["src"]
}
```
```jsonc
// packages/api/tsconfig.json  (haengt von core ab)
{
  "compilerOptions": { "composite": true, "outDir": "./dist" },
  "references": [{ "path": "../core" }]
}
```
```jsonc
// tsconfig.json (Root — nur Orchestrierung, keine eigenen Dateien)
{
  "files": [],
  "references": [
    { "path": "./packages/core" },
    { "path": "./packages/api" }
  ]
}
```
Build mit `tsc --build` (bzw. `tsc -b`). `composite: true` ist Pflicht fuer referenzierte Projekte; ohne `--build` baut tsc Dependencies NICHT automatisch. (offiziell, typescriptlang.org Project References)

---

## 🔗 Kopplung zum Bug-Almanach (wechselseitige Bezugstabelle)

Best-Practices (diese Datei) <-> Bug-Almanach `~/proggs/bugs/web/typescript.md`. Die identische Tabelle
steht auch dort. So bleibt jede Best-Practice mit ihrer konkreten Bug-Loesung verlinkt (und umgekehrt).

| Best-Practice-Abschnitt (hier) | Zugehoeriger Bug-Almanach-Abschnitt (`bugs/web/typescript.md`) |
|--------------------------------|-----------------------------------------------------------|
| **A — Strikte tsconfig & Compiler-Strenge** | **A** (TS-6.0 geaenderte Defaults), **B** (Deprecations), **D24-D39** (Typ-Fallen / fehlende Strict-Flags / skipLibCheck) |
| **B — Sauberes ESM-Setup + package.json** | **C** (ESM vs CommonJS, #15-23), **G69** (require(esm) default), **J90** (exports-Reihenfolge) |
| **C — Typsichere Patterns ohne any** | **D** (Typ-Fallen #24-39), **J91** (never-Exhaustiveness), **J92** (typeof null), **J93** (Branded IDs) |
| **D — Robuste async-Fehlerbehandlung** | **E** (Async/Promises #40-52), **J94** (await using Runtime) |
| **E — Dependency-Hygiene & npm/Toolchain** | **F** (npm/Dependencies #53-68), **J104** (--provenance bei fail) |
| **F — Node 24 APIs + Bun-Interop** | **G** (Node-24-APIs #69-74), **H** (Bun #75-83), **J95-J98, J105** (Type-Stripping, --watch/.env, node:sqlite, C++20) |
| **G — Build, Publishing & Projektstruktur** | **A/B** (tsconfig), **C20** (exports-Kapselung), **J99-J103** (source-maps, .npmignore, isolatedDeclarations, composite, tsup) |

---

## Pflicht-Checkliste vor TypeScript-/Node-Arbeit (Best-Practices-Seite)

- [ ] **tsconfig strikt?** `strict` + `noUncheckedIndexedAccess` + `exactOptionalPropertyTypes` EXPLIZIT; `module`/`target`/`types`/`rootDir` festgenagelt (nicht TS-6.0-Defaults floaten lassen)? `moduleResolution: nodenext` (Node) bzw. `bundler` nur bei Bundler-Emit? (→ A)
- [ ] **ESM sauber?** `"type": "module"`, relative Imports mit `.js`, `import.meta.dirname` statt `__dirname`? `exports`-Map mit `types` zuerst, `default` zuletzt? Fuer neue Pakete ESM-only? (→ B/G)
- [ ] **Aussengrenzen typsicher?** `unknown` statt `any`, Laufzeit-Validierung (Zod/Valibot/ArkType) + Typ aus Schema? Discriminated Union mit `never`-Default? Kein gestreutes `as`/`!`? (→ C)
- [ ] **Async robust?** `no-floating-promises` aktiv? `Promise.allSettled` wo Teilfehler zaehlen? `AbortSignal.timeout()` fuer Timeouts? `await using` fuers Cleanup? `unhandledRejection` nur fuer Shutdown? (→ D)
- [ ] **Deps gepflegt?** `package-lock.json` committed + `npm ci` in CI? `@types/node`-Major == Node-Major (24)? `overrides` statt `--force`? `save-exact`? (→ E)
- [ ] **Built-ins genutzt?** `node:test`, `fetch`, `AbortSignal.timeout`, `crypto.randomUUID`, `--env-file`, `parseArgs` statt Pakete? `node file.ts` nur mit separatem `tsc --noEmit`? (→ F)
- [ ] **Build/Publish verifiziert?** `tsc --noEmit` als eigener Typecheck-Schritt? `attw` + `publint` in CI? `files`-Allowlist + `npm pack --dry-run`? `npm publish --provenance`? (→ G)

---

*Neue belegte Best-Practices hier ergaenzen (mit Quelle + Datum + `offiziell`/`extern`). Bei jedem neu erlebten Bug zusaetzlich `bugs/web/typescript.md` ergaenzen und die Bezugstabelle synchron halten. Bei Versionssprung (Node 26, TS 7.0 "Corsa") Re-Check von A (tsconfig-Defaults) und F (Node-APIs).*
