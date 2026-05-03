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

You should see a screen titled **My Apps** with two mock entries (Notes, Focus Timer). The buttons won't actually install anything yet because the URLs are fake — that's expected. The next step is wiring up real publishing.

## What's implemented

- ✅ Material 3 UI with light / dark / Monokai / wallpaper-tinted themes
- ✅ App list, app detail, settings screens
- ✅ Manifest fetching (Ktor) with kotlinx-serialization
- ✅ APK download with progress + SHA-256 verification
- ✅ System installer hand-off via FileProvider intent
- ✅ Compose Navigation, edge-to-edge, splash screen
- ✅ Per-package install state computed from `PackageManager`
- ✅ Mock manifest baked in so the UI works before any real apps exist

## What's *not* yet built (intentional next steps)

- ⛔ The GitHub Actions release pipeline (see `docs/GITHUB_ACTIONS.md`)
- ⛔ A real `manifest.json` host (see `docs/MANIFEST_SPEC.md`)
- ⛔ Background update checks (WorkManager) — the dependency is wired, the worker isn't yet
- ⛔ Actual app icons (placeholder vector for now)

## Going live (replace mock data with the real thing)

Once you've generated your keystore and pushed your first signed APK to a public manifest:

1. Open `app/build.gradle.kts`
2. Change:
   ```kotlin
   buildConfigField("boolean", "USE_MOCK_MANIFEST", "true")
   buildConfigField("String", "MANIFEST_URL", "\"https://…\"")
   ```
   to point at your real manifest URL and set the flag to `false`.
3. Or use the in-app **Settings → Manifest URL** field to override at runtime.

## Project layout

```
app/src/main/java/dev/matejgroombridge/store/
├── MainActivity.kt              ← single-Activity host + nav graph
├── StoreApplication.kt          ← Application class
├── data/
│   ├── model/                   ← Serializable manifest + state types
│   ├── network/                 ← Shared Ktor client
│   ├── repository/              ← Manifest (real + mock) + installed apps
│   └── settings/                ← DataStore-backed prefs
├── install/                     ← Download, verify, install flow
└── ui/
    ├── components/              ← Reusable bits (AppRow, ActionButton)
    ├── screens/                 ← AppListScreen / AppDetailScreen / SettingsScreen
    ├── theme/                   ← Color / Type / Theme (Monokai + Material You)
    └── StoreViewModel.kt
```

See `docs/` for keystore setup, GitHub Actions, and the manifest format.
