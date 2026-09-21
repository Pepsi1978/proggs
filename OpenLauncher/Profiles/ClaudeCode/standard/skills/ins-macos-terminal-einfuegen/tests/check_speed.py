"""Regression tests for wrapped submission and positional compact reads; isolated tmux."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
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
    data+=os.read(0,65536)
    Path(sys.argv[1]).write_bytes(data)
    if not data.endswith(b'\\r'): paint()
''', encoding='utf-8')
    received = root / 'bytes'
    tmux = [shutil.which('tmux'), '-S', str(root / 'socket')]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-x', '90', '-y', '24', sys.executable, str(receiver), str(received)], check=True)
    def call(*args):
        result = subprocess.run([sys.executable, str(bridge), *args], text=True, capture_output=True, timeout=15)
        assert result.returncode == 0, result.stdout + result.stderr
        return json.loads(result.stdout), len(result.stdout.encode())
    common = ['--run', directory]
    try:
        time.sleep(.2)
        info, _ = call('list', '--socket', str(root / 'socket'))
        call('bind', *common, '--socket', str(root / 'socket'), '--pane', info['pane'], '--agent-pid', info['pane_pid'], '--cwd', info['cwd'])
        first, full_bytes = call('read', *common, '--compact')
        assert first['view'] == 'snapshot'
        text = 'R2: Dies ist eine längere Nachricht mit mehreren Wörtern und Umlauten für den überprüften automatischen Versand ohne zusätzliche Modellrunde.'
        (root / 'R2.txt').write_text(text, encoding='utf-8')
        start = time.monotonic()
        sent, _ = call('submit', *common, '--id', 'R2', '--sha256', hashlib.sha256(text.encode()).hexdigest(), '--observed', first['observed'], '--literal-line')
        assert sent['transport'] == 'enter_sent', sent
        elapsed = time.monotonic() - start
        time.sleep(.1)
        assert received.read_bytes() == text.encode() + b'\r'
        compact, compact_bytes = call('read', *common, '--compact')
        assert compact['view'] == 'replacement_excerpt', compact
        assert compact['unchanged_prefix_rows'] == 9
        assert 'gleiche Zeile' not in compact['text']
        assert compact['replace_rows'][0] == 9
        repeat, _ = call('read', *common, '--compact')
        assert repeat['event'] == 'unchanged'
        forced, _ = call('read', *common, '--compact', '--force-view')
        assert forced['view'] == 'snapshot' and forced['text'].count('gleiche Zeile') == 8
        print(json.dumps({'wrapped_submit_seconds': round(elapsed, 3), 'first_snapshot_bytes': full_bytes, 'changed_excerpt_bytes': compact_bytes}))
        print('OK: wrapped send, exact whitespace, changed surroundings, repeated lines, compact range and force-view.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
