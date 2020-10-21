package org.telegram.bot.domain.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AccessLevels {
    BANNED(-1),
    NEWCOMER(0),
    FAMILIAR(1),
    TRUSTED(5),
    ADMIN(10);

    private final Integer value;

    AccessLevels(Integer value) {
        this.value = value;
    }

    public static AccessLevels getUserLevelByValue(Integer value) {
        return Arrays.stream(AccessLevels.values())
                .filter(accessLevels -> accessLevels.value.equals(value))
                .findFirst()
                .orElse(AccessLevels.NEWCOMER);
    }
}
