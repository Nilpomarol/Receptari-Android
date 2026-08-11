package cat.receptari.app.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cat.receptari.app.core.designsystem.theme.ReceptariTextStyles
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

/**
 * The pieces every screen is built from. Between these and the theme, a screen should not
 * need to name a colour or a border of its own.
 */

/**
 * A [Scaffold] on paper.
 *
 * Every container is transparent so the page shows through — a Scaffold left to its own
 * defaults paints `colorScheme.background` over the grain and the vignette, which is exactly
 * the bug this wrapper exists to make impossible.
 */
@Composable
fun PaperScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.paperBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/**
 * The standard top bar: a centred title in the display face, over a hairline.
 *
 * The rule is what stops the bar dissolving into the page once the content scrolls under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    showRule: Boolean = true,
) {
    Column(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = transparentTopBarColors(),
        )
        if (showRule) Hairline()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentTopBarColors(): TopAppBarColors =
    TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

/**
 * A sheet laid on the page: lighter than the paper, inside a printed double rule.
 *
 * Elevation is kept to a hairline's worth. A card that floats belongs to a different design
 * language; this one is glued in.
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.pageFrame(shape, ReceptariTheme.palette.rule),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = CardShadow,
    ) {
        Box(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
        ) {
            content()
        }
    }
}

/**
 * The filter and sort controls along the top of the library.
 *
 * Selected is filled ink-on-olive; unselected is a bare outline, so a row of them reads as a
 * printed index rather than as a toolbar.
 */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    val palette = ReceptariTheme.palette
    Surface(
        // Null onClick means this is a label, not a control. Making it selectable anyway
        // would have TalkBack offer a button that does nothing.
        modifier = if (onClick == null) {
            modifier
        } else {
            modifier.selectable(selected = selected, role = Role.Button, onClick = onClick)
        },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else palette.rule,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Text fields as ruled boxes on the page.
 *
 * Material's outlined field is the right shape already; it just arrives in the wrong ink.
 * Passing these colours everywhere keeps the editor and the import screens from drifting
 * into two different-looking forms.
 */
@Composable
fun paperFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = ReceptariTheme.palette.rule,
    disabledBorderColor = ReceptariTheme.palette.rule.copy(alpha = 0.4f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
)

/**
 * A recipe's rating out of five, in gold.
 *
 * Always five stars. Showing only the earned ones makes a three-star recipe look like a
 * different, shorter widget than a five-star one, and the eye can no longer compare a column
 * of them at a glance.
 */
@Composable
fun StarRating(
    rating: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    starSize: Dp = 15.dp,
) {
    val gold = ReceptariTheme.palette.gold
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        repeat(MaxRating) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                // Only the first star carries the label, so a screen reader announces the
                // rating once rather than five times.
                contentDescription = contentDescription.takeIf { index == 0 },
                tint = if (index < rating) gold else gold.copy(alpha = 0.35f),
                modifier = Modifier.size(starSize),
            )
        }
    }
}

/** A small caption in the aside voice: sources, notices, footnotes. */
@Composable
fun Aside(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        style = ReceptariTextStyles.Aside,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** A section name inside a recipe — an ingredient group, a stage of the method. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        modifier = modifier,
    )
}

private const val MaxRating = 5
private val CardShadow: Dp = 1.dp
