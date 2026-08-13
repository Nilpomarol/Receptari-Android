package cat.receptari.app.ui.transfer

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.Aside
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperModalBottomSheet
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
        ) {
            OrnamentHeading(title = stringResource(R.string.transfer_method_title))
            Aside(
                text = stringResource(R.string.transfer_method_explanation),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
            OutlinedButton(onClick = onSendNearby, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null)
                Text(
                    text = stringResource(R.string.transfer_send_nearby),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = onShareWithOtherApps, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(
                    text = stringResource(R.string.transfer_other_apps),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
