package cat.receptari.app.core.designsystem

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import kotlin.math.max
import kotlin.random.Random

/**
 * The page the whole app is printed on.
 *
 * Paper is a colour plus two imperfections: grain, and darkening towards the edges. Both are
 * generated rather than shipped as a bitmap — a photographic paper texture would be a
 * megabyte, would need a light and a dark variant, and would tile visibly on a tall scroll.
 * The grain here is a 96 px tile drawn once into a repeating shader, so the cost is one small
 * allocation per theme and a single rect per frame.
 */
@Composable
fun Modifier.paperBackground(): Modifier {
    val base = MaterialTheme.colorScheme.background
    val palette = ReceptariTheme.palette
    val grain = rememberGrainBrush(palette.paperEdge, palette.grainAlpha)
    val edge = palette.paperEdge

    return this.drawWithCache {
        // Centred slightly above the middle: light falls on a page from above, and a
        // perfectly centred vignette reads as a lens effect rather than as ageing.
        val vignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, edge.copy(alpha = VIGNETTE_ALPHA)),
            center = Offset(size.width / 2f, size.height * VIGNETTE_CENTER_FRACTION),
            radius = max(size.width, size.height) * VIGNETTE_RADIUS_FRACTION,
        )

        onDrawBehind {
            drawRect(base)
            drawRect(grain)
            drawRect(vignette)
        }
    }
}

/**
 * The printed frame around a card: a rule, and a fainter rule just inside it.
 *
 * Drawn rather than composed from two [androidx.compose.foundation.border] calls so it costs
 * no layout inset — the caller's padding stays the padding it asked for.
 */
fun Modifier.pageFrame(
    shape: Shape,
    color: Color,
    inset: Dp = 3.5.dp,
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val insetPx = inset.toPx()
    val innerSize = Size(size.width - insetPx * 2, size.height - insetPx * 2)
    val innerOutline = if (insetPx > 0f && innerSize.minDimension > 0f) {
        shape.createOutline(innerSize, layoutDirection, this)
    } else {
        null
    }

    onDrawWithContent {
        drawContent()
        drawOutline(outline, color = color, style = Stroke(width = HAIRLINE_PX))
        innerOutline?.let {
            translate(insetPx, insetPx) {
                drawOutline(it, color = color.copy(alpha = INNER_RULE_ALPHA), style = Stroke(HAIRLINE_PX))
            }
        }
    }
}

@Composable
private fun rememberGrainBrush(color: Color, alpha: Float): Brush =
    remember(color, alpha) {
        ShaderBrush(ImageShader(grainTile(color, alpha), TileMode.Repeated, TileMode.Repeated))
    }

/**
 * One tile of paper grain: mostly a very faint speckle, with a scattering of darker flecks
 * standing in for the foxing an old page picks up.
 *
 * The seed is fixed so the grain is the same on every launch. A page that re-speckles itself
 * when the theme changes looks like a rendering bug, not like paper.
 */
private fun grainTile(color: Color, alpha: Float): ImageBitmap {
    val random = Random(GRAIN_SEED)
    val rgb = color.toArgb() and 0x00FFFFFF
    val pixels = IntArray(GRAIN_TILE * GRAIN_TILE) {
        // Squared so the distribution leans transparent: paper is smooth with texture in it,
        // not texture with smoothness in it.
        val n = random.nextFloat()
        val a = (n * n * alpha * 255f).toInt()
        (a shl 24) or rgb
    }

    repeat(FLECK_COUNT) {
        val index = random.nextInt(pixels.size)
        val a = ((FLECK_MIN + random.nextFloat() * FLECK_RANGE) * 255f).toInt().coerceAtMost(255)
        pixels[index] = (a shl 24) or rgb
    }

    return Bitmap.createBitmap(pixels, GRAIN_TILE, GRAIN_TILE, Bitmap.Config.ARGB_8888)
        .asImageBitmap()
}

private const val GRAIN_TILE = 96
private const val GRAIN_SEED = 0x2E1C0A
private const val FLECK_COUNT = 40
private const val FLECK_MIN = 0.05f
private const val FLECK_RANGE = 0.09f

private const val VIGNETTE_ALPHA = 0.28f
private const val VIGNETTE_CENTER_FRACTION = 0.38f
private const val VIGNETTE_RADIUS_FRACTION = 0.85f

private const val HAIRLINE_PX = 1.6f
private const val INNER_RULE_ALPHA = 0.45f
