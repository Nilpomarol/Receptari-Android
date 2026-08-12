package cat.receptari.app.domain.timer

import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.CookingTimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerMathTest {

    @Test
    fun `running timer derives remaining time from its deadline`() {
        val timer = timer(endsAt = 61_001L)

        assertEquals(61L, TimerMath.remainingSeconds(timer, nowEpochMillis = 1L))
        assertEquals(1L, TimerMath.remainingSeconds(timer, nowEpochMillis = 60_001L))
        assertEquals(0L, TimerMath.remainingSeconds(timer, nowEpochMillis = 62_000L))
    }

    @Test
    fun `paused timer uses its stored remainder`() {
        val timer = timer(endsAt = null).copy(
            status = CookingTimerStatus.PAUSED,
            remainingSeconds = 125L,
        )

        assertEquals(125L, TimerMath.remainingSeconds(timer, nowEpochMillis = Long.MAX_VALUE))
    }

    @Test
    fun `formats minute and hour durations`() {
        assertEquals("01:05", TimerMath.format(65L))
        assertEquals("1:01:01", TimerMath.format(3_661L))
        assertEquals("00:00", TimerMath.format(-1L))
    }

    private fun timer(endsAt: Long?) = CookingTimer(
        id = "timer",
        recipeId = "recipe",
        label = "Pas 1",
        durationSeconds = 60L,
        status = CookingTimerStatus.RUNNING,
        endsAtEpochMillis = endsAt,
    )
}
