package cat.receptari.app.core.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * The page the whole app is printed on.
 *
 * Paper is built here at two scales, and which layer goes at which scale is the whole design:
 *
 * - **Sheet scale**, baked into one seamless 256 px tile — fine value-noise grain, short
 *   pulp fibres, and the rust-coloured specks of foxing. All of it is high-frequency on
 *   purpose. Anything coarse in the tile becomes a *motif*, and a motif laid across a screen
 *   four times over is unmistakably wallpaper. An earlier version put the broad mottling in
 *   the tile and the repeat was the first thing you saw.
 * - **Page scale**, drawn against the composable's own size so it cannot repeat — the cloudy
 *   variation in tone, a vignette, and the shadow of the binding down the inner edge.
 *
 * The tile is baked once at full strength, so how strongly the paper prints is just the
 * alpha it is drawn with. One bitmap serves the page, the cards, and anything else that
 * wants a surface.
 */
@Composable
fun Modifier.paperBackground(): Modifier {
    val base = MaterialTheme.colorScheme.background
    val palette = ReceptariTheme.palette
    val grain = rememberPaperTexture(palette.paperEdge)
    val edge = palette.paperEdge
    val intensity = palette.paperIntensity

    return this.drawWithCache {
        // Centred above the middle: light falls on a page from above, and a perfectly
        // centred vignette reads as a lens effect rather than as ageing.
        val vignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, edge.copy(alpha = VignetteAlpha)),
            center = Offset(size.width / 2f, size.height * VignetteCenterFraction),
            radius = max(size.width, size.height) * VignetteRadiusFraction,
        )

        // The gutter: a bound page is darker where it disappears into the spine.
        val gutterWidth = GutterWidth.toPx()
        val gutter = Brush.horizontalGradient(
            colors = listOf(edge.copy(alpha = GutterAlpha), Color.Transparent),
            startX = 0f,
            endX = gutterWidth,
        )
        val outerEdge = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, edge.copy(alpha = GutterAlpha * 0.5f)),
            startX = size.width - gutterWidth * 0.6f,
            endX = size.width,
        )

        val mottling = Mottles.map { mottle ->
            Brush.radialGradient(
                colors = listOf(edge.copy(alpha = mottle.alpha), Color.Transparent),
                center = Offset(size.width * mottle.x, size.height * mottle.y),
                radius = size.minDimension * mottle.radius,
            )
        }

        onDrawBehind {
            drawRect(base)
            drawRect(grain, alpha = intensity)
            mottling.forEach { drawRect(it) }
            drawRect(gutter)
            drawRect(outerEdge)
            drawRect(vignette)
        }
    }
}

/**
 * Just the sheet texture, with none of the page-scale marks.
 *
 * For cards and other surfaces laid *on* the page: they are paper too, but a fresh sheet
 * rather than the aged page underneath, so they get the grain at a fraction of the strength
 * and no vignette, stain or gutter.
 */
@Composable
fun Modifier.paperGrain(intensity: Float = 0.55f): Modifier {
    val palette = ReceptariTheme.palette
    val grain = rememberPaperTexture(palette.paperEdge)
    val alpha = palette.paperIntensity * intensity

    return this.drawWithCache {
        onDrawBehind { drawRect(grain, alpha = alpha) }
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
        drawOutline(outline, color = color, style = Stroke(width = HairlinePx))
        innerOutline?.let {
            translate(insetPx, insetPx) {
                drawOutline(it, color = color.copy(alpha = InnerRuleAlpha), style = Stroke(HairlinePx))
            }
        }
    }
}

@Composable
private fun rememberPaperTexture(color: Color): Brush =
    remember(color) {
        ShaderBrush(ImageShader(paperTile(color), TileMode.Repeated, TileMode.Repeated))
    }

/**
 * One seamless tile of paper, baked at full strength.
 *
 * The seed is fixed, so the sheet is the same on every launch. A page that re-speckles itself
 * looks like a rendering bug rather than like paper.
 */
private fun paperTile(color: Color): ImageBitmap {
    val random = Random(PaperSeed)
    val rgb = color.toArgb() and 0x00FFFFFF

    // Amplitudes sum to 1, so the combined field still spans roughly 0..1 and the curve
    // below behaves the same however many octaves are used. Every grid is fine: see the
    // note on the file's KDoc about what a coarse octave does to a tiled texture.
    val octaves = listOf(
        NoiseField(grid = 24, random = random) to 0.45f,
        NoiseField(grid = 56, random = random) to 0.33f,
        NoiseField(grid = 128, random = random) to 0.22f,
    )

    val pixels = IntArray(TileSize * TileSize)
    for (y in 0 until TileSize) {
        val row = y * TileSize
        for (x in 0 until TileSize) {
            var value = 0f
            for ((field, amplitude) in octaves) {
                val scale = field.grid.toFloat() / TileSize
                value += amplitude * field.sample(x * scale, y * scale)
            }
            // Squared so the distribution leans transparent: paper is smooth with texture in
            // it, not texture with smoothness in it.
            val alpha = (value * value * GrainStrength * 255f).toInt().coerceIn(0, 255)
            pixels[row + x] = (alpha shl 24) or rgb
        }
    }

    val bitmap = Bitmap.createBitmap(TileSize, TileSize, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, TileSize, 0, 0, TileSize, TileSize)

    val canvas = Canvas(bitmap)
    drawFibres(canvas, random, rgb)
    drawFoxing(canvas, random, rgb)

    return bitmap.asImageBitmap()
}

