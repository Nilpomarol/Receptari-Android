package cat.receptari.app.domain.repository

/**
 * Stores recipe images as files. Images never live in the database as BLOBs — a recipe
 * photo is a file, and keeping it one makes both rendering and backup straightforward.
 *
 * Paths handed back are relative, so the stored value survives the app's data directory
 * changing between installs.
 */
interface ImageStore {

    /** Downscales, compresses and stores the image. Returns the relative path to store. */
    suspend fun save(recipeId: String, bytes: ByteArray): String

    suspend fun delete(relativePath: String)

    /** Absolute path for rendering. */
    fun absolutePathOf(relativePath: String): String
}
