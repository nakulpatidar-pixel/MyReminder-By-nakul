# My Reminder AI — Android

A lightweight local-first reminder app prototype for Android. It includes voice-to-text capture using Android Speech Recognition, a typed reminder input, local reminders, Android notifications, a simple date selector/calendar view, and a WhatsApp follow-up view that opens WhatsApp at reminder time (it does **not** auto-send WhatsApp messages).

## Build an APK using GitHub Actions (cloud build)
1. Create a GitHub repository and upload/push this project to its `main` branch.
2. In GitHub, open **Actions** and enable workflows if prompted.
3. Open **Build My Reminder AI APK**, choose **Run workflow**, and start it.
4. When the run completes, open its run page and download the `MyReminderAI-debug-apk` artifact.
5. Extract the artifact ZIP to find `app-debug.apk`, transfer/download it to Android, tap it to install. Android may ask you to allow installation from that source.

## Android Studio build
Open this folder in Android Studio, allow Gradle sync, then choose Build > Build APK(s). The output is `app/build/outputs/apk/debug/app-debug.apk`.

## Notes / limitations
- Voice recognition uses Android's speech-recognition service and availability may depend on device settings, internet, and installed speech services.
- Hindi/Hinglish time parsing is intentionally simple. Phrases containing today/tomorrow/आज/कल and an explicit clock time are recognized. Review the generated reminder before relying on it.
- For other phrases or missing date/time, app opens date/time pickers.
- Calendar is an in-app date-filtered reminder list, not synced with Google Calendar in this version.
- WhatsApp reminders are notification prompts and can open WhatsApp. Automatic self-messaging is not supported by the official WhatsApp app integration.
- Android 13+ notifications permission and exact-alarm access may be needed; if exact alarm access is denied, Android schedules a best-effort alarm.
