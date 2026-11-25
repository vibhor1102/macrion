#!/usr/bin/env python3
"""Build, install and ART-compile Macrion's debug APK. Standard library only."""

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
DEFAULT_AVD = "Macrion_Issue73_Android16"
ASSEMBLE = ":smartautoclicker:assembleFDroidDebug"
DEFAULT_MODE = "performance"
PHONE_ABI = "arm64-v8a"
EMULATOR_ABI = "x86_64"
EMULATOR_UNIT = "macrion-test-emulator.service"
EMULATOR_PORT = 5554


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


class AdbDevice:
    def __init__(self, adb, serial):
        self.adb, self.serial = adb, serial

    def command(self, *args):
        return [self.adb, "-s", self.serial, *args]


class Phone(AdbDevice):
    def __init__(self, adb, serial, alert, stop, timeout=35, reminder=10, interval=2):
        super().__init__(adb, serial)
        self.alert, self.stop = alert, stop
        self.timeout, self.reminder, self.interval = timeout, reminder, interval

    def online(self, timeout=3):
        result = capture(self.command("shell", "echo", "macrion-ready"), timeout=timeout)
        return result.returncode == 0 and result.stdout.strip() == "macrion-ready"

    def wait(self):
        if self.online():
            return
        start = time.monotonic()
        alerted = False
        reminded = False
        while not self.stop.is_set():
            elapsed = time.monotonic() - start
            if elapsed >= self.timeout:
                raise Failure(f"Phone {self.serial} remained unreachable for {self.timeout}s. "
                              "Enable wireless ADB/wake the phone and rerun.")
            if alerted and elapsed >= self.reminder and not reminded:
                self.alert()
                reminded = True
            remaining = self.timeout - (time.monotonic() - start)
            if remaining <= 0:
                continue
            capture([self.adb, "connect", self.serial], timeout=min(2, remaining))
            remaining = self.timeout - (time.monotonic() - start)
            if remaining > 0 and self.online(timeout=min(2, remaining)):
                return
            if not alerted and not self.stop.is_set():
                self.alert()
                alerted = True
            self.stop.wait(min(self.interval, max(0, self.timeout - (time.monotonic() - start))))
        raise Failure("Deployment cancelled", 130)


class Emulator(AdbDevice):
    def online(self, timeout=3):
        result = capture(self.command("shell", "getprop", "sys.boot_completed"), timeout=timeout)
        return result.returncode == 0 and result.stdout.strip() == "1"

    def wait(self):
        if not self.online():
            raise Failure(f"Emulator {self.serial} is offline or still booting.")

    def avd_name(self):
        result = capture(self.command("emu", "avd", "name"), timeout=3)
        lines = result.stdout.splitlines()
        return lines[0] if result.returncode == 0 and len(lines) == 2 and lines[1] == "OK" else None


