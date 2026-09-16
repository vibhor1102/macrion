# Crash diagnostics

## Release archives

The release job creates `compiler-diagnostics/diagnostics.zip`, uploads a 90-day Actions backup, then publishes and downloads it from `vibhor1102/macrion-assets` to verify checksums before publishing APKs. The archive contains R8 mapping/configuration/seeds/usage and a manifest with source commit, version, compiler version and file checksums. Its release tag is `r8-<mapping-id>`; conflicting files are never overwritten.

Native libraries from the release CMake output are archived in separate content-addressed ZIPs. A native-only code change can share a Java mapping ID without overwriting another native build. These libraries retain whatever symbols the release compiler emitted; the existing release configuration does not generate full source-level native debug information.

`ASSETS_REPO_TOKEN` is a fine-grained token stored in the main repository's `github-release` environment, scoped to Contents write on `macrion-assets`. No token is included in artifacts. The separate repository is public and must contain only compiler/build diagnostics, never crash reports. Credential expiration requires rotation, not work on each release. The `Check diagnostic archive` workflow exercises create/upload/download using a temporary draft and removes it afterward.

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

## Validation workflows

`Check minified diagnostics` builds an unsigned ARM64 F-Droid release on GitHub Actions, checks that Retrace recovers 20 Macrion composable locations from the generated group-key mappings, and validates archive packaging against the real compiler output. It retains compiler artifacts for 14 days and does not publish APKs or release tags. The synthetic frames verify mapping/decoding compatibility; they do not prove that a runtime Compose failure attaches its diagnostic exception.

Both diagnostic checks are manual-only (`workflow_dispatch`), for targeted validation of credentials or diagnostic tooling. Routine release archival and upload verification run inside `release.yml` before APK publication; no separate post-release workflow is required. The installed ARM64 debug build passed the user's device smoke test on September 16, 2026.
