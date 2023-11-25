package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum Horoscope {
    COM("Общий"),
    ERO("Эротический"),
    ANTI("Анти"),
    BUS("Бизнес"),
    HEA("Здоровье"),
    COOK("Кулинарный"),
    LOV("Любовный"),
    MOB("Мобильный"),
    ;

    private final String ruName;

    public static Horoscope findByName(String name) {
        return Arrays.stream(Horoscope.values())
                .filter(horoscope -> (horoscope.name().equalsIgnoreCase(name) || horoscope.getRuName().equalsIgnoreCase(name)))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