def sdk_tool(relative_path):
    properties = ROOT / "local.properties"
    local_sdk = None
    if properties.is_file():
        for line in properties.read_text().splitlines():
            key, _, value = line.partition("=")
            if key.strip() == "sdk.dir":
                local_sdk = re.sub(r"\\([\\: =#])", r"\1", value.strip())
                break
    for sdk_root in (local_sdk, os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")):
        if sdk_root:
            candidate = Path(sdk_root).expanduser() / relative_path
            if candidate.is_file() and os.access(candidate, os.X_OK):
                return str(candidate.resolve())
    tool = shutil.which(Path(relative_path).name)
    if tool is None:
        raise Failure(f"Android SDK executable unavailable: {relative_path}")
    return tool


def running_emulators(adb):
    result = capture([adb, "devices"], timeout=10)
    if result.returncode:
        raise Failure(f"Could not list ADB devices: {result.stdout.strip()}")
    return re.findall(r"^(emulator-[0-9]+)\s", result.stdout, re.MULTILINE)


def prepare_emulator(adb, avd, logs, boot_timeout=150):
    serials = running_emulators(adb)
    if len(serials) > 1:
        raise Failure(f"Close additional emulators before deployment: {', '.join(serials)}")

    managed_serial = f"emulator-{EMULATOR_PORT}"
    managed_active = capture(["systemctl", "--user", "is-active", "--quiet", EMULATOR_UNIT]).returncode == 0
    if managed_active and serials and serials != [managed_serial]:
        raise Failure(f"Managed emulator is starting and {serials[0]} is already attached.")

    device = Emulator(adb, serials[0] if serials else managed_serial)
    emulator_log = logs / "emulator.log"
    started_here = False
    try:
        if not serials and not managed_active:
            executable = sdk_tool("emulator/emulator")
            available = capture([executable, "-list-avds"], timeout=10)
            if available.returncode or avd not in available.stdout.splitlines():
                raise Failure(f"Android virtual device {avd!r} was not found. Available AVDs:\n{available.stdout.strip()}")
            if not os.environ.get("DISPLAY"):
                raise Failure("A desktop DISPLAY is required to start a windowed emulator.")
            launcher = ["systemd-run", "--user", "--collect", f"--unit={EMULATOR_UNIT}",
                        f"--property=StandardOutput=append:{emulator_log}",
                        f"--property=StandardError=append:{emulator_log}",
                        "--setenv=QT_QPA_PLATFORM=xcb"]
            for name in ("DISPLAY", "XAUTHORITY", "ANDROID_AVD_HOME", "ANDROID_USER_HOME", "ANDROID_EMULATOR_HOME"):
                if name in os.environ:
                    launcher.append(f"--setenv={name}={os.environ[name]}")
            result = capture([*launcher, executable, "-avd", avd, "-port", str(EMULATOR_PORT),
                              "-cores", "2", "-memory", "2048", "-gpu", "host",
                              "-no-audio", "-no-snapshot"], timeout=10)
            if result.returncode:
                raise Failure(f"Could not start AVD {avd}: {result.stdout.strip()}")
            started_here = True
            print(f"Starting windowed AVD {avd} ({device.serial})…", flush=True)

        deadline = time.monotonic() + boot_timeout
        resolved_name = None
        while time.monotonic() < deadline:
            if resolved_name is None:
                resolved_name = device.avd_name()
                if resolved_name is not None and resolved_name != avd:
                    raise Failure(f"{device.serial} is running AVD {resolved_name!r}. Close it before starting {avd!r}.")
            if resolved_name and device.online():
                if running_emulators(adb) != [device.serial]:
                    raise Failure("The attached emulator list changed while waiting for Android to boot.")
                print(f"AVD {avd} ready on {device.serial}", flush=True)
                return device
            if started_here and capture(["systemctl", "--user", "is-active", "--quiet", EMULATOR_UNIT]).returncode != 0:
                raise Failure(f"AVD {avd} exited before booting. Log: {emulator_log}")
            time.sleep(1)
        raise Failure(f"AVD {avd} did not finish booting within {boot_timeout}s. Logs: {logs}")
    except (Failure, KeyboardInterrupt):
        if started_here:
            capture(["systemctl", "--user", "stop", EMULATOR_UNIT], timeout=20)
        raise


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


def device_step(device, runner, label, arguments, timeout, success):
    # One transport retry only; never disguise an actual package-manager failure.
    for attempt in range(2):
        device.wait()
        code, log = runner.run(f"{label}-{attempt + 1}", device.command(*arguments),
                               timeout=timeout, report=False)
        output = log.read_text(errors="replace")
        if code == 0 and success(output):
            return output
        transport_error = (any(fragment in output.lower() for fragment in (
            "device offline", "device not found", "no devices/emulators found",
            "connection reset", "connection closed", "closed", "broken pipe",
            "transport error", "failed to read", "failed to write",
        )) or re.search(r"device .+ not found", output.lower())) and "failure [" not in output.lower()
        if attempt == 0 and (transport_error or code == 124) and not device.online():
            continue
        sys.stderr.write(output)
        raise Failure(f"{label} failed; full output: {log}", code or 1)


def verify_speed(output, abi):
    section = output.partition("Dexopt state:")[2].partition("Compiler stats:")[0]
    instruction_set = "arm64" if abi == PHONE_ABI else abi
    filters = re.findall(rf"{re.escape(instruction_set)}:\s*\[status=([^\]]+)\]", section)
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


def apk_from_metadata(root, abi):
    directory = root / "smartautoclicker/build/outputs/apk/fDroid/debug"
    metadata = json.loads((directory / "output-metadata.json").read_text())
    matches = [entry for entry in metadata["elements"] if any(
        item["filterType"] == "ABI" and item["value"] == abi
        for item in entry.get("filters", []))]
    if len(matches) != 1:
        raise Failure(f"Expected exactly one {abi} debug APK in build metadata")
    apk = (directory / matches[0]["outputFile"]).resolve()
    if apk.parent != directory.resolve() or not apk.is_file():
        raise Failure("Build metadata points to a missing or unexpected APK")
    package = metadata["applicationId"]
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.]*\.debug", package):
        raise Failure(f"Refusing to deploy unexpected non-debug package: {package}")
    return apk, package


