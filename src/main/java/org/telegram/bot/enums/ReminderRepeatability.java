package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Getter
public enum ReminderRepeatability {
    //do not change the order!
    MINUTES1("1 м.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(1))),
    MINUTES5("5 м.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(5))),
    MINUTES10("10 м.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(10))),
    MINUTES15("15 м.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(15))),
    MINUTES30("30 м.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(30))),
    HOURS1("1 ч.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(1))),
    HOURS2("2 ч.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(2))),
    HOURS3("3 ч.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(3))),
    HOURS6("6 ч.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(6))),
    HOURS12("12 ч.", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(12))),
    MONDAY("Пн.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)))),
    TUESDAY("Вт.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)))),
    WEDNESDAY("Ср.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)))),
    THURSDAY("Чт.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY)))),
    FRIDAY("Пт.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)))),
    SATURDAY("Сб.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)))),
    SUNDAY("Вс.", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)))),
    DAY("День", () -> Period.ofDays(1)),
    WEEK("Неделю", () -> Period.ofWeeks(1)),
    MONTH("Месяц", () -> Period.ofMonths(1)),
    YEAR("Год", () -> Period.ofYears(1)),
    ;

    private final String caption;
    private final Supplier<TemporalAmount> temporalAmountSupplier;
}
