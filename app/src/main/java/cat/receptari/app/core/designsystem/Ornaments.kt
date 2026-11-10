package cat.receptari.app.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.theme.ReceptariTextStyles
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The printer's marks and the hand-drawn ones: rules, section headings, the engraved sprigs
 * and the masthead (ADR-012). Sprigs are decoration and stay at the edges of things — beside a
 * heading, flanking the wordmark, in the corner of a card — never between lines of a recipe.
 */

/** A single hairline. The quiet separator, for rows within a section. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = ReceptariTheme.palette.rule,
    alpha: Float = 0.5f,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = RuleStroke,
        )
    }
}

/**
 * A section heading: an optional wax-red icon, the title in the display face, and [trailing]
 * on the same line at the right — the servings stepper beside "Ingredients", for instance.
 *
 * Headings with an icon stand on their own, the way the cookbook pages do; plain headings
 * keep a rule under them so a long form still reads as sections.
 */
@Composable
fun OrnamentHeading(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    showRule: Boolean = icon == null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
        if (showRule) Hairline(color = color, alpha = 0.35f)
    }
}

/**
 * An engraved olive sprig: a curved stem with alternating almond leaves, outlined in
 * [ReceptariTheme.palette.botanical] with a faint wash. It grows from the lower left towards
 * the upper right; [mirrored] grows it the other way, so a pair can flank something.
 *
 * The paths are built once per size in a draw cache: sprigs sit in list rows, and rebuilding
 * a dozen paths on every scrolled frame is exactly the kind of work that makes a list stutter.
 */
@Composable
fun Sprig(
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    color: Color = ReceptariTheme.palette.botanical,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val stroke = 1.1.dp.toPx()
            val sprig = sprigGeometry(size.width, size.height)
            val wash = color.copy(alpha = 0.16f)
            val rib = color.copy(alpha = 0.7f)
            val stemStyle = Stroke(width = stroke * 1.2f)
            val leafStyle = Stroke(width = stroke)
            onDrawBehind {
                scale(scaleX = if (mirrored) -1f else 1f, scaleY = 1f) {
                    drawPath(sprig.stem, color = color, style = stemStyle)
                    sprig.leaves.forEach { leaf ->
                        drawPath(leaf.outline, color = wash)
                        drawPath(leaf.outline, color = color, style = leafStyle)
                        drawLine(rib, leaf.ribStart, leaf.ribEnd, strokeWidth = stroke * 0.7f)
                    }
                }
            }
        },
    )
}

private class SprigLeaf(val outline: Path, val ribStart: Offset, val ribEnd: Offset)

private class SprigGeometry(val stem: Path, val leaves: List<SprigLeaf>)

private fun sprigGeometry(w: Float, h: Float): SprigGeometry {
    val start = Offset(w * 0.02f, h * 0.92f)
    val control = Offset(w * 0.55f, h * 0.95f)
    val end = Offset(w * 0.98f, h * 0.12f)

    fun point(t: Float) = Offset(
        (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x,
        (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y,
    )

    fun tangent(t: Float): Offset {
        val d = Offset(
            2 * (1 - t) * (control.x - start.x) + 2 * t * (end.x - control.x),
            2 * (1 - t) * (control.y - start.y) + 2 * t * (end.y - control.y),
        )
        val length = sqrt(d.x * d.x + d.y * d.y)
        return Offset(d.x / length, d.y / length)
    }

    val stem = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(control.x, control.y, end.x, end.y)
    }
    val leafLength = h * 0.46f
    val leaves = LeafStops.mapIndexed { index, t ->
        val side = if (index % 2 == 0) -1f else 1f
        val along = tangent(t)
        val angle = LeafAngle * side
        val direction = Offset(
            along.x * cos(angle) - along.y * sin(angle),
            along.x * sin(angle) + along.y * cos(angle),
        )
        leaf(point(t), direction, leafLength * (1f - 0.35f * t))
    } + leaf(end, tangent(1f), leafLength * 0.62f) // the terminal leaf carries on from the stem
    return SprigGeometry(stem, leaves)
}

private fun leaf(base: Offset, direction: Offset, length: Float): SprigLeaf {
    val tip = base + direction * length
    val mid = base + direction * (length * 0.5f)
    val normal = Offset(-direction.y, direction.x) * (length * LeafWidth)
    val outline = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(mid.x + normal.x, mid.y + normal.y, tip.x, tip.y)
        quadraticTo(mid.x - normal.x, mid.y - normal.y, base.x, base.y)
        close()
    }
    return SprigLeaf(outline, base, base + direction * (length * 0.8f))
}

/**
 * The app's name in script, thickened: the same text is drawn twice, once filled and once as
 * a thin outline, which gives Pinyon's hairlines enough weight to read at a glance without
 * swapping the face for a bolder one.
 *
 * [ReceptariTextStyles.Wordmark] is the only place the script face is allowed, and this is
 * the only place that style is used.
 */
@Composable
fun Wordmark(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = ReceptariTextStyles.Wordmark,
) {
    val outline = with(LocalDensity.current) { WordmarkOutline.toPx() }
    Box(modifier = modifier) {
        Text(text = text, style = style, color = color)
        Text(
            text = text,
            style = style.copy(drawStyle = Stroke(width = outline)),
            color = color,
        )
    }
}

