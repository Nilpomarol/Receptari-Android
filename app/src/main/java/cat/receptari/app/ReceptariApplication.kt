package cat.receptari.app

import android.app.Application
import android.content.Context
import android.widget.Toast
import cat.receptari.app.data.backup.PendingRestoreApplier
import cat.receptari.app.data.backup.RestoreApplyResult
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReceptariApplication : Application() {
    private var restoreResult = RestoreApplyResult.NONE

    override fun attachBaseContext(base: Context) {
        restoreResult = PendingRestoreApplier(base).applyIfPending()
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        when (restoreResult) {
            RestoreApplyResult.SUCCESS -> Toast.makeText(
                this,
                R.string.settings_restore_completed,
                Toast.LENGTH_LONG,
            ).show()
            RestoreApplyResult.FAILURE -> Toast.makeText(
                this,
                R.string.settings_restore_failed,
                Toast.LENGTH_LONG,
            ).show()
            RestoreApplyResult.NONE -> Unit
        }
    }
}
