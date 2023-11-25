package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Zodiac {
    ARIES("${enum.zodiac.aries}", "♈️"),
    TAURUS("${enum.zodiac.taurus}", "♉️"),
    GEMINI("${enum.zodiac.gemini}", "♊️"),
    CANCER("${enum.zodiac.cancer}", "♋️"),
    LEO("${enum.zodiac.leo}", "♌️"),
    VIRGO("${enum.zodiac.virgo}", "♍️"),
    LIBRA("${enum.zodiac.libra}", "♎️"),
    SCORPIO("${enum.zodiac.scorpio}", "♏️"),
    SAGITTARIUS("${enum.zodiac.sagittarius}", "♐️"),
    CAPRICORN("${enum.zodiac.capricorn}", "♑️"),
    AQUARIUS("${enum.zodiac.aquarius}", "♒️"),
    PISCES("${enum.zodiac.pisces}", "♓️"),
    NOT_CHOSEN("${enum.zodiac.notchosen}", "");

    private final String name;
    private final String emoji;

    public static Zodiac findByNames(String name) {
        return Arrays.stream(Zodiac.values())
                .filter(zodiac -> (zodiac.name().equalsIgnoreCase(name) || zodiac.getName().equalsIgnoreCase(name)))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
