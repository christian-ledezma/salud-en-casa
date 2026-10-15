package bo.saludencasa.core.vo

import java.time.LocalTime

enum class PartOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    ;

    companion object {
        private val NOON: LocalTime = LocalTime.NOON
        private val SIX_PM: LocalTime = LocalTime.of(18, 0)

        fun of(time: LocalTime): PartOfDay =
            when {
                time < NOON -> MORNING
                time < SIX_PM -> AFTERNOON
                else -> EVENING
            }
    }
}
