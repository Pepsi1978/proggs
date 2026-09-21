#!/usr/bin/env python3
"""Einzelaufrufe für eine bestätigte tmux-Sitzung; keine automatische Bereitschaftserkennung."""
import argparse
import difflib
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import uuid

FIELDS = ('socket', 'server_pid', 'session', 'pane', 'pane_pid', 'cwd',
          'dead', 'in_mode', 'input_off', 'bracket_paste', 'command', 'cursor_x', 'cursor_y', 'title')
FORMAT = '\t'.join('#{' + key + '}' for key in (
    'socket_path', 'pid', 'session_id', 'pane_id', 'pane_pid', 'pane_current_path',
    'pane_dead', 'pane_in_mode', 'pane_input_off', 'bracket_paste_flag',
    'pane_current_command', 'cursor_x', 'cursor_y', 'pane_title'))
MAX_LITERAL_BYTES = 512
MAX_HANDOFF_BYTES = 8 * 1024 * 1024


class BridgeError(RuntimeError):
    def __init__(self, code, message):
        super().__init__(message)
        self.code = code


class Parser(argparse.ArgumentParser):
    def error(self, message):
        raise BridgeError('E_ARGS', message)


CHECK_CANCEL = None

def run(args, data=None, cleanup=False):
    if CHECK_CANCEL and not cleanup:
        CHECK_CANCEL()
    result = subprocess.run(args, input=data, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=10)
    if result.returncode:
        raise BridgeError('E_TOOL', result.stderr.decode('utf-8', 'replace').strip()[:240] or 'Werkzeug fehlgeschlagen')
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
        raise BridgeError('E_STATE', 'Unerwartetes tmux-Format; Zuordnung nicht möglich')
    return dict(zip(FIELDS, values))


def identity(info, agent_pid):
    if info['dead'] != '0':
        raise BridgeError('E_TARGET', 'Pane ist beendet')
    if not descendant(agent_pid, int(info['pane_pid']), processes()):
        raise BridgeError('E_TARGET', 'Agent gehört nicht mehr zu diesem Pane')
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


def regular_utf8(path, max_bytes, allow_cr=False):
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    with os.fdopen(fd, 'rb') as source:
        if not stat.S_ISREG(os.fstat(source.fileno()).st_mode):
            raise BridgeError('E_TEXT', 'Auftrag muss eine reguläre Datei ohne Symlink sein')
        data = source.read(max_bytes + 1)
    if len(data) > max_bytes:
        raise BridgeError('E_TEXT', f'Datei überschreitet die technische Grenze von {max_bytes} Bytes')
    try:
        text = data.decode('utf-8')
    except UnicodeDecodeError as error:
        raise BridgeError('E_TEXT', 'Datei ist kein gültiges UTF-8') from error
    allowed = '\n\t\r' if allow_cr else '\n\t'
    if any((ord(c) < 32 and c not in allowed) or ord(c) == 127 for c in text):
        raise BridgeError('E_TEXT', 'Steuerzeichen im Auftrag nicht erlaubt')
    return text, data


def delivery_marker(delivery, delivery_id, source_path):
    if delivery.get('payload_sha256'):
        marker = f"{delivery_id}: Lies {delivery.get('display_path', '')}"
    else:
        if not source_path or not source_path.exists():
            raise BridgeError('E_TEXT', 'Quelltext des offenen Einfügeversuchs fehlt; Verlauf nicht sicher prüfbar')
        old_text, _ = regular_utf8(source_path, 65536)
        marker = old_text
    marker = re.sub(r'\s+', ' ', marker.strip())[:60]
    if not marker:
        raise BridgeError('E_TEXT', 'Kein markanter Auftragstext für die Verlaufsprüfung vorhanden')
    return marker


def marker_in_history(marker, history):
    return marker in re.sub(r'\s+', ' ', history)


def resume_workstate(path):
    try:
        fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
        with os.fdopen(fd, 'rb') as source:
            if not stat.S_ISREG(os.fstat(source.fileno()).st_mode):
                raise ValueError('keine reguläre Datei')
            data = source.read(1025)
        if len(data) > 1024:
            raise ValueError('größer als 1 KiB')
        parsed = json.loads(data.decode('utf-8'))
        if not isinstance(parsed, dict):
            raise ValueError('JSON-Wurzel ist kein Objekt')
        keys = ('ziel_rev', 'zugestellt', 'offen', 'phase', 'status')
        return {key: parsed[key] for key in keys if key in parsed}, None
    except (OSError, ValueError, UnicodeError, json.JSONDecodeError) as error:
        return None, str(error)[:240]


