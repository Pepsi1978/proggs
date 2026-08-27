import Foundation
import SQLite3

/// Thin SQLite wrapper that reads and writes the same file the Windows VTO
/// uses (schema-compatible). Uses the sqlite3 C API directly — no external
/// Swift packages, no SPM dependency — so `bash build.sh` keeps working
/// without further setup.
///
/// DB location on macOS:
///   ~/Library/Application Support/PromptBoard/promptboard.db
///
/// Schema is created on first start if the tables don't exist yet, matching
/// the migration EF Core produced for the Windows side.
final class PromptBoardStore {

    static let shared = PromptBoardStore()

    private let queue = DispatchQueue(label: "PromptBoardStore")
    private var db: OpaquePointer?
    private(set) var dbPath: String = ""

    private static let SQLITE_TRANSIENT = unsafeBitCast(
        OpaquePointer(bitPattern: -1), to: sqlite3_destructor_type.self)

    private init() {}

    // MARK: - Bootstrap

    func open() throws {
        let fm = FileManager.default
        guard let appSupport = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            throw NSError(domain: "PromptBoardStore", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Application Support not available"])
        }
        let dir = appSupport.appendingPathComponent("PromptBoard", isDirectory: true)
        try fm.createDirectory(at: dir, withIntermediateDirectories: true)
        dbPath = dir.appendingPathComponent("promptboard.db").path

        if sqlite3_open(dbPath, &db) != SQLITE_OK {
            let msg = String(cString: sqlite3_errmsg(db))
            sqlite3_close(db); db = nil
            throw NSError(domain: "PromptBoardStore", code: 2,
                userInfo: [NSLocalizedDescriptionKey: "Could not open DB: \(msg)"])
        }
        try createSchemaIfNeeded()
        try ensureAppSettingsRow()
    }

    private func createSchemaIfNeeded() throws {
        let statements = [
            """
            CREATE TABLE IF NOT EXISTS Categories (
                Id TEXT PRIMARY KEY,
                Name TEXT NOT NULL,
                SortOrder INTEGER NOT NULL DEFAULT 0,
                BackgroundColorHex TEXT NOT NULL DEFAULT '#DCEDEC',
                Type INTEGER NOT NULL DEFAULT 0,
                CreatedAt TEXT NOT NULL,
                UpdatedAt TEXT NOT NULL
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Prompts (
                Id TEXT PRIMARY KEY,
                CategoryId TEXT NOT NULL,
                ShortLabel TEXT NOT NULL DEFAULT '',
                OriginalText TEXT NOT NULL DEFAULT '',
                ImprovedText TEXT,
                ActiveVersion INTEGER NOT NULL DEFAULT 0,
                IsAlwaysOn INTEGER NOT NULL DEFAULT 0,
                IsPrePrompt INTEGER NOT NULL DEFAULT 1,
                IsPostPrompt INTEGER NOT NULL DEFAULT 0,
                SortOrder INTEGER NOT NULL DEFAULT 0,
                PromptKind TEXT NOT NULL DEFAULT 'Prompt',
                GeminiModel TEXT,
                IsActiveForImprovement INTEGER NOT NULL DEFAULT 0,
                ImprovedByAiPromptId TEXT,
                HotkeyNumber INTEGER,
                HotkeyLetter TEXT,
                CreatedAt TEXT NOT NULL,
                UpdatedAt TEXT NOT NULL,
                FOREIGN KEY (CategoryId) REFERENCES Categories(Id) ON DELETE CASCADE
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS AppSettings (
                Id TEXT PRIMARY KEY,
                GroqApiKey TEXT,
                GeminiApiKey TEXT,
                GoogleOAuthRefreshToken TEXT,
                GoogleClientId TEXT,
                GoogleClientSecret TEXT,
                GoogleAccountEmail TEXT,
                GroqModel TEXT NOT NULL DEFAULT 'whisper-large-v3-turbo',
                AlwaysOnTop INTEGER NOT NULL DEFAULT 1,
                BarHeight REAL NOT NULL DEFAULT 140,
                SeparatorTemplate TEXT NOT NULL DEFAULT ' ; ',
                CreatedAt TEXT NOT NULL,
                UpdatedAt TEXT NOT NULL
            );
            """,
            "CREATE INDEX IF NOT EXISTS IX_Prompts_CategoryId ON Prompts (CategoryId);",
            "CREATE INDEX IF NOT EXISTS IX_Prompts_IsAlwaysOn ON Prompts (IsAlwaysOn);",
        ]
        for sql in statements {
            try exec(sql)
        }
        // Idempotent migrations for older DBs that pre-date the
        // Pre/Post-prompt split. SQLite ignores the ALTER TABLE add
        // when the column already exists if we run inside a try/catch.
        try? exec("ALTER TABLE Prompts ADD COLUMN IsPrePrompt INTEGER NOT NULL DEFAULT 1")
        try? exec("ALTER TABLE Prompts ADD COLUMN IsPostPrompt INTEGER NOT NULL DEFAULT 0")
        // HotkeyNumber: optional Cmd+1..9 binding fuer einzelne Prompts.
        // Mirrors Windows-Migration in EF Core. Nullable INTEGER weil viele
        // Prompts keinen Hotkey haben.
        try? exec("ALTER TABLE Prompts ADD COLUMN HotkeyNumber INTEGER")
        // HotkeyLetter: optional Cmd+Opt+A..Z binding (Windows: Win+Alt+A..Z).
        try? exec("ALTER TABLE Prompts ADD COLUMN HotkeyLetter TEXT")
        // AppSettings: Windows-1:1-Felder nachziehen. Read/Write nutzt
        // aktuell noch Default-Werte aus dem Swift-struct — die Spalten
        // existieren aber, damit PromptHistoryDriveSync sie spaeter
        // serialisieren kann.
        try? exec("ALTER TABLE AppSettings ADD COLUMN AutoHide INTEGER NOT NULL DEFAULT 1")
        try? exec("ALTER TABLE AppSettings ADD COLUMN Orientation TEXT NOT NULL DEFAULT 'vertical'")
        try? exec("ALTER TABLE AppSettings ADD COLUMN PersistOverlayPosition INTEGER NOT NULL DEFAULT 0")
        try? exec("ALTER TABLE AppSettings ADD COLUMN OverlayVerticalLeft REAL")
        try? exec("ALTER TABLE AppSettings ADD COLUMN OverlayVerticalTop REAL")
        try? exec("ALTER TABLE AppSettings ADD COLUMN OverlayHorizontalLeft REAL")
        try? exec("ALTER TABLE AppSettings ADD COLUMN OverlayHorizontalTop REAL")
    }

