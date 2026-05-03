package dev.matejgroombridge.store.data.repository

import dev.matejgroombridge.store.BuildConfig
import dev.matejgroombridge.store.data.model.AppEntry
import dev.matejgroombridge.store.data.model.AppManifest
import dev.matejgroombridge.store.data.network.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns "what does the user have available to install?".
 *
 * Two implementations share the same interface so the rest of the app doesn't care
 * whether the data is real or mocked.
 */
interface ManifestRepository {
    suspend fun fetchManifest(url: String = BuildConfig.MANIFEST_URL): Result<AppManifest>
}

/** Hits the network. Used in production. */
class RemoteManifestRepository : ManifestRepository {
    override suspend fun fetchManifest(url: String): Result<AppManifest> = withContext(Dispatchers.IO) {
        runCatching {
            HttpClientProvider.client.get(url).body<AppManifest>()
        }
    }
}

/**
 * Returns a hand-crafted manifest with two example apps so the UI is meaningful
 * before any real apps exist. Toggle via BuildConfig.USE_MOCK_MANIFEST.
 */
class MockManifestRepository : ManifestRepository {
    override suspend fun fetchManifest(url: String): Result<AppManifest> {
        // Tiny artificial delay so the loading state is visible during development.
        kotlinx.coroutines.delay(400)
        return Result.success(MOCK_MANIFEST)
    }

    companion object {
        val MOCK_MANIFEST = AppManifest(
            generated_at = "2026-05-02T20:55:00Z",
            apps = listOf(
                AppEntry(
                    package_name = "dev.matejgroombridge.notes",
                    display_name = "Notes",
                    description = "Distraction-free markdown notes with instant search and a Monokai-tinted editor.",
                    icon_url = null,
                    version_code = 7,
                    version_name = "1.3.0",
                    apk_url = "https://example.com/notes-1.3.0.apk",
                    apk_sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
                    apk_size_bytes = 8_421_337,
                    min_sdk = 26,
                    released_at = "2026-05-02T20:55:00Z",
                    changelog = """
                        - Added wallpaper-tinted theme
                        - Quick capture from notification shade
                        - Fixed crash when opening empty notes
                    """.trimIndent(),
                    source_url = "https://github.com/matejgroombridge/notes",
                    category = "Productivity",
                ),
                AppEntry(
                    package_name = "dev.matejgroombridge.timer",
                    display_name = "Focus Timer",
                    description = "A minimalist Pomodoro timer with calm haptics and zero ads.",
                    icon_url = null,
                    version_code = 3,
                    version_name = "0.4.1",
                    apk_url = "https://example.com/timer-0.4.1.apk",
                    apk_sha256 = "1111111111111111111111111111111111111111111111111111111111111111",
                    apk_size_bytes = 3_145_728,
                    min_sdk = 26,
                    released_at = "2026-04-28T14:10:00Z",
                    changelog = """
                        - New "long break" preset
                        - Subtle progress ring animation
                    """.trimIndent(),
                    source_url = "https://github.com/matejgroombridge/focus-timer",
                    category = "Utilities",
                ),
            )
        )
    }
}
