# Momentum 4.4.1 — task picker contrast

The task-state sheet previously placed a default ListItem on a selected secondaryContainer surface. ListItem default text colors ignore the parent surface's content color; in dark mode this yielded pale text on lavender. Headline, supporting text and checkmark now explicitly use onSecondaryContainer for selected rows, and onSurface/onSurfaceVariant for unselected rows. The pairing also follows dynamic color schemes.

Two physical-device test executions passed, one in each theme. They check the brand palette's selected/unselected text pairings meet 4.5:1 contrast, capture the native state list with synthetic English content, and verify another state can be selected. Both captures were visually reviewed. Dynamic wallpaper palettes were not individually tested. Release lint has zero errors. The 4.4 report covers the compact layout regressions.

The signed release installed in place with the same UID, matching APK checksum and successful cold launch. No data was cleared; QA packages were removed.
