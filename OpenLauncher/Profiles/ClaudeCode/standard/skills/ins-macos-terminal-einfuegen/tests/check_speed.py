"""Regression tests for wrapped submission and positional compact reads; isolated tmux."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import threading
import time

sys.dont_write_bytecode = True
bridge = Path(__file__).resolve().parents[1] / 'scripts/tmux_bridge.py'
spec = importlib.util.spec_from_file_location('bridge', bridge)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
assert module.own_draft('eins zwei\n  drei', 'eins zwei drei')
assert module.own_draft('abcdef\n  ghij', 'abcdefghij')
assert not module.own_draft('einszwei', 'eins zwei')
assert not module.own_draft('eins zwei\n  fremd', 'eins zwei drei')
assert not module.own_draft('eins  zwei\n  drei', 'eins zwei drei')
bar = '─' * 60
before = '\n'.join(['alt', 'gleich', bar, '❯ ', bar, '  ⏵⏵ bypass permissions on (shift+tab to cycle) · ← for agents', '', ''])
after = '\n'.join(['gleich', bar, '❯ eins zwei', '  drei', bar, '  ⏵⏵ bypass permissions on (shift+tab to cycle)', ''])
assert module.same_surroundings(before, after)
assert not module.same_surroundings(before, after.replace('gleich', 'fremd'))
assert not module.same_surroundings(before, after.replace('bypass permissions', 'changed permissions'))
footer = '  🧠 ctx █░ 15% ┃ 🤖 Opus 5 ┃ ⚡ HIGH ┃ ⏱ 5h ░░ 3% (3h52m) slow ─●─┃─── fast ┃ 📅 7d ░░ 38% (1D14H) slow ●──┃─── fast ┃ 🕐 22:17 /rc'
screen = '\n'.join(['Antwort 22:17 (3h52m)', bar, '❯ ', bar, footer])
new_clock = screen.replace('🕐 22:17', '🕐 22:18').replace('(3h52m) slow', '(3h51m) slow').replace('(1D14H)', '(1D13H)')
assert module.normalize_clock(screen) == module.normalize_clock(new_clock)
assert 'Antwort 22:17 (3h52m)' in module.normalize_clock(screen)
for replacement in [('15%', '16%'), ('38%', '39%'), ('Opus 5', 'Anderes Modell'), ('HIGH', 'LOW')]:
    assert module.normalize_clock(screen.replace(*replacement)) != module.normalize_clock(screen)
for old_rows, new_rows in [
    (['alt', 'gleich', 'gleich', 'Ende'], ['gleich', 'gleich', 'Ende', 'neu']),
    (['A', 'B', 'A'], ['A', 'A', 'B', 'A']),
    (['A', '', 'B'], ['A', 'B']),
    (['A', 'B'], ['A', '', 'B']),
]:
    hashes = [hashlib.sha256(row.encode()).hexdigest() for row in old_rows]
    _, edits = module.compact_edits(hashes, new_rows)
    reconstructed = old_rows[:]
    for edit in reversed(edits):
        a, b = edit['old_rows']
        c, d = edit['new_rows']
        reconstructed[a:b] = edit['text'].split('\n') if d > c else []
    assert reconstructed == new_rows, (old_rows, new_rows, edits)

with tempfile.TemporaryDirectory(prefix='bridge-speed-') as directory:
    root = Path(directory)
    receiver = root / 'receiver.py'
    receiver.write_text('''import os,sys,tty,textwrap
from pathlib import Path
tty.setraw(0)
data=b''
def paint():
    draft=data.decode('utf-8')
    wrapped=textwrap.wrap(draft,width=55,replace_whitespace=False,drop_whitespace=True) or ['']
    rows=['gleiche Zeile']*8+['─'*70]+['❯ '+wrapped[0]]+['  '+line for line in wrapped[1:]]+['─'*70]
    rows+=['  ⏵⏵ bypass permissions on (shift+tab to cycle)'+('' if draft else ' · ← for agents')]
    os.write(1,('\\x1b[H\\x1b[2J'+'\\r\\n'.join(rows)+'\\x1b['+str(10+len(wrapped)-1)+';'+str(3+len(wrapped[-1]))+'H').encode())
paint()
while True:
    chunk=os.read(0,65536)
    if chunk == b'\\x02':
        os.write(1,b'\\x1b[1;1HFinished response')
        continue
    data+=chunk
    Path(sys.argv[1]).write_bytes(data)
    if data.endswith(b'\\x01'):
        os.write(1,b'\\x1b[H\\x1b[2JApproval waiting')
        continue
    if not data.endswith(b'\\r'): paint()
''', encoding='utf-8')
    received = root / 'bytes'
    tmux = [shutil.which('tmux'), '-S', str(root / 'socket')]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-x', '90', '-y', '24', sys.executable, str(receiver), str(received)], check=True)
    def call(*args, ok=True):
        result = subprocess.run([sys.executable, str(bridge), *args], text=True, capture_output=True, timeout=15)
        assert (result.returncode == 0) == ok, result.stdout + result.stderr
        return json.loads(result.stdout), len(result.stdout.encode())
    common = ['--run', directory]
    try:
        time.sleep(.2)
        info, _ = call('list', '--socket', str(root / 'socket'))
        call('bind', *common, '--socket', str(root / 'socket'), '--pane', info['pane'], '--agent-pid', info['pane_pid'], '--cwd', info['cwd'])
        first, full_bytes = call('read', *common, '--compact')
        assert first['view'] == 'snapshot'
        subprocess.run(tmux + ['copy-mode', '-t', info['pane']], check=True)
        blocked, _ = call('submit', *common, '--id', 'SCROLL', '--sha256', '0' * 64, '--observed', first['observed'], '--literal-line', ok=False)
        assert blocked['error'] == 'E_INPUT' and 'Verlauf' in blocked['message'], blocked
        subprocess.run(tmux + ['send-keys', '-X', '-t', info['pane'], 'cancel'], check=True)
        first, _ = call('read', *common)
        text = 'R2: Dies ist eine längere Nachricht mit mehreren Wörtern und Umlauten für den überprüften automatischen Versand ohne zusätzliche Modellrunde.'
        (root / 'R2.txt').write_text(text, encoding='utf-8')
        start = time.monotonic()
        sent, _ = call('submit', *common, '--id', 'R2', '--sha256', hashlib.sha256(text.encode()).hexdigest(), '--observed', first['observed'], '--literal-line')
        assert sent['transport'] == 'enter_sent', sent
        elapsed = time.monotonic() - start
        time.sleep(.1)
        assert received.read_bytes() == text.encode() + b'\r'
        compact, compact_bytes = call('read', *common, '--compact')
        assert compact['view'] == 'replacement_excerpts', compact
        assert all('gleiche Zeile' not in edit['text'] for edit in compact['edits'])
        assert compact['edits'][0]['old_rows'][0] == 9
        repeat, _ = call('read', *common, '--compact')
        assert repeat['event'] == 'unchanged'
        forced, full_after_bytes = call('read', *common, '--compact', '--force-view')
        assert forced['view'] == 'snapshot' and forced['text'].count('gleiche Zeile') == 8
        start = time.monotonic()
        waited, _ = call('read', *common, '--compact', '--wait-mode', 'status', '--wait', '.5')
        assert waited['wake_reason'] == 'timeout' and time.monotonic() - start >= .5
        timer = threading.Timer(.2, lambda: subprocess.run(tmux + ['select-pane', '-t', info['pane'], '-T', 'READY'], check=True))
        timer.start()
        start = time.monotonic()
        awakened, _ = call('read', *common, '--compact', '--wait-mode', 'status', '--wait', '3')
        timer.join()
        assert awakened['wake_reason'] == 'status_changed' and time.monotonic() - start < 2
        subprocess.run(tmux + ['send-keys', '-t', info['pane'], 'C-b'], check=True)
        time.sleep(.1)
        completed, _ = call('read', *common, '--wait-mode', 'status', '--wait', '2')
        assert completed['waited_ms'] < 500 and completed['wake_reason'] == 'status_changed'
        assert 'Finished response' in completed.get('text', '') or any('Finished response' in edit['text'] for edit in completed.get('edits', []))
        subprocess.run(tmux + ['select-pane', '-t', info['pane'], '-T', '◐ Claude'], check=True)
        call('read', *common)
        timer = threading.Timer(.15, lambda: subprocess.run(tmux + ['select-pane', '-t', info['pane'], '-T', '◑ Claude'], check=True))
        timer.start()
        start = time.monotonic()
        animated, _ = call('read', *common, '--wait-mode', 'status', '--wait', '.5')
        timer.join()
        assert animated['wake_reason'] == 'timeout' and time.monotonic() - start >= .5
        assert animated['state_changes']['title'] == '◑ Claude'  # Raw metadata stays available.
        subprocess.run(tmux + ['send-keys', '-t', info['pane'], 'C-a'], check=True)
        time.sleep(.1)
        missing, _ = call('read', *common, '--wait-mode', 'status', '--wait', '.5')
        assert missing['wake_reason'] == 'status_changed'
        start = time.monotonic()
        still_missing, _ = call('read', *common, '--wait-mode', 'status', '--wait', '.5')
        assert still_missing['wake_reason'] == 'timeout' and time.monotonic() - start >= .5
        print(json.dumps({'wrapped_submit_seconds': round(elapsed, 3), 'first_snapshot_bytes': full_bytes,
                          'changed_excerpt_bytes': compact_bytes, 'same_view_full_bytes': full_after_bytes}))
        print('OK: wrapped send, exact whitespace, changed surroundings, repeated lines, compact range and force-view.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