def options():
    parser = argparse.ArgumentParser(description=__doc__, epilog=f"Default deployment target: {DEFAULT_AVD}.")
    target = parser.add_mutually_exclusive_group()
    target.add_argument("--device", action="append", metavar="SERIAL",
                        help="Wireless ADB host:port or a running x86_64 emulator serial. Repeat to deploy to multiple targets.")
    target.add_argument("--avd", help="Start or reuse the named windowed x86_64 AVD, preserving its app data")
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--mode", choices=("debug", "performance", "test-only"),
                        default=os.environ.get("MACRION_DEBUG_MODE", DEFAULT_MODE),
                        help="performance (default): profileable with verified ART speed compilation; debug: full debugger access; test-only: run tests without device deployment")
    parser.add_argument("--test-only", action="store_true",
                        help="Run focused unit tests without assembling an APK or contacting a device")
    operation = parser.add_mutually_exclusive_group()
    operation.add_argument("--build-only", action="store_true",
                           help="Assemble APKs without connecting to a device or starting an emulator")
    operation.add_argument("--install-only", action="store_true",
                           help="Install existing APK outputs and verify them without running Gradle")
    parser.add_argument("--both-abis", action="store_true",
                        help="Build separate ARM64 and x86_64 APKs in one Gradle invocation")
    parser.add_argument("--test", nargs=2, action="append", default=[],
                        metavar=("TASK", "FILTER"), help="Focused unit-test task and class/method filter; repeatable")
    parser.add_argument("--source-info", action="store_true", help="Include Compose source metadata for diagnostics")
    parser.add_argument("--skip-precompile", action="store_true", help="Skip device ART compilation for diagnostics")
    parser.add_argument("--verbose", action="store_true", help="Stream Gradle output (ADB recovery always stays quiet)")
    args = parser.parse_args()
    if args.mode not in ("debug", "performance", "test-only"):
        parser.error("MACRION_DEBUG_MODE must be debug, performance, or test-only")
    args.device = args.device or []
    if not args.avd and not args.device:
        args.avd = DEFAULT_AVD
    if args.workers < 1 or any(not re.fullmatch(r"(?:[A-Za-z0-9_.-]+:[0-9]+|emulator-[0-9]+)", serial)
                               for serial in args.device):
        parser.error("Use a positive worker count and a wireless host:port or emulator serial")
    if len(set(args.device)) != len(args.device):
        parser.error("Specify each device only once")
    is_test_only = args.test_only or args.mode == "test-only"
    if is_test_only and (args.build_only or args.install_only or args.both_abis):
        parser.error("Test-only mode cannot be combined with --build-only, --install-only, or --both-abis")
    if args.install_only and (args.test or args.both_abis or args.source_info):
        parser.error("--install-only cannot be combined with build or test options")
    if is_test_only and not args.test:
        parser.error("Test-only mode requires at least one --test TASK FILTER")
    for task, pattern in args.test:
        if not re.fullmatch(r":[A-Za-z0-9_:-]+:test[A-Za-z0-9]*DebugUnitTest", task):
            parser.error("--test requires a fully qualified debug unit-test task")
        if not pattern or pattern.startswith("-") or pattern == "*":
            parser.error("--test requires a focused class or method filter")
    return args


