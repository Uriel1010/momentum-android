# Momentum 4.6 — starter-task stack

The opening card is now a finite stack of up to five distinct eligible tasks. The latest eligible resume task is first, followed by ranked suggestions. Layered cards indicate more choices; a task-position label and Previous/Next buttons complement horizontal swiping. Swipe direction follows layout direction. Single-task and empty states retain their direct action. Suggestions-off preferences are respected.

Browsing changes selection only: it does not start, complete or postpone tasks. Explicit Not now still postpones. If the visible task becomes ineligible, selection safely falls back to the first remaining task. Existing reduced-motion preferences disable the card transition.

46 JVM tests and 17 physical Android 16 tests passed: two new stack tests, four home journeys, six prior resume/widget/suggestions tests and five capture/notification tests. New tests exercise next/previous, swipe, starting the visible card, no browsing mutations, and removing the selected task. The native screenshot uses synthetic English content and was visually reviewed. RTL swipe logic follows layout direction but was not separately exercised on hardware in this release. Release lint has zero errors.

The signed update installed in place, retained its UID, matched the APK checksum, and cold-launched. No schema or data clearing changes. Temporary QA apps were removed.