    private func ensureAppSettingsRow() throws {
        var count = 0
        try query("SELECT COUNT(*) FROM AppSettings") { stmt in
            count = Int(sqlite3_column_int64(stmt, 0))
        }
        if count == 0 {
            let now = Self.isoNow()
            let settings = PBAppSettings(
                id: UUID(), groqApiKey: nil, geminiApiKey: nil,
                googleOAuthRefreshToken: nil, googleClientId: nil,
                googleClientSecret: nil, googleAccountEmail: nil,
                groqModel: "whisper-large-v3-turbo", alwaysOnTop: true,
                barHeight: 140, separatorTemplate: "\n\n;\n\n",
                createdAt: isoDate(now), updatedAt: isoDate(now))
            try insertSettings(settings)
        }
    }

    // MARK: - Categories

    func allCategories() throws -> [PBCategory] {
        var list: [PBCategory] = []
        try query("SELECT Id,Name,SortOrder,BackgroundColorHex,Type,CreatedAt,UpdatedAt FROM Categories ORDER BY SortOrder, Name") { stmt in
            list.append(readCategory(stmt))
        }
        return list
    }

    func addCategory(_ c: PBCategory) throws {
        let sql = "INSERT INTO Categories (Id,Name,SortOrder,BackgroundColorHex,Type,CreatedAt,UpdatedAt) VALUES (?,?,?,?,?,?,?)"
        try prepared(sql) { stmt in
            bindText(stmt, 1, c.id.uuidString)
            bindText(stmt, 2, c.name)
            sqlite3_bind_int64(stmt, 3, sqlite3_int64(c.sortOrder))
            bindText(stmt, 4, c.backgroundColorHex)
            sqlite3_bind_int64(stmt, 5, sqlite3_int64(c.type))
            bindText(stmt, 6, Self.fmt(c.createdAt))
            bindText(stmt, 7, Self.fmt(c.updatedAt))
        }
    }

