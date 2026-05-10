#!/usr/bin/env python3
"""
strategy-tracker.py — Strategie-Erfolg-Tracking fuer /self-improve (I7, 2026-05-10)

Hintergrund:
Darwin Goedel Machine (arXiv 2505.22954): Agent verbessert nicht nur seinen Code,
sondern auch die Logik seiner Verbesserungs-Prozesse. SWE-bench 20%→50%.

Adaption fuer /self-improve:
- Self-improve-cache wird zu Evolutions-Archiv
- Welche Strategie wurde in welchem Lauf verwendet? (thorough/standard/focus)
- Welche Findings wurden umgesetzt (USER sagte JA)?
- Welche wurden umgesetzt aber wirkten NICHT?
- Naechster Lauf bevorzugt Strategien mit hoher Erfolgsrate.

Verwendung:
  python3 strategy-tracker.py log <date> <strategy> <total_findings> <accepted> <implemented>
  python3 strategy-tracker.py rate-finding <finding_id> <success_score>  # 1-5
  python3 strategy-tracker.py history             # Strategie-Statistik
  python3 strategy-tracker.py best-strategy       # Welche Strategie war erfolgreichster?

Datei: ~/.claude/self-improve-cache/strategies.json (laeuft NICHT ueber Git)
"""

import json
import os
import sys
from pathlib import Path
from datetime import datetime, timezone

CACHE_DIR = Path(os.path.expanduser("~/.claude/self-improve-cache"))
STRATEGIES_FILE = CACHE_DIR / "strategies.json"


def load() -> dict:
    if not STRATEGIES_FILE.exists():
        return {"runs": [], "findings": {}}
    try:
        with open(STRATEGIES_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {"runs": [], "findings": {}}


def save(data: dict) -> None:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    # Atomic write
    tmp = STRATEGIES_FILE.with_suffix(".tmp")
    with open(tmp, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    os.replace(str(tmp), str(STRATEGIES_FILE))


def log_run(date: str, strategy: str, total: int, accepted: int, implemented: int) -> None:
    data = load()
    data["runs"].append({
        "date": date,
        "strategy": strategy,
        "total_findings": int(total),
        "accepted": int(accepted),
        "implemented": int(implemented),
        "acceptance_rate": int(accepted) / max(int(total), 1),
        "implementation_rate": int(implemented) / max(int(accepted), 1) if int(accepted) > 0 else 0,
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    })
    save(data)
    print(f"Logged: {strategy} run on {date} — {accepted}/{total} accepted, {implemented} implemented")


def rate_finding(finding_id: str, score: int) -> None:
    """Bewertet wie nuetzlich sich ein Finding spaeter erwiesen hat. 1=schlecht, 5=durchbruch."""
    data = load()
    if finding_id not in data["findings"]:
        data["findings"][finding_id] = {"scores": [], "first_seen": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")}
    data["findings"][finding_id]["scores"].append({
        "score": int(score),
        "rated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    })
    avg = sum(s["score"] for s in data["findings"][finding_id]["scores"]) / len(data["findings"][finding_id]["scores"])
    data["findings"][finding_id]["avg_score"] = round(avg, 2)
    save(data)
    print(f"Rated {finding_id}: score {score}, avg now {avg:.2f}")


def history() -> None:
    data = load()
    if not data["runs"]:
        print("Keine Runs aufgezeichnet.")
        return
    print(f"Total runs: {len(data['runs'])}")
    print(f"Total findings tracked: {len(data['findings'])}")
    by_strategy = {}
    for run in data["runs"]:
        s = run["strategy"]
        if s not in by_strategy:
            by_strategy[s] = {"count": 0, "accept_sum": 0, "impl_sum": 0}
        by_strategy[s]["count"] += 1
        by_strategy[s]["accept_sum"] += run["acceptance_rate"]
        by_strategy[s]["impl_sum"] += run["implementation_rate"]
    print("\nPer-Strategy-Stats:")
    for s, stats in by_strategy.items():
        n = stats["count"]
        avg_accept = stats["accept_sum"] / n
        avg_impl = stats["impl_sum"] / n
        print(f"  {s}: {n} runs, avg-acceptance {avg_accept:.2%}, avg-implementation {avg_impl:.2%}")


def best_strategy() -> None:
    data = load()
    if len(data["runs"]) < 3:
        print("Brauche min. 3 Runs fuer Strategie-Empfehlung.")
        return
    by_strategy = {}
    for run in data["runs"]:
        s = run["strategy"]
        if s not in by_strategy:
            by_strategy[s] = {"count": 0, "score_sum": 0}
        by_strategy[s]["count"] += 1
        # Score = acceptance * implementation (beide hoch = beste Strategie)
        by_strategy[s]["score_sum"] += run["acceptance_rate"] * run["implementation_rate"]
    ranked = sorted(
        [(s, stats["score_sum"] / stats["count"]) for s, stats in by_strategy.items()],
        key=lambda x: x[1], reverse=True
    )
    print("Strategie-Ranking (Score = acceptance * implementation):")
    for s, score in ranked:
        print(f"  {s}: {score:.4f}")
    print(f"\nEmpfehlung: '{ranked[0][0]}' war bisher am erfolgreichsten")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "log" and len(sys.argv) >= 7:
        log_run(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6])
    elif cmd == "rate-finding" and len(sys.argv) >= 4:
        rate_finding(sys.argv[2], sys.argv[3])
    elif cmd == "history":
        history()
    elif cmd == "best-strategy":
        best_strategy()
    else:
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()
