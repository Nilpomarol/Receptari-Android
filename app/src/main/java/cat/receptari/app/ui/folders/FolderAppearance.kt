package cat.receptari.app.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.BreakfastDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.IconDisc
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.Rubric
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon

@Composable
fun FolderColor.toColor(): Color = when (this) {
    FolderColor.CLARET -> ReceptariTheme.palette.heart
    FolderColor.TERRACOTTA -> ReceptariTheme.palette.folderTerracotta
    FolderColor.GOLD -> ReceptariTheme.palette.gold
    FolderColor.OLIVE -> ReceptariTheme.palette.folderOlive
    FolderColor.FOREST -> ReceptariTheme.palette.folderForest
    FolderColor.TEAL -> ReceptariTheme.palette.folderTeal
    FolderColor.INDIGO -> ReceptariTheme.palette.folderIndigo
    FolderColor.PLUM -> ReceptariTheme.palette.folderPlum
    FolderColor.COCOA -> ReceptariTheme.palette.folderCocoa
    FolderColor.SLATE -> ReceptariTheme.palette.folderSlate
}

@Composable
private fun FolderColor.label(): String = stringResource(
    when (this) {
        FolderColor.CLARET -> R.string.folder_color_claret
        FolderColor.TERRACOTTA -> R.string.folder_color_terracotta
        FolderColor.GOLD -> R.string.folder_color_gold
        FolderColor.OLIVE -> R.string.folder_color_olive
        FolderColor.FOREST -> R.string.folder_color_forest
        FolderColor.TEAL -> R.string.folder_color_teal
        FolderColor.INDIGO -> R.string.folder_color_indigo
        FolderColor.PLUM -> R.string.folder_color_plum
        FolderColor.COCOA -> R.string.folder_color_cocoa
        FolderColor.SLATE -> R.string.folder_color_slate
    },
)

fun FolderIcon.toImageVector(): ImageVector = when (this) {
    FolderIcon.FOLDER -> Icons.Outlined.Folder
    FolderIcon.RESTAURANT -> Icons.Outlined.Restaurant
    FolderIcon.FAMILY -> Icons.Outlined.FamilyRestroom
    FolderIcon.CELEBRATION -> Icons.Outlined.Celebration
    FolderIcon.VEGETARIAN -> Icons.Outlined.Eco
    FolderIcon.BREAKFAST -> Icons.Outlined.BreakfastDining
    FolderIcon.SOUP -> Icons.Outlined.SoupKitchen
    FolderIcon.RAMEN -> Icons.Outlined.RamenDining
    FolderIcon.PIZZA -> Icons.Outlined.LocalPizza
    FolderIcon.LUNCH -> Icons.Outlined.LunchDining
    FolderIcon.GRILL -> Icons.Outlined.OutdoorGrill
    FolderIcon.SEAFOOD -> Icons.Outlined.SetMeal
    FolderIcon.BAKERY -> Icons.Outlined.BakeryDining
    FolderIcon.CAKE -> Icons.Outlined.Cake
    FolderIcon.COOKIE -> Icons.Outlined.Cookie
    FolderIcon.ICE_CREAM -> Icons.Outlined.Icecream
    FolderIcon.COFFEE -> Icons.Outlined.LocalCafe
    FolderIcon.WINE -> Icons.Outlined.WineBar
}

@Composable
private fun FolderIcon.label(): String = stringResource(
    when (this) {
        FolderIcon.FOLDER -> R.string.folder_icon_folder
        FolderIcon.RESTAURANT -> R.string.folder_icon_restaurant
        FolderIcon.FAMILY -> R.string.folder_icon_family
        FolderIcon.CELEBRATION -> R.string.folder_icon_celebration
        FolderIcon.VEGETARIAN -> R.string.folder_icon_vegetarian
        FolderIcon.BREAKFAST -> R.string.folder_icon_breakfast
        FolderIcon.SOUP -> R.string.folder_icon_soup
        FolderIcon.RAMEN -> R.string.folder_icon_ramen
        FolderIcon.PIZZA -> R.string.folder_icon_pizza
        FolderIcon.LUNCH -> R.string.folder_icon_lunch
        FolderIcon.GRILL -> R.string.folder_icon_grill
        FolderIcon.SEAFOOD -> R.string.folder_icon_seafood
        FolderIcon.BAKERY -> R.string.folder_icon_bakery
        FolderIcon.CAKE -> R.string.folder_icon_cake
        FolderIcon.COOKIE -> R.string.folder_icon_cookie
        FolderIcon.ICE_CREAM -> R.string.folder_icon_ice_cream
        FolderIcon.COFFEE -> R.string.folder_icon_coffee
        FolderIcon.WINE -> R.string.folder_icon_wine
    },
)

private const val ColorsPerRow = 5
private const val IconsPerRow = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderEditorSheet(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    color: FolderColor,
    onColorChange: (FolderColor) -> Unit,
    icon: FolderIcon,
    onIconChange: (FolderIcon) -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            OrnamentHeading(title = title)
            FolderAppearanceFields(
                name = name,
                onNameChange = onNameChange,
                color = color,
                onColorChange = onColorChange,
                icon = icon,
                onIconChange = onIconChange,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

/**
 * The folder's disc beside its name, so the pictogram and ink show as they are chosen, then the
 * inks and the pictograms in plain grids — no boxes, only the current choice is marked.
 */
@Composable
fun FolderAppearanceFields(
    name: String,
    onNameChange: (String) -> Unit,
    color: FolderColor,
    onColorChange: (FolderColor) -> Unit,
    icon: FolderIcon,
    onIconChange: (FolderIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = color.toColor()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconDisc(
                icon = icon.toImageVector(),
                size = 64.dp,
                container = accent.copy(alpha = 0.16f),
                content = accent,
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                colors = paperFieldColors(),
                placeholder = { Text(stringResource(R.string.folders_add_hint)) },
                singleLine = true,
            )
        }

        Rubric(
            text = stringResource(R.string.folder_color_label),
            modifier = Modifier.padding(top = 24.dp, bottom = 6.dp),
        )
        EvenGrid(items = FolderColor.entries, perRow = ColorsPerRow) { candidate ->
            ColorDot(color = candidate, selected = candidate == color, onClick = { onColorChange(candidate) })
        }

        Rubric(
            text = stringResource(R.string.folder_icon_label),
            modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        )
        EvenGrid(items = FolderIcon.entries, perRow = IconsPerRow) { candidate ->
            IconChoice(icon = candidate, accent = accent, selected = candidate == icon, onClick = { onIconChange(candidate) })
        }
    }
}

/** Rows of [perRow] equal cells, so the choices line up whatever the screen's width. */
@Composable
private fun <T> EvenGrid(
    items: List<T>,
    perRow: Int,
    cell: @Composable (T) -> Unit,
) {
    items.chunked(perRow).forEach { rowItems ->
        Row(modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { item ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { cell(item) }
            }
            repeat(perRow - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

/** An ink as a round dot; the chosen one gets a ring and a tick. */
@Composable
private fun ColorDot(
    color: FolderColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = color.toColor()
    val label = color.label()
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(46.dp).border(2.dp, accent, CircleShape))
        }
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** A pictogram on the bare page; the chosen one is tinted in the folder's ink on a soft disc. */
@Composable
private fun IconChoice(
    icon: FolderIcon,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon.toImageVector(),
            contentDescription = icon.label(),
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp),
        )
    }
}