    func updateCategory(_ c: PBCategory) throws {
        let sql = "UPDATE Categories SET Name=?, SortOrder=?, BackgroundColorHex=?, Type=?, UpdatedAt=? WHERE Id=?"
        try prepared(sql) { stmt in
            bindText(stmt, 1, c.name)
            sqlite3_bind_int64(stmt, 2, sqlite3_int64(c.sortOrder))
            bindText(stmt, 3, c.backgroundColorHex)
            sqlite3_bind_int64(stmt, 4, sqlite3_int64(c.type))
            bindText(stmt, 5, Self.fmt(Date()))
            bindText(stmt, 6, c.id.uuidString)
        }
    }

    func upsertCategory(_ c: PBCategory) throws {
        if try categoryExists(c.id) {
            try updateCategory(c)
        } else {
            try addCategory(c)
        }
    }

    func deleteCategory(_ id: UUID) throws {
        try prepared("DELETE FROM Prompts WHERE CategoryId=?") { stmt in
            bindText(stmt, 1, id.uuidString)
        }
        try prepared("DELETE FROM Categories WHERE Id=?") { stmt in
            bindText(stmt, 1, id.uuidString)
        }
    }

    private func categoryExists(_ id: UUID) throws -> Bool {
        var found = false
        try prepared("SELECT 1 FROM Categories WHERE Id=?") { stmt in
            bindText(stmt, 1, id.uuidString)
        } step: { stmt in
            found = true
        }
        return found
    }

    // MARK: - Prompts

    /// Column list shared by every Prompts SELECT — keep in sync with
    /// readPrompt(...) below. Adding a column means: schema CREATE TABLE,
    /// ALTER TABLE migration, this constant, the bind() in addPrompt /
    /// updatePrompt, AND readPrompt(stmt). Five places, in lockstep.
    private static let promptColumns =
        "Id,CategoryId,ShortLabel,OriginalText,ImprovedText,ActiveVersion,IsAlwaysOn,IsPrePrompt,IsPostPrompt,SortOrder,PromptKind,GeminiModel,IsActiveForImprovement,ImprovedByAiPromptId,HotkeyNumber,HotkeyLetter,CreatedAt,UpdatedAt"

    func prompts(in categoryId: UUID) throws -> [PBPrompt] {
        var list: [PBPrompt] = []
        try prepared("SELECT \(Self.promptColumns) FROM Prompts WHERE CategoryId=? ORDER BY SortOrder, ShortLabel") { stmt in
            bindText(stmt, 1, categoryId.uuidString)
        } step: { [weak self] stmt in
            guard let self = self, let p = self.readPrompt(stmt) else { return }
            list.append(p)
        }
        return list
    }

    /// Alle Prompts ueber alle Kategorien. Grundlage fuer die Kurzbefehl-Tabellen
    /// (Windows: PromptBoardPanel liest beim Rendern ebenfalls den Gesamtbestand).
    func allPrompts() throws -> [PBPrompt] {
        var list: [PBPrompt] = []
        try prepared("SELECT \(Self.promptColumns) FROM Prompts ORDER BY SortOrder, ShortLabel") { _ in
        } step: { [weak self] stmt in
            guard let self = self, let p = self.readPrompt(stmt) else { return }
            list.append(p)
        }
        return list
    }

    func allAlwaysOnPrompts() throws -> [PBPrompt] {
        var list: [PBPrompt] = []
        try query("SELECT \(Self.promptColumns) FROM Prompts WHERE IsAlwaysOn=1 ORDER BY SortOrder") { stmt in
            if let p = self.readPrompt(stmt) { list.append(p) }
        }
        return list
    }

    func addPrompt(_ p: PBPrompt) throws {
        let sql = "INSERT INTO Prompts (\(Self.promptColumns)) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
        try prepared(sql) { stmt in
            bindText(stmt, 1, p.id.uuidString)
            bindText(stmt, 2, p.categoryId.uuidString)
            bindText(stmt, 3, p.shortLabel)
            bindText(stmt, 4, p.originalText)
            bindTextOrNull(stmt, 5, p.improvedText)
            sqlite3_bind_int64(stmt, 6, sqlite3_int64(p.activeVersion))
            sqlite3_bind_int64(stmt, 7, p.isAlwaysOn ? 1 : 0)
            sqlite3_bind_int64(stmt, 8, p.isPrePrompt ? 1 : 0)
            sqlite3_bind_int64(stmt, 9, p.isPostPrompt ? 1 : 0)
            sqlite3_bind_int64(stmt, 10, sqlite3_int64(p.sortOrder))
            bindText(stmt, 11, p.promptKind)
            bindTextOrNull(stmt, 12, p.geminiModel)
            sqlite3_bind_int64(stmt, 13, p.isActiveForImprovement ? 1 : 0)
            bindTextOrNull(stmt, 14, p.improvedByAiPromptId?.uuidString)
            bindIntOrNull(stmt, 15, p.hotkeyNumber)
            bindTextOrNull(stmt, 16, p.hotkeyLetter)
            bindText(stmt, 17, Self.fmt(p.createdAt))
            bindText(stmt, 18, Self.fmt(p.updatedAt))
        }
    }

