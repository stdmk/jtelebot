package org.telegram.bot.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Zodiac {
    ARIES("Овен", "♈️"),
    TAURUS("Телец", "♉️"),
    GEMINI("Близнецы", "♊️"),
    CANCER("Рак", "♋️"),
    LEO("Лев", "♌️"),
    VIRGO("Дева", "♍️"),
    LIBRA("Весы", "♎️"),
    SCORPIO("Скорпион", "♏️"),
    SAGITTARIUS("Стрелец", "♐️"),
    CAPRICORN("Козерог", "♑️"),
    AQUARIUS("Водолей", "♒️"),
    PISCES("Рыбы", "♓️"),
    NOT_CHOSEN("Не выбрано", "");

    private final String nameRu;
    private final String emoji;

    public static Zodiac findByNames(String name) {
        return Arrays.stream(Zodiac.values())
                .filter(zodiac -> (zodiac.name().equalsIgnoreCase(name) || zodiac.getNameRu().equalsIgnoreCase(name)))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
