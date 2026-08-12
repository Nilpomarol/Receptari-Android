package cat.receptari.app.domain.timer

import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.CookingTimerStatus

object TimerMath {

    /** Rounds up so a newly started one-minute timer initially renders as 1:00, not 0:59. */
    fun remainingSeconds(timer: CookingTimer, nowEpochMillis: Long): Long =
        when (timer.status) {
            CookingTimerStatus.RUNNING -> {
                val remainingMillis = (timer.endsAtEpochMillis ?: nowEpochMillis) - nowEpochMillis
                ((remainingMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND).coerceAtLeast(0L)
            }

            CookingTimerStatus.PAUSED -> timer.remainingSeconds?.coerceAtLeast(0L) ?: 0L
        }

    fun format(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0L)
        val hours = safeSeconds / SECONDS_PER_HOUR
        val minutes = (safeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val remainder = safeSeconds % SECONDS_PER_MINUTE
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, remainder)
        } else {
            "%02d:%02d".format(minutes, remainder)
        }
    }

    private const val MILLIS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
}
