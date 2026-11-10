package cat.receptari.app.core.designsystem

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.intl.Locale
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
    Box(modifier = modifier) {
        // The page is painted into its own offscreen layer. Nothing in it changes while the
        // content scrolls, so the GPU keeps the rendered sheet and composites it, instead of
        // redrawing the grain, the mottling and the vignette on every frame.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .paperBackground(),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            content = content,
        )
    }
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
    Column(
        modifier = modifier
            // The top bar must be opaque while content scrolls beneath it, but it should
            // still read as the very same sheet rather than as a Material toolbar.
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f),
    ) {
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
 * A recipe card laid on the page: a fresh, lighter sheet with soft corners and one faint
 * rule, lifted by a hairline's worth of shadow (ADR-012).
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
        modifier = modifier.pageFrame(shape, ReceptariTheme.palette.rule.copy(alpha = 0.55f), inset = 0.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = CardShadow,
    ) {
        Box(
            modifier = Modifier
                // Inside the Surface, so the grain is clipped to the card's own shape.
                .paperGrain()
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

/**
 * A rubric: a short label printed in red capitals, the way old cookbooks mark the parts of
 * a page. Use it for labels that introduce something ("CARPETES", "PREPARACIÓ"), never for
 * actions.
 */
@Composable
fun Rubric(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = text.uppercase(Locale.current.platformLocale),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = color,
        modifier = modifier,
    )
}

/**
 * Particulars in the aside voice separated by middots: "45 min · 4 racions · Cuinada 3
 * vegades". Each part is already a whole translated phrase; the dot is a list separator, not
 * grammar, so no sentence is being assembled from fragments.
 */
@Composable
fun MetaLine(
    parts: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (parts.isEmpty()) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                Text(
                    text = "·", // i18n-exempt: list separator, not text
                    style = ReceptariTextStyles.Aside,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            Text(
                text = part,
                style = ReceptariTextStyles.Aside,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/**
 * Two or three views of the same thing, as a pill-shaped switch: the selected segment is
 * filled wax red, the rest sit on a sunk track.
 */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A small fact with its pictogram: "🕓 2 h 30 min", "👥 6 racions". */
@Composable
fun IconFact(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * The app's bottom navigation, printed on the same sheet as the page above it: a flat bar under
 * a hairline, small-caps tab names, and a wax-red ribbon bookmark that hangs from the rule over
 * the current tab and slides to the next. [selectedIndex] places the ribbon over one of
 * [itemCount] equal-width items.
 */
@Composable
fun PaperNavigationBar(
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .paperGrain(intensity = 1f),
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(NavigationBarHeight),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val itemWidth = maxWidth / itemCount
                val ribbonX by animateDpAsState(
                    targetValue = itemWidth * selectedIndex.coerceAtLeast(0) + (itemWidth - RibbonWidth) / 2,
                    label = "navRibbon",
                )
                Row(modifier = Modifier.fillMaxSize(), content = content)
                Ribbon(modifier = Modifier.offset(x = ribbonX))
            }
        }
    }
}

/** A bookmark ribbon: a wax-red strip with a swallow-tail cut into its lower end. */
@Composable
private fun Ribbon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(width = RibbonWidth, height = RibbonHeight)
            .drawBehind {
                val tail = size.height * 0.35f
                drawPath(
                    path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2f, size.height - tail)
                        lineTo(0f, size.height)
                        close()
                    },
                    color = color,
                )
            },
    )
}

/** One tab of [PaperNavigationBar]: an ink icon over a small-caps name, both wax red when current. */
@Composable
fun RowScope.PaperNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val ink by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navItemInk",
    )
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            // Below the ribbon, which hangs over the top of the tab.
            .padding(top = RibbonHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** The page's one action, floating over it: a wax seal, a red disc with a ring pressed into it. */
@Composable
fun PaperActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(SealSize)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(4.dp)
            .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private val NavigationBarHeight = 68.dp
private val RibbonWidth = 16.dp
private val RibbonHeight = 14.dp
private val SealSize = 56.dp

private const val MaxRating = 5
private val CardShadow: Dp = 1.dp
