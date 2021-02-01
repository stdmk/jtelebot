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
    BACK("↩️"),
    RIGHT_ARROW_CURVING_UP("⤴️"),
    ZAP("⚡"),
    UMBRELLA("☂️"),
    UMBRELLA_WITH_RAIN_DROPS("☔"),
    SNOWFLAKE("❄"),
    FOG("\uD83C\uDF2B"),
    SUNNY("☀"),
    WHITE_SUN_WITH_SMALL_CLOUD("\uD83C\uDF24"),
    SUN_BEHIND_CLOUD("⛅️"),
    SUN_BEHIND_LARGE_CLOUD("\uD83C\uDF25"),
    CLOUD("☁️"),
    DOWN_ARROW("⬇️"),
    DOWN_LEFT_ARROW("↙️"),
    LEFT_ARROW("⬅️"),
    UP_LEFT_ARROW("↖️"),
    UP_ARROW("⬆️"),
    UP_RIGHT_ARROW("↗️"),
    RIGHT_ARROW("➡️"),
    DOWN_RIGHT_ARROW("↘️"),
    DROPLET("\uD83D\uDCA7"),
    THUMBS_UP("\uD83D\uDC4D"),
    THUMBS_DOWN("\uD83D\uDC4E");

    private final String emoji;

    Emoji(String value) {
        this.emoji = value;
    }
}
