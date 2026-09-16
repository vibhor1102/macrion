#!/usr/bin/env python3
"""Build, install and ART-compile Macrion's ARM64 debug APK. Standard library only."""

import argparse
import concurrent.futures
import fcntl
import json
import math
import os
from pathlib import Path
import re
import shutil
import signal
import struct
import subprocess
import sys
import tempfile
import threading
import time
import wave

ROOT = Path(__file__).resolve().parents[1]
DEVICE = "192.168.1.170:5555"
ASSEMBLE = ":smartautoclicker:assembleFDroidDebug"
DEFAULT_MODE = "debug"


class Failure(Exception):
    def __init__(self, message, code=1):
        super().__init__(message)
        self.code = code if code > 0 else 1


def capture(command, timeout=5, **kwargs):
    try:
        return subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, timeout=timeout, **kwargs)
    except subprocess.TimeoutExpired:
        return subprocess.CompletedProcess(command, 124, "Command timed out\n")


def make_alert(path):
    """A one-second soft two-tone alert, with fades to avoid clicks."""
    rate = 22050
    samples = []
    for i in range(rate):
        t = i / rate
        local = t % 0.5
        envelope = max(0, min(1, local / 0.025, (0.45 - local) / 0.06))
        frequency = 660 if t < 0.5 else 520
        samples.append(int(7000 * envelope * math.sin(2 * math.pi * frequency * t)))
    with wave.open(str(path), "wb") as output:
        output.setparams((1, 2, rate, 0, "NONE", "not compressed"))
        output.writeframes(struct.pack(f"<{len(samples)}h", *samples))


class Phone:
    def __init__(self, serial, alert, stop, timeout=35, reminder=10, interval=2):
        self.serial, self.alert, self.stop = serial, alert, stop
        self.timeout, self.reminder, self.interval = timeout, reminder, interval

    def command(self, *args):
        return ["adb", "-s", self.serial, *args]

    def online(self, timeout=3):
        result = capture(self.command("shell", "echo", "macrion-ready"), timeout=timeout)
        return result.returncode == 0 and result.stdout.strip() == "macrion-ready"

    def wait(self):
        if self.online():
            return
        start = time.monotonic()
        self.alert()
        reminded = False
        while not self.stop.is_set():
            elapsed = time.monotonic() - start
            if elapsed >= self.timeout:
                raise Failure(f"Phone {self.serial} remained unreachable for {self.timeout}s. "
                              "Enable wireless ADB/wake the phone and rerun.")
            if elapsed >= self.reminder and not reminded:
                self.alert()
                reminded = True
            remaining = self.timeout - (time.monotonic() - start)
            if remaining <= 0:
                continue
            capture(["adb", "connect", self.serial], timeout=min(2, remaining))
            remaining = self.timeout - (time.monotonic() - start)
            if remaining > 0 and self.online(timeout=min(2, remaining)):
                return
            self.stop.wait(min(self.interval, max(0, self.timeout - (time.monotonic() - start))))
        raise Failure("Deployment cancelled", 130)


def stop_child(process):
    # Signal the Gradle client so it can cancel its build; never kill shared daemons.
    if process.poll() is None:
        process.send_signal(signal.SIGINT)
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


class Runner:
    def __init__(self, root, logs, verbose=False):
        self.root, self.logs, self.verbose = root, logs, verbose

    def run(self, label, command, monitor=None, timeout=None, report=True):
        path = self.logs / f"{label}.log"
        started = time.monotonic()
        timed_out = False
        with path.open("w") as log:
            process = subprocess.Popen(command, cwd=self.root, stdout=log,
                                       stderr=subprocess.STDOUT, start_new_session=True)
            try:
                with path.open() as reader:
                    while process.poll() is None:
                        if monitor is not None and monitor.done():
                            monitor.result()
                        if timeout and time.monotonic() - started > timeout:
                            timed_out = True
                            break
                        if self.verbose and report:
                            print(reader.read(), end="", flush=True)
                        time.sleep(0.1)
                    if self.verbose and report:
                        print(reader.read(), end="", flush=True)
            finally:
                stop_child(process)
        if timed_out:
            with path.open("a") as log:
                log.write(f"\n{label} timed out after {timeout}s\n")
        code = 124 if timed_out else process.returncode
        if code and report:
            if not self.verbose:
                sys.stderr.write(path.read_text(errors="replace"))
            raise Failure(f"{label} failed; full output: {path}", code)
        return code, path