    func updatePrompt(_ p: PBPrompt) throws {
        let sql = "UPDATE Prompts SET CategoryId=?, ShortLabel=?, OriginalText=?, ImprovedText=?, ActiveVersion=?, IsAlwaysOn=?, IsPrePrompt=?, IsPostPrompt=?, SortOrder=?, PromptKind=?, GeminiModel=?, IsActiveForImprovement=?, ImprovedByAiPromptId=?, HotkeyNumber=?, HotkeyLetter=?, UpdatedAt=? WHERE Id=?"
        try prepared(sql) { stmt in
            bindText(stmt, 1, p.categoryId.uuidString)
            bindText(stmt, 2, p.shortLabel)
            bindText(stmt, 3, p.originalText)
            bindTextOrNull(stmt, 4, p.improvedText)
            sqlite3_bind_int64(stmt, 5, sqlite3_int64(p.activeVersion))
            sqlite3_bind_int64(stmt, 6, p.isAlwaysOn ? 1 : 0)
            sqlite3_bind_int64(stmt, 7, p.isPrePrompt ? 1 : 0)
            sqlite3_bind_int64(stmt, 8, p.isPostPrompt ? 1 : 0)
            sqlite3_bind_int64(stmt, 9, sqlite3_int64(p.sortOrder))
            bindText(stmt, 10, p.promptKind)
            bindTextOrNull(stmt, 11, p.geminiModel)
            sqlite3_bind_int64(stmt, 12, p.isActiveForImprovement ? 1 : 0)
            bindTextOrNull(stmt, 13, p.improvedByAiPromptId?.uuidString)
            bindIntOrNull(stmt, 14, p.hotkeyNumber)
            bindTextOrNull(stmt, 15, p.hotkeyLetter)
            bindText(stmt, 16, Self.fmt(Date()))
            bindText(stmt, 17, p.id.uuidString)
        }
    }

    /// Implements the "last wins" rule for prompt hotkey assignments:
    /// when a prompt is given hotkey N, every OTHER prompt that previously
    /// owned N gets its hotkey cleared. Mirrors
    /// `PromptBoardPanel.StripHotkeyFromOthersAsync` on Windows.
    func stripHotkeyFromOthers(hotkey: Int, exceptId: UUID) throws {
        try prepared("UPDATE Prompts SET HotkeyNumber=NULL, UpdatedAt=? WHERE HotkeyNumber=? AND Id<>?") { stmt in
            bindText(stmt, 1, Self.fmt(Date()))
            sqlite3_bind_int64(stmt, 2, sqlite3_int64(hotkey))
            bindText(stmt, 3, exceptId.uuidString)
        }
    }

    /// Last-wins fuer HotkeyLetter (Cmd+Opt+A..Z): wenn der Prompt einen
    /// Letter zugewiesen bekommt, alle anderen verlieren ihn.
    func stripLetterFromOthers(letter: String, exceptId: UUID) throws {
        let upper = letter.uppercased()
        try prepared("UPDATE Prompts SET HotkeyLetter=NULL, UpdatedAt=? WHERE HotkeyLetter=? AND Id<>?") { stmt in
            bindText(stmt, 1, Self.fmt(Date()))
            bindText(stmt, 2, upper)
            bindText(stmt, 3, exceptId.uuidString)
        }
    }

    /// Returns the prompt currently bound to Cmd+N (1..9), or nil if none.
    func promptByHotkey(_ hotkey: Int) throws -> PBPrompt? {
        var result: PBPrompt?
        try prepared("SELECT \(Self.promptColumns) FROM Prompts WHERE HotkeyNumber=? LIMIT 1") { stmt in
            sqlite3_bind_int64(stmt, 1, sqlite3_int64(hotkey))
        } step: { [weak self] stmt in
            result = self?.readPrompt(stmt)
        }
        return result
    }

