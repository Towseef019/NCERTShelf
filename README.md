# NCERT Shelf (Android)

Class 11 & 12 NCERT Physics, Chemistry, Zoology and Botany, fully offline.
The app is small; the books live inside the app's private storage after a one-time import.

## Build the APK
**Android Studio:** File > Open > this folder > wait for sync > Build > Build APK(s).
APK: `app/build/outputs/apk/debug/app-debug.apk`

**No PC (GitHub):** push this folder to a GitHub repo > Actions > "Build APK" > Run workflow >
download the `NCERTShelf-debug-apk` artifact.

## Get the books into the app (pick one)
1. **Import on the phone (recommended):** install the APK, open it, tap *Choose folder* and pick the
   folder holding the PDFs (your `ncert` or `ncert/pdfs` folder). They are copied once into the app.
   Later: *Import PDFs* from the menu.
2. **Bundle inside the APK:** put all PDFs into `app/src/main/assets/pdfs/` before building.
   The app copies them to its storage on first launch. (APK will be ~330 MB.)

Missing from your Drive copy: `keph1ps.pdf` (Class 11 Physics Part I contents). Add it to either place.

## Notes
- Chapters open in your phone's PDF app (zoom/search). If none is installed, a built-in reader opens.
- NCERT books are copyrighted. Keep this app for personal study; do not publish it with the PDFs inside.
