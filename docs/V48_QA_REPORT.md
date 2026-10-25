# Momentum 4.8 validation

Existing task rows and task links open the full-screen task view, with notification pinning available near the top for eligible tasks. Editing remains a separate action. Pinned project tasks show their project name and emoji, including after a project rename. The lock-screen public notification remains generic.

Projects support five persistent colors. Colors appear in project icons, task indicators and project badges. Database migration 5 to 6 preserves existing project data and defaults colors to purple. Backup schema 5 supports older backups with a default color.

Removed repeated Home, project, history and task-filter explanatory text, including the empty project progress placeholder. Action labels, relevant empty states and optional help remain.

24 physical-device tests passed: existing-task navigation and pinning, notification context and refresh, project color editing, backup compatibility, migration, recurring tasks, task creation, Home navigation and completed/archive restoration. JVM tests and release lint passed. Synthetic project and task screenshots were reviewed. The main app upgraded in place with the same UID and matching APK checksum, and cold launch succeeded. No user data was cleared; temporary QA apps were removed.
