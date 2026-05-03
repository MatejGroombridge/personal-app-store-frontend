# Manifest Specification

The Store App polls a single JSON file. Whatever URL the app is configured to hit must return this shape.

## Recommended hosting

For zero-cost bootstrapping: a **public GitHub repo** named e.g. `personal-app-manifest` containing one `manifest.json` at the root. Raw URL:

```
https://raw.githubusercontent.com/<you>/personal-app-manifest/main/manifest.json
```

Each app's APKs live as **GitHub Release assets** in their own repo, e.g.
`https://github.com/<you>/notes/releases/download/v1.3.0/notes-1.3.0.apk`.

GitHub serves both via fast CDN, no auth needed for public repos. Move to Cloudflare R2 later if you want even more speed or to keep things private.

## Schema

```jsonc
{
  "generated_at": "2026-05-02T20:55:00Z",  // ISO-8601 UTC, when the file was rewritten
  "apps": [
    {
      "package_name":   "dev.matejgroombridge.notes",   // REQUIRED, unique, stable
      "display_name":   "Notes",                         // REQUIRED, shown in UI
      "description":    "Markdown notes…",               // optional
      "icon_url":       "https://…/icon.png",            // optional, square PNG/WebP
      "screenshots":    ["https://…/1.png"],             // optional list

      "version_code":   7,                               // REQUIRED, monotonically ↑
      "version_name":   "1.3.0",                         // REQUIRED, human-readable

      "apk_url":        "https://…/notes-1.3.0.apk",     // REQUIRED, direct download
      "apk_sha256":     "abc123…",                       // REQUIRED, hex, lower or UPPER
      "apk_size_bytes": 8421337,                         // optional but recommended

      "min_sdk":        26,                              // optional, default 26
      "released_at":    "2026-05-02T20:55:00Z",          // optional ISO-8601
      "changelog":      "- Added X\n- Fixed Y",          // optional, Markdown
      "source_url":     "https://github.com/…",          // optional
      "category":       "Productivity"                   // optional free-text tag
    }
  ]
}
```

## Field rules

- `package_name` is **immutable**. Once shipped, never rename it — Android treats a different package name as a different app.
- `version_code` must **strictly increase** between releases of the same package, or Android will refuse the upgrade. Recommended scheme: derive from semver — `1.3.0` → `10300`, `2.0.0` → `20000`.
- `apk_sha256` is verified before install. A mismatch aborts with "Checksum mismatch — refusing to install."
- Unknown fields are ignored by the client (forward-compatible).

## Updating the manifest from CI

The pipeline:
1. Fetches the current `manifest.json` (or starts with `{ "apps": [] }` if 404)
2. Removes any existing entry where `package_name == BUILDING_APP_PACKAGE`
3. Appends the new entry
4. Writes back `generated_at = now()`
5. Commits + pushes to the manifest repo (or PUTs to R2)

A reference implementation is in [`GITHUB_ACTIONS.md`](GITHUB_ACTIONS.md).

## Validating manually

```bash
curl -s https://raw.githubusercontent.com/<you>/personal-app-manifest/main/manifest.json | jq .
```

If it parses cleanly and contains your app, the Store App will see it on next refresh.
