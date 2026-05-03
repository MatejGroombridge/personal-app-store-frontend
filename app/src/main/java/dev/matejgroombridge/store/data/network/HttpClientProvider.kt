package dev.matejgroombridge.store.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Single shared Ktor client. Cheap to keep around for the app's lifetime; expensive
 * to create repeatedly.
 */
object HttpClientProvider {

    val json: Json = Json {
        ignoreUnknownKeys = true       // forward-compatible: new manifest fields don't break old clients
        explicitNulls = false
        prettyPrint = false
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            defaultRequest {
                header("User-Agent", "MatejStore/1.0 (Android)")
                header("Cache-Control", "no-cache")
            }
            expectSuccess = true
        }
    }
}