def device_step(phone, runner, label, arguments, timeout, success):
    # One transport retry only; never disguise an actual package-manager failure.
    for attempt in range(2):
        phone.wait()
        code, log = runner.run(f"{label}-{attempt + 1}", phone.command(*arguments),
                               timeout=timeout, report=False)
        output = log.read_text(errors="replace")
        if code == 0 and success(output):
            return output
        transport_error = (any(fragment in output.lower() for fragment in (
            "device offline", "device not found", "no devices/emulators found",
            "connection reset", "connection closed", "closed", "broken pipe",
            "transport error", "failed to read", "failed to write",
        )) or re.search(r"device .+ not found", output.lower())) and "failure [" not in output.lower()
        if attempt == 0 and (transport_error or code == 124) and not phone.online():
            continue
        sys.stderr.write(output)
        raise Failure(f"{label} failed; full output: {log}", code or 1)


def verify_speed(output):
    section = output.partition("Dexopt state:")[2].partition("Compiler stats:")[0]
    filters = re.findall(r"arm64:\s*\[status=([^\]]+)\]", section)
    if not filters or any(value != "speed" for value in filters):
        raise Failure("ART speed compilation was not applied (reported: "
                      + (", ".join(filters) or "unknown")
                      + "). Android 14 disables AOT for debuggable apps. "
                      "See the verify-speed log; command success alone is insufficient.")


def verify_mode(output, mode):
    flags = re.search(r"pkgFlags=\[([^\]]*)\]", output)
    if flags is None:
        raise Failure("Cannot verify installed application's debugging flags")
    debuggable = "DEBUGGABLE" in flags.group(1).split()
    if debuggable != (mode == "debug"):
        raise Failure(f"Installed application's debuggable flag does not match {mode} mode")


def apk_from_metadata(root):
    directory = root / "smartautoclicker/build/outputs/apk/fDroid/debug"
    metadata = json.loads((directory / "output-metadata.json").read_text())
    matches = [entry for entry in metadata["elements"] if any(
        item["filterType"] == "ABI" and item["value"] == "arm64-v8a"
        for item in entry.get("filters", []))]
    if len(matches) != 1:
        raise Failure("Expected exactly one ARM64 debug APK in build metadata")
    apk = (directory / matches[0]["outputFile"]).resolve()
    if apk.parent != directory.resolve() or not apk.is_file():
        raise Failure("Build metadata points to a missing or unexpected APK")
    package = metadata["applicationId"]
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.]*\.debug", package):
        raise Failure(f"Refusing to deploy unexpected non-debug package: {package}")
    return apk, package


def options():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", default=DEVICE, help="Wireless ADB host:port")
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--mode", choices=("debug", "performance"),
                        default=os.environ.get("MACRION_DEBUG_MODE", DEFAULT_MODE),
                        help="debug: full debugger access; performance: profileable with verified ART speed compilation")
    parser.add_argument("--test", nargs=2, action="append", default=[],
                        metavar=("TASK", "FILTER"), help="Focused unit-test task and class/method filter; repeatable")
    parser.add_argument("--source-info", action="store_true", help="Include Compose source metadata for diagnostics")
    parser.add_argument("--skip-precompile", action="store_true", help="Skip device ART compilation for diagnostics")
    parser.add_argument("--verbose", action="store_true", help="Stream Gradle output (ADB recovery always stays quiet)")
    args = parser.parse_args()
    if args.mode not in ("debug", "performance"):
        parser.error("MACRION_DEBUG_MODE must be debug or performance")
    if args.workers < 1 or not re.fullmatch(r"[A-Za-z0-9_.-]+:[0-9]+", args.device):
        parser.error("Use a positive worker count and a wireless host:port")
    for task, pattern in args.test:
        if not re.fullmatch(r":[A-Za-z0-9_:-]+:test[A-Za-z0-9]*DebugUnitTest", task):
            parser.error("--test requires a fully qualified debug unit-test task")
        if not pattern or pattern.startswith("-") or pattern == "*":
            parser.error("--test requires a focused class or method filter")
    return args


