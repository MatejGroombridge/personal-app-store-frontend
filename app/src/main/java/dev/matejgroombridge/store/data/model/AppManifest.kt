package dev.matejgroombridge.store.data.model

import kotlinx.serialization.Serializable

/**
 * Top-level shape of the JSON file the Store App polls.
 *
 * The CI/CD pipeline rewrites this file on every successful release of any app:
 *   1. Pull current manifest.json from the host
 *   2. Replace/insert the entry whose package_name matches the building app
 *   3. Bump generated_at
 *   4. Push it back
 *
 * Example URL: https://raw.githubusercontent.com/matej/personal-app-manifest/main/manifest.json
 */
@Serializable
data class AppManifest(
    val generated_at: String,
    val apps: List<AppEntry> = emptyList(),
)

/** A single published app version. */
@Serializable
data class AppEntry(
    /** Reverse-DNS package id, e.g. "dev.matejgroombridge.notes". Stable & unique. */
    val package_name: String,

    /** Human-readable name shown in the UI, e.g. "Notes". */
    val display_name: String,

    /** Short, one-paragraph description for the detail screen. */
    val description: String = "",

    /** Optional URL to a square icon (PNG/WebP). Coil loads + caches it. */
    val icon_url: String? = null,

    /** Optional list of screenshot URLs for the detail screen carousel. */
    val screenshots: List<String> = emptyList(),

    /** Monotonically increasing integer Android uses to detect upgrades. */
    val version_code: Int,

    /** Human-readable version, e.g. "1.3.0". */
    val version_name: String,

    /** Direct download URL for the signed release APK. */
    val apk_url: String,

    /** SHA-256 hex digest of the APK. The Store App verifies this after download. */
    val apk_sha256: String,

    /** Size in bytes (used for download progress + UI labels). */
    val apk_size_bytes: Long = 0L,

    /** Minimum Android SDK level required to install. */
    val min_sdk: Int = 26,

    /** ISO-8601 release timestamp. */
    val released_at: String = "",

    /** Markdown changelog shown on the detail screen. */
    val changelog: String = "",

    /** Source repo URL (Bitbucket / GitHub) — optional, shown in detail. */
    val source_url: String? = null,

    /** Optional category tag, e.g. "productivity", "utilities". */
    val category: String? = null,
)
