package cat.receptari.app.ui.transfer

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import cat.receptari.app.BuildConfig
import cat.receptari.app.R
import cat.receptari.app.domain.transfer.RecipeTransferArchive

const val RECIPE_TRANSFER_MIME_TYPE = "application/vnd.cat.receptari.recipe-transfer"

fun Context.shareRecipeTransfer(archive: RecipeTransferArchive) {
    val uri = FileProvider.getUriForFile(
        this,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        archive.file,
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = RECIPE_TRANSFER_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, archive.suggestedFileName)
        clipData = ClipData.newRawUri(archive.suggestedFileName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(sendIntent, getString(R.string.transfer_share_chooser)))
}
