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
    MINUTES1("command.remind.repeatability.minutes1", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(1))),
    MINUTES5("command.remind.repeatability.minutes5", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(5))),
    MINUTES10("command.remind.repeatability.minutes10", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(10))),
    MINUTES15("command.remind.repeatability.minutes15", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(15))),
    MINUTES30("command.remind.repeatability.minutes30", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(30))),
    HOURS1("command.remind.repeatability.hours1", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(1))),
    HOURS2("command.remind.repeatability.hours2", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(2))),
    HOURS3("command.remind.repeatability.hours3", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(3))),
    HOURS6("command.remind.repeatability.hours6", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(6))),
    HOURS12("command.remind.repeatability.hours12", () -> Duration.between(LocalDateTime.now(), LocalDateTime.now().plusHours(12))),
    MONDAY("command.remind.repeatability.monday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)))),
    TUESDAY("command.remind.repeatability.tuesday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)))),
    WEDNESDAY("command.remind.repeatability.wednesday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)))),
    THURSDAY("command.remind.repeatability.thursday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY)))),
    FRIDAY("command.remind.repeatability.friday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)))),
    SATURDAY("command.remind.repeatability.saturday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)))),
    SUNDAY("command.remind.repeatability.sunday", () -> Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)))),
    DAY("command.remind.repeatability.day", () -> Period.ofDays(1)),
    WEEK("command.remind.repeatability.week", () -> Period.ofWeeks(1)),
    MONTH("command.remind.repeatability.month", () -> Period.ofMonths(1)),
    YEAR("command.remind.repeatability.year", () -> Period.ofYears(1)),
    ;

    private final String caption;
    private final Supplier<TemporalAmount> temporalAmountSupplier;
}
