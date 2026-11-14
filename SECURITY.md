# Security policy

Report a security problem privately through GitHub: open the repository's **Security** tab and choose **Report a vulnerability**. Please don't open a public issue for it.

Things we treat as security bugs:

- The phone app sending anything to the watch beyond what the watch shows: calendar entries outside the chosen calendars, private event titles when "Hide private event titles" is on, or message content (only counts are sent).
- Any of the apps sending data off the phone or the watch. They have no internet permission.
- Health data (heart rate, steps) leaving the watch.
- A release APK whose SHA-256 doesn't match `SHA256SUMS.txt` on its release page, or that isn't signed with the Sable Watch release key.
