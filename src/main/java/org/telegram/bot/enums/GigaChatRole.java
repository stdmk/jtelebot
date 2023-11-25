package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GigaChatRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    ;

    private final String name;
}
