package cat.receptari.app.data.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cat.receptari.app.core.util.IoDispatcher
import cat.receptari.app.domain.repository.CookingTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerBootReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: CookingTimerRepository
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                repository.restoreAfterBoot()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
