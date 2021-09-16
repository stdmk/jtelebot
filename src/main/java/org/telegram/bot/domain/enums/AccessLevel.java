package org.telegram.bot.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AccessLevel {
    BANNED(-1),
    NEWCOMER(0),
    FAMILIAR(1),
    TRUSTED(5),
    MODERATOR(8),
    ADMIN(10);

    private final Integer value;

    public static AccessLevel getUserLevelByValue(Integer value) {
        return Arrays.stream(AccessLevel.values())
                .filter(accessLevels -> accessLevels.value.equals(value))
                .findFirst()
                .orElse(AccessLevel.NEWCOMER);
    }
}
