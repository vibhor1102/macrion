# Crash diagnostics

## Release archives

The release job creates `compiler-diagnostics/diagnostics.zip`, uploads a 90-day Actions backup, then publishes and downloads it from `vibhor1102/macrion-assets` to verify checksums before publishing APKs. The archive contains R8 mapping/configuration/seeds/usage and a manifest with source commit, version, compiler version and file checksums. Its release tag is `r8-<mapping-id>`; conflicting files are never overwritten.

Native libraries from the release CMake output are archived in separate content-addressed ZIPs. A native-only code change can share a Java mapping ID without overwriting another native build. These libraries retain whatever symbols the release compiler emitted; the existing release configuration does not generate full source-level native debug information.

`ASSETS_REPO_TOKEN` is a fine-grained token stored in the main repository's `github-release` environment, scoped to Contents write on `macrion-assets`. No token is included in artifacts. The separate repository is public and must contain only compiler/build diagnostics, never crash reports. Credential expiration requires rotation, not work on each release.

The recovered 0.5.0 mapping has already been archived. Old published APKs are unchanged.

From `backend/crash-reports`:

```sh
RETRACE=/path/to/android-sdk/cmdline-tools/latest/bin/retrace npm run reports -- decode <report-id>
```

This reads D1 privately, downloads the archive identified by the report, verifies it, and invokes Retrace. A missing/ambiguous mapping is an error, never a guessed version. Neither the report nor decoded trace is uploaded to GitHub. Native frames provide library/build ID/relative PC for symbolication; automatic native symbolication is not performed by this command.

## Capture format v2

The server continues accepting v1. Deploy the compatible validator before distributing a v2 client. Local storage accepts both versions, preserving pending old reports. Upload remains per-report opt-in.

- The original Throwable graph, messages, suppressed exceptions and stack frames remain the source of Java/Kotlin diagnostics. Android log levels have no effect on this capture.
- Compose `GroupKeys` is enabled before UI creation. It does not enable source-information tracing, layout inspection or continuous sampling. Minified builds need their exact compiler mapping for composition frames.
- A fixed 128-slot ring records technical activity/overlay/tab/dropdown transitions and action types. Repeated identical events within one second are counted together. Ring writes perform no I/O, string formatting, stack capture or per-entry allocation. Synchronization and a monotonic clock read occur on each event.
- Up to four caught errors retain sanitized messages and 16 frames each, with no strong Throwable reference. This cost occurs on failure paths only.
- Exception messages can retain 8,192 characters; the overall report still has a 256 KiB cap. Flags indicate truncation.
- API 31+ native exits may include up to 64 frames from the crashing thread, signal/code, basename-only library names, build IDs and relative offsets. Parsing is bounded to 4 MiB of input and occurs only after a newly observed native crash. No native parser is run during normal scenario execution. Oversized/malformed traces fall back to the existing exit metadata.

Excluded: user-chosen scenario/action/condition names, captured images, recognized or typed text, action payloads, intent extras, notification content, logcat, memory/register dumps, credentials, thread names, stable user identifiers and arbitrary databases/preferences. Technical exception messages remain best-effort redacted, not guaranteed anonymous.

Tests cover v1/v2 storage and server compatibility, ring bounds/coalescing, native field filtering, malformed data and compiler archive integrity. Debug testing does not substitute for exercising diagnostic composition traces in a minified build. Device performance has not been benchmarked; the normal-use design is bounded and event-driven rather than sampled.

## Validation

Initial verification passed for archive credentials and cleanup, unsigned ARM64 release packaging, and Retrace decoding of 20 synthetic Compose group-key frames. The installed debug build passed the user's device smoke test on September 16, 2026. Synthetic decoding does not prove runtime Compose exception capture.

The one-time validation workflows and their dedicated helpers were removed after verification. Routine archive upload and verification remain inside `release.yml` before APK publication; no separate post-release workflow is required. Use `python3 scripts/diagnostics/test_archive.py` for focused archive regression tests when changing the archive tooling.

## Post-crash update suggestions

`CrashReportFlow` wraps the existing consent dialog without changing report storage or upload behavior. An independent, bounded HTTPS GET checks the detected channel while the prompt is open. Only after the prompt closes (including after a failed send) can an update dialog appear. No crash payload, installed version, ABI, device model, or identifiers are included in that request. Unknown channels, debug package IDs, malformed responses, HTTP failures, and offline checks silently skip the suggestion. There is no periodic worker or automatic APK download.

GitHub uses the public latest stable release endpoint, numeric three-part tag comparison, and a matching named ABI/universal APK asset. F-Droid uses its informational package API, requires an actual published entry at or below the suggested release, and compares Macrion's base version codes separately from ABI suffixes. These checks establish published channel availability, not a known fix for the reported crash. The lightweight F-Droid API has no per-device SDK requirements; F-Droid remains responsible for final installation compatibility.

Update checks use strict installer detection: recognized F-Droid clients or the explicit GitHub Android package. Browser, file-manager, Obtainium, ADB, and other ambiguous installations are skipped because Android does not reliably identify their download origin. The existing Settings source label retains its historical sideload heuristic. Neither installer detection nor this informational check verifies remote APK signatures.

An offered channel/version is recorded in private preferences before display, so it is not offered again after dismissal or activity recreation. Updating the app naturally suppresses notices for that version. The destinations are a fixed-host GitHub release page or the F-Droid Macrion page; the app never installs updates itself.

Focused verification, when authorized, should cover `CrashUpdateCheckerTests` and `InstallationSourceTest` via `scripts/deploy-debug.py`. UI verification needs controlled update/source fixtures because the normal debug application ID deliberately skips distribution checks. Cover send success/failure, dismissal, slow/offline responses, activity recreation, both link destinations, and suppression after an offered version. Do not induce a real crash or send a synthetic report to production merely to exercise this UI.
