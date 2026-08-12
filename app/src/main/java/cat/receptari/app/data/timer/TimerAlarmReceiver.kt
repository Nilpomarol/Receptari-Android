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
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: CookingTimerRepository
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
        val control = intent.getStringExtra(EXTRA_CONTROL)
            ?.let { runCatching { TimerControl.valueOf(it) }.getOrNull() }
        if (intent.action != ACTION_TIMER_EXPIRED && control == null) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                when (control) {
                    TimerControl.ADD_MINUTE -> repository.addTime(timerId, SECONDS_PER_MINUTE)
                    TimerControl.PAUSE -> repository.pause(timerId)
                    TimerControl.RESUME -> repository.resume(timerId)
                    TimerControl.CANCEL -> repository.cancel(timerId)
                    null -> repository.complete(timerId)
                }
            } catch (_: SecurityException) {
                // Exact-alarm access can be revoked while a timer is paused. Leave the
                // timer paused and keep its notification usable instead of crashing.
                repository.refreshNotifications()
            } catch (_: IllegalStateException) {
                repository.refreshNotifications()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TIMER_EXPIRED = "cat.receptari.app.action.TIMER_EXPIRED"
        const val ACTION_TIMER_CONTROL = "cat.receptari.app.action.TIMER_CONTROL"
        const val EXTRA_TIMER_ID = "timerId"
        const val EXTRA_CONTROL = "control"
        const val SECONDS_PER_MINUTE = 60L
    }
}
