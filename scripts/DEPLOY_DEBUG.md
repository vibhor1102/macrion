# Local debug deployment

Run from any directory:

```sh
/home/vibhor/Scripts/Macrion/scripts/deploy-debug.py
```

The helper builds the ARM64 F-Droid debug APK and installs it over wireless ADB.
Performance mode also runs `cmd package compile -m speed -f` for the package
recorded in APK metadata and verifies the result. It does not launch the app,
publish releases, change versions, or
clear application data. Python 3, JDK 25, ADB, and `paplay` or `pw-play` are
required. `ccache` is enabled when installed.

## Modes

```sh
scripts/deploy-debug.py                     # Mode 1: debug, the default
scripts/deploy-debug.py --mode performance  # Mode 2: profileable, non-debuggable, ART speed
scripts/deploy-debug.py --mode debug        # Explicitly return to full debugging
```

Both use the same debug package, signing key and application data. Switching
modes replaces the installed APK without clearing data. Both are unminified,
retain readable names/line numbers and native debug build settings, and include
the app's local diagnostic controls. The helper verifies the installed debugging
flag. Set `MACRION_DEBUG_MODE=debug` or `performance` in your shell to change the
local default; an explicit `--mode` wins. Direct Gradle builds default to debug;
`-PmacrionDebugMode=performance` selects the other mode.

| Capability over wireless ADB | Debug (default) | Performance |
| --- | --- | --- |
| Logcat, dumpsys, gfxinfo, meminfo | Yes | Yes |
| Perfetto system/UI/Compose tracing | Yes | Yes |
| Shell CPU profiling and native heap sampling | Yes | Yes, with device/tool support |
| In-app diagnostic reports and crash-testing controls | Yes | Yes |
| Debugger/JVMTI attachment, breakpoints, `run-as` | Yes | No |
| Debugger-based Java/Kotlin allocation recording and heap dumps | Yes | No |
| Verified ART `speed` compilation on Android 14 | No | Yes |

The debug-only manifest declares `<profileable android:shell="true"/>`, and a
debug-only Compose runtime-tracing dependency enables composable slices. No
profiling declaration or tracing dependency is added to release builds. Profiling
has overhead while recording; compare performance with tracing disabled. Shell
profiling does not grant private-data access. See Android's
[profileable documentation](https://developer.android.com/guide/topics/manifest/profileable-element)
and [profiling limitations](https://developer.android.com/studio/profile).

## Phone connection

The default device is `192.168.1.170:5555`; override it with `--device HOST:PORT`.
The first connection check runs alongside the build. If unreachable, a one-second
alert plays, another plays after about 10 seconds, and recovery ends after
35 seconds. Connection attempts have short timeouts. Successful recovery is
silent, even with `--verbose`. The phone is checked again before installation
and ART compilation. A transport interruption can retry the affected operation
once; package-manager errors fail immediately. A timeout during the initial
check cancels the active build client. No daemon-wide stop/kill is used.

## Output and failures

Normal output contains stage messages and a final APK/log path. Complete build
output is retained under `.gradle/deploy-debug/`; failures print raw failing
command output and exit non-zero. `--verbose` streams Gradle output. Transient
ADB failures and audio-player output are never streamed. Installation and device
compilation each have a 180-second command timeout. Ctrl-C cancels the operation.
An unavailable audio server can prevent the alert from being heard; it does not
prevent connection retries or turn a successful deployment into a failure.

## Focused tests

No test suite runs by default. For logic changes, pass the relevant test task
and a class or method filter, repeating `--test` as needed:

```sh
scripts/deploy-debug.py \
  --test :core:smart:processing:testFDroidDebugUnitTest '*ExecutionLimiterTimingTests'
```

To run unit tests without compiling the APK or connecting to the phone, use `--test-only` (or `--mode test-only`):

```sh
scripts/deploy-debug.py --test-only \
  --test :core:common:settings:testFDroidDebugUnitTest '*ScenarioSortItemTest'
```

Tests and assembly share one Gradle invocation unless `--test-only` is passed. Deployment requires the whole
invocation to succeed. A test task uses one worker JVM; empty test discovery or
unmatched filters fail. Different selected modules can still run in parallel
within the Gradle worker limit. Do not substitute broad suites for selecting
relevant tests. Cosmetic changes normally need the build and a focused phone
check. Backend tests remain separate because that backend uses Node.js.

## Daemons and performance

The wrapper version, canonical JDK 25 path, existing Gradle user home, and stable
JVM arguments allow compatible daemons to stay warm. A lock in the Gradle user
home serializes helper runs across worktrees. A busy Gradle daemon for the same
wrapper version causes an early failure rather than spawning another one.
Manual Gradle commands do not acquire this lock: do not launch them during a
deployment. The helper does not delete caches, retry failed builds, stop all
daemons, or kill other projects' processes.

One compatible Gradle daemon is the target, not a guaranteed global process
count. Gradle legitimately replaces expired/crashed daemons or starts another
for incompatible JVM settings. Kotlin daemon fallback is disabled in both the
root and included build, so compiler-daemon failures fail the build. Both use
the same 2 GiB heap setting. Distinct compiler versions still need separate
Kotlin daemons: currently build logic uses Gradle's embedded Kotlin 2.4.0 while
the Android project uses 2.4.10. The Node.js backend has no Kotlin daemon.

The default worker limit remains six; `--workers 4` is available for reduced
concurrency. Build/configuration caches and ARM64-only output remain enabled.
Java bytecode still targets 21. Native debug settings, Kotlin line information,
and Compose trace markers remain available in both modes.

Compose source information defaults to off for all Compose modules (the compiler
extension is module-wide, including release compilations). Use `--source-info`
or `-PmacrionIncludeComposeSourceInfo=true` for diagnostic builds. Device ART
compilation is enabled only in performance mode; `--skip-precompile` is a
diagnostic escape hatch.
The helper verifies the actual ARM64 compiler filter after the command; a
`Success` response that leaves the filter at `verify` fails rather than claiming
optimization. Android 14 forces debuggable apps to the safe filter, so standard
debuggable APKs cannot satisfy this check. Debug mode therefore skips AOT.
See Android's
[Dexopter implementation](https://android.googlesource.com/platform/art/+/refs/heads/android14-release/libartservice/service/java/com/android/server/art/Dexopter.java).
ART compilation trades device compilation time and disk space for reduced
interpreter/JIT warm-up work; it is not native C++ compilation or release-mode
optimization. Android may later change compiled artifacts through its own
maintenance. No global Android compilation settings are modified.
