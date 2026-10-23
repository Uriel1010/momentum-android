# Momentum 4.4 — tasks first

The Tasks screen uses two compact rows: title/count with search and filter actions, then the current task view. Search expands on request. State selection and explanations move into a scrollable bottom sheet; project/time/context filters use a separate sheet with clear and show-results actions. Active filters have a count badge. Duplicate group headings and the separate result-count block are removed. Task status, project and estimate share one metadata line.

46 JVM tests and six focused physical Android 16 tests passed. The new layout test verifies the first task starts within 140dp of the list top, at least three rows are displayed, search narrows and resets correctly, and filters open/close. The screenshot shows five complete example tasks on the test device. Two state tests cover restoring completed/archived tasks through the new picker; three widget tests passed. Native UI capture uses synthetic English content. This is one phone size and theme, not exhaustive device or accessibility validation.

Release lint has zero errors. The main release installed in place with its UID retained, APK checksum verified, and successful cold launch. No data or schema changes; temporary QA packages were removed.
