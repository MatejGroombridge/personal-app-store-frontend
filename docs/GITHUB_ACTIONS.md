# GitHub Actions Release Pipeline

This is the workflow you drop into **every app repo** (including this Store App's own repo). Push a tag like `v1.3.0` and ~3 minutes later the Store App on your phone sees the update.

## One-time repo setup

### A. Create a manifest repo

```
github.com/<you>/personal-app-manifest        ← public, holds manifest.json
```

Initial `manifest.json`:
```json
{ "generated_at": "1970-01-01T00:00:00Z", "apps": [] }
```

### B. Per-app repo secrets

Set these on **each** app repo (or organization-wide once and inherit):

| Secret | Purpose |
|--------|---------|
| `KEYSTORE_BASE64` | Your `.jks` file, base64-encoded |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | e.g. `matej-key` |
| `KEY_PASSWORD` | Key password |
| `MANIFEST_REPO_TOKEN` | Personal Access Token with `contents:write` on the manifest repo |

(For PAT creation: GitHub → Settings → Developer settings → Fine-grained tokens → only the manifest repo, only Contents: Read/Write.)

## The workflow

Save as `.github/workflows/release.yml` in every app repo:

```yaml
name: Release

on:
  push:
    tags: ['v*.*.*']

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write    # to attach the APK to the GitHub Release of THIS repo

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      # ── Decode keystore ───────────────────────────────────────
      - name: Decode keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > $RUNNER_TEMP/release.jks
          echo "STORE_FILE=$RUNNER_TEMP/release.jks" >> $GITHUB_ENV

      # ── Build signed release APK ──────────────────────────────
      - name: Assemble release APK
        env:
          MATEJ_STORE_FILE:     ${{ env.STORE_FILE }}
          MATEJ_STORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          MATEJ_KEY_ALIAS:      ${{ secrets.KEY_ALIAS }}
          MATEJ_KEY_PASSWORD:   ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew :app:assembleRelease --no-daemon

      # ── Compute metadata ──────────────────────────────────────
      - name: Extract metadata
        id: meta
        run: |
          APK=$(ls app/build/outputs/apk/release/*.apk | head -n1)
          BUILD_TOOLS=$(ls $ANDROID_HOME/build-tools | sort -V | tail -n1)
          AAPT="$ANDROID_HOME/build-tools/$BUILD_TOOLS/aapt"

          PKG=$($AAPT dump badging "$APK" | awk -F"'" '/package: name=/{print $2}')
          VC=$($AAPT dump badging "$APK"  | awk -F"'" '/package: name=/{print $4}')
          VN=$($AAPT dump badging "$APK"  | awk -F"'" '/package: name=/{print $6}')
          MIN=$($AAPT dump badging "$APK" | awk -F"'" '/sdkVersion:/{print $2}')

          SHA=$(sha256sum "$APK" | awk '{print $1}')
          SIZE=$(stat -c%s "$APK")

          NEW_NAME="${PKG}-${VN}.apk"
          mv "$APK" "app/build/outputs/apk/release/$NEW_NAME"
          APK="app/build/outputs/apk/release/$NEW_NAME"

          {
            echo "apk_path=$APK"
            echo "apk_name=$NEW_NAME"
            echo "package=$PKG"
            echo "version_code=$VC"
            echo "version_name=$VN"
            echo "min_sdk=$MIN"
            echo "sha256=$SHA"
            echo "size=$SIZE"
          } >> $GITHUB_OUTPUT

      # ── Publish APK as a GitHub Release asset ─────────────────
      - name: Create GitHub Release
        id: gh_release
        uses: softprops/action-gh-release@v2
        with:
          files: ${{ steps.meta.outputs.apk_path }}
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      # ── Update the central manifest.json ──────────────────────
      - name: Checkout manifest repo
        uses: actions/checkout@v4
        with:
          repository: matejgroombridge/personal-app-manifest
          token: ${{ secrets.MANIFEST_REPO_TOKEN }}
          path: manifest-repo

      - name: Patch manifest.json
        env:
          PKG:      ${{ steps.meta.outputs.package }}
          NAME:     ${{ github.event.repository.name }}
          VC:       ${{ steps.meta.outputs.version_code }}
          VN:       ${{ steps.meta.outputs.version_name }}
          MIN_SDK:  ${{ steps.meta.outputs.min_sdk }}
          SHA:      ${{ steps.meta.outputs.sha256 }}
          SIZE:     ${{ steps.meta.outputs.size }}
          APK_NAME: ${{ steps.meta.outputs.apk_name }}
          REPO:     ${{ github.repository }}
          TAG:      ${{ github.ref_name }}
        run: |
          python3 - <<'PY'
          import json, os, datetime, urllib.parse, pathlib
          path = pathlib.Path("manifest-repo/manifest.json")
          data = json.loads(path.read_text()) if path.exists() else {"apps": []}
          apps = [a for a in data.get("apps", []) if a.get("package_name") != os.environ["PKG"]]
          apk_url = (
              f"https://github.com/{os.environ['REPO']}/releases/download/"
              f"{urllib.parse.quote(os.environ['TAG'])}/{os.environ['APK_NAME']}"
          )
          apps.append({
              "package_name":   os.environ["PKG"],
              "display_name":   os.environ["NAME"].replace("-", " ").title(),
              "version_code":   int(os.environ["VC"]),
              "version_name":   os.environ["VN"],
              "apk_url":        apk_url,
              "apk_sha256":     os.environ["SHA"],
              "apk_size_bytes": int(os.environ["SIZE"]),
              "min_sdk":        int(os.environ["MIN_SDK"]),
              "released_at":    datetime.datetime.utcnow().isoformat() + "Z",
              "source_url":     f"https://github.com/{os.environ['REPO']}",
          })
          data["apps"] = sorted(apps, key=lambda a: a["display_name"].lower())
          data["generated_at"] = datetime.datetime.utcnow().isoformat() + "Z"
          path.write_text(json.dumps(data, indent=2) + "\n")
          PY

      - name: Commit & push manifest update
        working-directory: manifest-repo
        run: |
          git config user.name  "release-bot"
          git config user.email "release-bot@users.noreply.github.com"
          git add manifest.json
          git commit -m "Update ${{ steps.meta.outputs.package }} → v${{ steps.meta.outputs.version_name }}"
          git push
```

## Releasing a new version

```bash
git tag v1.4.0
git push origin v1.4.0
```

That's it. ~3 min later your phone polls the manifest, sees the bumped `version_code`, and shows **Update**.

## Optional: AI-assisted changelog & sanity check

Insert this step before the build to have Claude/GPT review your diff:

```yaml
- name: AI changelog & sanity check
  uses: anthropics/claude-code-action@v1
  with:
    api_key: ${{ secrets.ANTHROPIC_API_KEY }}
    prompt: |
      You are a release reviewer. Given the diff between the previous tag and this one,
      (1) write a clean changelog, (2) flag any obvious red flags (hardcoded secrets,
      version_code not incremented, etc.). Refuse the release if you find a critical issue.
```

## Self-update for the Store App itself

The Store App is just another app in the manifest. Add this same workflow to the store-app repo, with `display_name` set to "Matej Store" via a manifest override step. The Store App will then offer to update *itself* whenever you publish a new version.
