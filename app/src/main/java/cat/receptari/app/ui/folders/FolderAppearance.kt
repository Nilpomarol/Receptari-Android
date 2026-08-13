package cat.receptari.app.ui.folders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.paperFieldColors
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.model.FolderColor
import cat.receptari.app.domain.model.FolderIcon

@Composable
fun FolderColor.toColor(): Color = when (this) {
    FolderColor.OLIVE -> MaterialTheme.colorScheme.primary
    FolderColor.TERRACOTTA -> MaterialTheme.colorScheme.secondary
    FolderColor.GOLD -> ReceptariTheme.palette.gold
    FolderColor.CLARET -> ReceptariTheme.palette.heart
    FolderColor.FOREST -> ReceptariTheme.palette.folderForest
    FolderColor.INDIGO -> ReceptariTheme.palette.folderIndigo
    FolderColor.COCOA -> ReceptariTheme.palette.folderCocoa
    FolderColor.SLATE -> ReceptariTheme.palette.folderSlate
}

@Composable
private fun FolderColor.label(): String = stringResource(
    when (this) {
        FolderColor.OLIVE -> R.string.folder_color_olive
        FolderColor.TERRACOTTA -> R.string.folder_color_terracotta
        FolderColor.GOLD -> R.string.folder_color_gold
        FolderColor.CLARET -> R.string.folder_color_claret
        FolderColor.FOREST -> R.string.folder_color_forest
        FolderColor.INDIGO -> R.string.folder_color_indigo
        FolderColor.COCOA -> R.string.folder_color_cocoa
        FolderColor.SLATE -> R.string.folder_color_slate
    },
)

fun FolderIcon.toImageVector(): ImageVector = when (this) {
    FolderIcon.FOLDER -> Icons.Outlined.Folder
    FolderIcon.RESTAURANT -> Icons.Outlined.Restaurant
    FolderIcon.CAKE -> Icons.Outlined.Cake
    FolderIcon.COOKIE -> Icons.Outlined.Cookie
    FolderIcon.PIZZA -> Icons.Outlined.LocalPizza
    FolderIcon.ICE_CREAM -> Icons.Outlined.Icecream
    FolderIcon.BAKERY -> Icons.Outlined.BakeryDining
    FolderIcon.COFFEE -> Icons.Outlined.LocalCafe
    FolderIcon.SOUP -> Icons.Outlined.SoupKitchen
    FolderIcon.GRILL -> Icons.Outlined.OutdoorGrill
    FolderIcon.SEAFOOD -> Icons.Outlined.SetMeal
    FolderIcon.VEGETARIAN -> Icons.Outlined.Eco
    FolderIcon.CELEBRATION -> Icons.Outlined.Celebration
    FolderIcon.FAMILY -> Icons.Outlined.FamilyRestroom
}

@Composable
private fun FolderIcon.label(): String = stringResource(
    when (this) {
        FolderIcon.FOLDER -> R.string.folder_icon_folder
        FolderIcon.RESTAURANT -> R.string.folder_icon_restaurant
        FolderIcon.CAKE -> R.string.folder_icon_cake
        FolderIcon.COOKIE -> R.string.folder_icon_cookie
        FolderIcon.PIZZA -> R.string.folder_icon_pizza
        FolderIcon.ICE_CREAM -> R.string.folder_icon_ice_cream
        FolderIcon.BAKERY -> R.string.folder_icon_bakery
        FolderIcon.COFFEE -> R.string.folder_icon_coffee
        FolderIcon.SOUP -> R.string.folder_icon_soup
        FolderIcon.GRILL -> R.string.folder_icon_grill
        FolderIcon.SEAFOOD -> R.string.folder_icon_seafood
        FolderIcon.VEGETARIAN -> R.string.folder_icon_vegetarian
        FolderIcon.CELEBRATION -> R.string.folder_icon_celebration
        FolderIcon.FAMILY -> R.string.folder_icon_family
    },
)

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
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    Column(modifier = modifier.fillMaxWidth()) {
        FolderStampPreview(name = name, color = color, icon = icon)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = paperFieldColors(),
            placeholder = { Text(stringResource(R.string.folders_add_hint)) },
            singleLine = true,
        )

        Text(
            text = stringResource(R.string.folder_color_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(FolderColor.entries, key = { it.name }) { candidate ->
                ColorStamp(
                    color = candidate,
                    selected = candidate == color,
                    onClick = { onColorChange(candidate) },
                )
            }
        }

        Text(
            text = stringResource(R.string.folder_icon_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 5,
        ) {
            FolderIcon.entries.forEach { candidate ->
                IconStamp(
                    icon = candidate,
                    accent = color,
                    selected = candidate == icon,
                    onClick = { onIconChange(candidate) },
                )
            }
        }
    }
}

@Composable
private fun FolderStampPreview(
    name: String,
    color: FolderColor,
    icon: FolderIcon,
    modifier: Modifier = Modifier,
) {
    val accent = color.toColor()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.09f).compositeOver(MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accent),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon.toImageVector(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = stringResource(R.string.folder_preview_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = name.ifBlank { stringResource(R.string.folder_preview_untitled) },
                    style = MaterialTheme.typography.titleLarge,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ColorStamp(
    color: FolderColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = color.toColor()
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .width(92.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) {
            accent.copy(alpha = 0.13f).compositeOver(MaterialTheme.colorScheme.surface)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else ReceptariTheme.palette.rule),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = color.label(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun IconStamp(
    icon: FolderIcon,
    accent: FolderColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = accent.toColor()
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .size(52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) {
            accentColor.copy(alpha = 0.14f).compositeOver(MaterialTheme.colorScheme.surface)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else ReceptariTheme.palette.rule,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon.toImageVector(),
                contentDescription = icon.label(),
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}
