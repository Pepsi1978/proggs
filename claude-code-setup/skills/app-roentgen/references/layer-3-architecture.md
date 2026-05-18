# Schicht 3 — Architektur-Inventar: Wie ist die App gebaut?

## Zweck

Schicht 3 extrahiert das Skelett der App: ViewModels (Feature-Logik-Cluster), Repositories (Datenquellen), UseCases (Geschaefts-Operationen), Hilt-Module (Abhaengigkeits-Graph), Room-Entities (persistente Datentypen), Workers (Background-Jobs). Das ist die strukturelle Zwischenschicht zwischen Daten und UI.

**Coverage-Beitrag: ~30 Prozent fuer Architektur, ~80 Prozent fuer "was kann die App technisch tun"**

> **FIX Z1 (Audit 9) — Kotlin + Java:** Die Patterns hier zeigen aus Lesbarkeitsgruenden nur `--include='*.kt'` bzw. `--type kotlin`. Bei Apps mit Java-Anteilen (Legacy-Modulen, alten SDKs wie Firebase-Java, RevenueCat-Java) MUSS `--include='*.java'` bzw. `--type java` ergaenzt werden — sonst werden Java-ViewModels, Java-Repositories, Java-Workers stillschweigend uebersehen. Reine Kotlin-Apps koennen den Java-Filter weglassen.

> **Optional fuer maximal vollstaendiges Composable-Inventar (Goldstandard 2026)**: Wenn das Projekt baubar ist, ergaenzend `references/layer-3b-compose-compiler-reports.md` einsetzen. Compose Compiler Reports liefern nachweislich alle `@Composable`-Funktionen direkt vom Compiler — die grep-Patterns hier sind robust ohne Build, aber nicht garantiert 100% vollstaendig bei dynamisch generierten Composables.

## 3.1 ViewModels — Ein VM = Ein Feature-Cluster

```bash
grep -rln '@HiltViewModel' --include='*.kt' . | sort
grep -rn 'class.*ViewModel\(' --include='*.kt' . | grep -v 'test/' | sort
```

Jeder ViewModel ist typischerweise einem Bildschirm zugeordnet (DashboardViewModel → DashboardScreen). Pro VM dokumentieren:
- Klassenname
- Zugeordneter Bildschirm
- Konstruktor-Dependencies (was injiziert wird)
- StateFlow/MutableStateFlow-Felder (= UI-State)
- Public-Funktionen (= moegliche Aktionen die der Bildschirm ausloesen kann)

Beispiel:
```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val billingManager: BillingManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun onNewEntryClicked() { ... }       // Aktion 1
    fun onExportClicked() { ... }          // Aktion 2
    fun onPaywallTriggered() { ... }       // Aktion 3
}
```

Das ergibt im Audit:
- Feature: Dashboard
- Aktionen: NewEntry, Export, PaywallTrigger
- Datenquellen: JournalRepository, BillingManager, AnalyticsTracker

## 3.2 Repositories — Datenquellen

```bash
grep -rln 'class.*Repository\|interface.*Repository' --include='*.kt' . | sort
```

Pro Repository dokumentieren:
- Welche Datenquellen werden orchestriert (Room, Retrofit, DataStore, FirebaseDb)
- Welche Entities/DTOs werden gelesen/geschrieben
- Welche Caching-Strategie (NetworkBoundResource, StateFlow-Cache, etc.)

Typische Repositories einer Journal-App:
- JournalRepository (Eintraege, Tags, Kategorien)
- UserRepository (Auth, Profil)
- SubscriptionRepository (Premium-Status)
- SettingsRepository (DataStore-Wrapper)
- BackupRepository (Cloud-Backup)

## 3.3 UseCases — Geschaefts-Operationen

```bash
grep -rln 'class.*UseCase\|class.*Interactor' --include='*.kt' . | sort
```

UseCases sind atomare Geschaefts-Operationen. Bei sauberer Architektur ein UseCase pro Aktion:
- CreateEntryUseCase
- UpdateEntryUseCase
- DeleteEntryUseCase
- ExportEntriesAsPdfUseCase
- AnalyzeEntryWithAiUseCase
- GenerateRetroSummaryUseCase
- StartTrialUseCase
- RestorePurchasesUseCase

Pro UseCase dokumentieren:
- Was tut er
- Welche Daten benoetigt er
- Welche Fehlerzustaende kann er ausloesen
- Wird er von welchen ViewModels aufgerufen

