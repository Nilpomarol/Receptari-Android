package cat.receptari.app.ui.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

@Composable
fun ImportRoute(
    onNavigateBack: () -> Unit,
    onCreateManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Phase 4 wires the three automatic sources to the shared import pipeline; manual
    // creation already goes straight to the editor.
    ImportScreen(
        onNavigateBack = onNavigateBack,
        onCreateManually = onCreateManually,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    onCreateManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ImportOption(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.import_manual),
                onClick = onCreateManually,
            )

            HorizontalDivider()

            // The three automatic sources arrive in Phase 4. They are listed but inert, so
            // the shape of the flow is visible without pretending to work.
            ImportOption(Icons.Default.Language, stringResource(R.string.import_from_website))
            ImportOption(Icons.Default.Image, stringResource(R.string.import_from_image))
            ImportOption(
                Icons.AutoMirrored.Filled.Notes,
                stringResource(R.string.import_from_text),
            )

            Text(
                text = stringResource(R.string.common_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ImportOption(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        headlineContent = { Text(label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        colors = ListItemDefaults.colors(
            headlineColor = if (onClick != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    )
}

@Preview
@Composable
private fun ImportScreenPreview() {
    ReceptariTheme(dynamicColor = false) {
        ImportScreen(onNavigateBack = {}, onCreateManually = {})
    }
}
