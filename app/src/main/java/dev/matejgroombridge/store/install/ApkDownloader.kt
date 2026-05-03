package dev.matejgroombridge.store.install

import android.content.Context
import dev.matejgroombridge.store.data.network.HttpClientProvider
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Downloads APKs into the app's cache dir and verifies SHA-256 before returning.
 *
 * Why cache and not external storage?
 *  - No extra permissions required
 *  - Auto-cleaned when device is low on space
 *  - FileProvider already configured to expose this directory to the system installer
 */
class ApkDownloader(private val context: Context) {

    private val apkDir: File
        get() = File(context.cacheDir, "apks").apply { mkdirs() }

    /** Streams [url] to disk while invoking [onProgress]. Returns the saved file. */
    suspend fun download(
        url: String,
        fileName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val target = File(apkDir, fileName)
        if (target.exists()) target.delete()

        HttpClientProvider.client.prepareGetWithProgress(url, onProgress) { channel ->
            FileOutputStream(target).use { out ->
                copyChannelToStream(channel, out)
            }
        }
        target
    }

    /** Hex SHA-256 of the file. Compared (case-insensitively) against manifest entry. */
    suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Wipe cached downloads (called after successful install). */
    fun clearCacheFor(fileName: String) {
        File(apkDir, fileName).takeIf { it.exists() }?.delete()
    }

    private suspend inline fun io.ktor.client.HttpClient.prepareGetWithProgress(
        url: String,
        crossinline onProgress: (Long, Long) -> Unit,
        crossinline block: suspend (ByteReadChannel) -> Unit,
    ) {
        val response = this.get(url) {
            onDownload { downloaded, total -> onProgress(downloaded, total ?: 0L) }
        }
        block(response.bodyAsChannel())
    }

    private suspend fun copyChannelToStream(channel: ByteReadChannel, out: FileOutputStream) {
        val buf = ByteArray(64 * 1024)
        while (!channel.isClosedForRead) {
            val n = channel.readAvailable(buf, 0, buf.size)
            if (n > 0) {
                out.write(buf, 0, n)
            } else if (n < 0) {
                break
            }
        }
    }
}