def deploy(args):
    for tool in ("java", "adb"):
        if not shutil.which(tool):
            raise Failure(f"Required executable unavailable: {tool}")
    java = Path(os.environ.get("JAVA_HOME", Path(shutil.which("java")).resolve().parent.parent))
    result = capture([str(java / "bin/java"), "-version"])
    if result.returncode or not re.search(r'version "25[.\"]', result.stdout):
        raise Failure("Select JDK 25 with JAVA_HOME before running this helper")
    os.environ["JAVA_HOME"] = str(java.resolve())
    if shutil.which("ccache"):
        os.environ["USE_CCACHE"] = "true"
    player = next((tool for tool in ("paplay", "pw-play") if shutil.which(tool)), None)
    if not player:
        raise Failure("Install paplay or pw-play for the phone reconnect alert")
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")).resolve()
    gradle_home.mkdir(parents=True, exist_ok=True)
    # A shared lock also serializes deployments from other Macrion worktrees.
    with (gradle_home / "macrion-deploy.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise Failure("Another Macrion deployment is running") from None
        log_root = ROOT / ".gradle/deploy-debug"
        log_root.mkdir(parents=True, exist_ok=True)
        logs = Path(tempfile.mkdtemp(prefix=time.strftime("%Y%m%d-%H%M%S-"), dir=log_root))
        runner = Runner(ROOT, logs, args.verbose)
        alert_file = logs / "alert.wav"
        make_alert(alert_file)

        def alert():
            # Never leak audio/ADB recovery output into the agent's terminal.
            capture([player, str(alert_file)], timeout=2)

        base = [str(ROOT / "gradlew"), "--daemon", "--console=plain",
                f"-Dorg.gradle.java.home={java.resolve()}",
                f"--max-workers={args.workers}", "--parallel", "--build-cache",
                "--configuration-cache", "-PmacrionDebugAbi=arm64-v8a",
                f"-PmacrionDebugMode={args.mode}"]
        if args.source_info:
            base += ["-PmacrionIncludeComposeSourceInfo=true"]
        # Do not silently create an extra daemon if an unmanaged build is busy.
        _, status = runner.run("daemon-status", [*base, "--status"], timeout=30)
        if re.search(r"^\s*\d+\s+BUSY\b", status.read_text(), re.MULTILINE):
            raise Failure("A Gradle daemon for this wrapper is busy; wait for that build to finish")
        stop = threading.Event()
        phone = Phone(args.device, alert, stop)
        started = time.monotonic()
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            monitor = pool.submit(phone.wait)
            try:
                # One invocation, grouping filters by task, keeps compiler daemons warm.
                tasks = {}
                for task, pattern in args.test:
                    tasks.setdefault(task, []).append(pattern)
                command = list(base)
                if tasks:
                    command.extend(["--init-script", str(ROOT / "scripts/debug-tests.init.gradle")])
                for task, patterns in tasks.items():
                    command.append(task)
                    for pattern in patterns:
                        command.extend(["--tests", pattern])
                command.append(ASSEMBLE)
                print(f"Building ARM64 debug APK ({args.mode} mode)" +
                      (" and running focused tests…" if tasks else "…"), flush=True)
                runner.run("build", command, monitor)
                monitor.result()
                apk, package = apk_from_metadata(ROOT)
                print("Installing…", flush=True)
                device_step(phone, runner, "install", ["install", "-r", str(apk)], 180,
                            lambda output: "Success" in output.splitlines())
                installed = device_step(phone, runner, "verify-mode", ["shell", "dumpsys", "package", package],
                                        15, lambda output: "pkgFlags=" in output)
                verify_mode(installed, args.mode)
                if args.mode == "performance" and not args.skip_precompile:
                    print("Optimizing on device…", flush=True)
                    device_step(phone, runner, "precompile", ["shell", "cmd", "package", "compile",
                                "-m", "speed", "-f", package], 180,
                                lambda output: "Success" in output.splitlines())
                    output = device_step(phone, runner, "verify-speed", ["shell", "dumpsys", "package", package],
                                         15, lambda output: "Dexopt state:" in output)
                    verify_speed(output)
                print(f"Done in {time.monotonic() - started:.0f}s. APK: {apk}\nLogs: {logs}", flush=True)
            except Failure as error:
                raise Failure(f"{error}\nLogs: {logs}", error.code) from None
            finally:
                stop.set()


def main():
    def interrupted(signum, frame):
        raise KeyboardInterrupt
    signal.signal(signal.SIGTERM, interrupted)
    try:
        deploy(options())
    except Failure as error:
        print(f"Error: {error}", file=sys.stderr)
        return error.code
    except KeyboardInterrupt:
        print("Deployment cancelled", file=sys.stderr)
        return 130
    except (OSError, ValueError, KeyError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
