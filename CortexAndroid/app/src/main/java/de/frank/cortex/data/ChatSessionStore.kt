package de.frank.cortex.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.frank.cortex.observability.CortexLog

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
    val recallHits: Int?,
    val stored: Boolean,
    val timestamp: Long
)

object ChatSessionStore {

    private lateinit var helper: Helper

    fun init(context: Context) {
        helper = Helper(context.applicationContext)
        helper.writableDatabase.execSQL("PRAGMA foreign_keys=ON")
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
                        title = cursor.getString(1),
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
        arrayOf("id", "text", "is_user", "action", "category", "title", "recall_hits", "stored", "timestamp"),
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
                        recallHits = if (cursor.isNull(6)) null else cursor.getInt(6),
                        stored = cursor.getInt(7) == 1,
                        timestamp = cursor.getLong(8)
                    )
                )
            }
        }
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
                    put("recall_hits", message.recallHits)
                    put("stored", if (message.stored) 1 else 0)
                    put("timestamp", message.timestamp)
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
        val title = titleHint?.trim()?.take(80)?.ifBlank { null } ?: "Neue Unterhaltung"
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

    private class Helper(context: Context) : SQLiteOpenHelper(context, "cortex_chat_sessions.db", null, 1) {
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
                    recall_hits INTEGER,
                    stored INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_sessions_updated_at ON sessions(updated_at DESC)")
            db.execSQL("CREATE INDEX idx_messages_session_timestamp ON messages(session_id, timestamp ASC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Version 1 ist das initiale Session-Schema.
        }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