/**
 * The book's emblem: the pot from the launcher icon, inked in the app's wax red with nothing
 * behind it, so the front page and the home-screen icon read as the same mark.
 */
@Composable
fun Emblem(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
        // The adaptive-icon foreground carries a wide safe-zone margin; scale past it.
        modifier = modifier.graphicsLayer {
            scaleX = EmblemZoom
            scaleY = EmblemZoom
        },
    )
}

/**
 * The front page's masthead, which folds itself into a running head as the page scrolls.
 *
 * It is one set of marks, not two headers: the emblem and the wordmark share a row, centred
 * on the page, and glide to the left of a compact bar, shrinking on the way, while the
 * tagline fades out. How far it has folded is read from [scrolledPx] — pixels the page has
 * scrolled past its top, or [Int.MAX_VALUE] once it is well down — in the layout phase only,
 * so scrolling re-places the marks without recomposing anything.
 *
 * The masthead overlays the page. Put a spacer of [onExpandedHeight]'s value at the top of
 * the list so the first row starts below it; until the page has scrolled the full fold
 * distance, the header's lower edge moves exactly with that spacer, so nothing ever slides
 * underneath the marks. The paper backing fades in only as the fold completes, when rows
 * start passing beneath the bar.
 */
@Composable
fun CollapsingMasthead(
    title: String,
    tagline: String,
    scrolledPx: () -> Int,
    onExpandedHeight: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val statusBars = WindowInsets.statusBars
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        content = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .paperGrain(intensity = 1f),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Hairline()
            }
            Emblem()
            Wordmark(text = title)
            Rubric(text = tagline, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(content = actions)
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val top = statusBars.getTop(this)
        val loose = Constraints(maxWidth = width)
        val emblemSize = MastheadEmblem.roundToPx()
        val emblemCompact = CompactEmblem.roundToPx()
        val gap = MastheadGap.roundToPx()
        val bar = CompactBarHeight.roundToPx()
        val inset = CompactInset.toPx()

        val emblem = measurables[1].measure(Constraints.fixed(emblemSize, emblemSize))
        val word = measurables[2].measure(loose)
        val tag = measurables[3].measure(loose)
        val actionRow = measurables[4].measure(loose)

        // Where everything sits when the page is at its top: emblem and wordmark in one
        // row, centred as a pair, the tagline under it.
        val rowTop = top + MastheadTop.roundToPx()
        val rowHeight = maxOf(emblemSize, word.height)
        val rowWidth = emblemSize + gap + word.width
        val tagTop = rowTop + rowHeight
        val expanded = tagTop + tag.height + MastheadBottom.roundToPx()
        val compact = top + bar
        onExpandedHeight(expanded)

        val range = (expanded - compact).coerceAtLeast(1)
        val fold = (scrolledPx().toFloat() / range).coerceIn(0f, 1f)
        val height = lerp(expanded.toFloat(), compact.toFloat(), fold).roundToInt()

        val emblemScale = lerp(1f, emblemCompact.toFloat() / emblemSize, fold)
        val emblemX = lerp((width - rowWidth) / 2f, inset, fold)
        val emblemY = lerp(rowTop + (rowHeight - emblemSize) / 2f, top + (bar - emblemCompact) / 2f, fold)

        val wordScale = lerp(1f, CompactWordmarkScale, fold)
        val wordX = lerp((width - rowWidth) / 2f + emblemSize + gap, inset + emblemCompact + gap, fold)
        val wordY = lerp(
            rowTop + (rowHeight - word.height) / 2f,
            top + (bar - word.height * CompactWordmarkScale) / 2f,
            fold,
        )

        // The tagline belongs to the full masthead only; it is gone by mid-fold.
        val ornamentAlpha = (1f - fold * 2f).coerceIn(0f, 1f)
        val backingAlpha = ((fold - BackingFadeStart) / (1f - BackingFadeStart)).coerceIn(0f, 1f)

        val backing = measurables[0].measure(Constraints.fixed(width, height))

        layout(width, height) {
            backing.placeWithLayer(0, 0) { alpha = backingAlpha }
            tag.placeWithLayer(
                x = (width - tag.width) / 2,
                y = tagTop - (expanded - height),
            ) { alpha = ornamentAlpha }
            emblem.placeWithLayer(emblemX.roundToInt(), emblemY.roundToInt()) {
                scaleX = emblemScale
                scaleY = emblemScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            word.placeWithLayer(wordX.roundToInt(), wordY.roundToInt()) {
                scaleX = wordScale
                scaleY = wordScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            actionRow.place(
                x = width - actionRow.width - CompactActionsInset.roundToPx(),
                y = top + (bar - actionRow.height) / 2,
            )
        }
    }
}

private val MastheadEmblem = 52.dp
private val CompactEmblem = MastheadEmblem
private val MastheadGap = 10.dp
private val MastheadTop = 6.dp
private val MastheadBottom = 10.dp
private val CompactBarHeight = 56.dp
private val CompactInset = 16.dp
private val CompactActionsInset = 4.dp
private const val CompactWordmarkScale = 30f / 46f
private const val BackingFadeStart = 0.8f

private const val RuleStroke = 1.5f
private const val LeafWidth = 0.32f
private const val LeafAngle = 0.85f
private val LeafStops = listOf(0.22f, 0.36f, 0.5f, 0.63f, 0.76f, 0.88f)
private val WordmarkOutline = 0.7.dp
private const val EmblemZoom = 1.9f