def resume_git(cwd):
    try:
        windows_git = shutil.which('git.exe') if cwd.startswith('/mnt/') else None
        if windows_git and shutil.which('wslpath'):
            git_cwd = run(['wslpath', '-w', cwd]).strip()
            command = [windows_git, '--no-optional-locks', '-C', git_cwd]
        else:
            native_git = shutil.which('git')
            if not native_git:
                raise BridgeError('E_TOOL', 'git fehlt')
            command = [native_git, '--no-optional-locks', '-C', cwd]
        status_rows = run(command + ['status', '-sb']).splitlines()
        result = {'status': status_rows[:20],
                  'log': run(command + ['log', '-1', '--oneline']).strip()}
        if len(status_rows) > 20:
            result['omitted_status_lines'] = len(status_rows) - 20
        return result, None
    except BridgeError as error:
        if error.code == 'E_STOP':
            raise
        return None, str(error)[:240]
    except (OSError, subprocess.TimeoutExpired) as error:
        return None, str(error)[:240]


def input_frame(screen):
    rows = screen.splitlines()
    separators = [i for i, row in enumerate(rows) if re.fullmatch(r'─{20,}', row.strip())]
    if len(separators) < 2:
        raise BridgeError('E_INPUT', 'Claude-Eingaberahmen nicht eindeutig erkannt')
    top, bottom = separators[-2:]
    field = '\n'.join(rows[top + 1:bottom]).strip()
    if not field.startswith(('❯', '>')):
        raise BridgeError('E_INPUT', 'Kein Claude-Eingabefeld im Rahmen')
    return rows, top, bottom, field[1:].lstrip(' \u00a0')


def normalize_clock(screen):
    # Nur Laufzeit, Uhrzeit und Restzeiten in erkannten Launcher-Fußzeilen unter dem Rahmen.
    # Antworttext, Preis, Modell, Pfad, Limits und unbekannte Footer bleiben unverändert.
    try:
        rows, _, bottom, _ = input_frame(screen)
    except RuntimeError:
        return screen
    pattern = r'^(  📁 .+   💰 \$[0-9.]+   ⏳ )([0-9]+[hms])+(   🏷️ v[0-9.]+) *$'
    for i in range(bottom + 1, len(rows)):
        rows[i] = re.sub(pattern, r'\1<Laufzeit>\3', rows[i])
        ctx = re.fullmatch(r'(  🧠 ctx [^┃]+┃ 🤖 [^┃]+┃ ⚡ [^┃]+┃ ⏱ 5h .+┃ 📅 7d .+┃ 🕐 )([0-2]\d:[0-5]\d)( *(?:/rc)? *)', rows[i])
        if ctx:
            row = ctx[1] + '<Uhrzeit>' + ctx[3]
            for label in ('⏱ 5h', '📅 7d'):
                row = re.sub(r'(' + re.escape(label) + r' [^()]*\()(?:\d+[DdHhms])+(\))', r'\1<Restzeit>\2', row)
            rows[i] = row
    return '\n'.join(rows)


def compact_edits(previous, rows):
    """Ordered sequence alignment; repeated lines keep their positions and multiplicity."""
    hashes = [hashlib.sha256(row.encode()).hexdigest() for row in rows]
    edits = []
    for tag, a, b, c, d in difflib.SequenceMatcher(None, previous, hashes, autojunk=False).get_opcodes():
        if tag != 'equal':
            edits.append({'old_rows': [a, b], 'new_rows': [c, d], 'text': '\n'.join(rows[c:d])})
    return hashes, edits


def read_status(info, screen):
    try:
        field = input_frame(screen)[3]
    except BridgeError:
        return None
    # These two animation frames were observed live in the same busy Claude session.
    # Normalize only the wake signal; raw metadata and write freshness stay untouched.
    title = re.sub(r'^[◐◑] ', '<busy> ', info['title'])
    data = [title, info['in_mode'], info['input_off'], field]
    return hashlib.sha256(json.dumps(data).encode()).hexdigest()


