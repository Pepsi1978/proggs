"""Reales Exitverhalten von tmux attach-session in einem eigenen Terminal; isolierter Socket."""
import os
import pty
import shutil
import subprocess
import tempfile
import time

with tempfile.TemporaryDirectory(prefix='tmux-attach-') as folder:
    tmux = [shutil.which('tmux'), '-S', os.path.join(folder, 'socket'), '-f', '/dev/null']

    def attach(target, detach_after=None):
        pid, fd = pty.fork()
        if pid == 0:
            os.execvp(tmux[0], tmux + ['attach-session', '-t', target])
        if detach_after:
            time.sleep(detach_after)
            subprocess.run(tmux + ['detach-client', '-s', target.lstrip('=')], check=True)
        deadline = time.time() + 10
        while time.time() < deadline:
            try:
                os.read(fd, 65536)
            except OSError:
                pass
            done, status = os.waitpid(pid, os.WNOHANG)
            if done:
                return os.waitstatus_to_exitcode(status)
            time.sleep(.05)
        raise AssertionError('attach-session endet nicht')

    try:
        # Eine normal endende CLI (auch mit eigenem Fehlercode) ist kein Anhängefehler.
        subprocess.run(tmux + ['new-session', '-d', '-s', 'ende', 'sh -c "sleep 1; exit 7"'], check=True)
        assert attach('=ende') == 0
        subprocess.run(tmux + ['new-session', '-d', '-s', 'trennen', 'sleep 30'], check=True)
        assert attach('=trennen', detach_after=1) == 0
        assert attach('=fehlt') != 0
        print('OK: Sitzungsende und Detach liefern 0, fehlende Sitzung liefert Fehler.')
    finally:
        subprocess.run(tmux + ['kill-server'], stderr=subprocess.DEVNULL)
