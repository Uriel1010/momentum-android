# Privacy

Momentum does not request internet access and has no account, remote analytics or cloud backend. Tasks, projects, activity, preferences and development suggestions stay on the device. Optional local metrics can be disabled and cleared.

User-selected suggestion screenshots are copied as resized JPEGs into local storage and included in manual JSON backups. Original image metadata is discarded. Backup exports contain readable personal data; choose their destination deliberately. Automatic Android backup/device transfer is disabled.

Share development list opens Android’s chooser with text only. Prepare GitHub issue displays a public-data confirmation, then opens a browser with the chosen title and notes; the user reviews and submits the issue on GitHub. Screenshots are not uploaded automatically. The browser and receiving apps have their own privacy practices.

The home-screen widget displays a task title and next action on the home screen. Its content may be visible to someone who can see your unlocked phone. Remove the widget if you prefer to keep task text inside the app.

Notifications support reminders, vibration supports optional feedback, and boot completion reschedules reminders. Uninstalling removes private app storage; keep an exported backup if you want to restore later.

## Pinned assignment notification

Keep in notifications is an explicit, optional action in Focus. The app stores one task identifier and an action token locally to restore the chosen notification after a normal restart. This temporary pin is not included in JSON backups. A generic public notification hides task content where Android uses lock-screen redaction; the device owner controls lock-screen notification settings. Removing or dismissing the notification clears the pin without completing the task. This feature does not use a foreground service or network connection.
