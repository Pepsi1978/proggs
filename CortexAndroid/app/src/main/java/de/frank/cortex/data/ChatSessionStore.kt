package de.frank.cortex.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.frank.cortex.data.model.ChatOption
import de.frank.cortex.observability.CortexLog
import org.json.JSONArray
import org.json.JSONObject

data class ChatSessionSummary(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)

data class StoredChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val action: String?,
    val category: String?,
    val title: String?,
    val categories: List<String> = emptyList(),
    val docId: String? = null,
    val memoryText: String? = null,
    val storedText: String? = null,
    val memoryEditable: Boolean = false,
    val recallHits: Int?,
    val options: List<ChatOption> = emptyList(),
    val stored: Boolean,
    val timestamp: Long,
    val responseTimeMs: Long? = null,
    val sourceUsage: List<String>? = null
)

/** Ein gemerkter Speicher-Auftrag (Offline-Outbox, Frank-Wunsch 2026-07-02): wird ohne VPN
 *  eingereiht und automatisch gesendet, sobald der Tunnel steht. request_id macht den
 *  spaeteren Versand idempotent (Server dedupliziert — keine Doppel-Speicherung). */
data class OutboxItem(
    val id: String,
    val sessionId: String,
    val text: String,
    val category: String?,
    val title: String?,
    val contextMode: String,
    val contextModeRevision: Int = 0,
    val responseSize: String,
    val requestId: String,
    val createdAt: Long,
    val memoryEdit: Boolean = false,
    val memoryDocId: String? = null,
    val memoryCategories: List<String> = emptyList()
)

data class PendingSessionEnd(
    val sessionId: String,
    val createdAt: Long
)

object ChatSessionStore {

    private lateinit var helper: Helper

    fun init(context: Context) {
        helper = Helper(context.applicationContext)
        CortexLog.info("ChatSessionStore", "init", "Chat-Session-Datenbank initialisiert")
    }

