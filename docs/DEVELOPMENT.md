# Development

## 4.0 UI architecture

`DesignSystem.kt` owns the semantic palette, bundled Heebo typography and custom pressable/segmented controls. `NowScreen.kt`, `FocusScreen.kt`, `JourneyScreens.kt` and `RecoveryMoment.kt` implement the new primary experiences. Storage schemas remain unchanged. See [visual specification](V4_VISUAL_SPEC.md) and [device verification](V4_QA_REPORT.md).

`NeoDeviceTest` covers recovery timer exclusion and Now waiting blockers. Progress tests scroll to history entries by stable event key because the seven-day chart precedes the history.

## Structure

- `Data.kt`: entities, additive Room migrations, repository and preferences.
- `MainActivity.kt`: language, theme and navigation, including widget capture intents.
- `MomentumWidget.kt`: native RemoteViews widget; observes local changes while running and supports refresh/periodic updates.
- `ResumePolicy.kt` / `ResumeCard.kt`: most-recent unfinished activity with real progress and next-step context.
- `DevelopmentScreen.kt` / `SuggestionImages.kt`: local backlog, categories/priority, bounded screenshots, deliberate GitHub draft preparation.
- `Backup.kt`: schema 3 JSON with schema 1/2 reading. Validation happens before replacement writes.
- `NowPolicy.kt` / `Engine.kt`: deterministic recommendations.
- `Reminders.kt` / `ProjectReminder.kt`: scheduling, permission handling, limits and quiet hours.

## Test isolation

Build with `-PmomentumQa=true` to use `com.momentum.app.qa`, the Momentum QA label and purple icon. Install the debug APK and AndroidTest APK from `app/build/outputs/apk/`, then run:

```sh
adb shell am instrument -w -e class com.momentum.app.Momentum32DeviceTest,com.momentum.app.DevelopmentDeviceTest,com.momentum.app.DataDeviceTest com.momentum.app.qa.test/androidx.test.runner.AndroidJUnitRunner
```

Fixtures may replace synthetic QA records. Never run them against a personal package. Reminder tests require notification permission; DeniedPermissionTest requires it revoked. Restore any system font/motion settings changed during tests.

## Signing

Build `assembleRelease` for an unsigned, minified and resource-shrunk APK. `scripts/sign-release.sh INPUT OUTPUT` signs it using externally provided paths: `APKSIGNER`, `MOMENTUM_KEYSTORE`, `MOMENTUM_PASSWORD_FILE`. To retain an existing key lineage, also provide `MOMENTUM_LINEAGE`, `MOMENTUM_LEGACY_KEYSTORE`, and `MOMENTUM_LEGACY_PASSWORD_FILE`. The helper assumes each keystore has one signing key with the same key/store password.

The published release rotates from its original debug signer to a private release signer using APK Signature Scheme v3 with a rotation minimum API of 28. The app minimum API is 29. Follow the [Android apksigner documentation](https://developer.android.com/tools/apksigner) when extending the lineage. Verify the old fingerprint against the installed APK before creating a lineage; Android SDK configuration can change the debug keystore location.

Keep release keys, password files and the correct signing lineage outside Git with restricted permissions. Maintain a secure independent backup: losing the release key prevents future compatible updates. Do not replace these keys between releases. Rollback to old-signature builds is disabled in the published lineage.

Published release certificate SHA-256: `ca17ea6d36bf17afbbff3df179e22539d51688c768de2f4f22255c72caf5a4c2`.

## Publication hygiene

The public tree excludes private data, device recovery files, internal preservation tests, host-specific scripts, credentials and signing files. Screenshots use synthetic fixtures. Public APKs come from build outputs, never from a device data directory.

No source-code license grant is added by this update. Third-party components retain their own licenses.

## 4.1 widgets and pinned assignments

`CaptureWidget.kt` provides a 1×1 RemoteViews capture action; existing capture intents preserve the keyboard-first flow. `FocusNotification.kt` stores a single explicit session pin outside Room, serializes changes, and uses a distinct notification tag/channel. An action token prevents old notification actions from removing a replacement pin. Task/project observers, app launch and rescheduling broadcasts reconcile state. Completion, waiting, postponement, deletion and project pause remove stale pins. Permission and channel settings are honored.

`Momentum41DeviceTest`, `FocusPinDeniedTest` and `FocusPinRestoreTest` cover capture, private/silent notifications, replacement, dismissal, state changes, denied permission and restoration. Notification/gallery renders use app-owned synthetic content rather than capturing the personal notification shade. Launcher placement remains an Android confirmation.

Android references: [widget size metadata](https://developer.android.com/develop/ui/views/appwidgets/layouts), [Android 14 ongoing notification dismissal](https://developer.android.com/about/versions/14/behavior-changes-all).
