# Momentum 4.6.2 — reveal the real card behind

Stack layers now render the actual upcoming tasks, not decorative blank rectangles. The front card moves off the stack while the next card straightens and grows into the front position. The incoming task retains its color. The palette is limited to five colors: lime, purple, blue, peach and yellow. Colors follow task positions within the current ranked stack; re-ranking after task edits can change that assignment. Background layers have no actionable start behavior and are hidden from accessibility semantics.

Seven physical Android 16 tests passed: three stack tests and four home journeys. The stack test captures the second task and verifies its purple pixels dominate lime, in addition to navigation, swipe, correct-task start, no browsing mutations, selected-task removal, short-drag recovery and reduced-motion navigation. The revealed purple card screenshot was visually reviewed. Release lint has zero errors. Prior 4.6 reports cover broader regressions; this is not exhaustive animation or accessibility validation.

The signed update installed in place with the same UID, matching APK checksum and successful cold launch. No data was cleared; QA packages were removed.