def gradle_command(args, build_abi, is_test_only, runner):
    java_path = shutil.which("java")
    java_home = os.environ.get("JAVA_HOME")
    if not java_home and not java_path:
        raise Failure("Required executable unavailable: java")
    java = Path(java_home) if java_home else Path(java_path).resolve().parent.parent
    result = capture([str(java / "bin/java"), "-version"])
    if result.returncode or not re.search(r'version "25[."]', result.stdout):
        raise Failure("Select JDK 25 with JAVA_HOME before running this helper")
    os.environ["JAVA_HOME"] = str(java.resolve())
    if shutil.which("ccache"):
        os.environ["USE_CCACHE"] = "true"

    effective_mode = "debug" if is_test_only else args.mode
    command = [str(ROOT / "gradlew"), "--daemon", "--console=plain",
               f"-Dorg.gradle.java.home={java.resolve()}",
               f"--max-workers={args.workers}", "--parallel", "--build-cache",
               "--configuration-cache", f"-PmacrionDebugAbi={build_abi}",
               f"-PmacrionDebugMode={effective_mode}"]
    if args.source_info:
        command.append("-PmacrionIncludeComposeSourceInfo=true")
    # Do not silently create an extra daemon if an unmanaged build is busy.
    _, status = runner.run("daemon-status", [*command, "--status"], timeout=30)
    if re.search(r"^\s*\d+\s+BUSY\b", status.read_text(), re.MULTILINE):
        raise Failure("A Gradle daemon for this wrapper is busy; wait for that build to finish")

    tasks = {}
    for task, pattern in args.test:
        tasks.setdefault(task, []).append(pattern)
    if tasks:
        command.extend(["--init-script", str(ROOT / "scripts/debug-tests.init.gradle")])
    for task, patterns in tasks.items():
        command.append(task)
        for pattern in patterns:
            command.extend(["--tests", pattern])
    if not is_test_only:
        command.append(ASSEMBLE)
    return command


def prepare_devices(args, adb, targets, logs, stop):
    if args.avd:
        devices = [(prepare_emulator(adb, args.avd, logs), EMULATOR_ABI)]
    else:
        requested_emulators = {serial for serial, abi in targets.items() if abi == EMULATOR_ABI}
        if requested_emulators and set(running_emulators(adb)) != requested_emulators:
            raise Failure(f"Expected these to be the only attached emulators: {', '.join(sorted(requested_emulators))}.")
        if PHONE_ABI in targets.values():
            player = next((tool for tool in ("paplay", "pw-play") if shutil.which(tool)), None)
            if not player:
                raise Failure("Install paplay or pw-play for the phone reconnect alert")
            alert_file = logs / "alert.wav"
            make_alert(alert_file)

            def alert():
                capture([player, str(alert_file)], timeout=2)

        devices = [
            (Emulator(adb, serial) if abi == EMULATOR_ABI else Phone(adb, serial, alert, stop), abi)
            for serial, abi in targets.items()
        ]

    for device, abi in devices:
        if abi == EMULATOR_ABI:
            device.wait()
            result = capture(device.command("shell", "getprop", "ro.product.cpu.abilist"))
            if result.returncode or abi not in result.stdout.strip().split(","):
                raise Failure(f"Emulator {device.serial} must support {abi}.")
    return devices


