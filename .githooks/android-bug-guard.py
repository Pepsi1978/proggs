#!/usr/bin/env python3
"""Pre-commit guard for Android/Kotlin projects (installed 2026-07-03, #47446).

One check, born from the EntropieReductor deep-debugging sweep (#47442):

BLOCK (exit 2): `@Insert(onConflict = OnConflictStrategy.REPLACE)` on an entity
that is the PARENT of a ForeignKey with `onDelete = CASCADE`. SQLite REPLACE is
DELETE+INSERT — every update of the parent silently deletes all child rows
(bug almanac room.md K1; real data loss: followups/tool permissions).
Override consciously: COMMIT_ALLOW_REPLACE_CASCADE=1 git commit ...

(A CancellationException guard already exists in the same pre-commit:
tools/check_cancellation_rethrow.py, installed 2026-07-02 — not duplicated here.)

Exit codes: 0 = clean/skipped, 2 = blocking finding. Any other exit is treated
as a script error by the hook and must NOT block the commit.
"""

import os
import re
import subprocess
import sys


def staged_kotlin_files():
    try:
        out = subprocess.run(
            ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"],
            capture_output=True, text=True, encoding="utf-8", errors="replace",
            check=True,
        ).stdout
    except Exception:
        return []
    return [f for f in out.splitlines() if f.endswith(".kt")]


def read(path):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            return fh.read()
    except OSError:
        return ""


def project_roots(files):
    """Everything up to '/app/src/' identifies one Android project."""
    roots = set()
    for f in files:
        idx = f.find("/app/src/")
        if idx > 0:
            roots.add(f[:idx])
    return roots


def kotlin_files_under(root):
    result = []
    base = os.path.join(root, "app", "src", "main")
    for dirpath, _dirs, files in os.walk(base):
        for fn in files:
            if fn.endswith(".kt"):
                result.append(os.path.join(dirpath, fn))
    return result


def cascade_parents(all_files):
    """Entity class names that appear as `entity = X::class` in a ForeignKey
    whose block also contains `onDelete = ForeignKey.CASCADE`."""
    parents = set()
    fk_block = re.compile(r"ForeignKey\s*\((.*?)\)", re.DOTALL)
    for path in all_files:
        text = read(path)
        if "ForeignKey" not in text or "CASCADE" not in text:
            continue
        for m in fk_block.finditer(text):
            block = m.group(1)
            if "CASCADE" not in block:
                continue
            em = re.search(r"entity\s*=\s*(\w+)::class", block)
            if em:
                parents.add(em.group(1))
    return parents


def replace_inserts_on(parents, all_files):
    """(file, line, entity) for @Insert(REPLACE) functions taking a parent entity."""
    hits = []
    for path in all_files:
        text = read(path)
        if "OnConflictStrategy.REPLACE" not in text:
            continue
        lines = text.splitlines()
        for i, line in enumerate(lines):
            if "OnConflictStrategy.REPLACE" not in line:
                continue
            # the annotated function may sit on the same or one of the next lines
            window = " ".join(lines[i:i + 3])
            fm = re.search(r"fun\s+\w+\s*\(([^)]*)\)", window)
            if not fm:
                continue
            params = fm.group(1)
            for parent in parents:
                if re.search(r"\b" + re.escape(parent) + r"\b", params):
                    hits.append((path, i + 1, parent))
    return hits


def main():
    staged = staged_kotlin_files()
    if not staged:
        return 0

    blocking = []
    for root in project_roots(staged):
        all_files = kotlin_files_under(root)
        if not all_files:
            continue
        parents = cascade_parents(all_files)
        if parents:
            blocking.extend(replace_inserts_on(parents, all_files))

    if blocking:
        sys.stderr.write(
            "[android-bug-guard] BLOCKIERT: @Insert(onConflict = REPLACE) auf einer Tabelle "
            "mit CASCADE-Kindern — jedes Update loescht die Kind-Datensaetze still "
            "(room-Almanach K1). Fix: @Upsert statt REPLACE.\n"
        )
        for path, line, parent in blocking:
            sys.stderr.write(f"  - {path}:{line} (Eltern-Entity: {parent})\n")
        sys.stderr.write(
            "Bewusst uebersteuern: COMMIT_ALLOW_REPLACE_CASCADE=1 git commit ...\n"
        )
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
