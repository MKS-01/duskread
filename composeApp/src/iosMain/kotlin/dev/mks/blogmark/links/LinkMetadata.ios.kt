package dev.mks.blogmark.links

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/** Darwin, so requests go through NSURLSession and inherit the system's proxy and TLS settings. */
actual fun createHttpClient(): HttpClient = HttpClient(Darwin)