def install(device, runner, apk, package, abi, args):
    print(f"Installing {apk.name} on {device.serial}…", flush=True)
    device_step(device, runner, "install", ["install", "-r", str(apk)], 180,
                lambda output: "Success" in output.splitlines())
    installed = device_step(device, runner, "verify-mode", ["shell", "dumpsys", "package", package],
                            15, lambda output: "pkgFlags=" in output)
    verify_mode(installed, args.mode)
    if args.mode == "performance" and not args.skip_precompile:
        print(f"Optimizing on {device.serial}…", flush=True)
        device_step(device, runner, "precompile", ["shell", "cmd", "package", "compile",
                    "-m", "speed", "-f", package], 180,
                    lambda output: "Success" in output.splitlines())
        output = device_step(device, runner, "verify-speed", ["shell", "dumpsys", "package", package],
                             15, lambda output: "Dexopt state:" in output)
        verify_speed(output, abi)
    print(f"Verified {package} on {device.serial}", flush=True)


def deploy(args):
    is_test_only = args.test_only or args.mode == "test-only"
    targets = {
        serial: EMULATOR_ABI if serial.startswith("emulator-") else PHONE_ABI
        for serial in args.device
    }
    abis = (EMULATOR_ABI,) if args.avd else tuple(dict.fromkeys(targets.values()))
    if is_test_only:
        abis = (PHONE_ABI,)
    elif args.both_abis:
        abis = (PHONE_ABI, EMULATOR_ABI)
    build_abi = abis[0] if len(abis) == 1 else "both"
    installs = not (is_test_only or args.build_only)
    adb = sdk_tool("platform-tools/adb") if installs else None

    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")).resolve()
    gradle_home.mkdir(parents=True, exist_ok=True)
    # Serialize builds and reads of the current APK metadata across helper runs.
    with (gradle_home / "macrion-deploy.lock").open("a") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise Failure("Another Macrion deployment is running") from None
        log_root = ROOT / ".gradle/deploy-debug"
        log_root.mkdir(parents=True, exist_ok=True)
        logs = Path(tempfile.mkdtemp(prefix=time.strftime("%Y%m%d-%H%M%S-"), dir=log_root))
        runner = Runner(ROOT, logs, args.verbose)
        started = time.monotonic()
        stop = threading.Event()

        try:
            command = None if args.install_only else gradle_command(args, build_abi, is_test_only, runner)
            if is_test_only:
                print("Running focused tests…", flush=True)
                runner.run("test", command)
                print(f"Done in {time.monotonic() - started:.0f}s. Focused tests passed.\nLogs: {logs}", flush=True)
                return

            if args.install_only:
                artifacts = {abi: apk_from_metadata(ROOT, abi) for abi in abis}
                print("Using existing APK outputs. Gradle will not run.", flush=True)
            devices = prepare_devices(args, adb, targets, logs, stop) if installs else []

            if command is not None:
                def wait_for_devices():
                    for device, _ in devices:
                        device.wait()

                description = " and ".join(abis)
                print(f"Building {description} debug APK ({args.mode} mode)" +
                      (" and running focused tests…" if args.test else "…"), flush=True)
                with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
                    monitor = pool.submit(wait_for_devices) if devices else None
                    try:
                        runner.run("build", command, monitor)
                        if monitor is not None:
                            monitor.result()
                    finally:
                        stop.set()
                stop.clear()
                artifacts = {abi: apk_from_metadata(ROOT, abi) for abi in abis}

            for abi, (apk, _) in artifacts.items():
                print(f"APK ({abi}): {apk}", flush=True)

            for index, (device, abi) in enumerate(devices, start=1):
                device_runner = runner
                if len(devices) > 1:
                    device_logs = logs / f"device-{index}"
                    device_logs.mkdir()
                    device_runner = Runner(ROOT, device_logs, args.verbose)
                try:
                    install(device, device_runner, *artifacts[abi], abi, args)
                except Failure as error:
                    raise Failure(f"{device.serial}: {error}", error.code) from None

            print(f"Done in {time.monotonic() - started:.0f}s.\nLogs: {logs}", flush=True)
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
