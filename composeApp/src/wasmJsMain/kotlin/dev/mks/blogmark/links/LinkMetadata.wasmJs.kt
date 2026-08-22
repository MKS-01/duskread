package dev.mks.blogmark.links

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

/**
 * `fetch` under the hood, which means the browser's CORS rules apply: most
 * publishers send no permissive `Access-Control-Allow-Origin`, so on web the
 * fetch usually fails and the link keeps its URL-derived title. Saving,
 * listing and opening all still work — only the title lookup is degraded.
 */
actual fun createHttpClient(): HttpClient = HttpClient(Js)
