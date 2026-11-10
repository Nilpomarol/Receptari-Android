package cat.receptari.app.ui.importer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.IconDisc
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.PaperScaffold
import cat.receptari.app.core.designsystem.PaperTopBar
import kotlinx.coroutines.launch

/**
 * The page the three extracting imports share: a wax-tinted disc and the source's name, what
 * Claude will do with it, the source's own controls, and one wax-red pill to start extracting
 * over the notice that nothing is saved without review. Only the controls in [content] differ.
 */
@Composable
internal fun ImportForm(
    icon: ImageVector,
    title: String,
    explanation: String,
    isExtracting: Boolean,
    canExtract: Boolean,
    onExtract: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable ColumnScope.() -> Unit,
) {
    PaperScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PaperTopBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                showRule = false,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(start = PageMargin, end = PageMargin, top = 8.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(icon = icon, size = 48.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 20.dp),
                content = content,
            )

            ExtractFooter(
                isExtracting = isExtracting,
                canExtract = canExtract,
                onExtract = onExtract,
            )
        }
    }
}

/**
 * A source's own sheet over the library: for the imports that are a single step, so a whole
 * page would be mostly empty. Same parts as [ImportForm], in the add sheet's margins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSheet(
    icon: ImageVector,
    title: String,
    explanation: String,
    isExtracting: Boolean,
    canExtract: Boolean,
    onExtract: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable ColumnScope.() -> Unit,
) {
    PaperModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)
                .imePadding(),
        ) {
            OrnamentHeading(title = title, icon = icon)
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )
            content()
            ExtractFooter(
                isExtracting = isExtracting,
                canExtract = canExtract,
                onExtract = onExtract,
                modifier = Modifier.padding(top = 20.dp),
            )
            SnackbarHost(snackbarHostState)
        }
    }
}

/** The wax-red pill that starts extraction, over the notice that nothing is saved unreviewed. */
@Composable
private fun ExtractFooter(
    isExtracting: Boolean,
    canExtract: Boolean,
    onExtract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(
            onClick = onExtract,
            enabled = canExtract,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text = stringResource(
                    if (isExtracting) R.string.import_extracting else R.string.import_action_extract,
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Aside(
            text = stringResource(R.string.import_review_notice),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
    }
}

/** Reads the clipboard's first item as text and hands it to [onPasted], trimmed, if there is any. */
@Composable
internal fun rememberPasteAction(onPasted: (String) -> Unit): () -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onPasted)
    return {
        scope.launch {
            val pasted = clipboard.getClipEntry()?.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString()?.trim()
            if (!pasted.isNullOrEmpty()) latest(pasted)
        }
    }
}

private val PageMargin = 20.dp
