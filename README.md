# ✦ Glintbox

A beautiful, private, offline status keeper for Android — built with Kotlin + Jetpack Compose (Material 3).

- **Package / applicationId:** `com.glintbox.app` (debug builds get `.debug` suffix)
- **minSdk** 26 · **target/compileSdk** 37 · AGP 9.3.1 · Kotlin 2.4.20 · Gradle 9.7.1 (same toolchain as your other projects)

## Features

- Statuses tab: images / videos / all filters, per-source switcher (standard + Business), stats hero card, "save all new", pull-to-refresh
- One-tap save on every tile, long-press multi-select → bulk save / share
- Full-screen viewer: swipe pager, pinch & double-tap zoom, ExoPlayer video, GIF/animated WebP, "~Xh left" expiry chip, Save / Share / Repost
- Saved gallery with filters, multi-select share/delete (with optional confirmation)
- **Default save path `Internal storage/WhatsApp Statuses`** + **choose any folder** (phone or SD card) + reset to default
- Optional background **auto-save** (WorkManager, ~30 min, battery/storage-aware, images-only option)
- 6 handcrafted palettes (Aurora, Ember, Lagoon, Orchid, Jade, Midas), light/dark/system, pure-black AMOLED, Material You
- Animated aurora backdrop, glass cards, floating pill nav bar, haptics, splash screen, themed icon, edge-to-edge, predictive back
- Guided onboarding with a privacy/ownership notice, in-app About + Privacy policy
- No internet permission, no ads, no analytics

## How storage works (and why — Play policy)

| Android | Reading statuses | Saving to `Internal storage/WhatsApp Statuses` |
|---|---|---|
| 8 – 10 | `READ_EXTERNAL_STORAGE` (capped at API 29) | Direct write, `WRITE_EXTERNAL_STORAGE` (capped at API 29) |
| 11 + | User grants the `.Statuses` folder once via the system picker (opens right inside it) | Android forbids apps from creating top-level folders without *All files access*, which Play doesn't allow for this category. So the app walks the user through creating/approving the folder once in the system picker. Until then it saves to `Download/WhatsApp Statuses` via MediaStore, so saving always works. |

Custom folders on any version use the Storage Access Framework with persisted permissions.

## Project layout

```
app/src/main/java/com/glintbox/app/
├── GlintboxApp.kt            Application, Coil image loader (video frames + GIF), DI container
├── MainActivity.kt           Splash, edge-to-edge, theme, nav host
├── data/
│   ├── model/Models.kt       StatusMedia, WaSource, AppSettings, SaveTarget …
│   ├── SettingsRepository.kt DataStore preferences
│   ├── StatusRepository.kt   Reads statuses (File API ≤ 10, SAF ≥ 11)
│   ├── SaveRepository.kt     Save / list / delete (SAF tree, legacy folder, MediaStore fallback)
│   └── storage/              SAF + MIME helpers
├── work/AutoSaveWorker.kt    Optional periodic auto-save
├── util/                     Share/rate intents, formatters
└── ui/
    ├── MainViewModel.kt
    ├── theme/                Palettes, color schemes, typography
    ├── components/           Aurora background, glass cards, gradient buttons, status tile
    ├── common/FolderAccess   Guided folder-picker flows
    ├── navigation/           Type-safe routes + NavHost
    ├── onboarding/ home/ viewer/ about/
```

## Release signing

1. Android Studio → *Build → Generate Signed App Bundle* → create a new keystore (keep it safe and backed up!).
2. Create `keystore.properties` in the project root (already git-ignored via `*.jks` — also add this file to `.gitignore` if you commit):

```
storeFile=C:/keys/glintbox-release.jks
storePassword=********
keyAlias=glintbox
keyPassword=********
```

3. `Build → Generate Signed App Bundle` (or `gradlew bundleRelease`) and upload the `.aab`. Enrol in Play App Signing.

## Roadmap

### v1.1 — Save by person
Status files carry no sender info (WhatsApp keeps that in its private DB), so the user names the person themselves:
- **"Save to…" option** (long-press Save, or a toggle in Settings for "Always ask"): a sheet asks for a name. It offers recent names, free-text input and the system contact picker (`ACTION_PICK`, so no `READ_CONTACTS` permission and no Data-safety change).
- The file goes to **`<save folder>/<Name>/`**, e.g. `Internal storage/WhatsApp Statuses/Rahul/`. The subfolder is created through SAF `createDocument(MIME_TYPE_DIR)`, through `File.mkdirs()` on Android 8–10, or through `RELATIVE_PATH` in the MediaStore fallback. Names are sanitised (strip `/\:*?"<>|`, trim, max 64 chars).
- The **Saved tab gets a "People" row** of chips (e.g. *Rahul (4)*) built from the subfolders. Listing moves from one level to two, and plain saves stay in the root folder.
- The viewer gets **"Move to person…"** for items that are already saved.
- Names are stored only on the device, in DataStore (recent-names list).

## Before publishing

- Replace `support@glintbox.app` in `app/src/main/res/values/strings.xml` and `PRIVACY_POLICY.md`.
- Host `PRIVACY_POLICY.md` and paste its URL into Play Console.
- Fill Play Console using `PLAY_STORE_LISTING.md` (listing text, Data safety = "no data collected", content rating, target audience 13+).
- Bump `versionCode` / `versionName` in `app/build.gradle.kts` for each release.