    /// Returns the prompt currently bound to Cmd+Opt+A..Z (single letter),
    /// case-insensitive on storage (uppercase normalized).
    func promptByLetter(_ letter: Character) throws -> PBPrompt? {
        let upper = String(letter).uppercased()
        var result: PBPrompt?
        try prepared("SELECT \(Self.promptColumns) FROM Prompts WHERE HotkeyLetter=? LIMIT 1") { stmt in
            bindText(stmt, 1, upper)
        } step: { [weak self] stmt in
            result = self?.readPrompt(stmt)
        }
        return result
    }

    func upsertPrompt(_ p: PBPrompt) throws {
        if try promptExists(p.id) {
            try updatePrompt(p)
        } else {
            try addPrompt(p)
        }
    }

    func deletePrompt(_ id: UUID) throws {
        try prepared("DELETE FROM Prompts WHERE Id=?") { stmt in
            bindText(stmt, 1, id.uuidString)
        }
    }

    private func promptExists(_ id: UUID) throws -> Bool {
        var found = false
        try prepared("SELECT 1 FROM Prompts WHERE Id=?") { stmt in
            bindText(stmt, 1, id.uuidString)
        } step: { _ in found = true }
        return found
    }

    // MARK: - AppSettings

    func settings() throws -> PBAppSettings {
        var result: PBAppSettings?
        try query("SELECT Id,GroqApiKey,GeminiApiKey,GoogleOAuthRefreshToken,GoogleClientId,GoogleClientSecret,GoogleAccountEmail,GroqModel,AlwaysOnTop,BarHeight,SeparatorTemplate,CreatedAt,UpdatedAt,AutoHide,Orientation,PersistOverlayPosition,OverlayVerticalLeft,OverlayVerticalTop,OverlayHorizontalLeft,OverlayHorizontalTop FROM AppSettings LIMIT 1") { stmt in
            result = self.readSettings(stmt)
        }
        if let s = result { return s }
        try ensureAppSettingsRow()
        return try settings()
    }

    func updateSettings(_ s: PBAppSettings) throws {
        let sql = "UPDATE AppSettings SET GroqApiKey=?, GeminiApiKey=?, GoogleOAuthRefreshToken=?, GoogleClientId=?, GoogleClientSecret=?, GoogleAccountEmail=?, GroqModel=?, AlwaysOnTop=?, BarHeight=?, SeparatorTemplate=?, UpdatedAt=?, AutoHide=?, Orientation=?, PersistOverlayPosition=?, OverlayVerticalLeft=?, OverlayVerticalTop=?, OverlayHorizontalLeft=?, OverlayHorizontalTop=? WHERE Id=?"
        try prepared(sql) { stmt in
            bindTextOrNull(stmt, 1, s.groqApiKey)
            bindTextOrNull(stmt, 2, s.geminiApiKey)
            bindTextOrNull(stmt, 3, s.googleOAuthRefreshToken)
            bindTextOrNull(stmt, 4, s.googleClientId)
            bindTextOrNull(stmt, 5, s.googleClientSecret)
            bindTextOrNull(stmt, 6, s.googleAccountEmail)
            bindText(stmt, 7, s.groqModel)
            sqlite3_bind_int64(stmt, 8, s.alwaysOnTop ? 1 : 0)
            sqlite3_bind_double(stmt, 9, s.barHeight)
            bindText(stmt, 10, s.separatorTemplate)
            bindText(stmt, 11, Self.fmt(Date()))
            sqlite3_bind_int64(stmt, 12, s.autoHide ? 1 : 0)
            bindText(stmt, 13, s.orientation)
            sqlite3_bind_int64(stmt, 14, s.persistOverlayPosition ? 1 : 0)
            if let v = s.overlayVerticalLeft   { sqlite3_bind_double(stmt, 15, v) } else { sqlite3_bind_null(stmt, 15) }
            if let v = s.overlayVerticalTop    { sqlite3_bind_double(stmt, 16, v) } else { sqlite3_bind_null(stmt, 16) }
            if let v = s.overlayHorizontalLeft { sqlite3_bind_double(stmt, 17, v) } else { sqlite3_bind_null(stmt, 17) }
            if let v = s.overlayHorizontalTop  { sqlite3_bind_double(stmt, 18, v) } else { sqlite3_bind_null(stmt, 18) }
            bindText(stmt, 19, s.id.uuidString)
        }
    }

