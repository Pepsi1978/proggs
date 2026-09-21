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
payload_entry = {'payload_sha256': 'a' * 64, 'display_path': r'C:\Temp\payload.txt'}
payload_marker = module.delivery_marker(payload_entry, 'F1', None)
assert payload_marker == r'F1: Lies C:\Temp\payload.txt'
assert module.marker_in_history(payload_marker, 'Antwort\n' + payload_marker + '\n─' * 10)
with tempfile.TemporaryDirectory(prefix='tmux-literal-') as folder:
    root = pathlib.Path(folder)
    sock = str(root / 'socket')
    receiver = root / 'receiver.py'
    received = root / 'bytes'
    receiver.write_text('''import os,sys,tty
from pathlib import Path
tty.setraw(0)
data=b''
draft=b''
history=[]
def paint():
    shown=draft.decode('utf-8', 'replace')
    prefix='\\r\\n'.join(history[-5:])
    screen=(prefix+'\\r\\n' if prefix else '')+'─'*70+'\\r\\n> '+shown+'\\r\\n'+'─'*70
    row=2
    os.write(1,('\\x1b[H\\x1b[2J'+screen+'\\x1b['+str(row)+';'+str(3+len(shown))+'H').encode())
paint()
while True:
    chunk=os.read(0,4096)
    data+=chunk
    if b'\\r' in chunk or b'\\n' in chunk:
        split_at=max(chunk.rfind(b'\\r'), chunk.rfind(b'\\n'))
        before, draft=chunk[:split_at], chunk[split_at+1:]
        history.append((draft+before).decode('utf-8', 'replace'))
    else: draft+=chunk
    Path(sys.argv[1]).write_bytes(data)
    paint()
''', encoding='utf-8')
    tmux = [shutil.which('tmux'), '-S', sock]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-x', '600', '-y', '24',
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
        cleared_text = 'manuell aus dem Eingabefeld entfernter Entwurf'
        cleared_hash = hashlib.sha256(cleared_text.encode()).hexdigest()
        state_path = root / 'target.json'
        (root / 'R0.txt').write_text(cleared_text, encoding='utf-8')
        state = json.loads(state_path.read_text())
        state['deliveries']['R0'] = {'status': 'pasted', 'sha256': cleared_hash}
        state_path.write_text(json.dumps(state), encoding='utf-8')
        read = call('read', *common)
        assert call('resolve', *common, '--id', 'R0', '--sha256', '0' * 64,
                    '--observed', read['observed'], ok=False)['error'] == 'E_TEXT'
        cleared = call('resolve', *common, '--id', 'R0', '--sha256', cleared_hash,
                       '--manual-clear-confirmed',
                       '--continued-as', 'R1', '--observed', read['observed'])
        assert cleared['transport'] == 'cleared', cleared
        resolved = json.loads(state_path.read_text())['deliveries']['R0']
        assert resolved['status'] == 'cleared' and resolved['continued_as'] == 'R1'
        payload = ('lange Windows-Zeile äöü\r\n' * 300).encode()
        payload_file = root / 'payload.txt'
        payload_file.write_bytes(payload)
        payload_hash = hashlib.sha256(payload).hexdigest()
        read = call('read', *common)
        bad_hash = call('submit-file', *common, '--id', 'F0', '--payload-file', str(payload_file),
                        '--display-path', r'C:\\Temp\\payload.txt', '--sha256', '0' * 64,
                        '--literal-line', '--observed', read['observed'], ok=False)
        assert bad_hash['error'] == 'E_TEXT', bad_hash
        handoff = call('submit-file', *common, '--id', 'F1', '--payload-file', str(payload_file),
                       '--display-path', r'C:\\Temp\\payload.txt', '--sha256', payload_hash,
                       '--literal-line', '--observed', read['observed'])
        assert handoff['transport'] == 'enter_sent' and handoff['payload_bytes'] == len(payload), handoff
        assert payload_hash.encode() in received.read_bytes() and payload not in received.read_bytes()
        read = call('read', *common)
        long_path = 'C:\\' + ('ü' * 300) + '\\payload.txt'
        path_error = call('submit-file', *common, '--id', 'F2', '--payload-file', str(payload_file),
                          '--display-path', long_path, '--sha256', payload_hash,
                          '--literal-line', '--observed', read['observed'], ok=False)
        assert path_error['error'] == 'E_TEXT' and 'display-path' in path_error['message'], path_error
        text = "Grüße 'Zitat' $(false)"
        (root / 'C1.txt').write_text(text, encoding='utf-8')
        args = [*common, '--id', 'C1', '--sha256', hashlib.sha256(text.encode()).hexdigest(), '--literal-line']
        read = call('read', *common, '--force-view')
        assert module.input_frame(read['text'])[3] == '', (repr(received.read_bytes()[-40:]), read)
        assert call('submit', *args, '--observed', 'stale', ok=False)['error'] == 'E_STALE'
        result = call('submit', *args, '--observed', read['observed'])
        assert result['transport'] == 'enter_sent', result
        time.sleep(.1)
        assert received.read_bytes().endswith(text.encode() + b'\r')
        read = call('read', *common)
        assert call('submit', *args, '--observed', read['observed'], ok=False)['error'] == 'E_DUPLICATE'
        for index, invalid in enumerate(('erste\nzweite', 'erste\tzweite', 'erste\rzweite')):
            name = 'B' + str(index)
            (root / (name + '.txt')).write_text(invalid, encoding='utf-8')
            rejected = call('paste', *common, '--id', name, '--sha256', hashlib.sha256(invalid.encode()).hexdigest(),
                            '--literal-line', '--observed', read['observed'], ok=False)
            assert rejected['error'] == 'E_TEXT', rejected
        long_name = 'L1'
        long_text = 'x' * 513
        (root / (long_name + '.txt')).write_text(long_text, encoding='utf-8')
        rejected = call('paste', *common, '--id', long_name,
                        '--sha256', hashlib.sha256(long_text.encode()).hexdigest(),
                        '--literal-line', '--observed', read['observed'], ok=False)
        assert rejected['error'] == 'E_TEXT' and 'submit-file' in rejected['message'], rejected
        (root / 'STOP').touch()
        assert call('read', *common, ok=False)['error'] == 'E_STOP'
        print('OK: beide Prompts, UTF-8 bytegenau, sicheres resolve, lange Dateiübergabe, Literal-Limit, Enter, Duplikate, Steuerzeichen und STOP.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
