package cat.receptari.app.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentalDivider
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import cat.receptari.app.core.designsystem.theme.ReceptariTheme

@Composable
fun ImportRoute(
    onNavigateBack: () -> Unit,
    onCreateManually: () -> Unit,
    onImportFromText: () -> Unit,
    onImportFromWebsite: () -> Unit,
    onImportFromImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImportScreen(
        onNavigateBack = onNavigateBack,
        onCreateManually = onCreateManually,
        onImportFromText = onImportFromText,
        onImportFromWebsite = onImportFromWebsite,
        onImportFromImage = onImportFromImage,
        modifier = modifier,
    )
}

@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    onCreateManually: () -> Unit,
    onImportFromText: () -> Unit,
    onImportFromWebsite: () -> Unit,
    onImportFromImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperScaffold(
        modifier = modifier,
        topBar = {
            PaperTopBar(
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
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_where_from),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            OrnamentalDivider(modifier = Modifier.padding(bottom = 4.dp))

            ImportOption(
                icon = Icons.AutoMirrored.Outlined.Notes,
                label = stringResource(R.string.import_from_text),
                onClick = onImportFromText,
            )
            ImportOption(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.import_from_website),
                onClick = onImportFromWebsite,
            )
            ImportOption(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.import_from_image),
                onClick = onImportFromImage,
            )

            OrnamentalDivider(modifier = Modifier.padding(vertical = 4.dp))

            ImportOption(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.import_manual),
                onClick = onCreateManually,
            )

            Aside(
                text = stringResource(R.string.import_review_notice),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

/** One way into the editor. All four end in the same place, so all four look the same. */
@Composable
private fun ImportOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ImportScreenPreview() {
    ReceptariTheme {
        ImportScreen(
            onNavigateBack = {},
            onCreateManually = {},
            onImportFromText = {},
            onImportFromWebsite = {},
            onImportFromImage = {},
        )
    }
}
