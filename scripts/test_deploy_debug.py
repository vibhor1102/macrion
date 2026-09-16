"""Fast orchestration checks; no Gradle, phone, audio server, or waiting required."""
import concurrent.futures
import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

SPEC = importlib.util.spec_from_file_location("deploy_debug", Path(__file__).with_name("deploy-debug.py"))
deploy = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(deploy)


class Clock:
    def __init__(self):
        self.now = 0

    def monotonic(self):
        return self.now

    def wait(self, seconds):
        self.now += seconds


class RecoveryTests(unittest.TestCase):
    def phone(self, clock, online):
        stop = Mock()
        stop.is_set.return_value = False
        stop.wait.side_effect = clock.wait
        phone = deploy.Phone("192.168.1.170:5555", Mock(), stop)
        phone.online = Mock(side_effect=online)
        return phone

    def test_recovery_is_silent_and_alerts_once(self):
        clock = Clock()
        phone = self.phone(clock, [False, False, True])
        output = io.StringIO()
        with patch.object(deploy.time, "monotonic", clock.monotonic), \
                patch.object(deploy, "capture") as capture, \
                contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            phone.wait()
        phone.alert.assert_called_once()
        self.assertEqual(output.getvalue(), "")
        self.assertEqual(capture.call_count, 2)

    def test_deadline_and_exactly_two_alerts(self):
        clock = Clock()
        phone = self.phone(clock, lambda **kwargs: False)
        with patch.object(deploy.time, "monotonic", clock.monotonic), patch.object(deploy, "capture"):
            with self.assertRaisesRegex(deploy.Failure, "unreachable"):
                phone.wait()
        self.assertEqual(phone.alert.call_count, 2)
        self.assertEqual(clock.now, 35)

    def test_connected_phone_never_alerts(self):
        phone = self.phone(Clock(), [True])
        phone.wait()
        phone.alert.assert_not_called()

    def test_recovery_after_reminder_has_two_alerts(self):
        clock = Clock()
        phone = self.phone(clock, lambda **kwargs: clock.now >= 12)
        with patch.object(deploy.time, "monotonic", clock.monotonic), patch.object(deploy, "capture"):
            phone.wait()
        self.assertEqual(phone.alert.call_count, 2)
        self.assertEqual(clock.now, 12)

    def test_package_failure_is_not_retried(self):
        with tempfile.TemporaryDirectory() as temporary:
            log = Path(temporary) / "install.log"
            log.write_text("Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]\n")
            runner = Mock()
            runner.run.return_value = (1, log)
            phone = Mock()
            with contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(deploy.Failure):
                    deploy.device_step(phone, runner, "install", [], 5, lambda s: False)
            self.assertEqual(runner.run.call_count, 1)
            phone.online.assert_not_called()

    def test_transport_retry_does_not_print_failure(self):
        with tempfile.TemporaryDirectory() as temporary:
            failed, passed = Path(temporary) / "failed", Path(temporary) / "passed"
            failed.write_text("adb: device offline\n")
            passed.write_text("Success\n")
            runner, phone = Mock(), Mock()
            runner.run.side_effect = [(1, failed), (0, passed)]
            phone.online.return_value = False
            output = io.StringIO()
            with contextlib.redirect_stderr(output), contextlib.redirect_stdout(output):
                deploy.device_step(phone, runner, "install", [], 5, lambda s: s == "Success\n")
            self.assertEqual(phone.wait.call_count, 2)
            self.assertEqual(output.getvalue(), "")


