#!/usr/bin/env python3
"""Verhaltens-Regressionstests fuer Rollback und Verifikation von PUT /entry."""
from __future__ import annotations

import ast
import logging
import threading
import time
from dataclasses import dataclass
from pathlib import Path
from functools import wraps
from types import SimpleNamespace


ROOT = Path(__file__).resolve().parents[1]
APP_PATH = ROOT / "brain-api" / "app.py"
SOURCE = APP_PATH.read_text(encoding="utf-8")


class HTTPException(Exception):
    def __init__(self, status_code: int, detail: str):
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


@dataclass
class MatchValue:
    value: object


@dataclass
class FieldCondition:
    key: str
    match: MatchValue


@dataclass
class Filter:
    must: list


@dataclass
class PointStruct:
    id: str
    vector: list
    payload: dict


def load_update_function(namespace: dict):
    node = next(item for item in ast.parse(SOURCE).body
                if isinstance(item, ast.FunctionDef) and item.name == "update_entry")
    node.decorator_list = []
    exec(compile(ast.Module(body=[node], type_ignores=[]), str(APP_PATH), "exec"), namespace)
    return namespace["update_entry"]


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def run_case(*, old_chunks: int, new_chunks: int, partial_upsert: bool = False,
             cleanup_failure: bool = False, missing_last: bool = False,
             title: str = "Alt") -> tuple[dict[str, PointStruct], HTTPException | None]:
    source_id = "doc-old"
    target_id = "doc-new" if title != "Alt" else source_id
    storage: dict[str, PointStruct] = {}
    for index in range(old_chunks):
        pid = f"{source_id}:{index}"
        storage[pid] = PointStruct(pid, [index], {
            "doc_id": source_id, "user_id": "frank", "title": "Alt", "category": "Geräte",
            "categories": ["Geräte"], "chunk_index": index, "chunk_count": old_chunks,
            "chunk_text": f"alt-{index}", "full_text": "ALTER TEXT", "created_at": "alt",
            "updated_at": "alt", "source": "chat", "layer": "kurzzeit",
        })
    original = {key: PointStruct(value.id, list(value.vector), dict(value.payload)) for key, value in storage.items()}

    def doc_id_from_filter(flt: Filter) -> str | None:
        return next((str(c.match.value) for c in flt.must if c.key == "doc_id"), None)

    def fake_scroll(flt: Filter, limit=None, with_payload=True, page=1000, with_vectors=False):
        doc_id = doc_id_from_filter(flt)
        chunk_index = next((c.match.value for c in flt.must if c.key == "chunk_index"), None)
        values = [p for p in storage.values() if p.payload.get("doc_id") == doc_id
                  and (chunk_index is None or p.payload.get("chunk_index") == chunk_index)]
        values.sort(key=lambda p: p.payload.get("chunk_index", 0))
        return values[:limit] if limit is not None else values

    def fake_upsert(points, wait=False):
        chosen = list(points)
        if partial_upsert:
            chosen = chosen[:1]
        elif missing_last:
            chosen = chosen[:-1]
        for point in chosen:
            storage[point.id] = point
        if partial_upsert:
            raise RuntimeError("Batch 2 fehlgeschlagen")

    def fake_delete_doc(doc_id: str):
        for pid in [pid for pid, point in storage.items() if point.payload.get("doc_id") == doc_id]:
            del storage[pid]

    def fake_restore(doc_id: str, snapshot: list[PointStruct]):
        fake_delete_doc(doc_id)
        for point in snapshot:
            storage[point.id] = PointStruct(point.id, list(point.vector), dict(point.payload))

    class FakeQc:
        def delete(self, collection_name, points_selector, wait=False):
            if cleanup_failure and isinstance(points_selector, list):
                raise RuntimeError("Cleanup fehlgeschlagen")
            if isinstance(points_selector, list):
                for pid in points_selector:
                    storage.pop(pid, None)

    namespace = {
        "_require_store": lambda: None,
        "_scroll": fake_scroll,
        "Filter": Filter, "FieldCondition": FieldCondition, "MatchValue": MatchValue,
        "cats_from_payload": lambda payload: list(payload.get("categories") or []),
        "norm_cats": lambda categories, category: list(categories or []),
        "iso_now": lambda: "neu", "payload_layer": lambda payload: payload.get("layer", "langzeit"),
        "make_doc_id": lambda user_id, new_title: target_id,
        "category_parents": lambda categories: categories[:1],
        "chunk_text": lambda text: [f"neu-{i}" for i in range(new_chunks)],
        "_guard_embed_budget": lambda count: None,
        "embed_many": lambda texts, task: [[i] for i, _ in enumerate(texts)],
        "embed_input": lambda title, categories, chunk: chunk,
        "PointStruct": PointStruct, "point_id": lambda doc_id, index: f"{doc_id}:{index}",
        "_upsert_batched": fake_upsert,
        "_delete_doc": fake_delete_doc, "_restore_doc_snapshot": fake_restore,
        "_bm25_invalidate": lambda: None, "qc": FakeQc(), "COLLECTION": "brain",
        "HTTPException": HTTPException, "checkpoint": lambda *args, **kwargs: None,
        "_log": lambda *args, **kwargs: None, "logging": logging, "time": time,
    }
    update = load_update_function(namespace)
    error = None
    try:
        update(SimpleNamespace(doc_id=source_id, user_id="frank", categories=["Geräte"],
                               text="NEUER TEXT", title=title))
    except HTTPException as exc:
        error = exc

    if error is not None:
        check(set(storage) == set(original), "Rollback muss exakt dieselben Chunk-IDs wiederherstellen")
        check(all(storage[key].payload == original[key].payload and storage[key].vector == original[key].vector
                  for key in original), "Rollback muss Payload und Vektor jedes alten Chunks wiederherstellen")
    return storage, error


