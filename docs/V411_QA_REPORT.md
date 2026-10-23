# Momentum 4.1.1 widget preview verification

The 1×1 capture widget retains its native preview layout and now also supplies a static vector preview image. The targeted physical-device test verified the registered provider uses both intended resources, Android loads the preview image, target dimensions remain 1×1, and tapping the widget opens the focused capture field and saves a thought.

One targeted device test passed; release lint has zero errors. This patch changes preview resources and the version only; the 4.1 acceptance report covers the underlying features. The signed 4.1.1 release installed in place, retained the UID, matched its APK hash and cold-launched. No app data was cleared. Temporary QA packages were removed.

The preview metadata and drawable loading were checked on Android 16. Launcher-specific picker presentation can vary; a manual review of every launcher was not performed.
