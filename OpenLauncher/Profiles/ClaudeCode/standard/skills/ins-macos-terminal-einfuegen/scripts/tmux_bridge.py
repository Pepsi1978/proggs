#!/usr/bin/env python3
"""Einzelaufrufe für eine bestätigte tmux-Sitzung; keine automatische Bereitschaftserkennung."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import uuid

FIELDS = ('socket', 'server_pid', 'session', 'pane', 'pane_pid', 'cwd',
          'dead', 'in_mode', 'input_off', 'bracket_paste', 'command', 'cursor_x', 'cursor_y')
FORMAT = '\t'.join('#{' + key + '}' for key in (
    'socket_path', 'pid', 'session_id', 'pane_id', 'pane_pid', 'pane_current_path',
    'pane_dead', 'pane_in_mode', 'pane_input_off', 'bracket_paste_flag',
    'pane_current_command', 'cursor_x', 'cursor_y'))


def run(args, data=None):
    result = subprocess.run(args, input=data, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=10)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', 'replace').strip() or 'Werkzeug fehlgeschlagen')
    return result.stdout.decode('utf-8', 'replace')


def emit(value):
    print(json.dumps(value, ensure_ascii=False))


def proc(pid):
    return run(['/bin/ps', '-p', str(pid), '-o', 'lstart=', '-o', 'comm=']).strip()


def processes():
    rows = {}
    for line in run(['/bin/ps', '-axo', 'pid=,ppid=,comm=']).splitlines():
        parts = line.strip().split(None, 2)
        if len(parts) == 3:
            rows[int(parts[0])] = (int(parts[1]), parts[2])
    return rows


def descendant(pid, root, rows):
    seen = set()
    while pid and pid not in seen:
        if pid == root:
            return True
        seen.add(pid)
        pid = rows.get(pid, (0, ''))[0]
    return False


def meta(tmux, pane):
    values = run(tmux + ['display-message', '-p', '-t', pane, FORMAT]).rstrip('\n').split('\t')
    if len(values) != len(FIELDS):
        raise RuntimeError('Unerwartetes tmux-Format; Zuordnung nicht möglich')
    return dict(zip(FIELDS, values))


def identity(info, agent_pid):
    if info['dead'] != '0':
        raise RuntimeError('Pane ist beendet')
    if not descendant(agent_pid, int(info['pane_pid']), processes()):
        raise RuntimeError('Agent gehört nicht mehr zu diesem Pane')
    return {key: info[key] for key in ('socket', 'server_pid', 'session', 'pane', 'pane_pid')} | {
        'cwd': os.path.realpath(info['cwd']), 'agent_pid': agent_pid,
        'server_process': proc(info['server_pid']), 'pane_process': proc(info['pane_pid']),
        'agent_process': proc(agent_pid)}


def save(path, state):
    fd, temporary = tempfile.mkstemp(prefix='.bridge-', dir=path.parent)
    try:
        with os.fdopen(fd, 'w') as file:
            json.dump(state, file, ensure_ascii=False)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def input_frame(screen):
    rows = screen.splitlines()
    separators = [i for i, row in enumerate(rows) if re.fullmatch(r'─{20,}', row.strip())]
    if len(separators) < 2:
        raise RuntimeError('Claude-Eingaberahmen nicht eindeutig erkannt')
    top, bottom = separators[-2:]
    field = '\n'.join(rows[top + 1:bottom]).strip()
    if not field.startswith('❯'):
        raise RuntimeError('Kein Claude-Eingabefeld im Rahmen')
    return rows, top, bottom, field[1:].lstrip(' \u00a0')


def normalize_clock(screen):
    # Nur Laufzeit in der bekannten Launcher-Fußzeile unter dem letzten Eingaberahmen.
    # Antworttext, Preis, Modell, Pfad, Limits und unbekannte Footer bleiben unverändert.
    try:
        rows, _, bottom, _ = input_frame(screen)
    except RuntimeError:
        return screen
    pattern = r'^(  📁 .+   💰 \$[0-9.]+   ⏳ )([0-9]+[hms])+(   🏷️ v[0-9.]+) *$'
    for i in range(bottom + 1, len(rows)):
        rows[i] = re.sub(pattern, r'\1<Laufzeit>\3', rows[i])
    return '\n'.join(rows)


def surroundings(screen):
    rows, top, bottom, _ = input_frame(normalize_clock(screen))
    # Ausschließlich diese beiden bekannten Hinweise wechseln bei Claudes Pasteblock.
    footer = rows[bottom:]
    hints = {'  paste again to expand',
             '  ⏵⏵ bypass permissions on (shift+tab to cycle) · ← for agents'}
    footer = ['<bekannter Eingabehinweis>' if row in hints else row for row in footer]
    return '\n'.join(rows[:top + 1] + footer)


def cancelled(args):
    if args.cancel_file and args.cancel_file.exists():
        raise RuntimeError('Abgebrochen: Cancel-Datei vorhanden; keine weitere Zustellung')


def snapshot(tmux, info):
    screen = run(tmux + ['capture-pane', '-p', '-t', info['pane']])
    digest = hashlib.sha256((json.dumps(info, sort_keys=True) + normalize_clock(screen)).encode()).hexdigest()
    return screen, digest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['list', 'bind', 'read', 'paste', 'enter', 'submit'])
    parser.add_argument('--socket', help='Expliziter tmux-Socket; beim ersten list optional')
    parser.add_argument('--state', type=Path, help='Private Laufzeitdatei außerhalb des Repos')
    parser.add_argument('--pane')
    parser.add_argument('--agent-pid', type=int)
    parser.add_argument('--cwd')
    parser.add_argument('--observed', help='Token aus einer gerade inhaltlich geprüften read-Ausgabe')
    parser.add_argument('--id', help='Eindeutige Auftragskennung, z. B. C17')
    parser.add_argument('--text-file', type=Path)
    parser.add_argument('--force-view', action='store_true', help='Momentaufnahme auch bei gleichem Inhalt ausgeben')
    parser.add_argument('--verbose', action='store_true')
    parser.add_argument('--wait', type=float, default=0, help='read: maximal 10 Sekunden auf Änderung warten')
    parser.add_argument('--cancel-file', type=Path)
    parser.add_argument('--sha256', help='submit: Hash des vollständig autorisierten Textes')
    parser.add_argument('--lines', type=int, default=0)
    parser.add_argument('--max-chars', type=int, default=6000)
    args = parser.parse_args()
    binary = shutil.which('tmux')
    if not binary:
        raise RuntimeError('tmux fehlt; keine Ersatzsitzung starten')
    tmux = [binary] + (['-S', args.socket] if args.socket else [])
    if args.action == 'list':
        rows = processes()
        panes = run(tmux + ['list-panes', '-a', '-F', '#{pane_id}']).splitlines()
        for pane in panes:
            info = meta(tmux, pane)
            info['processes'] = [{'pid': pid, 'command': command} for pid, (_, command) in rows.items()
                                 if descendant(pid, int(info['pane_pid']), rows)]
            emit(info)
        return
    if args.state is None:
        parser.error('--state fehlt')
    # Ein privates Verzeichnis anlegen (mktemp -d); verhindert versehentliche Mitbenutzung.
    if not args.state.parent.is_dir() or args.state.parent.stat().st_mode & 0o077:
        raise RuntimeError('State-Verzeichnis muss vorhanden und privat sein (chmod 700)')
    if args.cancel_file and args.cancel_file.parent.resolve() != args.state.parent.resolve():
        parser.error('--cancel-file muss im privaten State-Verzeichnis liegen')
    if not 0 <= args.wait <= 10:
        parser.error('--wait muss zwischen 0 und 10 Sekunden liegen')
    cancelled(args)
    with open(str(args.state) + '.lock', 'a') as lock:
        os.chmod(lock.name, 0o600)
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if args.action == 'bind':
            if args.state.exists():
                raise RuntimeError('Bindung existiert; für bewusste Neuzuordnung eine neue State-Datei verwenden')
            if not args.socket or not args.pane or not args.cwd or not args.agent_pid:
                parser.error('bind braucht --socket, --pane, --agent-pid und --cwd')
            if not re.fullmatch(r'%\d+', args.pane):
                parser.error('Exakte Pane-ID erforderlich, kein Index oder Name')
            info = meta(tmux, args.pane)
            bound = identity(info, args.agent_pid)
            if bound['cwd'] != os.path.realpath(args.cwd):
                raise RuntimeError('Arbeitsverzeichnis stimmt nicht mit dem bestätigten Ziel überein')
            save(args.state, {'identity': bound, 'deliveries': {}})
            emit({'bound': bound})
            return
        state = json.loads(args.state.read_text())
        bound = state['identity']
        tmux = [binary, '-S', bound['socket']]
        info = meta(tmux, bound['pane'])
        if identity(info, bound['agent_pid']) != bound:
            raise RuntimeError('Ziel/Prozess/Arbeitsverzeichnis geändert; zuerst neu zuordnen')
        screen, observed = snapshot(tmux, info)
        if args.action == 'read':
            if not 0 <= args.lines <= 500 or not 1000 <= args.max_chars <= 30000:
                parser.error('Grenzen: --lines 0..500, --max-chars 1000..30000')
            started = time.monotonic()
            while True:
                cancelled(args)
                text = screen if args.lines == 0 else run(tmux + [
                    'capture-pane', '-p', '-t', bound['pane'], '-S', str(-args.lines)])
                digest = hashlib.sha256(normalize_clock(text).encode()).hexdigest()
                changed = digest != state.get('last_view')
                delta = {k: v for k, v in info.items() if state.get('last_state', {}).get(k) != v}
                if changed or delta or args.force_view or time.monotonic() - started >= args.wait:
                    break
                time.sleep(min(0.25, max(0, args.wait - (time.monotonic() - started))))
                cancelled(args)
                info = meta(tmux, bound['pane'])
                if identity(info, bound['agent_pid']) != bound:
                    raise RuntimeError('Identität während des Wartens geändert')
                screen, observed = snapshot(tmux, info)
            result = {'event': 'snapshot' if changed or delta or args.force_view else 'unchanged',
                      'observed': observed}
            if args.wait:
                result['waited_ms'] = round((time.monotonic() - started) * 1000)
            if args.verbose:
                result['state'] = info
            elif delta:
                result['state_changes'] = delta
            if changed or args.force_view:
                if len(text) > args.max_chars:
                    half = args.max_chars // 2
                    result.update(text=text[:half] + '\n[… Mittelteil ausgelassen …]\n' + text[-half:],
                                  omitted_chars=len(text) - 2 * half)
                else:
                    result['text'] = text
            state['last_view'] = digest
            state['last_state'] = info
            save(args.state, state)
            emit(result)
            return
        if args.observed != observed:
            raise RuntimeError('Momentaufnahme geändert/veraltet; erneut lesen, nicht blind zustellen')
        if info['in_mode'] != '0' or info['input_off'] != '0':
            raise RuntimeError('Pane nimmt keine normale Eingabe an')
        if not args.id or not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.id):
            parser.error('--id muss eine kurze eindeutige Auftragskennung sein')
        def recheck():
            cancelled(args)
            latest = meta(tmux, bound['pane'])
            if identity(latest, bound['agent_pid']) != bound or snapshot(tmux, latest)[1] != args.observed:
                raise RuntimeError('Ziel oder Ansicht unmittelbar vor Eingabe geändert; erneut lesen')

        deliveries = state['deliveries']
        if args.action in ('paste', 'submit'):
            if args.id in deliveries:
                raise RuntimeError('Kennung bereits versucht; zuerst Zustellung klären, nicht erneut einfügen')
            if args.text_file is None:
                parser.error('--text-file fehlt')
            text = args.text_file.read_text(encoding='utf-8')
            if not text.strip() or len(text.encode()) > 65536:
                raise RuntimeError('Leerer oder zu großer Auftrag')
            if any((ord(c) < 32 and c not in '\n\t') or ord(c) == 127 for c in text):
                raise RuntimeError('Steuerzeichen im Auftrag nicht erlaubt')
            text_hash = hashlib.sha256(text.encode()).hexdigest()
            if args.action == 'submit':
                if args.sha256 != text_hash:
                    raise RuntimeError('Auftragshash fehlt oder Inhalt nach Autorisierung geändert')
                if any(d['status'] in ('pasted', 'paste_attempted', 'enter_attempted') for d in deliveries.values()):
                    raise RuntimeError('Andere Zustellung noch offen; zuerst klären')
                frame = input_frame(screen)
                if frame[3] != '' or info['cursor_x'] != '2' or int(info['cursor_y']) != frame[1] + 1:
                    raise RuntimeError('submit verlangt ein eindeutig leeres Eingabefeld; auch Ghosts zuerst klären')
                before = screen
            # paste-buffer -p klammert nur bei bereits aktivierter Unterstützung des Zielprogramms.
            if info['bracket_paste'] != '1':
                raise RuntimeError('Bracketed Paste ist nicht aktiv; nicht mit Zeilenumbrüchen experimentieren')
            buffer = 'codex-' + uuid.uuid4().hex
            try:
                run(tmux + ['load-buffer', '-b', buffer, '-'], text.encode())
                recheck()
                deliveries[args.id] = {'status': 'paste_attempted', 'sha256': text_hash}
                save(args.state, state)
                run(tmux + ['paste-buffer', '-p', '-r', '-b', buffer, '-t', bound['pane']])
            finally:
                run(tmux + ['delete-buffer', '-b', buffer])
            deliveries[args.id]['status'] = 'pasted'
            save(args.state, state)
            if args.action == 'submit':
                deadline = time.monotonic() + 1
                while True:
                    cancelled(args)
                    latest = meta(tmux, bound['pane'])
                    if identity(latest, bound['agent_pid']) != bound:
                        raise RuntimeError('Identität nach Paste geändert; kein Enter')
                    after, after_token = snapshot(tmux, latest)
                    draft = input_frame(after)[3]
                    own = draft == text or re.fullmatch(r'\[Pasted text #\d+\]', draft)
                    if own and surroundings(after) == surroundings(before):
                        break
                    if draft or time.monotonic() >= deadline:
                        emit({'id': args.id, 'transport': 'pasted', 'submitted': False,
                              'reason': 'Entwurf/Umgebung nicht eindeutig; lesen, kein automatisches Enter'})
                        return
                    time.sleep(0.05)
                args.observed = after_token
                recheck()
                deliveries[args.id]['status'] = 'enter_attempted'
                save(args.state, state)
                run(tmux + ['send-keys', '-t', bound['pane'], 'Enter'])
                deliveries[args.id]['status'] = 'enter_sent'
                save(args.state, state)
                emit({'id': args.id, 'transport': 'enter_sent'})
            else:
                emit({'id': args.id, 'transport': 'pasted', 'submitted': False})
        else:
            if deliveries.get(args.id, {}).get('status') != 'pasted':
                raise RuntimeError('Kein eindeutig eingefügter, noch ungesendeter Auftrag für diese Kennung')
            recheck()
            deliveries[args.id]['status'] = 'enter_attempted'
            save(args.state, state)
            run(tmux + ['send-keys', '-t', bound['pane'], 'Enter'])
            deliveries[args.id]['status'] = 'enter_sent'
            save(args.state, state)
            emit({'id': args.id, 'transport': 'enter_sent',
                  'next': 'Annahme durch Claude anhand neuer Ausgabe prüfen; kein Zustellungsbeleg allein'})


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, ValueError, subprocess.TimeoutExpired) as error:
        emit({'error': str(error), 'action': 'Anhalten und lesen; keine automatische Wiederholung'})
        sys.exit(1)
