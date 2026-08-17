# Android File Manager Scaffold

Minimal Android scaffold (Kotlin + Jetpack Compose) demonstrating:

- Storage Access Framework directory picker (ACTION_OPEN_DOCUMENT_TREE)
- Persistable URI permission
- DocumentFile-based listing of the selected directory
- Simple Jetpack Compose UI with a toolbar, directory picker button, and file list

This is intentionally minimal so you can extend it with cloud connectors, transfer manager, vault, root helpers, and AV scanning later.

Prerequisites
- Android Studio Flamingo or later
- JDK 11+

Run
1. Open this project in Android Studio.
2. Build and run on an emulator or device (minSdk 21).
3. Tap "Pick directory" to choose a directory. The app will list files in the directory and let you tap to open them.

Notes
- This scaffold uses DocumentFile and the Storage Access Framework. No MANAGE_EXTERNAL_STORAGE or special permissions required.
- Package: com.example.filemanager

License
- Add your preferred license.
