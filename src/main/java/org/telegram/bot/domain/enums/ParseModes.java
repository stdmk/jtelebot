package org.telegram.bot.domain.enums;

import lombok.Getter;

@Getter
public enum ParseModes {
    MARKDOWN("Markdown"),
    HTML("HTML");

    private final String value;

    ParseModes(String value) {
        this.value = value;
    }
}
