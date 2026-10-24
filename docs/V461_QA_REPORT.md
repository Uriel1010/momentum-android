# Momentum 4.6.1 — a loose animated card stack

The backing cards have different angles and offsets, while the front card rests at a slight tilt. During a horizontal drag it follows the finger and rotates; releasing a short drag springs back. Advancing slides the outgoing card away and brings the next card in with a spring. Previous/Next trigger the same transition. Reduced motion disables translation, front-card tilt and transitions while retaining navigation.

Seven focused physical Android 16 tests passed: three stack tests and four home journeys. Tests cover browsing without task mutations, swiping across the card face, starting the visible task, removing the selected task, short-drag recovery and reduced-motion navigation. An initial edge-to-edge test missed the tilted card face; the final test swipes within the visible card. The at-rest screenshot was visually reviewed. No manual accessibility or cross-device animation review was performed. Release lint has zero errors.

The signed update installed in place with the same UID, matching APK checksum and successful cold launch. No data was cleared; QA packages were removed. The preceding 4.6 report covers broader regressions.
