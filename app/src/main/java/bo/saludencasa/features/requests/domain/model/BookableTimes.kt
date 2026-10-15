package bo.saludencasa.features.requests.domain.model

import bo.saludencasa.core.vo.PartOfDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class BookableStart(
    val startAt: Instant,
    val date: LocalDate,
    val time: LocalTime,
)

data class BookableTimes(
    val starts: List<BookableStart>,
) {
    private val byDate: Map<LocalDate, List<BookableStart>> =
        starts.groupBy(BookableStart::date).mapValues { (_, day) -> day.sortedBy(BookableStart::time) }

    val isEmpty: Boolean get() = starts.isEmpty()

    val days: List<LocalDate>
        get() {
            val first = byDate.keys.minOrNull() ?: return emptyList()
            val last = byDate.keys.max()
            return generateSequence(first) { it.plusDays(1) }.takeWhile { it <= last }.toList()
        }

    fun hasTimesOn(date: LocalDate): Boolean = byDate.containsKey(date)

    fun firstDayWithTimes(): LocalDate? = byDate.keys.minOrNull()

    fun on(date: LocalDate): Map<PartOfDay, List<BookableStart>> =
        byDate[date]
            .orEmpty()
            .groupBy { PartOfDay.of(it.time) }
            .toSortedMap()
}
