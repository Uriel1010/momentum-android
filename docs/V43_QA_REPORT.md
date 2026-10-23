# Momentum 4.3 — clearer task states

To do combines the former Inbox and Ready labels without migrating or rewriting existing records. In progress, Waiting, Completed and Archived retain their stored meanings. Unfinished contains To do, In progress and Waiting; Scheduled is explicitly described as a timing filter. Tasks holds all records, Projects groups related tasks, and Now suggests a next action.

The editor exposes status above Advanced. Completed and archived list rows and editors offer direct restore actions. Restoring sets Ready, clears completion and the deferred-start time, retains task details and reminder settings, and uses the existing repository operation to correct completion events/counts. No database or backup schema changed.

46 JVM tests and 19 physical Android 16 device tests passed. New device tests reopen a completed task from its list, verify its details and corrected completion event, and restore an archived task from the editor without opening Advanced. Regression suites cover storage, to-do widgets, capture and pinned notifications. Release lint has zero errors. The signed update installed in place with the same UID, matching APK checksum, and successful cold launch; temporary QA packages were removed.

Broader launcher coverage and manual accessibility review remain outstanding.
