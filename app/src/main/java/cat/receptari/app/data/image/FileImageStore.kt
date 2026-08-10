package cat.receptari.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.repository.ImageStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileImageStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ImageStore {

    private val imagesDir: File
        get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override suspend fun save(recipeId: String, bytes: ByteArray): String =
        withContext(ioDispatcher) {
            val bitmap = decodeDownscaled(bytes)
            val fileName = "$recipeId-${UUID.randomUUID()}.jpg"
            val file = File(imagesDir, fileName)

            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()

            "$DIRECTORY/$fileName"
        }

    override suspend fun delete(relativePath: String) {
        withContext(ioDispatcher) {
            File(context.filesDir, relativePath).delete()
        }
    }

    override fun absolutePathOf(relativePath: String): String =
        File(context.filesDir, relativePath).absolutePath

    /**
     * Cookbook photos come off a modern phone camera at 50 megapixels. Decoding one at full
     * size to store a recipe thumbnail is how an app runs out of memory, so the bounds are
     * read first and the decoder is told to subsample on the way in.
     */
    private fun decodeDownscaled(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longestEdge / sampleSize > MAX_EDGE_PX * 2) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)) {
            "Could not decode image"
        }

        val edge = maxOf(decoded.width, decoded.height)
        if (edge <= MAX_EDGE_PX) return decoded

        val scale = MAX_EDGE_PX.toFloat() / edge
        val scaled = decoded.scale(
            width = (decoded.width * scale).toInt().coerceAtLeast(1),
            height = (decoded.height * scale).toInt().coerceAtLeast(1),
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private companion object {
        const val DIRECTORY = "images"
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85
    }
}
