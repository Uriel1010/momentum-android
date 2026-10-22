# Development

## Structure

- `Data.kt`: Room entities, additive migrations, repository operations, preferences.
- `MainActivity.kt`: theme/language and navigation.
- `DevelopmentScreen.kt`: local development suggestions, editor, filters, and share text.
- `Backup.kt`: validated JSON import/export. Validation precedes the replacement transaction.
- `NowPolicy.kt` / `Engine.kt`: deterministic local next-action selection.
- `Reminders.kt` / `ProjectReminder.kt`: reminder scheduling and limits.
- `app/schemas/`: exported Room schemas 1–3.

## Suggestions

`development_ideas` is separate from the existing `suggestions` table, which records recommendation outcomes. Each development idea has an ID, title, notes, status, and creation/update times. Allowed states are `IDEA`, `PLANNED`, `DONE`; title is required and limited to 200 characters, notes to 10,000. No suggestion content is sent automatically.

Migration 2→3 adds only this table. Migration 1→2→3 is registered. Backup schema 2 includes `developmentIdeas`; schema 1 import preserves current development ideas. Use Settings → Suggestions for Momentum → Share development list to transfer your backlog deliberately.

## Device testing

Build both QA APKs, install them with `adb install -r`, then run:

```sh
adb shell am instrument -w -e class com.momentum.app.DevelopmentDeviceTest,com.momentum.app.DataDeviceTest com.momentum.app.qa.test/androidx.test.runner.AndroidJUnitRunner
```

Use an emulator or a dedicated QA installation. Fixture tests replace synthetic data in the QA package. Never run fixture tests against a personal installation. The two APK paths are `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` after building with `-PmomentumQa=true`.

## Release hygiene

This repository starts from a clean source snapshot. Internal build briefs, machine-specific scripts, device reports, prior Git history, database files, backups, signing keys, and credentials are excluded. Screenshots use synthetic QA data and capture app content only. Release APKs are build products, not copies of an installed device’s data.

Public previews are debug-signed. A future production release needs a private release-signing setup and a deliberate migration plan for existing installs. Never commit a signing key or password.
