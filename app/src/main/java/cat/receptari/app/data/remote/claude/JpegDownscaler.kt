package cat.receptari.app.data.remote.claude

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Shrinks a photo to something worth sending over a phone connection.
 *
 * The API resizes anything longer than ~1568 px on its longest edge before the model sees
 * it, so sending more than that costs the user bandwidth for no extra detail. Text stays
 * legible at this size, which is the only thing that matters for reading a recipe.
 */
internal object JpegDownscaler {

    private const val MAX_EDGE_PX = 1568
    private const val JPEG_QUALITY = 85

    fun downscale(bytes: ByteArray): ByteArray {
        val bitmap = decodeDownscaled(bytes) ?: return bytes
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    /** Two-stage: `inSampleSize` to avoid decoding the full image, then an exact scale. */
    private fun decodeDownscaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (longestEdge <= 0) return null

        var sampleSize = 1
        while (longestEdge / sampleSize > MAX_EDGE_PX * 2) {
            sampleSize *= 2
        }

        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null

        val edge = maxOf(decoded.width, decoded.height)
        if (edge <= MAX_EDGE_PX) return decoded

        val scale = MAX_EDGE_PX.toFloat() / edge
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }
}