    fun listSessions(): List<ChatSessionSummary> = helper.readableDatabase.rawQuery(
        """
        SELECT s.id, s.title, s.created_at, s.updated_at, COUNT(m.id) AS message_count
        FROM sessions s
        LEFT JOIN messages m ON m.session_id = s.id
        GROUP BY s.id, s.title, s.created_at, s.updated_at
        HAVING message_count > 0
        ORDER BY s.updated_at DESC
        """.trimIndent(),
        emptyArray()
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    ChatSessionSummary(
                        id = cursor.getString(0),
                        title = compactSessionTitle(cursor.getString(1)),
                        createdAt = cursor.getLong(2),
                        updatedAt = cursor.getLong(3),
                        messageCount = cursor.getInt(4)
                    )
                )
            }
        }
    }

    fun loadMessages(sessionId: String): List<StoredChatMessage> = helper.readableDatabase.query(
        "messages",
        arrayOf("id", "text", "is_user", "action", "category", "title", "categories", "doc_id", "memory_text", "stored_text", "memory_editable", "recall_hits", "options", "stored", "timestamp", "response_time_ms", "source_usage"),
        "session_id = ?",
        arrayOf(sessionId),
        null,
        null,
        "timestamp ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    StoredChatMessage(
                        id = cursor.getString(0),
                        text = cursor.getString(1),
                        isUser = cursor.getInt(2) == 1,
                        action = cursor.getStringOrNull(3),
                        category = cursor.getStringOrNull(4),
                        title = cursor.getStringOrNull(5),
                        categories = decodeCategories(cursor.getStringOrNull(6)),
                        docId = cursor.getStringOrNull(7),
                        memoryText = cursor.getStringOrNull(8),
                        storedText = cursor.getStringOrNull(9),
                        memoryEditable = cursor.getInt(10) == 1,
                        recallHits = if (cursor.isNull(11)) null else cursor.getInt(11),
                        options = decodeOptions(cursor.getStringOrNull(12)),
                        stored = cursor.getInt(13) == 1,
                        timestamp = cursor.getLong(14),
                        responseTimeMs = if (cursor.isNull(15)) null else cursor.getLong(15),
                        sourceUsage = decodeSourceUsage(cursor.getStringOrNull(16))
                    )
                )
            }
        }
    }

    /** Setzt den (KI-generierten Intentions-)Titel einer Session — Frank-Wunsch 2026-07-02. */
    fun updateSessionTitle(sessionId: String, title: String) {
        val clean = compactSessionTitle(title).ifBlank { return }
        helper.writableDatabase.update(
            "sessions",
            ContentValues().apply { put("title", clean) },
            "id = ?",
            arrayOf(sessionId)
        )
    }

    // --- Offline-Outbox (Speicher-Auftraege ohne VPN) ---

    fun enqueueOutbox(item: OutboxItem) {
        helper.writableDatabase.insertWithOnConflict(
            "outbox",
            null,
            ContentValues().apply {
                put("id", item.id)
                put("session_id", item.sessionId)
                put("text", item.text)
                put("category", item.category)
                put("title", item.title)
                put("context_mode", item.contextMode)
                put("context_mode_revision", item.contextModeRevision)
                put("response_size", item.responseSize)
                put("request_id", item.requestId)
                put("memory_edit", if (item.memoryEdit) 1 else 0)
                put("memory_doc_id", item.memoryDocId)
                put("memory_categories", encodeCategories(item.memoryCategories))
                put("created_at", item.createdAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listOutbox(): List<OutboxItem> = helper.readableDatabase.query(
        "outbox",
        arrayOf("id", "session_id", "text", "category", "title", "context_mode", "context_mode_revision", "response_size", "request_id", "memory_edit", "memory_doc_id", "memory_categories", "created_at"),
        null, null, null, null,
        "created_at ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    OutboxItem(
                        id = cursor.getString(0),
                        sessionId = cursor.getString(1),
                        text = cursor.getString(2),
                        category = cursor.getStringOrNull(3),
                        title = cursor.getStringOrNull(4),
                        contextMode = cursor.getString(5),
                        contextModeRevision = cursor.getInt(6),
                        responseSize = cursor.getString(7),
                        requestId = cursor.getString(8),
                        memoryEdit = cursor.getInt(9) == 1,
                        memoryDocId = cursor.getStringOrNull(10),
                        memoryCategories = decodeCategories(cursor.getStringOrNull(11)),
                        createdAt = cursor.getLong(12)
                    )
                )
            }
        }
    }

    fun deleteOutboxItem(id: String) {
        helper.writableDatabase.delete("outbox", "id = ?", arrayOf(id))
    }

    fun enqueueSessionEnd(sessionId: String, createdAt: Long = System.currentTimeMillis()) {
        helper.writableDatabase.insertWithOnConflict(
            "pending_session_end",
            null,
            ContentValues().apply {
                put("session_id", sessionId)
                put("created_at", createdAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun listPendingSessionEnds(): List<PendingSessionEnd> = helper.readableDatabase.query(
        "pending_session_end",
        arrayOf("session_id", "created_at"),
        null, null, null, null,
        "created_at ASC"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(PendingSessionEnd(sessionId = cursor.getString(0), createdAt = cursor.getLong(1)))
            }
        }
    }

    fun deletePendingSessionEnd(sessionId: String) {
        helper.writableDatabase.delete("pending_session_end", "session_id = ?", arrayOf(sessionId))
    }

    fun deleteSession(sessionId: String) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("messages", "session_id = ?", arrayOf(sessionId))
            db.delete("sessions", "id = ?", arrayOf(sessionId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun saveMessage(sessionId: String, message: StoredChatMessage) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            ensureSession(db, sessionId, titleHint = if (message.isUser) message.text else null, now = message.timestamp)
            db.insertWithOnConflict(
                "messages",
                null,
                ContentValues().apply {
                    put("id", message.id)
                    put("session_id", sessionId)
                    put("text", message.text)
                    put("is_user", if (message.isUser) 1 else 0)
                    put("action", message.action)
                    put("category", message.category)
                    put("title", message.title)
                    put("categories", encodeCategories(message.categories))
                    put("doc_id", message.docId)
                    put("memory_text", message.memoryText)
                    put("stored_text", message.storedText)
                    put("memory_editable", if (message.memoryEditable) 1 else 0)
                    put("recall_hits", message.recallHits)
                    put("options", encodeOptions(message.options))
                    put("stored", if (message.stored) 1 else 0)
                    put("timestamp", message.timestamp)
                    put("response_time_ms", message.responseTimeMs)
                    put("source_usage", encodeSourceUsage(message.sourceUsage))
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
            db.update(
                "sessions",
                ContentValues().apply { put("updated_at", message.timestamp) },
                "id = ?",
                arrayOf(sessionId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun ensureSession(db: SQLiteDatabase, sessionId: String, titleHint: String?, now: Long) {
        val title = titleHint?.let(::compactSessionTitle)?.ifBlank { null } ?: "Neue Unterhaltung"
        val exists = db.query(
            "sessions",
            arrayOf("title"),
            "id = ?",
            arrayOf(sessionId),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                false
            } else {
                val currentTitle = cursor.getString(0)
                if (currentTitle == "Neue Unterhaltung" && title != currentTitle) {
                    db.update(
                        "sessions",
                        ContentValues().apply { put("title", title) },
                        "id = ?",
                        arrayOf(sessionId)
                    )
                }
                true
            }
        }
        if (!exists) {
            db.insert(
                "sessions",
                null,
                ContentValues().apply {
                    put("id", sessionId)
                    put("title", title)
                    put("created_at", now)
                    put("updated_at", now)
                }
            )
        }
    }

    private fun compactSessionTitle(title: String): String = title
        .trim()
        .replace(Regex("\\s+"), " ")
        .split(' ')
        .filter { it.isNotBlank() }
        .take(4)
        .joinToString(" ")

    private const val OUTBOX_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS outbox (
            id TEXT PRIMARY KEY,
            session_id TEXT NOT NULL,
            text TEXT NOT NULL,
            category TEXT,
            title TEXT,
            context_mode TEXT NOT NULL,
            context_mode_revision INTEGER NOT NULL DEFAULT 0,
            response_size TEXT NOT NULL,
            request_id TEXT NOT NULL,
            memory_edit INTEGER NOT NULL DEFAULT 0,
            memory_doc_id TEXT,
            memory_categories TEXT,
            created_at INTEGER NOT NULL
        )
    """

    private const val PENDING_SESSION_END_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS pending_session_end (
            session_id TEXT PRIMARY KEY,
            created_at INTEGER NOT NULL
        )
    """

    private class Helper(context: Context) : SQLiteOpenHelper(context, "cortex_chat_sessions.db", null, 8) {
        override fun onConfigure(db: SQLiteDatabase) {
            // Statt einmaligem "PRAGMA foreign_keys=ON" in init(): das Pragma gilt nur pro
            // Connection — onConfigure setzt es garantiert fuer JEDE Connection des Helpers.
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE sessions (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE messages (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    text TEXT NOT NULL,
                    is_user INTEGER NOT NULL,
                    action TEXT,
                    category TEXT,
                    title TEXT,
                    categories TEXT,
                    doc_id TEXT,
                    memory_text TEXT,
                    stored_text TEXT,
                    memory_editable INTEGER NOT NULL DEFAULT 0,
                    recall_hits INTEGER,
                    options TEXT,
                    stored INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    response_time_ms INTEGER,
                    source_usage TEXT,
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_sessions_updated_at ON sessions(updated_at DESC)")
            db.execSQL("CREATE INDEX idx_messages_session_timestamp ON messages(session_id, timestamp ASC)")
            db.execSQL(OUTBOX_TABLE_SQL.trimIndent())
            db.execSQL(PENDING_SESSION_END_TABLE_SQL.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 1 -> 2: Offline-Outbox fuer Speicher-Auftraege (additiv, keine
            // bestehenden Daten betroffen — echte Migration statt destruktivem Neuaufbau).
            if (oldVersion < 2) {
                db.execSQL(OUTBOX_TABLE_SQL.trimIndent())
            }
            // Version 2 -> 3: robuste Nachholung fuer Plus-/Neue-Session-/end-Calls, falls
            // der VPN-Tunnel oder Server genau beim Beenden nicht erreichbar ist.
            if (oldVersion < 3) {
                db.execSQL(PENDING_SESSION_END_TABLE_SQL.trimIndent())
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE messages ADD COLUMN categories TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN doc_id TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN memory_text TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN stored_text TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN memory_editable INTEGER NOT NULL DEFAULT 0")
                if (oldVersion >= 2) {
                    db.execSQL("ALTER TABLE outbox ADD COLUMN memory_edit INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE outbox ADD COLUMN memory_doc_id TEXT")
                    db.execSQL("ALTER TABLE outbox ADD COLUMN memory_categories TEXT")
                }
            }
            if (oldVersion in 2..4) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN context_mode_revision INTEGER NOT NULL DEFAULT 0")
            }
            if (oldVersion < 6) {
                db.execSQL("ALTER TABLE messages ADD COLUMN options TEXT")
            }
            if (oldVersion < 7) {
                db.execSQL("ALTER TABLE messages ADD COLUMN response_time_ms INTEGER")
            }
            if (oldVersion < 8) {
                db.execSQL("ALTER TABLE messages ADD COLUMN source_usage TEXT")
            }
        }
    }

    private fun encodeCategories(categories: List<String>): String? =
        categories.map(String::trim).filter(String::isNotEmpty).distinct().take(12)
            .takeIf { it.isNotEmpty() }?.joinToString("\u001F")

    private fun decodeCategories(value: String?): List<String> =
        value?.split('\u001F')?.map(String::trim)?.filter(String::isNotEmpty)?.distinct().orEmpty()

    // null bleibt "Metadaten unbekannt" fuer Altbestand; der leere String bedeutet bewusst
    // "ohne Suche". So behauptet die Migration nichts ueber historische Antworten.
    private fun encodeSourceUsage(sourceUsage: List<String>?): String? =
        sourceUsage?.filter { it == "memory" || it == "internet" }?.distinct()?.joinToString("\u001F")

    private fun decodeSourceUsage(value: String?): List<String>? =
        value?.split('\u001F')?.map(String::trim)?.filter(String::isNotEmpty)?.distinct()

    private fun encodeOptions(options: List<ChatOption>): String? = options.takeIf { it.isNotEmpty() }?.let { list ->
        JSONArray().apply {
            list.forEach { option -> put(JSONObject().put("label", option.label).put("send", option.send)) }
        }.toString()
    }

    private fun decodeOptions(value: String?): List<ChatOption> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: return emptyList()
                    val label = item.opt("label")
                    val send = item.opt("send")
                    if (label !is String || send !is String) return emptyList()
                    add(ChatOption(label, send))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