def main() -> int:
    _, partial_error = run_case(old_chunks=3, new_chunks=3, partial_upsert=True)
    check(partial_error is not None, "Teilweiser Mehrbatch-Upsert muss fehlschlagen und rollen")

    _, cleanup_error = run_case(old_chunks=3, new_chunks=1, cleanup_failure=True)
    check(cleanup_error is not None, "Fehlgeschlagenes Restchunk-Cleanup muss vollständig rollen")

    _, missing_error = run_case(old_chunks=2, new_chunks=3, missing_last=True)
    check(missing_error is not None, "Fehlender mittlerer/letzter Chunk darf nicht bestätigt werden")

    migrated, migration_error = run_case(old_chunks=2, new_chunks=2, title="Neu")
    check(migration_error is None, "Vollständige Titelmigration muss erfolgreich sein")
    check(all(point.payload["doc_id"] == "doc-new" for point in migrated.values()) and len(migrated) == 2,
          "Titelmigration darf keine alte doc_id oder Restchunks hinterlassen")

    lock_namespace = {"threading": threading, "wraps": wraps, "HTTPException": HTTPException}
    lock_nodes = []
    for node in ast.parse(SOURCE).body:
        if isinstance(node, ast.Assign) and any(isinstance(target, ast.Name) and target.id in {"_ENTRY_WRITE_LOCK", "ENTRY_WRITE_LOCK_TIMEOUT_S"}
                                                for target in node.targets):
            lock_nodes.append(node)
        elif isinstance(node, ast.FunctionDef) and node.name == "serialized_entry_write":
            lock_nodes.append(node)
    exec(compile(ast.Module(body=lock_nodes, type_ignores=[]), str(APP_PATH), "exec"), lock_namespace)
    serialized = lock_namespace["serialized_entry_write"]
    entered_a = threading.Event()
    release_a = threading.Event()
    entered_b = threading.Event()
    order = []

    @serialized
    def controlled(name: str):
        order.append(f"{name}-enter")
        if name == "A":
            entered_a.set()
            check(release_a.wait(2), "Testfreigabe für A fehlt")
        else:
            entered_b.set()
        order.append(f"{name}-exit")

    thread_a = threading.Thread(target=controlled, args=("A",))
    thread_b = threading.Thread(target=controlled, args=("B",))
    thread_a.start()
    check(entered_a.wait(1), "Thread A muss den Schreib-Lock betreten")
    thread_b.start()
    check(not entered_b.wait(0.1), "Thread B darf einen laufenden Schreibvorgang nicht überholen")
    release_a.set()
    thread_a.join(2)
    thread_b.join(2)
    check(order == ["A-enter", "A-exit", "B-enter", "B-exit"],
          "Parallele Schreibvorgänge müssen vollständig serialisiert sein")

    lock_namespace["ENTRY_WRITE_LOCK_TIMEOUT_S"] = 0.05
    timeout_entered = threading.Event()
    timeout_release = threading.Event()
    timed_calls = []
    timed_errors = []

    @serialized
    def timed(name: str):
        timed_calls.append(name)
        if name == "A":
            timeout_entered.set()
            timeout_release.wait(1)

    timed_a = threading.Thread(target=timed, args=("A",))
    def run_timed_b():
        try:
            timed("B")
        except HTTPException as exc:
            timed_errors.append(exc)

    timed_b = threading.Thread(target=run_timed_b)
    timed_a.start()
    check(timeout_entered.wait(1), "Timeout-Test muss Lockhalter A starten")
    timed_b.start()
    timed_b.join(1)
    check(len(timed_errors) == 1 and timed_errors[0].status_code == 503,
          "Ein zu lange wartender Schreibvorgang muss vor Clienttimeout mit 503 abbrechen")
    timeout_release.set()
    timed_a.join(1)
    check(timed_calls == ["A"], "Ein abgebrochener Lock-Warter darf später niemals doch schreiben")

    required_locked = (
        "store", "entry_validity", "entries_touch", "promote_layer", "rename_category",
        "detach_category", "set_entry_category", "set_entry_categories", "forget",
        "delete_entry", "update_entry", "trash_restore", "backfill_parent", "purge_user",
        "trash_edit", "trash_empty", "reembed_all",
    )
    tree = ast.parse(SOURCE)
    functions = {node.name: node for node in tree.body if isinstance(node, ast.FunctionDef)}
    for name in required_locked:
        check(name in functions, f"Mutierender Endpoint fehlt im Lock-Test: {name}")
        check(any(isinstance(decorator, ast.Name) and decorator.id == "serialized_entry_write"
                  for decorator in functions[name].decorator_list),
              f"Mutierender Endpoint ist nicht serialisiert: {name}")

    print("PASS: PUT /entry rollt Teilfehler zurück; alle Eintrags-Schreibwege sind serialisiert")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL: {exc}")
        raise SystemExit(1)
