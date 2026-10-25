# Momentum 4.7 validation

All six development suggestions are implemented: save and pin with immediate task details, a one-hour preset, fixed-schedule and after-completion recurrence, compact Home and task rows, and personal/project task scopes with project emoji labels.

50 JVM tests and 25 physical-device tests passed. Coverage includes monthly boundaries and daylight saving, recurrence completion/undo/redo, migration from database version 4 to 5, backup round trips and invalid input, save-and-pin notification posting, immediate details and duration editing, project filtering, card-stack behavior, Home journeys, completed/archive restoration, and picker contrast. Synthetic English screenshots of Home, Tasks and task details were reviewed. Release lint passed.

Recurring tasks create a single next occurrence when completed. Fixed schedules skip missed dates and retain their original monthly anchor. After-completion intervals begin at completion. Undo removes an untouched generated occurrence; an edited or started occurrence is preserved. Backups export schema 4 and import schemas 1–4. Existing tasks migrate with recurrence disabled and projects with an empty emoji.

The signed main app was upgraded in place with the same application UID and matching APK checksum, then cold-launched successfully. No app data was cleared. Temporary QA packages were removed. This is targeted verification, not exhaustive device or accessibility coverage.
