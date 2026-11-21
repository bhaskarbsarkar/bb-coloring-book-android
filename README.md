# BB Coloring Book (Android)

Coloring-book app for tablets, built for stylus use (tested target: Xiaomi Pad 6 + Xiaomi Smart Pen).

## Features
- Import any picture (gallery/files, or "Share → BB Coloring Book").
- Fill tool: tap a region to fill it.
- Brush: pressure-sensitive stylus strokes.
- "Inside lines": strokes stay inside the region where they start.
- Pen-only mode (auto on when a stylus is detected): pen draws, fingers pan/pinch-zoom, palm is ignored.
- Stylus side button or eraser tip = eraser. Hover shows a brush cursor.
- Any color: HSV picker + hex input; recent colors remembered.
- My Drawings: multiple saved drawings, each resumable later (thumbnails, rename, delete).
  Progress autosaves a few seconds after each change and when leaving the app; "Save" saves now.
- Backup guardrail: ⚠ Back up button (red when changes aren't backed up) saves a restorable .zip to
  Download/BB Coloring Book, exports all drawings as PNG, or restores a backup. One-time warning that
  uninstalling deletes drawings; Android's uninstall dialog offers "Keep app data" (hasFragileUserData).
- Eyedropper, undo/redo, clear, export PNG to Pictures/BB Coloring Book.

## Build
```
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest   # unit + Robolectric UI tests
```
Requires Android SDK (platform 35). minSdk 29.
