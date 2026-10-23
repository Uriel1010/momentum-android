# Momentum 4.0 verification

- 41 JVM tests passed; 63 device test executions passed across the final acceptance suite.
- Release lint: zero errors, 39 warnings.
- New recovery check verifies that resuming retains the task and excludes the break from the optional focus timer. The Now blocker check verifies waiting tasks leave recommendations.
- Existing widget, suggestions, database/backup, reminders, denied-permission, project review, and process-death tests passed.
- Hebrew RTL and English LTR: real device captures in light and dark themes. Focus intentionally remains charcoal.
- 1.5x font previews and three interaction flows passed; reduced-motion and completion/blocker/milestone preview flows passed.
- Visual review corrected RTL completion fractions, aligned statistic heights, reduced chart height to keep labels visible, restored the gentle repeated-postponement hint, and corrected Undo/Continue contrast on lavender feedback.
- Main 3.2 → 4.0 update installed in place, retained the application UID, matched the signed artifact SHA-256, and cold-launched successfully. Room schema 4, backup schema 3, Data.kt and Backup.kt are unchanged. No main-app data was cleared, exported or replaced. Private records were not re-read from the non-debuggable installation.
- Temporary QA and instrumentation packages were removed; only the main Momentum package remains on the phone.
- Main APK: 3620220 bytes; optimized, non-debuggable, private release signature, no internet permission.

Screenshots contain synthetic QA records and only app windows. Tests ran on one physical OnePlus device with Android 16. Large-font layouts scroll rather than shrinking text. Human TalkBack/switch-access review, overnight Doze testing, and broad hardware/launcher certification remain outstanding. Earlier failing checks were repaired and rerun; the count above represents the final acceptance results, not every retry.
