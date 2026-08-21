package cat.receptari.app.ui.transfer

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
import cat.receptari.app.core.designsystem.pageFrame
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.domain.transfer.RecipeTransferArchive

fun Context.sendRecipeTransferNearby(archive: RecipeTransferArchive) {
    startActivity(NearbyTransferActivity.sendIntent(this, archive))
}

fun Context.receiveRecipeTransferNearby() {
    startActivity(NearbyTransferActivity.receiveIntent(this))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMethodSheet(
    onSendNearby: () -> Unit,
    onShareWithOtherApps: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OrnamentHeading(title = stringResource(R.string.transfer_method_title))
            Aside(
                text = stringResource(R.string.transfer_method_explanation),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )

            // The two ways to send read as a pair of choices rather than a stack of buttons:
            // each names itself and says, in one line, what it actually does.
            TransferMethodCard(
                icon = Icons.Outlined.PhoneAndroid,
                title = stringResource(R.string.transfer_send_nearby),
                description = stringResource(R.string.transfer_send_nearby_desc),
                onClick = onSendNearby,
            )
            TransferMethodCard(
                icon = Icons.Default.Share,
                title = stringResource(R.string.transfer_other_apps),
                description = stringResource(R.string.transfer_other_apps_desc),
                onClick = onShareWithOtherApps,
            )
        }
    }
}

/** One way to send: an icon pressed into a ruled roundel, its name, and a line on what it is. */
@Composable
private fun TransferMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .pageFrame(CircleShape, ReceptariTheme.palette.rule, inset = 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
