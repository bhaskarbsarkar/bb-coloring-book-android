# BB Coloring Book (Android)

Coloring-book app for Android tablets and phones, built for stylus use (Xiaomi Pad 6 + Xiaomi Smart Pen, Samsung S Pen, any Android active stylus).

## Features
- Import any picture (gallery/files, or "Share → BB Coloring Book").
- Fill tool: tap a region to fill it.
- Brush: pressure-sensitive stylus strokes.
- "No spill": brush strokes stay inside the shape where they start, even if the pen slips over a line.
- Pen-only mode (auto on when a stylus is detected): pen draws, fingers pan/pinch-zoom, palm is ignored.
- Stylus side button or eraser tip = eraser. Hover shows a brush cursor.
- Any color: HSV picker + hex input; recent colors remembered.
- My Drawings: multiple saved drawings, each resumable later (thumbnails, rename, delete).
  Progress autosaves a few seconds after each change and when leaving the app; "Save" saves now.
- Backup: a short note in My Drawings says drawings live only on this device, with "Back up"
  (restorable .zip in Download/BB Coloring Book, or all drawings as PNG) and "Restore" links.
  Android's uninstall dialog may also offer "Keep app data" (hasFragileUserData).
- Eyedropper, undo/redo, clear, export PNG to Pictures/BB Coloring Book.

## Build
```
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest   # unit + Robolectric UI tests
```
Requires Android SDK (platform 35). minSdk 29.
