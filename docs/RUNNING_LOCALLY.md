# Running locally in Android Studio

A minimal walkthrough — first build, first run, what to do when things break.

## Prerequisites

- **Android Studio** Koala/Ladybug or newer
- During the Android Studio setup wizard, accept all license prompts (this installs the SDK)
- A device: either a USB-connected phone with **Developer options → USB debugging** enabled, OR an emulator created via **Tools → Device Manager → Create Device**

## Open the project

1. Android Studio → **File → Open** → select the `store-app/` folder (the one containing `settings.gradle.kts`).
2. A dialog may ask "Trust project?" → **Trust**.
3. Gradle sync runs automatically. First time it'll download Kotlin, the Android Gradle Plugin, Compose BOM, Ktor, etc. (~3–5 min, less on subsequent runs).
4. When sync finishes, the toolbar shows a Run ▶ button enabled with `app` selected and a device dropdown.

## Run

Hit Run ▶. The first build takes ~1–2 min. You should land on a screen titled **My Apps** with two mock entries.

## Try the flow

- **Tap a row** → detail screen
- **Tap "Install"** → the system will prompt you to grant "install unknown apps" permission for Matej Store. Grant it. (After granting, tap Install again.) The download will fail because the URL is fake, and you'll see "Failed". This proves the whole pipeline up to download is working.
- **Open Settings** (gear icon, top right) → flip themes, toggle "wallpaper colors". On Android 12+ the accent color updates from your wallpaper instantly.

## Common errors

| Symptom | Cause | Fix |
|---------|-------|-----|
| `SDK location not found` | Android SDK path missing | Android Studio → File → Project Structure → SDK Location, or accept the prompt to install the SDK |
| Gradle download timeout | Slow network / corporate proxy | Configure proxy in Android Studio → Appearance & Behavior → System Settings → HTTP Proxy |
| `Failed to find target with hash string 'android-35'` | API 35 platform not installed | Tools → SDK Manager → tick "Android 15 (API 35)" → Apply |
| App won't install on device | USB debugging off, or "verify apps over USB" disabled | Toggle in device's Developer options |
| Compose previews blank | Need to build once first | Build → Make Project (Ctrl/⌘ + F9) |
| Crash on launch with `MissingResourceException` | Stale build cache | Build → Clean Project, then Build → Rebuild |

## Going from mock to live

Once you've published a real manifest:

1. Open `app/build.gradle.kts`
2. Change:
   ```kotlin
   buildConfigField("boolean", "USE_MOCK_MANIFEST", "true")
   ```
   to `"false"`, and update `MANIFEST_URL` to your actual JSON URL.
3. Sync Gradle (banner at top), Run again.

Or, leave the build defaults and override the URL at runtime via **Settings → Manifest URL** — handy for testing against staging endpoints.

## Building a signed APK locally

```bash
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Requires the keystore properties in `~/.gradle/gradle.properties` and the signing config wired up in `app/build.gradle.kts` — see `KEYSTORE_SETUP.md`.

## Where to look when something breaks

- **Logcat** (bottom panel) shows live runtime logs. Filter to your package: `package:dev.matejgroombridge.store`
- **Build Output** panel shows Gradle errors with clickable file:line links
- **Network requests** — add `Logger.SIMPLE` to the Ktor client in `HttpClientProvider.kt` if you need to debug manifest fetches
