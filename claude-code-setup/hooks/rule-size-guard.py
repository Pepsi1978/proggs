#!/usr/bin/env python3
# rule-size-guard: blockiert Write/Edit auf ~/.claude/rules/*.md und claude-code-setup/rules/*.md,
# wenn das Ergebnis > 1536 Byte (1,5 KB) waere. Regel: rule-size-limit.md. Poka-Yoke Stufe 2.
# WICHTIG (claude-hooks.md 1.6): NICHT exit 2 zum Blocken -> permissionDecision=deny + exit 0.
# Detail-Volltexte in claude-code-setup/docs/rules/ sind ausgenommen (unbegrenzt).
# Notaus: leere Datei rule-size-guard-disable.flag im TEMP -> nie blockieren.
import json, sys, os

LIMIT = 1536

def main():
    try:
        d = json.load(sys.stdin)
    except Exception:
        return
    tool = d.get('tool_name', '') or ''
    if tool not in ('Write', 'Edit'):
        return
    ti = d.get('tool_input') or {}
    fp = (ti.get('file_path') or '').replace('\\', '/')
    if not fp.endswith('.md'):
        return
    is_rule = ('/.claude/rules/' in fp or '/claude-code-setup/rules/' in fp)
    if not is_rule or '/docs/rules/' in fp:
        return
    tmp = os.environ.get('TEMP') or os.environ.get('TMPDIR') or '/tmp'
    if os.path.exists(os.path.join(tmp, 'rule-size-guard-disable.flag')):
        return
    if tool == 'Write':
        size = len((ti.get('content') or '').encode('utf-8'))
    else:  # Edit: aktuelle Groesse - old_string + new_string
        try:
            cur = os.path.getsize(fp) if os.path.exists(fp) else 0
        except Exception:
            cur = 0
        oldb = len((ti.get('old_string') or '').encode('utf-8'))
        newb = len((ti.get('new_string') or '').encode('utf-8'))
        size = cur - oldb + newb
    if size > LIMIT:
        name = os.path.basename(fp)
        reason = ("STOPP - Regel-Groessenlimit (rule-size-limit.md): '%s' waere %d Byte (max %d = 1,5 KB). "
                  "Regel-Dateien duerfen max 1,5 KB haben. Kuerze den Inhalt ODER lagere Details nach "
                  "claude-code-setup/docs/rules/%s aus (Kern in die Regel, Verweis auf den Volltext). "
                  "Notaus bei Fehlalarm: leere Datei rule-size-guard-disable.flag im TEMP anlegen."
                  % (name, size, LIMIT, name))
        out = {"hookSpecificOutput": {"hookEventName": "PreToolUse",
                                      "permissionDecision": "deny",
                                      "permissionDecisionReason": reason}}
        sys.stdout.write(json.dumps(out))

if __name__ == '__main__':
    try:
        main()
    except Exception:
        pass
    sys.exit(0)
