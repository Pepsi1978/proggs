"""Simulate wheel events in an isolated tmux client; never attach to a user session."""
import fcntl
import os
from pathlib import Path
import pty
import shutil
import struct
import subprocess
import tempfile
import termios
import time

with tempfile.TemporaryDirectory(prefix='launcher-wheel-') as folder:
    root = Path(folder)
    received = root / 'received'
    fixture = root / 'receiver.py'
    fixture.write_text('''import os,sys,tty
from pathlib import Path
tty.setraw(0)
os.write(1,('\\r\\n'.join('History '+str(i) for i in range(100))+'\\r\\nPrompt: ').encode())
while True:
    with open(sys.argv[1],'ab') as target: target.write(os.read(0,4096))
''')
    tmux = [shutil.which('tmux'), '-S', str(root / 'socket')]
    subprocess.run(tmux + ['-f', '/dev/null', 'new-session', '-d', '-s', 'test', 'python3', str(fixture), str(received)], check=True)
    for args in [
        ['set-option', '-g', 'mouse', 'on'],
        ['bind-key', '-T', 'root', 'WheelUpPane', 'if-shell', '-F', '#{pane_in_mode}', 'send-keys -M', 'copy-mode -e; send-keys -M'],
        ['bind-key', '-T', 'root', 'WheelDownPane', 'if-shell', '-F', '#{pane_in_mode}', 'send-keys -M', ''],
        ['bind-key', '-T', 'root', 'WheelUpStatus', 'if-shell', '-F', '#{pane_in_mode}', 'send-keys -X -N 5 scroll-up', 'copy-mode -e; send-keys -X -N 5 scroll-up'],
        ['bind-key', '-T', 'root', 'WheelDownStatus', 'if-shell', '-F', '#{pane_in_mode}', 'send-keys -X -N 5 scroll-down', ''],
    ]:
        subprocess.run(tmux + args, check=True)
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack('HHHH', 24, 90, 0, 0))
    client = subprocess.Popen(tmux + ['attach-session', '-t', 'test'], stdin=slave, stdout=slave, stderr=slave,
                              env={**os.environ, 'TERM': 'xterm-256color'})
    os.close(slave)
    try:
        time.sleep(.4)
        os.write(master, b'\x1b[<65;10;10M')  # Down at prompt: must not become Down key.
        time.sleep(.15)
        assert not received.read_bytes(), received.read_bytes()
        os.write(master, b'\x1b[<64;10;10M')
        time.sleep(.2)
        mode = subprocess.check_output(tmux + ['display-message', '-p', '-t', 'test', '#{pane_in_mode}']).strip()
        assert mode == b'1', mode
        assert not received.read_bytes(), received.read_bytes()
        os.write(master, b'\x1b[<65;10;10M')
        time.sleep(.15)
        assert not received.read_bytes(), received.read_bytes()
        mode = subprocess.check_output(tmux + ['display-message', '-p', '-t', 'test:0', '#{pane_in_mode}']).strip()
        if mode == b'1':
            subprocess.run(tmux + ['send-keys', '-X', '-t', 'test:0', 'cancel'], check=True)
        subprocess.run(tmux + ['new-window', '-d', '-t', 'test', 'sleep', '30'], check=True)
        os.write(master, b'\x1b[<64;10;24M')
        time.sleep(.2)
        state = subprocess.check_output(tmux + ['display-message', '-p', '-t', 'test', '#{window_index}:#{pane_in_mode}']).strip()
        assert state == b'0:1', state
        os.write(master, b'\x1b[<65;10;24M')
        time.sleep(.15)
        window = subprocess.check_output(tmux + ['display-message', '-p', '-t', 'test', '#{window_index}']).strip()
        assert window == b'0', window
        assert not received.read_bytes(), received.read_bytes()
        print('OK: pane and status wheel scroll history, never switch windows, and send zero bytes to CLI.')
    finally:
        subprocess.run(tmux + ['kill-server'], check=True)
        client.wait(timeout=5)
        os.close(master)