    private func insertSettings(_ s: PBAppSettings) throws {
        let sql = "INSERT INTO AppSettings (Id,GroqApiKey,GeminiApiKey,GoogleOAuthRefreshToken,GoogleClientId,GoogleClientSecret,GoogleAccountEmail,GroqModel,AlwaysOnTop,BarHeight,SeparatorTemplate,CreatedAt,UpdatedAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
        try prepared(sql) { stmt in
            bindText(stmt, 1, s.id.uuidString)
            bindTextOrNull(stmt, 2, s.groqApiKey)
            bindTextOrNull(stmt, 3, s.geminiApiKey)
            bindTextOrNull(stmt, 4, s.googleOAuthRefreshToken)
            bindTextOrNull(stmt, 5, s.googleClientId)
            bindTextOrNull(stmt, 6, s.googleClientSecret)
            bindTextOrNull(stmt, 7, s.googleAccountEmail)
            bindText(stmt, 8, s.groqModel)
            sqlite3_bind_int64(stmt, 9, s.alwaysOnTop ? 1 : 0)
            sqlite3_bind_double(stmt, 10, s.barHeight)
            bindText(stmt, 11, s.separatorTemplate)
            bindText(stmt, 12, Self.fmt(s.createdAt))
            bindText(stmt, 13, Self.fmt(s.updatedAt))
        }
    }

    // MARK: - SQLite helpers

    private func exec(_ sql: String) throws {
        var err: UnsafeMutablePointer<Int8>?
        if sqlite3_exec(db, sql, nil, nil, &err) != SQLITE_OK {
            let msg = err != nil ? String(cString: err!) : "unknown"
            sqlite3_free(err)
            throw NSError(domain: "PromptBoardStore", code: 3, userInfo: [NSLocalizedDescriptionKey: msg])
        }
    }

    private func query(_ sql: String, step: (OpaquePointer) -> Void) throws {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK, let stmt = stmt else {
            throw sqliteError("prepare: \(sql)")
        }
        defer { sqlite3_finalize(stmt) }
        while sqlite3_step(stmt) == SQLITE_ROW { step(stmt) }
    }

    private func prepared(_ sql: String,
                          bind: (OpaquePointer) -> Void,
                          step: (OpaquePointer) -> Void = { _ in }) throws {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK, let stmt = stmt else {
            throw sqliteError("prepare: \(sql)")
        }
        defer { sqlite3_finalize(stmt) }
        bind(stmt)
        while true {
            let rc = sqlite3_step(stmt)
            if rc == SQLITE_ROW { step(stmt) }
            else if rc == SQLITE_DONE { break }
            else { throw sqliteError("step: \(sql)") }
        }
    }

    private func bindText(_ stmt: OpaquePointer, _ idx: Int32, _ value: String) {
        sqlite3_bind_text(stmt, idx, value, -1, Self.SQLITE_TRANSIENT)
    }

    private func bindTextOrNull(_ stmt: OpaquePointer, _ idx: Int32, _ value: String?) {
        if let v = value { bindText(stmt, idx, v) } else { sqlite3_bind_null(stmt, idx) }
    }

    private func bindIntOrNull(_ stmt: OpaquePointer, _ idx: Int32, _ value: Int?) {
        if let v = value { sqlite3_bind_int64(stmt, idx, sqlite3_int64(v)) } else { sqlite3_bind_null(stmt, idx) }
    }

    private func readOptInt(_ stmt: OpaquePointer, _ idx: Int32) -> Int? {
        if sqlite3_column_type(stmt, idx) == SQLITE_NULL { return nil }
        return Int(sqlite3_column_int64(stmt, idx))
    }

    private func readString(_ stmt: OpaquePointer, _ idx: Int32) -> String {
        guard let c = sqlite3_column_text(stmt, idx) else { return "" }
        return String(cString: c)
    }

    private func readOptString(_ stmt: OpaquePointer, _ idx: Int32) -> String? {
        if sqlite3_column_type(stmt, idx) == SQLITE_NULL { return nil }
        return readString(stmt, idx)
    }

    private func readCategory(_ stmt: OpaquePointer) -> PBCategory {
        PBCategory(
            id: UUID(uuidString: readString(stmt, 0)) ?? UUID(),
            name: readString(stmt, 1),
            sortOrder: Int(sqlite3_column_int64(stmt, 2)),
            backgroundColorHex: readString(stmt, 3),
            type: Int(sqlite3_column_int64(stmt, 4)),
            createdAt: Self.parse(readString(stmt, 5)) ?? Date(),
            updatedAt: Self.parse(readString(stmt, 6)) ?? Date())
    }

