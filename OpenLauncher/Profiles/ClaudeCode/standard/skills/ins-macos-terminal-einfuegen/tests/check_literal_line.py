"""Isolierter Linux/macOS-tmux-Test; keine echten CLI-/Modellaufrufe."""
import hashlib
import importlib.util
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import time

sys.dont_write_bytecode = True
bridge = pathlib.Path(__file__).resolve().parents[1] / 'scripts/tmux_bridge.py'
spec = importlib.util.spec_from_file_location('bridge', bridge)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
for prompt in ('❯', '>'):
    assert module.input_frame('─' * 30 + '\n' + prompt + ' \n' + '─' * 30)[3] == ''
with tempfile.TemporaryDirectory(prefix='tmux-literal-') as folder:
    root = pathlib.Path(folder)
    sock = str(root / 'socket')
    receiver = root / 'receiver.py'
    received = root / 'bytes'
    receiver.write_text('''import os,sys,tty
from pathlib import Path
tty.setraw(0)
data=b''
def paint():
    draft=data.decode('utf-8', 'replace')
    screen='─'*70+'\\r\\n> '+draft+'\\r\\n'+'─'*70
    os.write(1,('\\x1b[H\\x1b[2J'+screen+'\\x1b[2;'+str(3+len(draft))+'H').encode())
paint()
while True:
    data+=os.read(0,4096)
    Path(sys.argv[1]).write_bytes(data)
    if not data.endswith(b'\\r'): paint()
''', encoding='utf-8')
    tmux = [shutil.which('tmux'), '-S', sock]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-x', '90', '-y', '24',
                          sys.executable, str(receiver), str(received)], check=True)
    def call(*args, ok=True):
        result = subprocess.run([sys.executable, str(bridge), *args], capture_output=True, text=True, timeout=15)
        assert (result.returncode == 0) == ok, result.stdout + result.stderr
        return json.loads(result.stdout)
    try:
        time.sleep(.2)
        info = call('list', '--socket', sock)
        common = ['--run', folder]
        call('bind', *common, '--socket', sock, '--pane', info['pane'],
             '--agent-pid', info['pane_pid'], '--cwd', info['cwd'])
        text = "Grüße 'Zitat' $(false)"
        (root / 'C1.txt').write_text(text, encoding='utf-8')
        args = [*common, '--id', 'C1', '--sha256', hashlib.sha256(text.encode()).hexdigest(), '--literal-line']
        read = call('read', *common)
        assert call('submit', *args, '--observed', 'stale', ok=False)['error'] == 'E_STALE'
        result = call('submit', *args, '--observed', read['observed'])
        assert result['transport'] == 'enter_sent', result
        time.sleep(.1)
        assert received.read_bytes() == text.encode() + b'\r'
        read = call('read', *common)
        assert call('submit', *args, '--observed', read['observed'], ok=False)['error'] == 'E_DUPLICATE'
        for index, invalid in enumerate(('erste\nzweite', 'erste\tzweite', 'erste\rzweite')):
            name = 'B' + str(index)
            (root / (name + '.txt')).write_text(invalid, encoding='utf-8')
            rejected = call('paste', *common, '--id', name, '--sha256', hashlib.sha256(invalid.encode()).hexdigest(),
                            '--literal-line', '--observed', read['observed'], ok=False)
            assert rejected['error'] == 'E_TEXT', rejected
        (root / 'STOP').touch()
        assert call('read', *common, ok=False)['error'] == 'E_STOP'
        print('OK: beide Prompts, UTF-8 bytegenau, einmal Enter, veraltete Tokens, Duplikate, Steuerzeichen und STOP.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
