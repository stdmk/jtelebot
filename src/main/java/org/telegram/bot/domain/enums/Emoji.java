package org.telegram.bot.domain.enums;

import lombok.Getter;

@Getter
public enum Emoji {
    DELETE("❌"),
    BAR_CHART("\uD83D\uDCCA"),
    PICTURE("🖼"),
    TEDDY_BEAR("\uD83E\uDDF8"),
    CAMERA("\uD83D\uDCF7"),
    MOVIE_CAMERA("🎥"),
    MUSIC("\uD83C\uDFB5"),
    FILM_FRAMES("\uD83C\uDF9E"),
    DOCUMENT("\uD83D\uDCC4"),
    EMAIL("\uD83D\uDCE7"),
    VHS("\uD83D\uDCFC"),
    PLAY_BUTTON("▶"),
    ROBOT("\uD83E\uDD16"),
    NEW("\uD83C\uDD95"),
    UPDATE("\uD83D\uDD04"),
    BACK("↩️");

    private final String emoji;

    Emoji(String value) {
        this.emoji = value;
    }
}
