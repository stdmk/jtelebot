package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RequestSource {
    TELEGRAM("Telegram"),
    EMAIL("Email"),
    ;

    private final String name;
}
