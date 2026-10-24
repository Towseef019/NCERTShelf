# NCERT Shelf (Android)

Class 11 and 12 NCERT Physics, Chemistry and Biology reader for Android. The app stores imported PDF books in private app storage so they can be opened offline.

## Build the APK

1. Open this repository in Android Studio.
2. Let Gradle sync complete.
3. Build with **Build > Build APK(s)**.

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

You can also run the **Build APK** workflow under GitHub Actions. Download the `NCERTShelf-debug-apk` artifact from a successful workflow run.

## Add NCERT PDFs

Install the app, tap **Choose folder**, and select the folder containing the NCERT PDF files. The app copies PDFs into its private storage for offline use. Alternatively, place PDFs in `app/src/main/assets/pdfs/` before building to bundle them in the APK.

The repository does not include NCERT textbook PDFs. Add only files you are permitted to use and distribute. NCERT books are copyrighted; do not publish an APK containing copyrighted PDFs without permission.

## Project structure

- `app/src/main/java/app/ncertshelf/` - Android activities
- `app/src/main/assets/index.html` - chapter browser interface
- `app/src/main/res/` - Android resources
- `.github/workflows/build.yml` - automated APK build
