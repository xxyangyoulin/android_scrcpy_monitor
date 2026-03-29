# Scrcpy Monitor

Scrcpy Monitor is a small Android app for rooted devices that monitors `scrcpy` sessions on-device, keeps a foreground notification alive, and lets you manage a few connection-related actions directly from the phone.

## Features

- Detects active `scrcpy` sessions on-device
- Shows a persistent foreground notification
- Exposes a one-tap disconnect action in the notification
- Restores monitoring after boot and after app updates
- Lets you view and toggle Wi-Fi debugging from the app
- Shows and edits the Wi-Fi debugging port
- Optionally disables Android animations while connected to reduce perceived latency
- Provides a Compose-based home screen optimized for quick status checks

## Requirements

- Android 8.0 or newer
- Root access with a working `su` binary
- `scrcpy` used through ADB on the same device

## How It Works

The app runs a foreground service so Android keeps the process alive in the background. It detects `scrcpy` by inspecting root-visible process and socket state, then updates a single ongoing notification. When a session is active, the notification exposes a disconnect action that restarts `adbd` without turning off developer options globally.

To reduce battery use, the monitor polls less frequently while idle, speeds up once a `scrcpy` session is active, and refreshes the home screen more aggressively while the app is in the foreground.

## Build

```bash
./gradlew assembleDebug
```

The debug APK will be generated at:

`app/build/outputs/apk/debug/app-debug.apk`

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Localization

- Default resources live in `app/src/main/res/values/strings.xml`
- Simplified Chinese resources live in `app/src/main/res/values-zh-rCN/strings.xml`

## Project Structure

- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/MainActivity.kt`: activity and UI state mapping
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/MainScreen.kt`: Compose home screen
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/ScrcpyMonitorService.kt`: foreground monitor service
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/ScrcpyStateDetector.kt`: scrcpy detection
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/RootDisconnect.kt`: disconnect command execution
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/WifiDebuggingManager.kt`: Wi-Fi debugging status, port detection, and toggle
- `app/src/main/java/com/xxyangyoulin/scrcpymonitor/MonitorSettings.kt`: persisted app settings

## Notes

- The app is designed for rooted devices. Without root, session detection and disconnect behavior are limited by Android's permission model.
