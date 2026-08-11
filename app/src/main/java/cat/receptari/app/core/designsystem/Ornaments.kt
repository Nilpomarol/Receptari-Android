package cat.receptari.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.receptari.app.core.designsystem.theme.ReceptariTextStyles
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

/**
 * The printer's marks. Purely decorative, so everything here is drawn rather than typed —
 * a fleuron glyph would mean shipping a fourth font and hoping it renders the same
 * everywhere.
 */

/** A rule broken by a centred fleuron. Separates the major parts of a page. */
@Composable
fun OrnamentalDivider(
    modifier: Modifier = Modifier,
    color: Color = ReceptariTheme.palette.rule,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(DividerHeight),
    ) {
        val midY = size.height / 2f
        val midX = size.width / 2f
        val gap = FleuronGap.toPx()

        drawLine(color, Offset(0f, midY), Offset(midX - gap, midY), RuleStroke)
        drawLine(color, Offset(midX + gap, midY), Offset(size.width, midY), RuleStroke)

        val dotOffset = FleuronDotOffset.toPx()
        drawCircle(color, DotRadius.toPx(), Offset(midX - dotOffset, midY))
        drawCircle(color, DotRadius.toPx(), Offset(midX + dotOffset, midY))

        drawPath(diamond(midX, midY, DiamondRadius.toPx()), color)
    }
}

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
 * A heading set between two rules — "— Ingredients —".
 *
 * The rules take whatever width is left over, so the heading stays optically centred at any
 * translation length. That matters here: `Ingredients` is one word in Catalan and two in
 * English on a narrow screen.
 */
@Composable
fun OrnamentHeading(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaperedRule(modifier = Modifier.weight(1f), pointsRight = true)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        TaperedRule(modifier = Modifier.weight(1f), pointsRight = false)
    }
}

/**
 * The masthead: the app's name in script over a flourish.
 *
 * [ReceptariTextStyles.Wordmark] is the only place the script face is allowed, and this is
 * the only place that style is used.
 */
@Composable
fun Wordmark(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = text,
            style = ReceptariTextStyles.Wordmark,
            color = color,
            textAlign = TextAlign.Center,
        )
        OrnamentalDivider(
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

/** A rule that thins to a point at one end, so it reads as pointing at the heading. */
@Composable
private fun TaperedRule(modifier: Modifier = Modifier, pointsRight: Boolean) {
    val color = ReceptariTheme.palette.rule
    Canvas(modifier = modifier.height(DividerHeight)) {
        val midY = size.height / 2f
        val thick = RuleStroke * 1.6f
        val path = Path().apply {
            if (pointsRight) {
                moveTo(0f, midY - thick / 2f)
                lineTo(size.width, midY - RuleStroke / 4f)
                lineTo(size.width, midY + RuleStroke / 4f)
                lineTo(0f, midY + thick / 2f)
            } else {
                moveTo(size.width, midY - thick / 2f)
                lineTo(0f, midY - RuleStroke / 4f)
                lineTo(0f, midY + RuleStroke / 4f)
                lineTo(size.width, midY + thick / 2f)
            }
            close()
        }
        drawPath(path, color)
    }
}

private fun diamond(centerX: Float, centerY: Float, radius: Float): Path = Path().apply {
    moveTo(centerX, centerY - radius)
    lineTo(centerX + radius * DiamondWaist, centerY)
    lineTo(centerX, centerY + radius)
    lineTo(centerX - radius * DiamondWaist, centerY)
    close()
}

private val DividerHeight: Dp = 16.dp
private val FleuronGap: Dp = 20.dp
private val FleuronDotOffset: Dp = 12.dp
private val DotRadius: Dp = 1.8.dp
private val DiamondRadius: Dp = 4.5.dp

private const val DiamondWaist = 0.6f
private const val RuleStroke = 1.5f
