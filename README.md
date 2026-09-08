# Macrion

Macrion is an open-source Android automation app that can react to what is visible on screen and perform actions for you. It supports image-aware scenarios as well as straightforward, position-based auto-clicking.

Macrion is derived from [Klick'r](https://github.com/Nain57/Smart-AutoClicker), created by Kevin Buzeau and its contributors. It is now developed as an independent app with its own package name, releases, and direction.

> [!WARNING]
> Macrion is currently an early release. Back up important scenarios before upgrading, and expect some inherited Klick'r wording or structure while the migration continues.

## Features

- Automate clicks, long presses, and swipes.
- Detect images and screen states before taking action.
- Build regular, position-based auto-clicking scenarios.
- Use counters, timers, flow control, Android intents, and broadcast triggers.
- Launch and control scenarios from compatible external automation apps.
- Export and import scenario backups, including a Klick'r-compatible export option.
- Generate Debug Reports with condition-performance information.

## Installation

Macrion is currently distributed through [GitHub Releases](https://github.com/vibhor1102/macrion/releases). Download the APK for your device and open it to install. For most modern Android phones, choose the `arm64-v8a` APK; choose the universal APK if you are unsure.

Android may ask you to allow installation from your browser or file manager. This permission can be disabled again after installation.

## Permissions

Macrion needs powerful Android access in order to automate other apps:

- **Accessibility:** performs configured taps, swipes, and other interactions.
- **Screen capture:** lets image-aware scenarios inspect what is displayed on screen.
- **Display over other apps:** shows Macrion's controls while another app is open.
- **Notifications:** keeps active automation visible and supports launch fallbacks.

Only grant these permissions if you trust the APK you installed. Release APKs published here are signed by the Macrion project.

## Backups and Klick'r compatibility

Before upgrading or making significant changes, use Macrion's backup screen to export your scenarios and keep the resulting file somewhere safe.

Macrion can import supported Klick'r backups. It can also create a Klick'r-compatible backup, but Macrion-only features may need to be removed from that compatibility copy; the app shows the impact before saving it. Keep your original Macrion backup as the complete copy.

## Status and feedback

The project is approaching its first public version, `0.1.0`. Bugs and compatibility reports are welcome in [GitHub Issues](https://github.com/vibhor1102/macrion/issues).

## License and attribution

Macrion is free software licensed under the [GNU General Public License v3.0](LICENSE). It retains the copyright and license notices of Klick'r and its contributors where applicable.
