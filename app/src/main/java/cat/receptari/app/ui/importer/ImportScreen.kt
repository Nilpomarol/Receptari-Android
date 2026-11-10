package cat.receptari.app.ui.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperModalBottomSheet

/**
 * "Where does this recipe come from?" as a sheet over the library rather than a screen of its
 * own: it is a four-way choice, and each choice leads to a real screen.
 *
 * Receiving from another Receptari is deliberately not here. It is rare, it needs the other
 * person's phone, and opening a shared file already goes straight to the transfer import, so
 * it lives in Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    onDismiss: () -> Unit,
    onImportFromWebsite: () -> Unit,
    onImportFromText: () -> Unit,
    onImportFromImage: () -> Unit,
    onCreateManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun choose(action: () -> Unit): () -> Unit = {
        onDismiss()
        action()
    }

    PaperModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            OrnamentHeading(title = stringResource(R.string.import_where_from))
            AddOption(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.import_from_website),
                onClick = choose(onImportFromWebsite),
            )
            Hairline()
            AddOption(
                icon = Icons.AutoMirrored.Outlined.Notes,
                label = stringResource(R.string.import_from_text),
                onClick = choose(onImportFromText),
            )
            Hairline()
            AddOption(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.import_from_image),
                onClick = choose(onImportFromImage),
            )
            Hairline()
            AddOption(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.import_manual),
                onClick = choose(onCreateManually),
            )
            Aside(
                text = stringResource(R.string.import_review_notice),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun AddOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
