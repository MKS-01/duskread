package dev.mks.duskread.notion

import dev.mks.duskread.data.NotionTokenKey
import dev.mks.duskread.data.SecretStore

/**
 * How a request proves who it is, kept behind an interface so the rest of the
 * Notion code never learns what kind of credential it is holding.
 *
 * That indirection is the whole point of this file. Notion offers two ways in
 * and only one of them works from a phone:
 *
 * - A **personal access token**, pasted once. It acts as the user who created
 *   it and inherits their workspace permissions, so there is no per-database
 *   "Add connections" step to forget. This is [PastedTokenAuth].
 * - **OAuth**, which would be the right answer for a public app but requires
 *   HTTP Basic auth with `CLIENT_ID:CLIENT_SECRET` at the token exchange and
 *   offers no PKCE variant. A client-only app cannot hold that secret, so
 *   OAuth needs a server DuskRead does not have.
 *
 * When that server does exist, an `OAuthAuth` writes its access token into the
 * same [SecretStore] and nothing else in `notion/` changes.
 */
interface NotionAuth {
    /** The bearer token, or null when nothing is connected. */
    suspend fun bearer(): String?

    /**
     * Forgets the credential — the logout, whichever kind it turns out to be.
     * Followed feeds are not the credential's to remove and stay put.
     */
    fun disconnect()
}

/** A token the reader pasted into Settings, held in [SecretStore]. */
class PastedTokenAuth(private val secrets: SecretStore) : NotionAuth {
    override suspend fun bearer(): String? = secrets.get(NotionTokenKey)?.takeIf { it.isNotBlank() }

    override fun disconnect() = secrets.put(NotionTokenKey, null)

    /** Saving is only meaningful for a pasted token, so it is not on the interface. */
    fun save(token: String) = secrets.put(NotionTokenKey, token.trim().takeIf { it.isNotBlank() })
}
