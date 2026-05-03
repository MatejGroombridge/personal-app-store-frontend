# Matej Store — Personal Android App Store

Your phone's home for every app you build. Polls a JSON manifest, lists your apps, downloads + verifies APKs, and hands them to Android's installer.

```
┌────────────┐  push tag    ┌──────────────────┐    upload    ┌──────────────────┐
│  Your repo │ ───────────▶ │  GitHub Actions  │ ───────────▶ │ GitHub Releases  │
└────────────┘              │  build & sign    │              │   (host APKs)    │
                            └──────────────────┘              └──────────────────┘
                                     │ rewrite                          ▲
                                     ▼                                  │ poll
                          ┌──────────────────────┐    fetch JSON   ┌────┴─────┐
                          │ manifest.json (repo) │ ──────────────▶ │ Phone    │
                          └──────────────────────┘                 │ (Store)  │
                                                                   └──────────┘
```

## Quick start (in Android Studio)

1. **File → Open** → pick the `store-app/` folder.
2. Wait for the Gradle sync to finish (first time: ~3–5 min while it downloads dependencies).
3. Plug in your phone (with USB debugging on) **or** start an emulator (Tools → Device Manager → Create).
4. Hit **Run ▶**.

You should see a screen titled **My Apps**, populated from the live manifest URL configured in `app/build.gradle.kts` (or whatever you've overridden it to in **Settings → Manifest URL**). If the manifest is empty, you'll see an empty state — that's expected before your first release lands.

## What's implemented

- ✅ Material 3 UI with light / dark / Monokai / wallpaper-tinted themes
- ✅ App list, app detail, settings screens
- ✅ Manifest fetching (Ktor) with kotlinx-serialization
- ✅ APK download with progress + SHA-256 verification
- ✅ System installer hand-off via FileProvider intent
- ✅ Compose Navigation, edge-to-edge, splash screen
- ✅ Per-package install state computed from `PackageManager`

## What's *not* yet built (intentional next steps)

- ⛔ The GitHub Actions release pipeline (see `docs/GITHUB_ACTIONS.md`)
- ⛔ A real `manifest.json` host (see `docs/MANIFEST_SPEC.md`)
- ⛔ Background update checks (WorkManager) — the dependency is wired, the worker isn't yet
- ⛔ Actual app icons (placeholder vector for now)

## Pointing at a different manifest

The default `MANIFEST_URL` lives in `app/build.gradle.kts`. To target a different host (e.g. a staging endpoint or a fork), either:

1. Edit the `MANIFEST_URL` `buildConfigField` and rebuild, or
2. Override at runtime via **Settings → Manifest URL** (handy for testing without a rebuild).

## Project layout

```
app/src/main/java/dev/matejgroombridge/store/
├── MainActivity.kt              ← single-Activity host + nav graph
├── StoreApplication.kt          ← Application class
├── data/
│   ├── model/                   ← Serializable manifest + state types
│   ├── network/                 ← Shared Ktor client
│   ├── repository/              ← Manifest fetcher + installed apps
│   └── settings/                ← DataStore-backed prefs
├── install/                     ← Download, verify, install flow
└── ui/
    ├── components/              ← Reusable bits (AppRow, ActionButton)
    ├── screens/                 ← AppListScreen / AppDetailScreen / SettingsScreen
    ├── theme/                   ← Color / Type / Theme (Monokai + Material You)
    └── StoreViewModel.kt
```

See `docs/` for keystore setup, GitHub Actions, and the manifest format.
