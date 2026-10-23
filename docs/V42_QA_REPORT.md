# Momentum 4.2 verification

43 JVM tests passed, including unfinished-task filtering and deterministic widget ordering. Fourteen physical Android 16 device tests passed: three new collection-widget tests, five capture/notification regressions, and six prior widget/suggestions/resume regressions. Release lint has zero errors.

New tests cover both registered target sizes, resize flags, preview layout/image metadata and drawable loading, real AppWidgetHostView rendering at representative dimensions, list scrolling, row click dispatch, capture focus, empty and overflow counts, completed-task removal, and the legacy factory contract. Images use synthetic English content from native widget renders. A physical Android 10/11 launcher, every launcher's picker presentation, and manual accessibility review were not tested.

The signed main release installed in place with the same UID, matching APK hash and successful cold launch. Room schema and backup implementation are unchanged; no main-app data was cleared. Temporary QA packages were removed.

Both presets share a resizable scrollable list. Up to 100 unfinished records plus an overflow link are rendered; waiting, future and paused-project tasks remain visible. Rows open task details without completing or starting tasks. Android 12+ uses RemoteCollectionItems; Android 10–11 uses a permission-protected RemoteViewsService. Changes refresh while the app process is running, with Android's periodic widget refresh as fallback.
