import fcntl
import os
from pathlib import Path
import pty
import select
import struct
import sys
import termios
import time

# skutecny terminal overi vstup klavesnici a ukonceni aplikace
java = sys.argv[1] if len(sys.argv) > 1 else "java"
pid, terminal = pty.fork()
if pid == 0:
    os.execvp(java, [java, "-cp", "build/check", "Main"])
fcntl.ioctl(terminal, termios.TIOCSWINSZ, struct.pack("HHHH", 24, 100, 0, 0))
output = bytearray()


def wait_for(text, start=0):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        if text.encode() in output[start:]:
            return
        if select.select([terminal], [], [], 0.1)[0]:
            try:
                output.extend(os.read(terminal, 65536))
            except OSError:
                break
    raise AssertionError("missing terminal output: " + text)


def send(keys, expected):
    start = len(output)
    os.write(terminal, keys)
    wait_for(expected, start)


try:
    wait_for("Připraveni")
    send("\rŽaneta\r".encode(), "Žaneta")
    send(b"\x1b[B\x1b[B\r", "První v obchodě")
    send(b"\r", "Společná nabídka")
    # pri vysce 24 je sedm nabidek a pak strankovani inventar a pokracovani
    send(b"\x1b[B" * 9 + b"\r", "Beze změny: 1/2")
    send(b"\x1b[B" * 9 + b"\r", "Přípravný tah")
    # pet tlacitek oddeluje zakladni utok od konce tahu
    send(b"\x1b[B" * 4 + b"\r", "Přípravný tah")
    send(b"\x1b[B" * 4 + b"\r", "Bojový tah 3")
    send(b"\r", "základní útok")
    os.write(terminal, b"\x03")
    deadline = time.monotonic() + 5
    status = None
    while time.monotonic() < deadline:
        finished, value = os.waitpid(pid, os.WNOHANG)
        if finished:
            status = value
            break
        if select.select([terminal], [], [], 0.1)[0]:
            try:
                output.extend(os.read(terminal, 65536))
            except OSError:
                pass
    assert status == 0, f"terminal process failed or did not exit: {status}"
    assert b"\x1b[?1049l" in output, "alternate screen was not restored"
    assert b"Exception" not in output, "unexpected exception in terminal"
    Path("build/terminal-smoke.txt").write_bytes(output)
    print("terminal_smoke: OK (naming, Czech input, shop, preparation, attack, exit)")
finally:
    try:
        os.kill(pid, 9)
        os.waitpid(pid, 0)
    except (ProcessLookupError, ChildProcessError):
        pass
    os.close(terminal)
