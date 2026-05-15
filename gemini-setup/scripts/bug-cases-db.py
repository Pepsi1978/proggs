#!/usr/bin/env python3
"""
bug-cases-db.py — SQLite-Layer ueber bug-cases.jsonl

Hintergrund (I6, 2026-05-10):
- bug-cases.jsonl ist die autoritative Quelle (manuelle + auto-erfasste Eintraege)
- SQLite-DB ist eine ABGELEITETE Lookup-Schicht fuer schnellen symptom_hash-Match
- Memstate-Benchmark 2026: strukturiertes Memory hat 92% Recall vs. 17% bei JSONL

Verwendung:
  python3 bug-cases-db.py sync           # Syncronisiere JSONL → SQLite (idempotent)
  python3 bug-cases-db.py lookup "<text>" # Suche nach Symptom (Volltextsuche)
  python3 bug-cases-db.py hash-lookup "<text>" # Exact symptom_hash match

Cross-Platform: Windows + macOS/Linux. Pfade via expanduser() — sicher gegen Git-Bash-Pfad-Bug.
"""

import json
import os
import sys
import sqlite3
import hashlib
from pathlib import Path

JSONL_PATH = Path(os.path.expanduser("~/proggs/.Gemini/agent-memory/shared/bug-cases.jsonl"))
DB_PATH = Path(os.path.expanduser("~/proggs/.Gemini/agent-memory/shared/bug-cases.db"))


def symptom_hash(symptom: str) -> str:
    """SHA256-Hash der ersten 80 Zeichen des Symptoms — robust gegen kleine Variationen."""
    normalized = symptom.lower().strip()[:80]
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]


def init_db(conn: sqlite3.Connection) -> None:
    cur = conn.cursor()
    cur.executescript("""
        CREATE TABLE IF NOT EXISTS bug_cases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            symptom_hash TEXT UNIQUE NOT NULL,
            date TEXT,
            symptom TEXT NOT NULL,
            root_cause TEXT,
            fix TEXT,
            files TEXT,
            tags TEXT,
            severity TEXT,
            prevention TEXT,
            near_miss INTEGER DEFAULT 0,
            jsonl_line INTEGER,
            confirmed_fixed_at TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX IF NOT EXISTS idx_hash ON bug_cases(symptom_hash);
        CREATE INDEX IF NOT EXISTS idx_severity ON bug_cases(severity);
        CREATE INDEX IF NOT EXISTS idx_date ON bug_cases(date);
        CREATE VIRTUAL TABLE IF NOT EXISTS bug_cases_fts USING fts5(
            symptom, root_cause, fix, tags,
            content='bug_cases',
            content_rowid='id',
            tokenize='unicode61'
        );
        CREATE TRIGGER IF NOT EXISTS bug_cases_fts_insert AFTER INSERT ON bug_cases BEGIN
            INSERT INTO bug_cases_fts(rowid, symptom, root_cause, fix, tags)
            VALUES (new.id, new.symptom, new.root_cause, new.fix, new.tags);
        END;
        CREATE TRIGGER IF NOT EXISTS bug_cases_fts_delete AFTER DELETE ON bug_cases BEGIN
            INSERT INTO bug_cases_fts(bug_cases_fts, rowid, symptom, root_cause, fix, tags)
            VALUES ('delete', old.id, old.symptom, old.root_cause, old.fix, old.tags);
        END;
    """)
    conn.commit()


def sync_jsonl_to_db() -> int:
    """Liest bug-cases.jsonl und schreibt neue/geaenderte Eintraege in SQLite. Idempotent."""
    if not JSONL_PATH.exists():
        print(f"FEHLT: {JSONL_PATH}", file=sys.stderr)
        return 0
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(DB_PATH))
    init_db(conn)
    cur = conn.cursor()
    inserted = 0
    skipped = 0
    with open(JSONL_PATH, "r", encoding="utf-8") as f:
        for line_num, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            symptom = entry.get("symptom", "").strip()
            if not symptom:
                continue
            sh = symptom_hash(symptom)
            try:
                cur.execute(
                    """INSERT INTO bug_cases
                       (symptom_hash, date, symptom, root_cause, fix, files, tags, severity, prevention, near_miss, jsonl_line)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    (
                        sh,
                        entry.get("date", ""),
                        symptom,
                        entry.get("root_cause", ""),
                        entry.get("fix", ""),
                        json.dumps(entry.get("files", []), ensure_ascii=False),
                        json.dumps(entry.get("tags", []), ensure_ascii=False),
                        entry.get("severity", ""),
                        entry.get("prevention", ""),
                        1 if entry.get("near_miss") else 0,
                        line_num,
                    ),
                )
                inserted += 1
            except sqlite3.IntegrityError:
                # symptom_hash existiert bereits — silent skip (Idempotenz)
                skipped += 1
    conn.commit()
    conn.close()
    print(f"Sync abgeschlossen: {inserted} neu, {skipped} bereits vorhanden")
    return inserted


def hash_lookup(query: str) -> list:
    """Exact match per symptom_hash. Schnellster Lookup."""
    if not DB_PATH.exists():
        return []
    sh = symptom_hash(query)
    conn = sqlite3.connect(str(DB_PATH))
    cur = conn.cursor()
    cur.execute(
        "SELECT date, symptom, root_cause, fix, severity FROM bug_cases WHERE symptom_hash = ?",
        (sh,),
    )
    rows = cur.fetchall()
    conn.close()
    return [
        {"date": r[0], "symptom": r[1], "root_cause": r[2], "fix": r[3], "severity": r[4]}
        for r in rows
    ]


def search(query: str, limit: int = 5) -> list:
    """Volltext-Suche via FTS5."""
    if not DB_PATH.exists():
        return []
    conn = sqlite3.connect(str(DB_PATH))
    cur = conn.cursor()
    # Quote query for safety, allow * for prefix matching
    safe_query = query.replace('"', '""')
    try:
        cur.execute(
            """SELECT bc.date, bc.symptom, bc.root_cause, bc.fix, bc.severity
               FROM bug_cases_fts fts
               JOIN bug_cases bc ON bc.id = fts.rowid
               WHERE bug_cases_fts MATCH ?
               ORDER BY rank
               LIMIT ?""",
            (f'"{safe_query}"', limit),
        )
        rows = cur.fetchall()
    except sqlite3.OperationalError as e:
        print(f"FTS error: {e}", file=sys.stderr)
        rows = []
    conn.close()
    return [
        {"date": r[0], "symptom": r[1], "root_cause": r[2], "fix": r[3], "severity": r[4]}
        for r in rows
    ]


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "sync":
        sync_jsonl_to_db()
    elif cmd == "lookup" and len(sys.argv) >= 3:
        results = search(" ".join(sys.argv[2:]))
        print(json.dumps(results, indent=2, ensure_ascii=False))
    elif cmd == "hash-lookup" and len(sys.argv) >= 3:
        results = hash_lookup(" ".join(sys.argv[2:]))
        print(json.dumps(results, indent=2, ensure_ascii=False))
    else:
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()