## 3.4 Hilt-Module — Der Abhaengigkeits-Graph

```bash
grep -rln '@Module' --include='*.kt' . | sort
grep -rn '@Provides\|@Binds' --include='*.kt' . | head -100
```

Pro Module dokumentieren:
- Module-Name
- @InstallIn(...) Component (SingletonComponent, ActivityComponent, ViewModelComponent)
- Welche Klassen werden bereitgestellt
- Sind Konstanten/Konfigurationen drin (z.B. API-Keys, Base-URLs)

Typische Module:
- NetworkModule (Retrofit, OkHttp, API-Services)
- DatabaseModule (Room-Database, DAOs)
- DataStoreModule (DataStore-Instanzen)
- FirebaseModule (FirebaseAuth, Firestore, RemoteConfig)
- BillingModule (BillingManager, BillingClient)
- UseCaseModule oder direkt @Inject im Konstruktor

**Audit-Hinweis:** Hardcodierte API-Keys oder Secrets in Modulen sind ein Sicherheits-Befund.

## 3.5 Room-Entities — Persistente Datentypen

```bash
grep -rln '@Entity\b' --include='*.kt' . | sort
grep -rln '@Database\b' --include='*.kt' . | sort
grep -rln '@Dao\b' --include='*.kt' . | sort
```

Pro Entity dokumentieren:
- Klassenname
- Tabellen-Name
- Felder (welche Daten gespeichert werden)
- Verschluesselung? (SQLCipher / EncryptedRoom)
- Foreign-Keys / Relations

Pro DAO dokumentieren:
- Welche Tabelle
- @Query-Methoden (= moegliche Lese-Operationen)
- @Insert/@Update/@Delete-Methoden (= Schreib-Operationen)
- Welche Repository-Methoden nutzen es

**Datenschutz-Aspekt:** Entitaeten mit personenbezogenen Daten (Name, Email, Standort, Gesundheit, Finanzdaten) MUSS in der Datenschutzerklaerung erwaehnt werden — pruefen!

## 3.6 Workers — Background-Jobs

```bash
grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' . | sort
grep -rn 'OneTimeWorkRequest\|PeriodicWorkRequest' --include='*.kt' .
```

Pro Worker dokumentieren:
- Was er tut (Sync, Reminder, Backup, Analytics-Upload)
- Wie wird er getriggert (OneTime, Periodic, Constraints)
- In welcher Worker-Tag-Gruppe
- Welche Constraints (Network, Charging, Battery)

Beispiele:
- DailyReminderWorker (Periodic, taeglich)
- CloudBackupWorker (OneTime nach Eintrags-Aenderung)
- AnalyticsFlushWorker (Periodic, alle 6h)
- ExportToPdfWorker (OneTime mit Notification)

## 3.7 Sealed Classes / Sealed Interfaces — State-Maschinen

```bash
grep -rn 'sealed class\|sealed interface' --include='*.kt' . | grep -E 'State|Event|Effect|Result|Action' | head -50
```

Sealed Classes sind oft die State-Definitionen einzelner Bildschirme:
- DashboardUiState (Loading, Success, Error, Empty)
- PaywallState (LoadingProducts, ShowingProducts, Purchasing, Success, Error)
- AuthState (LoggedOut, LoggingIn, LoggedIn, Error)

Pro Sealed dokumentieren:
- Klassenname
- Alle Subklassen/Subobjects
- Welche UI/VM nutzt sie

**Wichtig fuer den Audit:** Aus den Sealed States laesst sich die State-Machine jedes Bildschirms rekonstruieren — wesentlich fuer Schicht 4.

## 3.8 Application-Klasse

```bash
grep -rn ': Application()\|@HiltAndroidApp' --include='*.kt' .
```

Die Application-Klasse ist der globale Init-Punkt. Hier werden oft initialisiert:
- Firebase
- Crashlytics
- Analytics
- Workmanager
- Timber/Logger
- Custom-Initializers

Aus der `onCreate()` der Application-Klasse laesst sich ablesen welche Cross-Cutting-Concerns aktiv sind.

## 3.9 Generierte Code-Verzeichnisse pruefen

Bei modernen Apps gibt es generierten Code (KSP, Hilt, Room). Diese Verzeichnisse zeigen final was instanziiert wird:

