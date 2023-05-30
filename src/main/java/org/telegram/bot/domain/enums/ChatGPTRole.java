package org.telegram.bot.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChatGPTRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    ;

    private final String name;
}
