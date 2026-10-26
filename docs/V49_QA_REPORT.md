# Momentum 4.9 — multi-task Today verification

One silent ongoing Today notification replaces the single-assignment pin. Select multiple unfinished tasks from Today, accessible from Home and Tasks, or add a task from its details/save flow. Adding another task preserves previous selections and does not start work or change task status. Projects retain their name/emoji context.

The expanded notification previews six lines and shows an overflow count. Tapping opens the complete Today list. Completed, archived, or deleted tasks leave the list; waiting/future selections retain their state. Removing a task preserves the others. Clear Today clears selection, not task data. Selection survives process restarts and does not expire at midnight. Android can dismiss notifications; selection remains available in the app.

Legacy single-pin preferences migrate to an ordered list. Database and JSON backup schemas are unchanged. Today selection is local notification preference state, not exported task data. Generic public lock-screen content and permission/channel checks remain. Notification updates are coalesced for 400 ms to avoid Android dropping the final update during rapid selection; stale removal tokens are rejected and pending updates are cancelled on clear.

28 physical-device tests passed, including multiple pins, order/deduplication, non-starting selection, individual removal/completion, stale actions, legacy migration, nine-task overflow, cancellation during a pending update, Today selection/navigation, project context, recurrence, backup, capture, Home and restored tasks. JVM tests and release lint passed. The native Today screenshot uses synthetic data and was visually reviewed. Tests are targeted, not exhaustive launcher/Android accessibility coverage.

The signed main app installed in place with the same UID and matching APK checksum and cold-launched successfully. No user data was cleared. Temporary QA packages were removed.
