# Keystore Setup

Every Android app must be cryptographically signed. **Once you ship v1.0.0 of an app signed with a particular keystore, every future update of that app must use the same keystore** — Android refuses to install upgrades signed by a different key.

You only need to do this once. Use the same keystore for *all* your personal apps.

---

## 1. Generate the keystore

```bash
keytool -genkey -v \
  -keystore matej-release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 36500 \
  -alias matej-key
```

`keytool` is bundled with the JDK that Android Studio installs. If `keytool: command not found`, run it from the Android Studio JBR directory:

- macOS: `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool`
- Windows: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`
- Linux: `~/android-studio/jbr/bin/keytool`

Answer the prompts (any sensible values — they're only metadata). Set strong passwords for both the keystore and the key.

You'll end up with `matej-release.jks` in your current directory.

## 2. Back it up. Three places. Right now.

If you lose this file you can never update *any* of your apps again — your only recourse is to publish each app under a brand-new package name.

Recommended:
1. Password manager (1Password, Bitwarden) — attach the file
2. Encrypted USB stick in a drawer
3. Encrypted cloud backup (e.g. an encrypted ZIP in Google Drive)

Also save the **passwords** somewhere separate from the keystore.

## 3. Wire it into Android Studio (for local release builds)

Create `~/.gradle/gradle.properties` (NOT in the project) and add:

```properties
MATEJ_STORE_FILE=/absolute/path/to/matej-release.jks
MATEJ_STORE_PASSWORD=••••••••
MATEJ_KEY_ALIAS=matej-key
MATEJ_KEY_PASSWORD=••••••••
```

Then in `app/build.gradle.kts`, replace the empty `release { }` block with:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(providers.gradleProperty("MATEJ_STORE_FILE").get())
        storePassword = providers.gradleProperty("MATEJ_STORE_PASSWORD").get()
        keyAlias = providers.gradleProperty("MATEJ_KEY_ALIAS").get()
        keyPassword = providers.gradleProperty("MATEJ_KEY_PASSWORD").get()
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // …rest of release block…
    }
}
```

Now `./gradlew assembleRelease` produces a signed APK at
`app/build/outputs/apk/release/app-release.apk`.

## 4. Wire it into GitHub Actions (for the auto-publish pipeline)

The pipeline can't read your `~/.gradle/gradle.properties`. Instead:

```bash
# Encode the keystore so it can live in a GitHub secret
base64 -i matej-release.jks -o matej-release.jks.b64
```

Then in your GitHub repo (or org): **Settings → Secrets and variables → Actions → New repository secret**, add:

| Name | Value |
|------|-------|
| `KEYSTORE_BASE64` | contents of `matej-release.jks.b64` |
| `KEYSTORE_PASSWORD` | your store password |
| `KEY_ALIAS` | `matej-key` |
| `KEY_PASSWORD` | your key password |

See [`GITHUB_ACTIONS.md`](GITHUB_ACTIONS.md) for the workflow that consumes them.

## 5. Verify a signed APK

```bash
# Confirms the APK is signed and shows the signing certificate fingerprint
$ANDROID_HOME/build-tools/<latest>/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

You want to see `Verified using v2 scheme: true` (or v3/v4).

## Common pitfalls

- ❌ **Committing the `.jks` to git.** It's in `.gitignore` for a reason.
- ❌ **Using a different key per app.** Painful to manage; no benefit for personal apps.
- ❌ **Letting passwords differ between key and store.** Pipelines assume they may differ; fine, but consistency is simpler.
- ❌ **Short validity (e.g. 1 year).** You don't want to deal with expiry. Use 100 years.
