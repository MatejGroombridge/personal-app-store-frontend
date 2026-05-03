package dev.matejgroombridge.store.data.repository

import dev.matejgroombridge.store.BuildConfig
import dev.matejgroombridge.store.data.model.AppManifest
import dev.matejgroombridge.store.data.network.HttpClientProvider
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns "what does the user have available to install?".
 *
 * Fetches the JSON manifest from the URL configured in BuildConfig (or overridden
 * at runtime via Settings). The rest of the app sees only this interface.
 */
interface ManifestRepository {
    suspend fun fetchManifest(url: String = BuildConfig.MANIFEST_URL): Result<AppManifest>
}

/** Hits the network. The only implementation. */
class RemoteManifestRepository : ManifestRepository {
    override suspend fun fetchManifest(url: String): Result<AppManifest> = withContext(Dispatchers.IO) {
        runCatching {
            HttpClientProvider.client.get(url).body<AppManifest>()
        }
    }
}