```bash
find . -path '*/build/generated/*' -name '*.kt' -not -path '*/test/*' | head -20
```

Besonders wichtig:
- `build/generated/ksp/release/kotlin/...` — KSP-generierte Daos, Hilt-Komponenten
- `build/generated/source/kapt/release/` — KAPT-generierte (legacy)

## Output-Format fuer Schicht 3

```markdown
## Schicht 3 — Architektur-Inventar

### ViewModels (N gefunden)

| ViewModel | Bildschirm | Aktionen (Public-Funktionen) | Dependencies |
|-----------|-----------|----------------------------|--------------|
| DashboardViewModel | DashboardScreen | onNewEntryClicked, onExportClicked, onPaywallTriggered | JournalRepository, BillingManager |
| ... | ... | ... | ... |

### Repositories (N gefunden)

| Repository | Datenquellen | Hauptmethoden |
|-----------|-------------|--------------|
| JournalRepository | Room (JournalDao), Firebase Firestore | getEntries(), saveEntry(), syncToCloud() |
| ... | ... | ... |

### UseCases (N gefunden)

| UseCase | Zweck | Aufgerufen von |
|---------|-------|---------------|
| AnalyzeEntryWithAiUseCase | Sendet Eintrag an Gemini-API, gibt Analyse zurueck | EntryDetailViewModel |
| ExportEntriesAsPdfUseCase | Generiert PDF aus allen Eintraegen | DashboardViewModel |
| ... | ... | ... |

### Hilt-Module (N gefunden)

| Modul | Component | Bereitgestellt |
|-------|-----------|---------------|
| NetworkModule | Singleton | Retrofit, OkHttpClient, GeminiApi |
| DatabaseModule | Singleton | Room-Database, JournalDao, UserDao |
| ... | ... | ... |

### Room-Datenmodell

Datenbank: AppDatabase (Version N)

| Entity | Tabelle | Felder | DSGVO-Sensibel |
|--------|--------|--------|---------------|
| JournalEntryEntity | journal_entries | id, content, createdAt, mood, ... | JA (personenbezogen) |
| ... | ... | ... | ... |

DAOs:

| DAO | Zugehoerige Entity | Operationen |
|-----|-------------------|-------------|
| JournalDao | JournalEntry | get(), insert(), update(), delete(), search() |
| ... | ... | ... |

### Workers (Background-Jobs)

| Worker | Trigger | Constraints | Zweck |
|--------|--------|------------|-------|
| DailyReminderWorker | Periodic 24h | None | Erinnerungs-Notification |
| CloudBackupWorker | OneTime | Connected, Charging | Cloud-Sync |
| ... | ... | ... | ... |

### State-Machines (Sealed Classes)

| Sealed | Subzustaende | Zugeordneter VM |
|--------|-------------|----------------|
| DashboardUiState | Loading, Success(entries), Error(msg), Empty | DashboardViewModel |
| PaywallState | LoadingProducts, ShowingProducts, Purchasing, Success, Error | PaywallViewModel |
| ... | ... | ... |

### Application-Init-Reihenfolge
- onCreate() initialisiert: Firebase, Crashlytics, Timber, WorkManager, ...
```

## Pruefroutine: ViewModel ↔ Screen ↔ Route

Fuer jeden gefundenen ViewModel pruefen:
1. Gibt es einen Compose-Screen der ihn nutzt? (siehe Schicht 4)
2. Gibt es eine Navigation-Route die diesen Screen registriert?

Wenn nein: Der VM ist verwaist (toter Code) ODER der Screen wird nur per Direkt-Aufruf gestartet (Sub-Screen, Dialog).

## Typische Fehlerquellen

- **DI-Module in Library-Modulen vergessen**: Bei Multi-Module-Apps kann das Hilt-Modul fuer Networking in einem Library-Modul liegen, nicht im App-Modul.
- **Kotlin Multiplatform Shared Code**: Bei KMP-Setups liegt Logik im `shared` oder `core` Modul (oft .kt mit `expect`/`actual`).
- **Compose Multiplatform**: Wenn `commonMain`/`androidMain`/`iosMain` existieren — Architektur ist Cross-Platform.
- **Manuelle DI ohne Hilt**: Manche Apps nutzen Koin oder selbst-gebaute Service-Locators. Patterns: `Koin.startKoin`, `module { single { ... } }`, `ServiceLocator`.
