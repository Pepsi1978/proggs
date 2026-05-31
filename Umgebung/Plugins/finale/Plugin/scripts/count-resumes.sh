#!/usr/bin/env bash
# finale Resume-Counter (FIN-055).
#
# Zaehlt, wie oft die Orchestrator-Chef-Rettung (Schicht 3, FIN-052) in einem Lauf greifen
# musste. Liest die audit-log.md der App und zaehlt die Pflicht-Marker
# "orchestrator-resume-after-crash". Das ist die Fruehwarn-Metrik: sinkende Resume-Zahlen ueber
# Laeufe = die Praevention (Schichten 1+2) wirkt; steigende = irgendwo ist ein Scope zu gross.
#
# Usage: bash count-resumes.sh <app-root>
#   z.B. bash count-resumes.sh ~/proggs/BestJournalAndroid
# Gibt JSON nach stdout + menschenlesbare Zeile nach stderr. Exit 0 immer.

APP_ROOT="${1:-.}"
LOG="$APP_ROOT/.android-shield/audit-log.md"

if [ ! -f "$LOG" ]; then
  echo "{\"resume_count\":0,\"found\":false,\"audit_log\":\"$LOG\"}"
  echo "Resume-Counter: kein audit-log unter $LOG gefunden -> 0 Resumes." >&2
  exit 0
fi

# Datei nur EINMAL scannen (Perf, Loop-2): Marker-Zeilen einmal holen, dann count UND per_phase
# aus dieser kleinen In-Memory-Variable ableiten (vorher 2 separate Datei-Scans). Werte identisch.
# count via grep -o | wc -l = tatsaechliche VORKOMMEN (robust falls >1 Marker pro Zeile), nicht
# nur Zeilen mit Treffer (was grep -c taete) -> korrekte Haeufigkeit bleibt erhalten.
marker_lines=$(grep "orchestrator-resume-after-crash" "$LOG" 2>/dev/null)
count=$(printf '%s\n' "$marker_lines" | grep -o "orchestrator-resume-after-crash" | wc -l | tr -d ' ')
count=${count:-0}
per_phase=$(printf '%s\n' "$marker_lines" \
            | grep -oE "phase=[^ |]+" | sort | uniq -c | tr -s ' ' | paste -sd ';' - 2>/dev/null)

echo "{\"resume_count\":$count,\"found\":true,\"audit_log\":\"$LOG\",\"per_phase\":\"${per_phase}\"}"

if [ "$count" = "0" ]; then
  echo "Resume-Counter: 0 Chef-Rettungen — sauberer Lauf, kein Worker-Crash." >&2
else
  echo "Resume-Counter: $count Chef-Rettung(en) gegriffen. Pro Phase: ${per_phase:-n/a}" >&2
fi
exit 0
