# Momentum 3.2 verification

Implemented: purple Momentum QA identity; removal of QA and helper packages from the personal phone; suggestions with priority/category/screenshot and explicit GitHub draft confirmation; native next-action/capture widget; Where you left off; private release signing with verified lineage; R8/resource shrinking.

- 41 JVM tests passed.
- 52 device test executions passed, including 1.5× text flows and denied notifications.
- Release lint: 0 errors, 30 warnings.
- Optimized QA signing-rotation trial passed, with launch and capture smoke checks.
- Private on-phone preservation check passed for original rows and preferences before final release upgrade.
- Final release retained the application UID; installed SHA-256 matches; cold launch 188 ms.
- Only com.momentum.app remains on the phone.
- Main release: 3579204 bytes; non-debuggable; private release signature; no internet permission.
- Widget, return card and new suggestions screenshots inspected using synthetic QA content.

Widget placement still uses Android launcher confirmation via Settings → Add home-screen widget. Human accessibility testing, overnight Doze and broad device/launcher certification remain outstanding. The private signing key remains on the server, outside both repositories; no key material is published.