class RunnerTests(unittest.TestCase):
    def test_default_mode_and_explicit_override(self):
        with patch.dict(os.environ, {}, clear=True), patch.object(sys, "argv", ["deploy-debug.py"]):
            self.assertEqual(deploy.options().mode, "debug")
        with patch.dict(os.environ, {"MACRION_DEBUG_MODE": "performance"}), \
                patch.object(sys, "argv", ["deploy-debug.py", "--mode", "debug"]):
            self.assertEqual(deploy.options().mode, "debug")

    def test_installed_mode_is_checked(self):
        debug = "pkgFlags=[ DEBUGGABLE HAS_CODE ]\nprivateFlags=[ PROFILEABLE_BY_SHELL ]"
        performance = "pkgFlags=[ HAS_CODE ]\nprivateFlags=[ PROFILEABLE_BY_SHELL ]"
        deploy.verify_mode(debug, "debug")
        deploy.verify_mode(performance, "performance")
        with self.assertRaisesRegex(deploy.Failure, "does not match"):
            deploy.verify_mode(debug, "performance")
        # Some Android builds omit profileability from dumpsys flag names.
        deploy.verify_mode("pkgFlags=[ HAS_CODE ]", "performance")

    def test_speed_verification_rejects_android_silent_downgrade(self):
        with self.assertRaisesRegex(deploy.Failure, "reported: verify"):
            deploy.verify_speed("Dexopt state:\n arm64: [status=verify] [reason=cmdline]\nCompiler stats:")
        with self.assertRaisesRegex(deploy.Failure, "unknown"):
            deploy.verify_speed("Success")
        deploy.verify_speed("Dexopt state:\n arm64: [status=speed] [reason=cmdline]\nCompiler stats:")

    def test_raw_failure_and_exit_code(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            output = io.StringIO()
            with contextlib.redirect_stderr(output):
                with self.assertRaises(deploy.Failure) as failure:
                    deploy.Runner(root, root).run("build", [sys.executable, "-c",
                        "import sys; print('raw compiler error'); sys.exit(7)"])
            self.assertEqual(failure.exception.code, 7)
            self.assertEqual(output.getvalue(), "raw compiler error\n")

    def test_success_retains_log_without_streaming(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                _, log = deploy.Runner(root, root).run("build", [sys.executable, "-c", "print('UP-TO-DATE')"])
            self.assertEqual(output.getvalue(), "")
            self.assertEqual(log.read_text(), "UP-TO-DATE\n")

    def test_timeout_is_nonzero_and_keeps_output(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            code, log = deploy.Runner(root, root).run("install", [sys.executable, "-c",
                "import time; print('partial output', flush=True); time.sleep(60)"], timeout=0.2, report=False)
            self.assertEqual(code, 124)
            self.assertIn("partial output", log.read_text())
            self.assertIn("timed out", log.read_text())

    def test_monitor_failure_cancels_active_process(self):
        with tempfile.TemporaryDirectory() as temporary:
            future = concurrent.futures.Future()
            future.set_exception(deploy.Failure("Phone unreachable"))
            root = Path(temporary)
            with patch.object(deploy, "stop_child", wraps=deploy.stop_child) as stop:
                with self.assertRaisesRegex(deploy.Failure, "Phone unreachable"):
                    deploy.Runner(root, root).run("build", [sys.executable, "-c",
                        "import time; time.sleep(60)"], future)
            self.assertIsNotNone(stop.call_args.args[0].poll())

    def test_metadata_selects_arm64_and_actual_package(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            folder = root / "smartautoclicker/build/outputs/apk/fDroid/debug"
            folder.mkdir(parents=True)
            apk = folder / "app-arm64.apk"
            apk.touch()
            metadata = {"applicationId": "io.github.example.debug", "elements": [
                {"filters": [{"filterType": "ABI", "value": "arm64-v8a"}], "outputFile": apk.name},
                {"filters": [{"filterType": "ABI", "value": "x86"}], "outputFile": "other.apk"}]}
            (folder / "output-metadata.json").write_text(json.dumps(metadata))
            self.assertEqual(deploy.apk_from_metadata(root), (apk, "io.github.example.debug"))
            metadata["elements"][0]["outputFile"] = "../../unexpected.apk"
            (folder / "output-metadata.json").write_text(json.dumps(metadata))
            with self.assertRaises(deploy.Failure):
                deploy.apk_from_metadata(root)

    def test_test_only_requires_filter(self):
        with patch.object(sys, "argv", ["deploy-debug.py", "--test-only"]):
            with self.assertRaises(SystemExit):
                with contextlib.redirect_stderr(io.StringIO()):
                    deploy.options()

    def test_test_only_options_parsing(self):
        with patch.object(sys, "argv", ["deploy-debug.py", "--test-only",
                                        "--test", ":core:common:settings:testFDroidDebugUnitTest", "*Test"]):
            opts = deploy.options()
            self.assertTrue(opts.test_only)
            self.assertEqual(len(opts.test), 1)
        with patch.object(sys, "argv", ["deploy-debug.py", "--mode", "test-only",
                                        "--test", ":core:common:settings:testFDroidDebugUnitTest", "*Test"]):
            opts = deploy.options()
            self.assertEqual(opts.mode, "test-only")

if __name__ == "__main__":
    unittest.main()
