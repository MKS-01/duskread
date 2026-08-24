package dev.mks.duskread.links

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** OkHttp rather than the `Android` engine: it follows redirects and speaks HTTP/2, which most publishers now serve. */
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)
