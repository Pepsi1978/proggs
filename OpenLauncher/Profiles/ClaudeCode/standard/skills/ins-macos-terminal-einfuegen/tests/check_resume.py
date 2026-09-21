"""Isolierter resume-Test mit tmux und temporärem Git-Repository."""
import hashlib
import importlib.util
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import time

bridge = pathlib.Path(__file__).resolve().parents[1] / 'scripts/tmux_bridge.py'
spec = importlib.util.spec_from_file_location('resume_bridge', bridge)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
original_which, original_run = module.shutil.which, module.run
seen = []
try:
    module.shutil.which = lambda name: '/usr/bin/git' if name == 'git' else None
    module.run = lambda args, data=None, cleanup=False: (
        seen.append(args) or ('## main' if 'status' in args else 'abc basis'))
    summary, error = module.resume_git('/tmp/repo')
    assert not error and summary['log'] == 'abc basis'
    assert all(command[1] == '--no-optional-locks' for command in seen), seen
finally:
    module.shutil.which, module.run = original_which, original_run
module.CHECK_CANCEL = lambda: (_ for _ in ()).throw(module.BridgeError('E_STOP', 'test-stop'))
try:
    try:
        module.resume_git('/tmp/repo')
        raise AssertionError('E_STOP wurde verschluckt')
    except module.BridgeError as error:
        assert error.code == 'E_STOP', error
finally:
    module.CHECK_CANCEL = None
with tempfile.TemporaryDirectory(prefix='tmux-resume-') as folder:
    root = pathlib.Path(folder)
    receiver = root / 'receiver.py'
    receiver.write_text("""import os,tty
tty.setraw(0)
screen='─'*70+'\\r\\n> '+'\\r\\n'+'─'*70
os.write(1,('\\x1b[H\\x1b[2J'+screen+'\\x1b[2;3H').encode())
while True: os.read(0,4096)
""", encoding='utf-8')
    subprocess.run(['git', '-C', folder, 'init', '-q'], check=True)
    subprocess.run(['git', '-C', folder, 'config', 'user.email', 'test@example.invalid'], check=True)
    subprocess.run(['git', '-C', folder, 'config', 'user.name', 'Resume Test'], check=True)
    (root / 'tracked.txt').write_text('basis', encoding='utf-8')
    subprocess.run(['git', '-C', folder, 'add', 'tracked.txt'], check=True)
    subprocess.run(['git', '-C', folder, 'commit', '-q', '-m', 'basis'], check=True)
    (root / 'tracked.txt').write_text('geändert', encoding='utf-8')
    sock = str(root / 'socket')
    tmux = [shutil.which('tmux'), '-S', sock]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-c', folder,
                          sys.executable, str(receiver)], check=True)

    def call(*args, ok=True):
        completed = subprocess.run([sys.executable, str(bridge), *args],
                                   capture_output=True, text=True, timeout=15)
        assert (completed.returncode == 0) == ok, completed.stdout + completed.stderr
        return json.loads(completed.stdout)

    try:
        time.sleep(.2)
        info = call('list', '--socket', sock)
        common = ['--run', folder]
        call('bind', *common, '--socket', sock, '--pane', info['pane'],
             '--agent-pid', info['pane_pid'], '--cwd', info['cwd'])
        workstate = {'ziel_rev': 4, 'zugestellt': {'id': 'R15', 'ziel_rev': 4},
                     'offen': [], 'phase': 'review', 'status': 'zwischenstand'}
        (root / 'arbeitsstand.json').write_text(json.dumps(workstate), encoding='utf-8')
        open_text = 'offener Auftrag'
        open_hash = hashlib.sha256(open_text.encode()).hexdigest()
        (root / 'R1.txt').write_text(open_text, encoding='utf-8')
        state_path = root / 'target.json'
        state = json.loads(state_path.read_text())
        state['deliveries']['R1'] = {'status': 'pasted', 'sha256': open_hash}
        state_path.write_text(json.dumps(state), encoding='utf-8')
        resumed = call('resume', *common, '--max-chars', '3000')
        assert resumed['arbeitsstand'] == workstate, resumed
        assert resumed['offene_zustellungen'] == [{'id': 'R1', 'status': 'pasted'}], resumed
        assert resumed['letzte_id'] == 'R1' and resumed['git']['log'].endswith('basis'), resumed
        assert any('tracked.txt' in row for row in resumed['git']['status']), resumed
        assert resumed['observed'] and '>' in resumed['pane']
        new_text = 'nächster Auftrag'
        (root / 'R2.txt').write_text(new_text, encoding='utf-8')
        blocked = call('submit', *common, '--id', 'R2',
                       '--sha256', hashlib.sha256(new_text.encode()).hexdigest(),
                       '--literal-line', '--observed', resumed['observed'], ok=False)
        assert blocked['error'] == 'E_DUPLICATE', blocked
        separate = (len((root / 'arbeitsstand.json').read_bytes()) +
                    len(json.dumps(json.loads(state_path.read_text())['deliveries'])) +
                    len(subprocess.check_output(['git', '-C', folder, 'status', '-sb'])) +
                    len(subprocess.check_output(['git', '-C', folder, 'log', '-1', '--oneline'])) +
                    len(json.dumps(call('read', *common, '--force-view'))))
        assert len(json.dumps(resumed)) < separate, (len(json.dumps(resumed)), separate)
        git_dir = root / '.git'
        hidden_git = root / '.git.hidden'
        git_dir.rename(hidden_git)
        no_git = call('resume', *common)
        assert 'git_error' in no_git and 'pane' in no_git, no_git
        hidden_git.rename(git_dir)
        (root / 'arbeitsstand.json').write_text('{', encoding='utf-8')
        damaged = call('resume', *common)
        assert 'arbeitsstand_error' in damaged and 'pane' in damaged, damaged
        (root / 'STOP').touch()
        assert call('resume', *common, ok=False)['error'] == 'E_STOP'
        print('OK: resume bündelt Arbeitsstand, offenes Ledger, Git und Pane; Teilfehler, Duplikatschutz und STOP greifen.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
