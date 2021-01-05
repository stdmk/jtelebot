package org.telegram.bot.domain.enums;

import lombok.Getter;

@Getter
public enum ParseMode {
    MARKDOWN("Markdown"),
    HTML("HTML");

    private final String value;

    ParseMode(String value) {
        this.value = value;
    }
}
