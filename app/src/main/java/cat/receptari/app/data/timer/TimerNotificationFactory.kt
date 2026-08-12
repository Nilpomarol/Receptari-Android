package cat.receptari.app.data.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cat.receptari.app.MainActivity
import cat.receptari.app.R
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.CookingTimerStatus
import cat.receptari.app.domain.timer.TimerMath
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TimerNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun sync(timers: List<CookingTimer>) {
        if (!canPostNotifications()) return
        ensureChannels()
        timers.forEach { timer ->
            when (timer.status) {
                CookingTimerStatus.RUNNING -> showRunning(timer)
                CookingTimerStatus.PAUSED -> showPaused(timer)
            }
        }
        if (timers.size > 1) {
            showSummary(timers.size)
        } else {
            notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        }
    }

    fun showFinished(timer: CookingTimer) {
        if (!canPostNotifications()) return
        ensureChannels()
        notificationManager.cancel(notificationId(timer.id))
        val notification = baseBuilder(timer, FINISHED_CHANNEL_ID)
            .setContentText(context.getString(R.string.timer_notification_finished))
            .setAutoCancel(true)
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        notify(timer.id, notification)
    }

    fun cancel(timerId: String) {
        notificationManager.cancel(notificationId(timerId))
    }

    private fun showRunning(timer: CookingTimer) {
        val deadline = requireNotNull(timer.endsAtEpochMillis)
        val notification = baseBuilder(timer, RUNNING_CHANNEL_ID)
            .setContentText(context.getString(R.string.timer_notification_running))
            .setWhen(deadline)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(controlAction(timer.id, TimerControl.ADD_MINUTE))
            .addAction(controlAction(timer.id, TimerControl.PAUSE))
            .addAction(controlAction(timer.id, TimerControl.CANCEL))
            .build()
        notify(timer.id, notification)
    }

    private fun showPaused(timer: CookingTimer) {
        val remaining = TimerMath.format(timer.remainingSeconds ?: 0L)
        val notification = baseBuilder(timer, RUNNING_CHANNEL_ID)
            .setContentText(context.getString(R.string.timer_notification_paused, remaining))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(controlAction(timer.id, TimerControl.ADD_MINUTE))
            .addAction(controlAction(timer.id, TimerControl.RESUME))
            .addAction(controlAction(timer.id, TimerControl.CANCEL))
            .build()
        notify(timer.id, notification)
    }

    private fun baseBuilder(timer: CookingTimer, channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setColor(ContextCompat.getColor(context, R.color.timer_notification_color))
            .setContentTitle(timer.label)
            .setContentIntent(contentIntent(timer.recipeId, timer.id))
            .setGroup(TIMER_GROUP_KEY)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun showSummary(timerCount: Int) {
        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setColor(ContextCompat.getColor(context, R.color.timer_notification_color))
            .setContentTitle(context.resources.getQuantityString(
                R.plurals.timer_notification_summary,
                timerCount,
                timerCount,
            ))
            .setGroup(TIMER_GROUP_KEY)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notify(SUMMARY_NOTIFICATION_ID, notification)
    }

    private fun controlAction(timerId: String, control: TimerControl): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            R.drawable.ic_timer_notification,
            context.getString(control.titleRes),
            controlIntent(timerId, control),
        ).build()

    private fun controlIntent(timerId: String, control: TimerControl): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = "${TimerAlarmReceiver.ACTION_TIMER_CONTROL}.${control.name}"
            data = Uri.parse("receptari://timer/$timerId/${control.name.lowercase()}")
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlarmReceiver.EXTRA_CONTROL, control.name)
        }
        return PendingIntent.getBroadcast(
            context,
            "$timerId:${control.name}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun contentIntent(recipeId: String, timerId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_RECIPE_ID, recipeId)
        }
        return PendingIntent.getActivity(
            context,
            timerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPostNotifications(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun notify(timerId: String, notification: Notification) {
        if (canPostNotifications()) {
            notificationManager.notify(notificationId(timerId), notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun notify(notificationId: Int, notification: Notification) {
        if (canPostNotifications()) {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                RUNNING_CHANNEL_ID,
                context.getString(R.string.timer_channel_running_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.timer_channel_running_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FINISHED_CHANNEL_ID,
                context.getString(R.string.timer_channel_finished_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.timer_channel_finished_description)
                enableVibration(true)
            },
        )
    }

    private fun notificationId(timerId: String): Int = timerId.hashCode()

    private companion object {
        const val RUNNING_CHANNEL_ID = "cooking_timer_running_v2"
        const val FINISHED_CHANNEL_ID = "cooking_timer_finished"
        const val TIMER_GROUP_KEY = "cat.receptari.app.COOKING_TIMERS"
        const val SUMMARY_NOTIFICATION_ID = 8_122
    }
}

enum class TimerControl(@param:androidx.annotation.StringRes val titleRes: Int) {
    ADD_MINUTE(R.string.timer_add_minute_short),
    PAUSE(R.string.timer_pause),
    RESUME(R.string.timer_resume),
    CANCEL(R.string.timer_cancel_short),
}