def surroundings(screen):
    rows, top, bottom, _ = input_frame(normalize_clock(screen))
    # Ausschließlich diese beiden bekannten Hinweise wechseln bei Claudes Pasteblock.
    footer = rows[bottom:]
    hints = {'  paste again to expand',
             '  ⏵⏵ bypass permissions on (shift+tab to cycle)',
             '  ⏵⏵ bypass permissions on (shift+tab to cycle) · ← for agents'}
    footer = ['<bekannter Eingabehinweis>' if row in hints else row for row in footer]
    return '\n'.join(rows[:top + 1] + footer)


def own_draft(draft, text):
    """Accept exact text or Claude's two-column continuation, never strip all whitespace."""
    if draft == text:
        return True
    if '\n' in text or '\n' not in draft:
        return False
    lines = draft.split('\n')
    if any(not line.startswith('  ') for line in lines[1:]):
        return False
    positions = {0}
    for index, line in enumerate(lines):
        segment = line if index == 0 else line[2:]
        next_positions = set()
        for pos in positions:
            # A word-wrap can replace exactly one space; a hard wrap replaces none.
            starts = (pos, pos + 1) if index and text[pos:pos + 1] == ' ' else (pos,)
            for start in starts:
                if text.startswith(segment, start):
                    next_positions.add(start + len(segment))
        positions = next_positions
        if not positions:
            return False
    return len(text) in positions


def same_surroundings(before, after):
    """Only tolerate top-of-screen loss caused by the input field growing."""
    old, top_old, bottom_old, _ = input_frame(before)
    new, top_new, bottom_new, _ = input_frame(after)
    growth = (bottom_new - top_new) - (bottom_old - top_old)
    lost = top_old - top_new
    if lost < 0 or lost > max(0, growth):
        return False
    if old[lost:top_old] != new[:top_new]:
        return False
    old_footer = surroundings(before).splitlines()[top_old + 1:]
    new_footer = surroundings(after).splitlines()[top_new + 1:]
    # Growing the field consumes trailing blank terminal rows, not footer content.
    while old_footer and old_footer[-1] == '':
        old_footer.pop()
    while new_footer and new_footer[-1] == '':
        new_footer.pop()
    return old_footer == new_footer


def cancelled(args):
    paths = ([args.state.parent / 'STOP'] if args.state else []) + ([args.cancel_file] if args.cancel_file else [])
    if any(path.exists() for path in paths):
        raise BridgeError('E_STOP', 'Stoppsignal vorhanden; keine weitere Zustellung')


def snapshot(tmux, info):
    screen = run(tmux + ['capture-pane', '-p', '-t', info['pane']])
    digest = hashlib.sha256((json.dumps(info, sort_keys=True) + normalize_clock(screen)).encode()).hexdigest()
    return screen, digest


