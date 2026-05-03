# Releasing — the `bin/changeset` workflow

The Matej app store family uses a single, identical release helper across every app repo. It exists because semver bumps + changelog entries + git tag wrangling are easy to get wrong by hand, and because Android's `versionCode` *must* monotonically increase or upgrades silently fail.

If you've used Atlassian's "AFM Changeset" tool internally, this is the same idea, slimmed down to one bash script.

## TL;DR

```bash
./bin/changeset
# pick patch/minor/major → type a one-line description → confirm → push
```

~3 minutes later your phone's Store App offers the new version.

## What it does, step by step

1. **Reads** the current `versionName` and `versionCode` out of `app/build.gradle.kts`.
2. **Asks** whether this release is a `patch`, `minor`, or `major` bump and previews the resulting semver.
3. **Asks** for a one-line description, used for the commit message, the GitHub Release title, and the changelog entry.
4. **Bumps** `versionName` and increments `versionCode` by 1 in `app/build.gradle.kts`.
5. **Prepends** the new section to `CHANGELOG.md`.
6. **Commits** as `Release vX.Y.Z — <description>` and creates an annotated git tag `vX.Y.Z`.
7. **Pushes** (after your confirmation), which triggers the `release.yml` GitHub Actions workflow.

## What the workflow then does

`.github/workflows/release.yml` runs on the pushed tag and:

1. Builds & signs the release APK using the keystore secrets.
2. Attaches the APK to a new GitHub Release on this repo.
3. Reads the most recent section of `CHANGELOG.md` (everything between the first `## ` heading and the next one) and stuffs it into the manifest entry's `changelog` field.
4. Patches the central `personal-app-manifest/manifest.json` with the new entry — bumped `version_code`, fresh SHA-256, new download URL, the changelog body — and commits.
5. ~3 minutes after `git push origin vX.Y.Z`, your phone polls the manifest and shows **Update**.

## Conventions the script relies on

So that the same script drops cleanly into every app repo, it makes a few assumptions:

| Convention | Why |
|---|---|
| `app/build.gradle.kts` contains literal `versionCode = N` and `versionName = "X.Y.Z"` lines | Cheap to parse with a one-line `grep`/`awk` |
| `versionName` is pure semver (no `-debug`, no `-rc.1`) | Lets the script unambiguously bump major/minor/patch |
| `CHANGELOG.md` lives at the repo root | Matches what GitHub auto-detects, and what the workflow reads |
| Tags are `vX.Y.Z` (with leading `v`) | Matches the `tags: ['v*.*.*']` trigger in `release.yml` |
| `versionCode` increments by 1 per release | Simplest possible scheme; if you ever need to encode something more elaborate (e.g. ABI splits) replace the script for that one repo |
| Releases are made from `main` (or `master`) | Script warns if you're on a feature branch — bypassable |

## The interactive flow

```
Current version: 0.2.3 (versionCode 4)

What kind of change?
  1) patch  (bug fix, small tweak)              0.2.3 → 0.2.4
  2) minor  (new feature, backward compatible)  0.2.3 → 0.3.0
  3) major  (breaking change)                   0.2.3 → 1.0.0
> 1

Description (one line — used for the commit message, GitHub release title, and changelog entry)
> Fixed crash when manifest URL is unreachable on startup

About to:
  • Bump versionName 0.2.3 → 0.2.4
  • Bump versionCode 4 → 5
  • Prepend release notes to CHANGELOG.md
  • Commit + tag v0.2.4

Proceed? [Y/n] 
```

## Resulting `CHANGELOG.md` entry

```markdown
## v0.2.4 — 2026-05-15

Fixed crash when manifest URL is unreachable on startup
```

## Resulting manifest entry (excerpt)

```json
{
  "package_name": "dev.matejgroombridge.store",
  "display_name": "Matej Store",
  "version_name": "0.2.4",
  "version_code": 5,
  "changelog": "## v0.2.4 — 2026-05-15\n\nFixed crash when manifest URL is unreachable on startup",
  ...
}
```

## Reusing the script across other apps

The script has zero hard-coded paths or app-specific config. To use it in any other app repo:

```bash
cp bin/changeset /path/to/other-app/bin/changeset
chmod +x /path/to/other-app/bin/changeset
```

The other repo just needs:
- `app/build.gradle.kts` with `versionCode` / `versionName` lines (every Android app already has these)
- `.github/workflows/release.yml` (the one in this repo, copied verbatim — see `docs/GITHUB_ACTIONS.md`)
- The 5 GitHub secrets set up

Or, if you want to centralise it later, consider hosting it in a small `matej-tooling` repo and downloading it into each release with `curl`. For now, the per-repo copy is simpler.

## Common situations

**"I made a typo in the changelog after committing."**

Run `git tag -d vX.Y.Z` to drop the local tag, edit `CHANGELOG.md`, `git commit --amend`, then `git tag vX.Y.Z` again. Don't push until you're happy. If you've already pushed, you're better off shipping a new patch release with a corrected entry than rewriting history on a public repo.

**"I need to skip a version number."**

Edit `app/build.gradle.kts` manually to whatever version you want, then run `./bin/changeset` and pick patch — it'll bump from wherever you set it.

**"I want to release without bumping the version."**

You don't. Android refuses to install an upgrade with the same `versionCode`. If you have to ship a "no behaviour change" patch (e.g. only updating the GitHub Release notes), bump the patch number anyway.

**"The script wrote the changelog but the workflow failed."**

The git tag has been created locally but possibly not pushed yet. To start over after fixing the workflow:

```bash
git push origin vX.Y.Z       # if not yet pushed
# OR, to scrap the release entirely:
git tag -d vX.Y.Z
git reset --hard HEAD~1      # only safe if not pushed
```

To re-run the release after fixing a CI issue without bumping the version, just re-run the failed workflow from the GitHub Actions UI — the manifest patch step is idempotent (it replaces any existing entry for this `package_name`).

## Why not Atlassian's `@changesets/cli` or `release-please`?

Both are great tools — `@changesets/cli` is what I'd reach for in a JavaScript monorepo with multiple contributors, and `release-please` is excellent if you want commit-message-driven releases with no human in the loop.

For a personal Android-app-per-repo setup with one contributor, both are overkill:
- `@changesets/cli` requires Node, designed around npm package versioning.
- `release-please` requires Conventional Commits discipline and adds a PR-based workflow.

`bin/changeset` is ~80 lines of dependency-free bash you can read, modify, and debug in one sitting. When you have 3+ contributors or 5+ apps with cross-dependencies, swap to `@changesets/cli`. Until then, this is the pragmatic choice.