    private func readPrompt(_ stmt: OpaquePointer) -> PBPrompt? {
        guard let id = UUID(uuidString: readString(stmt, 0)) else { return nil }
        guard let catId = UUID(uuidString: readString(stmt, 1)) else { return nil }
        return PBPrompt(
            id: id,
            categoryId: catId,
            shortLabel: readString(stmt, 2),
            originalText: readString(stmt, 3),
            improvedText: readOptString(stmt, 4),
            activeVersion: Int(sqlite3_column_int64(stmt, 5)),
            isAlwaysOn: sqlite3_column_int64(stmt, 6) != 0,
            isPrePrompt: sqlite3_column_int64(stmt, 7) != 0,
            isPostPrompt: sqlite3_column_int64(stmt, 8) != 0,
            sortOrder: Int(sqlite3_column_int64(stmt, 9)),
            promptKind: readOptString(stmt, 10) ?? "Prompt",
            geminiModel: readOptString(stmt, 11),
            isActiveForImprovement: sqlite3_column_int64(stmt, 12) != 0,
            improvedByAiPromptId: (readOptString(stmt, 13)).flatMap(UUID.init(uuidString:)),
            hotkeyNumber: readOptInt(stmt, 14),
            hotkeyLetter: readOptString(stmt, 15),
            createdAt: Self.parse(readString(stmt, 16)) ?? Date(),
            updatedAt: Self.parse(readString(stmt, 17)) ?? Date())
    }

    private func readSettings(_ stmt: OpaquePointer) -> PBAppSettings {
        var s = PBAppSettings(
            id: UUID(uuidString: readString(stmt, 0)) ?? UUID(),
            groqApiKey: readOptString(stmt, 1),
            geminiApiKey: readOptString(stmt, 2),
            googleOAuthRefreshToken: readOptString(stmt, 3),
            googleClientId: readOptString(stmt, 4),
            googleClientSecret: readOptString(stmt, 5),
            googleAccountEmail: readOptString(stmt, 6),
            groqModel: readString(stmt, 7),
            alwaysOnTop: sqlite3_column_int64(stmt, 8) != 0,
            barHeight: sqlite3_column_double(stmt, 9),
            separatorTemplate: readString(stmt, 10),
            createdAt: Self.parse(readString(stmt, 11)) ?? Date(),
            updatedAt: Self.parse(readString(stmt, 12)) ?? Date())
        // Windows-1:1-Felder (Spalten 13-19). NULL/MISSING → Defaults aus struct.
        if sqlite3_column_type(stmt, 13) != SQLITE_NULL {
            s.autoHide = sqlite3_column_int64(stmt, 13) != 0
        }
        if sqlite3_column_type(stmt, 14) != SQLITE_NULL {
            s.orientation = readString(stmt, 14)
        }
        if sqlite3_column_type(stmt, 15) != SQLITE_NULL {
            s.persistOverlayPosition = sqlite3_column_int64(stmt, 15) != 0
        }
        if sqlite3_column_type(stmt, 16) != SQLITE_NULL {
            s.overlayVerticalLeft = sqlite3_column_double(stmt, 16)
        }
        if sqlite3_column_type(stmt, 17) != SQLITE_NULL {
            s.overlayVerticalTop = sqlite3_column_double(stmt, 17)
        }
        if sqlite3_column_type(stmt, 18) != SQLITE_NULL {
            s.overlayHorizontalLeft = sqlite3_column_double(stmt, 18)
        }
        if sqlite3_column_type(stmt, 19) != SQLITE_NULL {
            s.overlayHorizontalTop = sqlite3_column_double(stmt, 19)
        }
        return s
    }

    private func sqliteError(_ context: String) -> NSError {
        let msg = String(cString: sqlite3_errmsg(db))
        return NSError(domain: "PromptBoardStore", code: 4,
            userInfo: [NSLocalizedDescriptionKey: "\(context): \(msg)"])
    }

    // ── Date helpers (ISO 8601 compatible with EF Core) ──
    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static func fmt(_ date: Date) -> String { isoFormatter.string(from: date) }
    private static func parse(_ s: String) -> Date? { isoFormatter.date(from: s) }
    private static func isoNow() -> String { fmt(Date()) }
    private func isoDate(_ s: String) -> Date { Self.parse(s) ?? Date() }
}
