package cat.receptari.app.data.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import cat.receptari.app.domain.model.CookingTimer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidCookingTimerScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notifications: TimerNotificationFactory,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean = alarmManager.canScheduleExactAlarms()

    fun schedule(timer: CookingTimer) {
        val deadline = requireNotNull(timer.endsAtEpochMillis)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            deadline,
            expirationIntent(timer.id),
        )
    }

    fun cancel(timerId: String) {
        cancelAlarm(timerId)
        notifications.cancel(timerId)
    }

    fun showFinished(timer: CookingTimer) {
        cancelAlarm(timer.id)
        notifications.showFinished(timer)
    }

    fun syncNotifications(timers: List<CookingTimer>) = notifications.sync(timers)

    fun cancelAlarm(timerId: String) {
        alarmManager.cancel(expirationIntent(timerId))
    }

    private fun expirationIntent(timerId: String): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_TIMER_EXPIRED
            data = Uri.parse("receptari://timer/$timerId/expired")
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
        }
        return PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
