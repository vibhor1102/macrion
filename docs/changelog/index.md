---
title: Changelog
description: Comprehensive release notes and version history for Macrion.
aside: false
prev: false
next: false
---

# Changelog

All notable changes to the **Macrion** project are documented on this page. Each version reflects a published release on GitHub.

---

## Macrion 0.4.1 {#v0-4-1}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.4.1" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


### Bug Fixes & Refinements

- Fixed accessibility overlays using an invalid context on Android 11 and 12.
- Fixed color-capture overlays entering an invalid state.

---

## Macrion 0.4.0 {#v0-4-0}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.4.0" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


### Documentation

- Macrion now has a comprehensive documentation site covering scenarios, conditions, triggers, actions, overlays, settings, diagnostics, backups, and migration: [Macrion Docs](https://vibhor1102.github.io/macrion/).
- The site includes a new purpose-built landing page for a clearer introduction to Macrion.
- Much of this initial documentation backlog was AI-generated; it establishes broad coverage that future feature work can maintain and improve with more focused, higher-quality pages.

### Vietnamese Localization

- Added and completed Vietnamese translations across the app, including tutorials, settings, permissions, notifications, scenario editing, backups, and debugging.

### Bug Fixes & Refinements

- Removed unused Play Store and Firebase-related code as project cleanup; these components were not included in prior F-Droid builds and were no longer needed.
- Fixed overlay appearance and theming across displays and rotations.
- Prevented crashes when an overlay view has already been detached.
- Added a safe fallback when Android cannot create Macrion’s custom service notification.
- Made preference-storage failures recover gracefully instead of crashing the app.
- Kept the running-scenario toolbar below system bars.
- Fixed the empty-scenario screen’s colors in dark and dynamic themes.
- Clarified tutorial terminology by consistently using “Wait.”
- Corrected the opening flow of the Spanish tutorial.

---

## Macrion 0.3.0 {#v0-3-0}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.3.0" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


Macrion 0.3.0 brings a major architectural migration to Jetpack Compose across the entire interface, privacy-focused FOSS crash reporting, and numerous refinements and stability fixes.

### Jetpack Compose UI Migration

- **Modern Architecture with Visual Parity**: Migrated nearly all screens, editors, dialogs, overlays, and tutorials from legacy Android Views to Jetpack Compose. This overhaul focuses on adopting a modern UI architecture while making every effort to preserve visual parity and familiarity.
- **Foundation for Future Updates**: Transitioning to Compose lays the groundwork for upcoming features and makes implementing future interface improvements significantly easier.
- **Migration Notice**: Because this was an extensive overhaul across nearly the entire application, minor visual bugs or edge cases may have slipped through. If you spot any issues, please report them so they can be addressed promptly.

### FOSS Crash Reporting & Privacy

- **Fully FOSS & F-Droid Compliant**: Proprietary crash libraries have been completely removed in favor of fully open-source (FOSS) solutions, preparing Macrion for and ensuring strict compliance with F-Droid policies.
- **Zero Automatic Transmission**: Absolutely no diagnostic or usage data is transmitted automatically in the background.
- **Privacy-First Diagnostics**: If a crash ever occurs, an option is presented on the next startup allowing you to review and send diagnostic details. Extensive steps have been taken to sanitize and scrub reports so that they contain almost no personally identifiable information (PII).

[Learn more about Diagnostics & Privacy](/documentation/settings/diagnostics)

### Bug Fixes & Refinements

- Added an optional confirmation prompt setting before stopping a running scenario.
- Improved scenario switcher and home navigation, including a dedicated home button and automatically hiding toolbars during switching.
- Fixed a crash when adjusting scenario resolution controls.
- Fixed a crash caused by duplicate or malformed Intent extra list keys.
- Fixed a crash when a tutorial success dialog detached during execution.
- Fixed an R8 minification rule failure that prevented building release APKs.
- Fixed bitmap cache retention to release memory after restoring screen recorder dimensions.
- Fixed Locale plug-in icons not rendering properly in automation action pickers.
- Fixed input fields not defocusing properly when closing overlay dialogs.
- Added direct in-app links to the Discord community, GitHub repository, and bug reporting.

---

## Macrion 0.2.1 {#v0-2-1}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.2.1" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


This maintenance release resolves an x86 crash during text detection and a startup issue in tutorials.

### Bug Fixes

- Fixed a possible crash on x86 devices and Android emulators while using text detection conditions.
- Fixed a startup deadlock in the Still Target screen condition tutorial that prevented the tutorial from progressing.

---

## Macrion 0.2.0 {#v0-2-0}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.2.0" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


This release introduces touch pass-through for the live Debug panel alongside targeted stability and interface fixes.

### Live Debug Panel Touch Pass-Through

The live Debug panel now allows interaction with the screen underneath while running:

- **Pass-Through Taps & Gestures**: Both user-initiated touches and Macrion-initiated automation clicks now pass directly through the debug overlay to the underlying app without obstruction.
- **Unified Controls**: The toolbar buttons remain fully interactive, and the toolbar and debug feed move together as one seamless overlay.
- **Platform Support**: Supported on Android 13 and newer via Android's native surface touch-region API. On Android 12 and older, the panel remains visible and interactive using the standard window capture model.

[Learn more about the Live Debug Panel](/documentation/advanced/debug-panel)

### Bug Fixes

- Fixed floating overlay positions along the top or left screen edges not being restored correctly.
- Cleaned up remaining legacy branding references across multiple translated languages.

---

## Macrion 0.1.0 {#v0-1-0}

<div class="release-actions">
  <a href="https://github.com/vibhor1102/macrion/releases/tag/v0.1.0" target="_blank" rel="noopener noreferrer" class="release-tag-link">
    <svg class="release-gh-icon" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"></path></svg>
    <span>View Release on GitHub</span>
  </a>
</div>


This is the first public release of Macrion. It introduces in-session scenario switching, deep third-party automation integration, and granular performance diagnostics.

### Scenario Switcher

Quickly switch between Smart scenarios directly during an automation session without exiting back to the main app:

- **Saved Sorting Preferences**: The switcher lists scenarios using the same sort order configured on your Macrion home screen.
- **Notification Quick Action**: The ongoing service notification action now opens the Scenario Switcher by default instead of opening the home screen.
- **Overlay Availability**: The notification switcher is always active by default. If you also want a quick-switch button directly on the floating overlay toolbar, it can be enabled in **Settings**.

[Learn more about Scenario Switcher settings](/documentation/settings/overview)

### External Automation (Tasker, MacroDroid, Automate)

Macrion now integrates directly with Android automation tools—including **Tasker**, **MacroDroid**, and **Automate**—using an interoperable plugin standard (Locale Plugin protocol under the hood):

- **Scenario Controls**: Launch a specific scenario, run/resume the currently loaded scenario, or stop automation directly from external automation flows.
- **Scenario State (Status Plugin)**: Queries Macrion's live status (running, paused, stopped, and active scenario) and maps it into variables in your automation app. This allows Tasker or MacroDroid to monitor Macrion in real time and act as an overarching coordinator across multiple scenarios.
- **External Action**: A new action type for Smart scenarios when Macrion's built-in actions are not sufficient and more granular device control is needed (such as changing volume, taking screenshots, sending toast notifications, or altering system settings). Where existing Android automation apps already excel, Macrion delegates to them to fulfill the gap.
  - *Scenario Chaining Workaround*: Because Macrion currently lacks the ability to automatically switch scenarios by itself within a scenario, you can call an External Action at the end of a scenario. Your external automation app intercepts it, executes the switch, and launches the next scenario. This acts as a practical workaround until native intra-scenario switching is introduced in a future update.

[Learn more about External Triggers](/documentation/advanced/external-triggers) · [Learn more about External Actions](/documentation/actions/action-external)

### Performance Diagnostics & Debug Reports

The Debug Report now provides in-depth performance analytics to help diagnose and optimize complex scenarios:

- **Execution Wait Time (Overview Tab)**: Displays active CPU processing time saved by the Execution Limiter, providing clear visibility into reduced processor load and improved battery efficiency.
- **Condition Performance Tab**: Shows the exact evaluation time spent on each condition, allowing you to pinpoint bottlenecks and optimize resource-heavy image, color, or text checks.
- **Activity & Timeline Refinements**: Added an Event Activity summary displaying trigger counts per event, alongside detection and action timing metadata and draggable scrollbars in the timeline.

[Learn more about Debug Reports](/documentation/advanced/debug-panel)

### Backup Compatibility

Importing and exporting scenarios is fully compatible with Klick'r backup archives, and full compatibility will continue to be preserved in future releases.

[Learn more about Backups & Storage](/documentation/migration/backups)

---

## Macrion 0.0.0 {#v0-0-0}


The birth of Macrion. Based on [Klick'r](https://github.com/Nain57/Smart-AutoClicker) v4.0.1, with an ambition far beyond what was contemporarily available in Klick'r. Macrion is maintained as an independent project with its own identity moving forward.

This version was unreleased.