/**
 * Short threads of pulp, drawn nine times each so any fibre crossing an edge reappears on
 * the opposite one and the tile stays seamless.
 */
private fun drawFibres(canvas: Canvas, random: Random, rgb: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }

    repeat(FibreCount) {
        val x = random.nextFloat() * TileSize
        val y = random.nextFloat() * TileSize
        val angle = random.nextFloat() * TWO_PI
        val length = FibreMinLength + random.nextFloat() * FibreLengthRange
        val endX = x + kotlin.math.cos(angle) * length
        val endY = y + kotlin.math.sin(angle) * length
        paint.color = rgb or (((FibreMinAlpha + random.nextFloat() * FibreAlphaRange) * 255f).toInt() shl 24)

        for (dx in -1..1) {
            for (dy in -1..1) {
                val offsetX = dx * TileSize.toFloat()
                val offsetY = dy * TileSize.toFloat()
                canvas.drawLine(x + offsetX, y + offsetY, endX + offsetX, endY + offsetY, paint)
            }
        }
    }
}

/** The small rust-coloured specks an old page picks up. Wrapped the same way as the fibres. */
private fun drawFoxing(canvas: Canvas, random: Random, rgb: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    repeat(FoxingCount) {
        val x = random.nextFloat() * TileSize
        val y = random.nextFloat() * TileSize
        val radius = FoxingMinRadius + random.nextFloat() * FoxingRadiusRange
        paint.color = rgb or (((FoxingMinAlpha + random.nextFloat() * FoxingAlphaRange) * 255f).toInt() shl 24)

        for (dx in -1..1) {
            for (dy in -1..1) {
                canvas.drawCircle(x + dx * TileSize, y + dy * TileSize, radius, paint)
            }
        }
    }
}

/**
 * Tileable value noise: a random lattice sampled with smooth interpolation.
 *
 * Lattice lookups wrap, which is the whole trick — it means the field is periodic over [grid]
 * cells, so sampling it across exactly one period produces a texture whose left edge matches
 * its right and whose top matches its bottom.
 */
private class NoiseField(val grid: Int, random: Random) {
    private val values = FloatArray(grid * grid) { random.nextFloat() }

    private fun at(x: Int, y: Int): Float = values[y.mod(grid) * grid + x.mod(grid)]

    /** [u] and [v] are in lattice units; any value is valid and the field repeats. */
    fun sample(u: Float, v: Float): Float {
        val x0 = floor(u).toInt()
        val y0 = floor(v).toInt()
        val fx = smoothStep(u - x0)
        val fy = smoothStep(v - y0)

        val top = lerp(at(x0, y0), at(x0 + 1, y0), fx)
        val bottom = lerp(at(x0, y0 + 1), at(x0 + 1, y0 + 1), fx)
        return lerp(top, bottom, fy)
    }

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

/**
 * A soft patch of tone, positioned as a fraction of the page so it never tiles.
 *
 * Deliberately irregular in size and spacing. Evenly placed patches of equal weight read as
 * a manufactured texture; an aged sheet is blotchy in a way that has no rhythm to it.
 */
private data class Mottle(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private val Mottles = listOf(
    Mottle(x = 0.14f, y = 0.05f, radius = 0.50f, alpha = 0.075f),
    Mottle(x = 0.90f, y = 0.17f, radius = 0.38f, alpha = 0.070f),
    Mottle(x = 0.48f, y = 0.30f, radius = 0.62f, alpha = 0.038f),
    Mottle(x = 0.03f, y = 0.47f, radius = 0.34f, alpha = 0.080f),
    Mottle(x = 0.97f, y = 0.61f, radius = 0.42f, alpha = 0.065f),
    Mottle(x = 0.29f, y = 0.73f, radius = 0.50f, alpha = 0.055f),
    Mottle(x = 0.81f, y = 0.89f, radius = 0.40f, alpha = 0.075f),
    Mottle(x = 0.56f, y = 0.54f, radius = 0.20f, alpha = 0.050f),
    Mottle(x = 0.21f, y = 0.33f, radius = 0.15f, alpha = 0.055f),
)

private const val TileSize = 256
private const val PaperSeed = 0x2E1C0A
private const val GrainStrength = 0.75f
private const val TWO_PI = 6.2831855f

private const val FibreCount = 150
private const val FibreMinLength = 3f
private const val FibreLengthRange = 16f
private const val FibreMinAlpha = 0.10f
private const val FibreAlphaRange = 0.22f

private const val FoxingCount = 44
private const val FoxingMinRadius = 0.6f
private const val FoxingRadiusRange = 2f
private const val FoxingMinAlpha = 0.16f
private const val FoxingAlphaRange = 0.34f

private const val VignetteAlpha = 0.30f
private const val VignetteCenterFraction = 0.38f
private const val VignetteRadiusFraction = 0.85f

private val GutterWidth: Dp = 28.dp
private const val GutterAlpha = 0.22f

private const val HairlinePx = 1.6f
private const val InnerRuleAlpha = 0.45f
