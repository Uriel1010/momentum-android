# Momentum 4.1 verification

- 41 JVM tests passed; 62 final device test executions passed.
- Release lint: zero errors, 46 warnings.
- 1×1 widget provider metadata verified; native view rendered at one-cell size; tapping opens the focused capture field and saves the typed thought.
- Focus pin UI posts a silent ongoing notification with private visibility and a generic public version. Content tap returns to Focus. Removing or dismissing does not complete the task.
- Replacement pins reject old action tokens. Title changes update the notification. Completion, waiting, postponement, deletion and project pause clear the pin.
- Notification permission denial does not pin or start the task.
- Force-stop/relaunch recovery restores the saved pin. This is not a real reboot test; reboot/update receivers share the reconciliation path.
- Two new feature flows passed at 1.5× text size. Existing data, backup, reminders, project review, suggestions, widget, recovery and UI tests passed.
- Native widget and notification content were rendered on-device using synthetic fixtures and reviewed. The preview host supplies the background; launcher/system placement and styling can differ.
- The initial restart-test harness had a JUnit return-type error; it was corrected and both restart stages passed. The acceptance count excludes failed attempts.
- Main app updated in place to 4.1, retaining UID, matching the release APK SHA-256, and cold-launching successfully. Room schema 4, backup schema 3, repository operations and stored entity definitions are unchanged. Only application lifecycle observers were extended. No main-app records were cleared or exported.
- QA and instrumentation packages removed after verification; only the main app remains installed.
- Main APK: 3645076 bytes; non-debuggable, optimized, privately signed, no internet permission.

The ongoing notification is explicitly enabled per assignment. Android controls notification visibility and user dismissal. No foreground service, recurring polling or screen-unlock listener is used. A pin is temporary local state and is not exported in JSON backups. Actual launcher placement needs the user confirmation provided by Android. Broad OEM/launcher coverage, real reboot testing and human TalkBack review remain outstanding.
