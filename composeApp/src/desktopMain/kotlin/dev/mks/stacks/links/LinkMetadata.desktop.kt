package dev.mks.stacks.links

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/** CIO: pure Kotlin, no JVM-only native bindings to ship in the desktop distribution. */
actual fun createHttpClient(): HttpClient = HttpClient(CIO)
