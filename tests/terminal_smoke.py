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
fcntl.ioctl(terminal, termios.TIOCSWINSZ, struct.pack("HHHH", 42, 140, 0, 0))
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
    wait_for("Duel 1V1")
    send(b"\r", "Jména hráčů")
    send(b"\r" + b"\x7f" * 6 + "Žaneta\r".encode(), "Žaneta")
    send(b"\x1b[B\x1b[B\r", "Obchod:")
    send(b"\x1b[B" * 10 + b"\r", "Zahájit souboj")
    send(b"\x1b[B" * 10 + b"\r", "Příprava")
    send(b"\x1b[B\r", "Příprava")
    send(b"\x1b[B\r", "Kolo 1 (1/2)")
    start = len(output)
    send(b"\r", "Přidáno do plánu: Pěst")
    assert b"37 / 40" not in output[start:], "attack resolved before End turn"
    send(b"h", "Začni tady")
    send(b"\x1b", "Plán")
    send(b"\x1b[B" * 3 + b"\r", "37 / 40")
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
    print("terminal_smoke: OK (welcome, Czech names, shop, preparation, queued attack, help return, resolution, exit)")
finally:
    Path("build/terminal-smoke.txt").write_bytes(output)
    try:
        os.kill(pid, 9)
        os.waitpid(pid, 0)
    except (ProcessLookupError, ChildProcessError):
        pass
    os.close(terminal)
