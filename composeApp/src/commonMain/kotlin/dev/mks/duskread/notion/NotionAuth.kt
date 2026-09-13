package dev.mks.duskread.notion

import dev.mks.duskread.data.NotionTokenKey
import dev.mks.duskread.data.SecretStore

/**
 * How a request proves who it is, kept behind an interface so the rest of the Notion code
 * never learns what kind of credential it is holding.
 */
interface NotionAuth {
    /** The bearer token, or null when nothing is connected. */
    suspend fun bearer(): String?

    /**
     * Forgets the credential — the logout, whichever kind it turns out to be. Followed
     * feeds are not the credential's to remove and stay put.
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
