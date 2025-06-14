package org.telegram.bot.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
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
    THUMBS_DOWN("\uD83D\uDC4E"),
    SMILING_FACE_WITH_HALO("\uD83D\uDE07"),
    SMILING_FACE_WITH_HORNS("\uD83D\uDE08"),
    RED_HEART("❤️"),
    BROKEN_HEART("\uD83D\uDC94"),
    FOLDER("\uD83D\uDCC1"),
    MEMO("\uD83D\uDCDD"),
    HEADPHONE("\uD83C\uDFA7"),
    CLIPBOARD("📋"),
    CHECK_MARK("✔️"),
    NO_ENTRY_SIGN("\uD83D\uDEAB"),
    CHECK_MARK_BUTTON("✅"),
    SETTINGS("⚙️"),
    HOURGLASS_DONE("⌛"),
    HOURGLASS_NOT_DONE("⏳"),
    BELL("\uD83D\uDD14"),
    NO_BELL("\uD83D\uDD15"),
    STOP_BUTTON("⏹"),
    TICKET("\uD83C\uDFAB"),
    GREEN_BOOK("\uD83D\uDCD7"),
    DATE("\uD83D\uDCC5"),
    GEAR("⚙️"),
    CANCELLATION("\uD83D\uDDD9"),
    WEIGHT_LIFTER("🏋"),
    GAME_DIE("\uD83C\uDFB2"),
    CALENDAR("\uD83D\uDCC6"),
    TROPHY("🏆"),
    WASTEBASKET("\uD83D\uDDD1️"),
    BEAMING_FACE_WITH_SMILING_EYES("\uD83D\uDE01"),
    FIRE("\uD83D\uDD25"),
    CHICKEN("\uD83D\uDC14"),
    PIG("\uD83D\uDC16"),
    BREAD("\uD83C\uDF5E")
    ;

    private final String symbol;
}