def main():
    global CHECK_CANCEL
    parser = Parser(description=__doc__)
    parser.add_argument('action', choices=[
        'list', 'bind', 'read', 'resume', 'paste', 'enter', 'submit', 'submit-file', 'resolve'])
    parser.add_argument('--socket', help='Expliziter tmux-Socket; beim ersten list optional')
    parser.add_argument('--run', dest='run_dir', type=Path, help='Privater Dialogordner: target.json, ID.txt und STOP')
    parser.add_argument('--state', type=Path, help='Private Laufzeitdatei außerhalb des Repos')
    parser.add_argument('--pane')
    parser.add_argument('--agent-pid', type=int)
    parser.add_argument('--cwd')
    parser.add_argument('--observed', help='Token aus einer gerade inhaltlich geprüften read-Ausgabe')
    parser.add_argument('--id', help='Eindeutige Auftragskennung, z. B. C17')
    parser.add_argument('--text-file', type=Path)
    parser.add_argument('--payload-file', type=Path,
                        help='submit-file: lange vollständige UTF-8-Datei, die das Ziel selbst lesen kann')
    parser.add_argument('--display-path',
                        help='submit-file: kurzer absoluter Pfad in der Umgebung des Zielagenten')
    parser.add_argument('--force-view', action='store_true', help='Momentaufnahme auch bei gleichem Inhalt ausgeben')
    parser.add_argument('--compact', action='store_true', help='read: positionsgebundener Änderungsauszug, keine globale Zeilendeduplizierung')
    parser.add_argument('--verbose', action='store_true')
    parser.add_argument('--wait', type=float, default=0, help='read: maximal 10 Sekunden auf Änderung warten')
    parser.add_argument('--wait-mode', choices=['activity', 'status'], default='activity',
                        help='status: frühes Wecken durch Titel/Eingabefeld/Modus, sonst spätestens --wait; kein Fertigbeweis')
    parser.add_argument('--cancel-file', type=Path)
    parser.add_argument('--sha256',
                        help='submit/submit-file/resolve: Hash des autorisierten Textes beziehungsweise der Payload')
    parser.add_argument('--continued-as', help='resolve: neue Kennung, unter der der Inhalt fortgeführt wird')
    parser.add_argument('--manual-clear-confirmed', action='store_true',
                        help='resolve: bestätigt die beobachtete manuelle Entfernung aus dem Eingabefeld')
    parser.add_argument('--literal-line', action='store_true',
                        help='Einzeiligen Text ohne Steuerzeichen literal senden, wenn tmux keinen Paste-Status meldet')
    parser.add_argument('--lines', type=int, default=0)
    parser.add_argument('--max-chars', type=int, default=6000)
    args = parser.parse_args()
    if args.run_dir:
        if args.run_dir.is_symlink():
            parser.error('--run darf kein Symlink sein')
        derived_state = args.run_dir / 'target.json'
        if args.state and args.state.absolute() != derived_state.absolute():
            parser.error('--state widerspricht --run/target.json')
        args.state = derived_state
        if args.action in ('paste', 'submit', 'submit-file'):
            if not args.id or not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.id):
                parser.error('Gültige --id vor Ableitung der Textdatei erforderlich')
            if not args.sha256:
                parser.error('--run verlangt auch beim Einfügen den autorisierten --sha256')
            if args.action == 'submit-file':
                if not args.payload_file or not args.display_path:
                    parser.error('submit-file braucht --payload-file und --display-path')
            else:
                derived_text = args.run_dir / (args.id + '.txt')
                if args.text_file and args.text_file.absolute() != derived_text.absolute():
                    parser.error('--text-file widerspricht --run/ID.txt')
                args.text_file = derived_text
    CHECK_CANCEL = lambda: cancelled(args)
    CHECK_CANCEL()
    binary = shutil.which('tmux')
    if not binary:
        raise BridgeError('E_STATE', 'tmux fehlt; keine Ersatzsitzung starten')
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
        raise BridgeError('E_STATE', 'State-Verzeichnis muss vorhanden und privat sein (chmod 700)')
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
                raise BridgeError('E_STATE', 'Bindung existiert; für bewusste Neuzuordnung eine neue State-Datei verwenden')
            if not args.socket or not args.pane or not args.cwd or not args.agent_pid:
                parser.error('bind braucht --socket, --pane, --agent-pid und --cwd')
            if not re.fullmatch(r'%\d+', args.pane):
                parser.error('Exakte Pane-ID erforderlich, kein Index oder Name')
            info = meta(tmux, args.pane)
            bound = identity(info, args.agent_pid)
            if bound['cwd'] != os.path.realpath(args.cwd):
                raise BridgeError('E_TARGET', 'Arbeitsverzeichnis stimmt nicht mit dem bestätigten Ziel überein')
            save(args.state, {'identity': bound, 'deliveries': {}})
            emit({'bound': bound})
            return
        state = json.loads(args.state.read_text())
        bound = state['identity']
        tmux = [binary, '-S', bound['socket']]
        info = meta(tmux, bound['pane'])
        if identity(info, bound['agent_pid']) != bound:
            raise BridgeError('E_TARGET', 'Ziel/Prozess/Arbeitsverzeichnis geändert; zuerst neu zuordnen')
        screen, observed = snapshot(tmux, info)
        if args.action == 'resume':
            if not 1000 <= args.max_chars <= 30000:
                parser.error('Grenzen: --max-chars 1000..30000')
            result = {'event': 'resume', 'observed': observed}
            workstate, workstate_error = resume_workstate(args.state.parent / 'arbeitsstand.json')
            if workstate_error:
                result['arbeitsstand_error'] = workstate_error
            else:
                result['arbeitsstand'] = workstate
            deliveries = state.get('deliveries', {})
            result['offene_zustellungen'] = [
                {'id': delivery_id, 'status': delivery.get('status'),
                 **({'payload_bytes': delivery['payload_bytes']} if 'payload_bytes' in delivery else {})}
                for delivery_id, delivery in deliveries.items()
                if delivery.get('status') not in ('enter_sent', 'cleared')]
            result['letzte_id'] = next(reversed(deliveries), None) if deliveries else None
            git, git_error = resume_git(bound['cwd'])
            if git_error:
                result['git_error'] = git_error
            else:
                result['git'] = git
            cancelled(args)
            view_digest = hashlib.sha256(normalize_clock(screen).encode()).hexdigest()
            if len(screen) > args.max_chars:
                half = args.max_chars // 2
                result['pane'] = screen[:half] + '\n[… Mittelteil ausgelassen …]\n' + screen[-half:]
                result['omitted_chars'] = len(screen) - 2 * half
                state['last_rows'] = []
            else:
                result['pane'] = screen
                state['last_rows'] = [hashlib.sha256(row.encode()).hexdigest()
                                      for row in screen.splitlines()]
            state['last_view'] = view_digest
            state['last_lines'] = 0
            state['last_state'] = info
            state['last_read_status'] = read_status(info, screen)
            cancelled(args)
            save(args.state, state)
            emit(result)
            return
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
                status = read_status(info, screen)
                wake = (changed or delta) if args.wait_mode == 'activity' else ('last_read_status' not in state or status != state['last_read_status'])
                if args.wait_mode == 'status' and not info['title'].startswith(('◐ ', '◑ ')):
                    # The whole busy -> idle cycle may have happened between two reads.
                    wake = wake or changed or delta
                if wake or args.force_view or time.monotonic() - started >= args.wait:
                    break
                time.sleep(min(0.25, max(0, args.wait - (time.monotonic() - started))))
                cancelled(args)
                info = meta(tmux, bound['pane'])
                if identity(info, bound['agent_pid']) != bound:
                    raise BridgeError('E_TARGET', 'Identität während des Wartens geändert')
                screen, observed = snapshot(tmux, info)
            result = {'event': 'snapshot' if changed or delta or args.force_view else 'unchanged',
                      'observed': observed}
            if args.wait:
                result['waited_ms'] = round((time.monotonic() - started) * 1000)
                if args.wait_mode == 'status':
                    result['wake_reason'] = 'status_changed' if wake else 'timeout'
            if args.verbose:
                result['state'] = info
            elif delta:
                result['state_changes'] = delta
            if changed or args.force_view:
                previous = state.get('last_rows', [])
                hashes, edits = compact_edits(previous, text.splitlines())
                full_text = text
                if args.compact and not args.force_view and previous and state.get('last_lines') == args.lines:
                    payload = json.dumps(edits, ensure_ascii=False)
                    if len(payload) < min(len(full_text), args.max_chars):
                        result['view'] = 'replacement_excerpts'
                        result['base_view'] = state.get('last_view')
                        result['edits'] = edits
                        text = ''
                    else:
                        result['view'] = 'snapshot'
                else:
                    result['view'] = 'snapshot'
                result['view_hash'] = digest
                if len(text) > args.max_chars:
                    half = args.max_chars // 2
                    result.update(text=text[:half] + '\n[… Mittelteil ausgelassen …]\n' + text[-half:],
                                  omitted_chars=len(text) - 2 * half)
                elif 'edits' not in result:
                    result['text'] = text
                # Never use an unseen/truncated range as the next compact baseline.
                state['last_rows'] = hashes if len(text) <= args.max_chars else []
                state['last_lines'] = args.lines
            state['last_view'] = digest
            state['last_state'] = info
            state['last_read_status'] = status
            save(args.state, state)
            emit(result)
            return
        if info['in_mode'] != '0':
            raise BridgeError('E_INPUT', 'Verlauf/Kopiermodus aktiv; Nutzer nach unten scrollen oder q drücken lassen, nicht automatisch verlassen')
        if info['input_off'] != '0':
            raise BridgeError('E_INPUT', 'Pane nimmt keine normale Eingabe an')
        if args.observed != observed:
            raise BridgeError('E_STALE', 'Momentaufnahme geändert/veraltet; erneut lesen, nicht blind zustellen')
        if not args.id or not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.id):
            parser.error('--id muss eine kurze eindeutige Auftragskennung sein')
        def recheck():
            cancelled(args)
            latest = meta(tmux, bound['pane'])
            if identity(latest, bound['agent_pid']) != bound or snapshot(tmux, latest)[1] != args.observed:
                raise BridgeError('E_STALE', 'Ziel oder Ansicht unmittelbar vor Eingabe geändert; erneut lesen')

        deliveries = state['deliveries']
        if args.action == 'resolve':
            delivery = deliveries.get(args.id, {})
            if delivery.get('status') not in ('pasted', 'paste_attempted'):
                raise BridgeError('E_TEXT', 'Nur ein offener, nie gesendeter Einfügeversuch darf als manuell entfernt geklärt werden')
            authorized_hash = delivery.get('payload_sha256', delivery.get('sha256'))
            if not args.sha256 or args.sha256 != authorized_hash:
                raise BridgeError('E_TEXT', 'Auftragshash stimmt nicht mit dem offenen Einfügeversuch überein')
            if not args.manual_clear_confirmed:
                parser.error('resolve braucht --manual-clear-confirmed nach beobachteter manueller Entfernung')
            if args.continued_as and not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.continued_as):
                parser.error('--continued-as muss eine gültige neue Auftragskennung sein')
            frame = input_frame(screen)
            if frame[3] != '' or info['cursor_x'] != '2' or int(info['cursor_y']) != frame[1] + 1:
                raise BridgeError('E_INPUT', 'resolve verlangt das frisch geprüfte, vollständig leere Eingabefeld')
            source_path = args.run_dir / (args.id + '.txt') if args.run_dir else args.text_file
            marker = delivery_marker(delivery, args.id, source_path)
            history = run(tmux + ['capture-pane', '-p', '-t', bound['pane'], '-S', '-300'])
            if marker_in_history(marker, history):
                raise BridgeError('E_DUPLICATE', 'Anfang des offenen Auftrags steht bereits im Verlauf; nicht als bloß entfernt klären')
            recheck()
            delivery['status'] = 'cleared'
            if args.continued_as:
                delivery['continued_as'] = args.continued_as
            save(args.state, state)
            emit({'id': args.id, 'transport': 'cleared', 'continued_as': args.continued_as,
                  'next': 'Nur der Transportversuch ist geklärt; Aufgabeninhalt und Autorisierung folgen dem aktuellen Auftrag'})
            return
        if args.action in ('paste', 'submit', 'submit-file'):
            if args.id in deliveries:
                raise BridgeError('E_DUPLICATE', 'Kennung bereits versucht; zuerst Zustellung klären, nicht erneut einfügen')
            payload = None
            if args.action == 'submit-file':
                payload_text, payload_data = regular_utf8(args.payload_file, MAX_HANDOFF_BYTES, allow_cr=True)
                if not payload_text.strip():
                    raise BridgeError('E_TEXT', 'Leere Payload-Datei')
                payload_hash = hashlib.sha256(payload_data).hexdigest()
                if args.sha256 != payload_hash:
                    raise BridgeError('E_TEXT', 'Payload-Hash fehlt oder Inhalt nach Autorisierung geändert')
                if (not args.display_path.strip() or
                        any(ord(c) < 32 or ord(c) == 127 for c in args.display_path)):
                    raise BridgeError('E_TEXT', 'display-path muss eine druckbare einzelne Zeile sein')
                text = (f'{args.id}: Lies {args.display_path} vollständig. '
                        f'Erwarte UTF-8-Bytes={len(payload_data)} und SHA-256={payload_hash}. '
                        'Bestätige payload_bytes, payload_sha256 und vollstaendig_gelesen=true; '
                        'führe dann ausschließlich den Datei-Auftrag aus.')
                payload = {'payload_sha256': payload_hash, 'payload_bytes': len(payload_data),
                           'display_path': args.display_path}
            else:
                if args.text_file is None:
                    parser.error('--text-file fehlt')
                text, _ = regular_utf8(args.text_file, 65536)
                if not text.strip():
                    raise BridgeError('E_TEXT', 'Leerer Auftrag')
            if args.literal_line and any(ord(c) < 32 or ord(c) == 127 for c in text):
                raise BridgeError('E_TEXT', 'Literal-Line erlaubt keine Zeilenumbrüche, Tabs oder Steuerzeichen')
            if args.literal_line and len(text.encode()) > MAX_LITERAL_BYTES:
                if args.action == 'submit-file':
                    raise BridgeError('E_TEXT', f'Dateiverweis über {MAX_LITERAL_BYTES} Bytes; kürzeren display-path verwenden')
                raise BridgeError('E_TEXT', f'Literal-Line über {MAX_LITERAL_BYTES} Bytes; lange Inhalte mit submit-file übergeben')
            text_hash = hashlib.sha256(text.encode()).hexdigest()
            if args.action == 'submit' or (args.run_dir and args.action != 'submit-file'):
                if args.sha256 != text_hash:
                    raise BridgeError('E_TEXT', 'Auftragshash fehlt oder Inhalt nach Autorisierung geändert')
            if args.action in ('submit', 'submit-file'):
                if any(d['status'] in ('pasted', 'paste_attempted', 'enter_attempted') for d in deliveries.values()):
                    raise BridgeError('E_DUPLICATE', 'Andere Zustellung noch offen; zuerst klären')
                frame = input_frame(screen)
                if frame[3] != '' or info['cursor_x'] != '2' or int(info['cursor_y']) != frame[1] + 1:
                    raise BridgeError('E_INPUT', 'submit verlangt ein eindeutig leeres Eingabefeld; auch Ghosts zuerst klären')
                before = screen
            # paste-buffer -p klammert nur bei bereits aktivierter Unterstützung des Zielprogramms.
            if not args.literal_line and info['bracket_paste'] != '1':
                raise BridgeError('E_INPUT', 'Bracketed Paste ist nicht aktiv; nicht mit Zeilenumbrüchen experimentieren')
            buffer = 'codex-' + uuid.uuid4().hex
            delivery = {'status': 'paste_attempted', 'sha256': text_hash}
            if payload:
                delivery.update(payload)
            if args.literal_line:
                recheck()
                deliveries[args.id] = delivery
                save(args.state, state)
                run(tmux + ['send-keys', '-l', '-t', bound['pane'], '--', text])
            else:
                try:
                    run(tmux + ['load-buffer', '-b', buffer, '-'], text.encode())
                    recheck()
                    deliveries[args.id] = delivery
                    save(args.state, state)
                    run(tmux + ['paste-buffer', '-p', '-r', '-b', buffer, '-t', bound['pane']])
                finally:
                    run(tmux + ['delete-buffer', '-b', buffer], cleanup=True)
            deliveries[args.id]['status'] = 'pasted'
            save(args.state, state)
            if args.action in ('submit', 'submit-file'):
                deadline = time.monotonic() + 1
                while True:
                    cancelled(args)
                    latest = meta(tmux, bound['pane'])
                    if identity(latest, bound['agent_pid']) != bound:
                        raise BridgeError('E_TARGET', 'Identität nach Paste geändert; kein Enter')
                    after, after_token = snapshot(tmux, latest)
                    try:
                        draft = input_frame(after)[3]
                    except BridgeError as error:
                        if error.code != 'E_INPUT':
                            raise
                        result = {'id': args.id, 'transport': 'pasted', 'submitted': False,
                                  'reason': 'Eingaberahmen nach Paste nicht vollständig sichtbar; gezielt lesen, kein Enter'}
                        result.update(payload or {})
                        emit(result)
                        return
                    own = own_draft(draft, text) or re.fullmatch(r'\[Pasted text #\d+\]', draft)
                    if own and same_surroundings(before, after):
                        break
                    if draft or time.monotonic() >= deadline:
                        result = {'id': args.id, 'transport': 'pasted', 'submitted': False,
                                  'reason': 'Entwurf/Umgebung nicht eindeutig; lesen, kein automatisches Enter'}
                        result.update(payload or {})
                        emit(result)
                        return
                    time.sleep(0.05)
                args.observed = after_token
                recheck()
                deliveries[args.id]['status'] = 'enter_attempted'
                save(args.state, state)
                run(tmux + ['send-keys', '-t', bound['pane'], 'Enter'])
                deliveries[args.id]['status'] = 'enter_sent'
                save(args.state, state)
                result = {'id': args.id, 'transport': 'enter_sent'}
                result.update(payload or {})
                emit(result)
            else:
                emit({'id': args.id, 'transport': 'pasted', 'submitted': False})
        else:
            if deliveries.get(args.id, {}).get('status') != 'pasted':
                raise BridgeError('E_TEXT', 'Kein eindeutig eingefügter, noch ungesendeter Auftrag für diese Kennung')
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
        emit({'error': getattr(error, 'code', 'E_TIMEOUT' if isinstance(error, subprocess.TimeoutExpired) else 'E_STATE'),
              'message': str(error)[:240]})
        sys.exit(1)
