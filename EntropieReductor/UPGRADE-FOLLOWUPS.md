# Upgrade-Followups — Entropie Reductor

> Erstellt 2026-05-10 nach Tooling-Upgrade-Sprint (Commits #2167-#2170).
> Diese Datei sammelt **Aufraeum-Aufgaben** die durch den Sprung von AGP 8.7.3
> auf 8.10.0 + Compose BOM 2025.01.01 auf 2026.03.00 sichtbar geworden sind.
>
> Status der App: lauft sauber, keine Bugs. Diese Aufgaben sind alle
> **Cleanup**, kein Bug. Reihenfolge nach **Aufwand-zu-Nutzen** sortiert,
> innerhalb einer Kategorie nach Datei.

---

## Tooling-Stand (Stand 2026-05-10 nach Upgrade-Sprint)

| Komponente | Version |
|------------|---------|
| AGP | 8.10.0 |
| Gradle | 8.11.1 |
| Kotlin | 2.1.0 |
| KSP | 2.1.0-1.0.29 |
| Compose BOM | 2026.03.00 |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 28 |
| versionName | 0.9.17 (versionCode 63) |

---

## Kategorie 1 — Icons.Outlined.* → AutoMirrored.Outlined.* (5 Stellen)

**Was**: Material-Icons mit "Richtung" (ArrowBack, DirectionsBike, DirectionsRun)
sollen die `AutoMirrored`-Variante verwenden, damit sie in RTL-Sprachen (arabisch,
hebraeisch) korrekt gespiegelt werden.

**Aufwand**: ca. 15 Min — pure Such-und-Ersetz-Aufgabe.

**Risiko**: minimal — APIs sind binaerkompatibel, nur Import + Bezeichner aendert sich.

**Stellen** (aus Compiler-Warnings):

| Datei | Zeile | Aktuell | Soll |
|-------|-------|---------|------|
| `app/src/main/java/de/frank/entropyreducer/presentation/insights/InsightBoardScreen.kt` | 93 | `Icons.Outlined.ArrowBack` | `Icons.AutoMirrored.Outlined.ArrowBack` |
| `app/src/main/java/de/frank/entropyreducer/presentation/insights/RepertoireScreen.kt` | 43 | `Icons.Outlined.ArrowBack` | `Icons.AutoMirrored.Outlined.ArrowBack` |
| `app/src/main/java/de/frank/entropyreducer/presentation/experimentcalendar/ExperimentCalendarScreen.kt` | 88 | `Icons.Outlined.ArrowBack` | `Icons.AutoMirrored.Outlined.ArrowBack` |
| `app/src/main/java/de/frank/entropyreducer/presentation/dashboard4/WorkoutCards.kt` | 433 | `Icons.Outlined.DirectionsBike` | `Icons.AutoMirrored.Outlined.DirectionsBike` |
| `app/src/main/java/de/frank/entropyreducer/presentation/dashboard4/WorkoutCards.kt` | 438 | `Icons.Outlined.DirectionsRun` | `Icons.AutoMirrored.Outlined.DirectionsRun` |

---

## Kategorie 2 — GoogleSignInAccount → Credential Manager Migration (4 Stellen)

**Was**: Die komplette `com.google.android.gms.auth.api.signin.*`-Bibliothek
ist deprecated. Google empfiehlt seit 2024 die Migration auf den **Credential
Manager** (`androidx.credentials:credentials` + `androidx.credentials:
credentials-play-services-auth` + `com.google.android.libraries.identity.
googleid:googleid`). Beide sind bereits in `libs.versions.toml` als Dependency
vorhanden — die Migration kann also schrittweise erfolgen.

**Aufwand**: ca. 2-3 Stunden — echte Code-Migration, OAuth-Flow muss nachher
manuell auf dem Geraet getestet werden (Google-Account-Login fuer Drive +
Calendar).

**Risiko**: mittel-hoch — wenn die Migration fehlschlaegt, kann sich Frank
nicht mehr bei Drive / Calendar einloggen. Daher: **eigene Session, eigener
Branch, gruendliche Verifikation auf dem Fold 6**.

**Stellen** (aus Compiler-Warnings):

| Datei | Zeile | Was |
|-------|-------|-----|
| `app/src/main/java/de/frank/entropyreducer/presentation/settings/SettingsViewModels.kt` | 6 | Import `GoogleSignInAccount` |
| `app/src/main/java/de/frank/entropyreducer/presentation/settings/SettingsViewModels.kt` | 352 | Verwendung `GoogleSignInAccount` |
| `app/src/main/java/de/frank/entropyreducer/presentation/settings/api/OAuthViewModel.kt` | 6 | Import `GoogleSignInAccount` |
| `app/src/main/java/de/frank/entropyreducer/presentation/settings/api/OAuthViewModel.kt` | 137 | Verwendung `GoogleSignInAccount` |

**Migrations-Strategie (Skizze)**:
1. Neuer Branch `feature/credential-manager-migration`.
2. `GoogleSignInClient`/`GoogleSignInAccount` durch `CredentialManager` +
   `GetCredentialRequest` mit `GetGoogleIdOption` ersetzen.
3. OAuth-Token-Holder ueber `CredentialManager.getCredential()` holen.
4. Auf Geraet testen: Drive-Backup-Login + Drive-Restore + Calendar-Sign-In.
5. Erst danach mergen.

**Quelle**: [Sign in with Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg)

---

## Kategorie 3 — statusBarColor/navigationBarColor → enableEdgeToEdge-only (2 Stellen)

**Was**: Ab Android 15 (API 35) ist Edge-to-Edge fuer Apps targetSdk 35+
Pflicht. Die direkten `Window.statusBarColor` / `Window.navigationBarColor`-
Setter sind deprecated. Stattdessen soll `enableEdgeToEdge()` mit
`SystemBarStyle.dark()` / `SystemBarStyle.light()` verwendet werden.

**Aufwand**: ca. 30 Min — Theme.kt schreibt direkt auf die Window-Properties,
das muss durch `EdgeToEdge`-API ersetzt werden.

**Risiko**: niedrig — `enableEdgeToEdge()` wird in MainActivity.onCreate
schon aufgerufen, das Theme.kt-Stueck ist redundant geworden.

**Stellen**:

| Datei | Zeile | Was |
|-------|-------|-----|
| `app/src/main/java/de/frank/entropyreducer/presentation/theme/Theme.kt` | 105 | `window.statusBarColor = ...` |
| `app/src/main/java/de/frank/entropyreducer/presentation/theme/Theme.kt` | 106 | `window.navigationBarColor = ...` |

**Strategie**: Pruefen, ob die Theme-Stelle ueberhaupt noch noetig ist (sehr
wahrscheinlich nicht, weil MainActivity schon `enableEdgeToEdge()` aufruft).
Falls nicht: einfach loeschen. Falls doch: durch `enableEdgeToEdge(
SystemBarStyle.auto(...), SystemBarStyle.auto(...))` ersetzen.

**Quelle**: [Display content edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge)

---

## Kategorie 4 — Always-True-Bedingungen pruefen (2 Stellen)

**Was**: Der neuere Kotlin-Compiler erkennt jetzt zwei Stellen wo eine
Bedingung **immer** `true` ist — also entweder dead code oder
Code-Smell. Beide muessen einzeln nachgesehen werden, ob die Bedingung
wirklich redundant ist oder ob sie auf einer falschen Annahme beruht.

**Aufwand**: ca. 20 Min — pruefen + entweder simplifizieren oder als
defensive Sicherung mit Kommentar erlaeutern.

**Risiko**: niedrig — das sind nur Compiler-Hinweise, keine Runtime-Bugs.

**Stellen**:

| Datei | Zeile | Hinweis |
|-------|-------|---------|
| `app/src/main/java/de/frank/entropyreducer/presentation/dashboard4/BiomarkerViewModel.kt` | 571 | "Check for instance is always 'true'" |
| `app/src/main/java/de/frank/entropyreducer/presentation/dashboard4/HealthConnectDetailScreen.kt` | 451 | "Condition is always 'true'" |

**Strategie**: Kontext lesen, entscheiden ob simplifizieren (Bedingung
weg) oder Kommentar erweitern (defensive Sicherung weil API-Contract
es theoretisch erlaubt aber praktisch nie auftritt).

---

## Kategorie 5 — AutoboxingStateCreation (2 Stellen, Performance-Hint)

**Was**: Lint empfiehlt `mutableFloatStateOf(0f)` statt `mutableStateOf<Float>(0f)`
— das vermeidet Boxing/Unboxing pro Recompose. Performance-Hinweis.

**Aufwand**: ca. 5 Min — pure Such-und-Ersetz-Aufgabe.

**Risiko**: minimal.

**Stellen**:

| Datei | Zeile | Aktuell | Soll |
|-------|-------|---------|------|
| `app/src/main/java/de/frank/entropyreducer/presentation/experimentcalendar/ExperimentCalendarScreen.kt` | 540 | `mutableStateOf<Float>(...)` | `mutableFloatStateOf(...)` |
| `app/src/main/java/de/frank/entropyreducer/presentation/insights/InsightBoardScreen.kt` | 340 | `mutableStateOf<Float>(...)` | `mutableFloatStateOf(...)` |

---

## Kategorie 6 — UnusedResources (124 Stellen, niedrige Prio)

**Was**: 124 ungenutzte Farb-Konstanten und sonstige Resources. Lint
zaehlt diese als "could be removed". Kein Bug, kein Risiko — nur
Cruft im Resources-Ordner.

**Aufwand**: 1-2 Stunden wenn manuell, oder 5 Min mit `./gradlew
removeUnusedResources` (aber: Vorsicht, kann zu viel loeschen wenn
Resources nur zur Laufzeit per `getIdentifier()` referenziert werden —
in unserer App ist das hoffentlich nicht der Fall).

**Risiko**: niedrig — wenn manuell, fast null. Wenn automatisch,
muss man jeden Match ansehen.

**Strategie**: Niedrigste Prio. Erst wenn alle anderen Kategorien
erledigt sind, wenn ueberhaupt.

**Liste der Top-Kandidaten**: siehe `app/build/reports/lint-results-debug.xml`
(grep nach `id="UnusedResources"`).

---

## Kategorie 7 — GradleDependency (66 Stellen, niedrige Prio)

**Was**: 66 Hinweise auf neuere Versionen einzelner Dependencies
(Kotlin, Hilt, Retrofit, Room, etc.). Diese werden mit dem Compose-BOM
NICHT mitgezogen weil sie ausserhalb des BOMs sind.

**Aufwand**: 1-3 Stunden — jeder Bump muss einzeln getestet werden,
einige (Hilt 2.55→2.x, Kotlin 2.1.0→2.x) haben moeglicherweise
Breaking-Changes.

**Risiko**: variiert pro Library — Kotlin-Bump ist riskant
(KSP-Compatibility), Retrofit/OkHttp/Room sind meist additiv.

**Strategie**: Eigener Sprint, idealerweise zusammen mit
BestJournalAndroid (Pattern wiederverwenden).

---

## Naechster Schritt fuer eine separate Session

Empfohlene Reihenfolge wenn Frank Zeit fuer Cleanup hat:

1. **Kategorie 1** (Icons-AutoMirrored) — schnell, sichtbar in der App.
2. **Kategorie 3** (statusBarColor) — einmalig fixen, Edge-to-Edge wird sauberer.
3. **Kategorie 5** (AutoboxingStateCreation) — fixiert subtil.
4. **Kategorie 4** (Always-True-Bedingungen) — Code-Hygiene.
5. **Kategorie 2** (Credential Manager) — eigene Session mit Verifikation.
6. **Kategorie 7** (GradleDependency) — eigener Sprint, gemeinsam mit BJA.
7. **Kategorie 6** (UnusedResources) — niedrigste Prio, optional.

---

## Nicht in dieser Liste enthalten (bereits abgeschlossen)

- TrustAllX509TrustManager-False-Positives → in `lint-baseline.xml` ausgeblendet (Commit #2167).
- AGP, Compose BOM, targetSdk Updates → durchgefuehrt (Commits #2167-#2170).
- 3 echte Bugfixes (Cancellation, Stale-Window, Doppel-Trigger-Race) → in
  `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` dokumentiert.

---

## ⚠️ PFLICHT für die nächste WINDOWS-Session: Debug-Keystore angleichen (2026-06-12)

**Was passiert ist:** Franks Handy hatte EntropieReductor 0.12.6, signiert mit dem
Windows-Debug-Keystore (Cert `A7:1F:7E:53…`, CN=Android Debug, C=DE). Der Mac nutzt
`~/SK/EntropieReductor/entropiereductor.debug.keystore` (Cert `BC:5F:72:42…`) — die
beiden "debug-shared"-Keystores waren NIE identisch. Folge: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
beim Mac-Update. Gelöst per Uninstall + Neuinstallation (Frank hatte Drive-Backup).

**Aktueller Zustand:** Die App auf dem Handy ist jetzt mit dem **Mac-Keystore** signiert
(`BC:5F:72:42:09:5F:65:52:28:A8:8E:2B:AF:4D:DE:C8:F8:EE:E6:58:CF:EF:BC:95:20:5E:47:84:98:B6:23:05`).

**Pflicht-Schritte auf Windows (BEVOR dort gebaut+installiert wird):**
1. Mac-Keystore nach Windows kopieren (z.B. via Google Drive/USB):
   Mac: `~/SK/EntropieReductor/entropiereductor.debug.keystore`
   → Windows: `C:\Users\barwa\SK\EntropieReductor\entropiereductor.debug.keystore` (ÜBERSCHREIBEN)
2. Verifizieren: `keytool -list -keystore <pfad> -storepass android` → Fingerprint muss mit `BC:5F:72:42…` beginnen.
3. Erst dann `gradlew installDebug` — sonst erneut INSTALL_FAILED + Datenverlust-Risiko.

Almanach-Eintrag: `bugs/android-build/gradle.md` §13.
