package org.telegram.bot.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.telegram.bot.utils.TextUtils.startsWithElementInList;

/**
 * Enum of UserStats params and methods to get values.
 */
@RequiredArgsConstructor
@Getter
public enum UserStatsParam {
    NUMBER_OF_MESSAGES(Arrays.asList("месяц", "сообщений", "сообщения", "сообщение"), "getNumberOfMessages"),
    NUMBER_OF_ALL_MESSAGES(Arrays.asList("все", "всё"), "getNumberOfAllMessages"),
    NUMBER_OF_KARMA(Arrays.asList("карма", "кармы"), "getNumberOfKarma"),
    NUMBER_OF_ALL_KARMA(Collections.singletonList(""), "getNumberOfAllKarma"),
    NUMBER_OF_STICKERS(Arrays.asList("стикеры", "стикер", "стикеров"), "getNumberOfStickers"),
    NUMBER_OF_PHOTOS(Arrays.asList("изображения", "изображений", "изоражение"), "getNumberOfPhotos"),
    NUMBER_OF_ANIMATIONS(Arrays.asList("анимаций", "анимация"), "getNumberOfAnimations"),
    NUMBER_OF_AUDIO(Arrays.asList("музыка", "музыки"), "getNumberOfAudio"),
    NUMBER_OF_DOCUMENTS(Arrays.asList("документы", "документ", "документов"), "getNumberOfDocuments"),
    NUMBER_OF_VIDEOS(Collections.singletonList("видео"), "getNumberOfVideos"),
    NUMBER_OF_VIDEO_NOTES(Arrays.asList("видеосообщений", "видеосообщение", "видеосообщения"), "getNumberOfVideoNotes"),
    NUMBER_OF_VOICES(Arrays.asList("голосовых", "голосовые", "голосовое"), "getNumberOfAudio"),
    NUMBER_OF_COMMANDS(Arrays.asList("команд", "команда"), "getNumberOfCommands"),
    NUMBER_OF_GOODNESS(Arrays.asList("доброты", "доброта", "добра"), "getNumberOfGoodness"),
    NUMBER_OF_WICKEDNESS(Arrays.asList("злоботы", "злобота", "злоба", "злобы"), "getNumberOfWickedness"),
    ;

    private final List<String> paramNames;
    private final String method;

    /**
     * Getting UserStatsParam by paramName.
     * @param name - name of UserStats param.
     * @return UserStatsParam.
     */
    public static UserStatsParam getParamByName(String name) {
        return Arrays.stream(UserStatsParam.values())
                .filter(param -> startsWithElementInList(name.toLowerCase(Locale.ROOT), param.getParamNames()))
                .findFirst()
                .orElse(null);
    }
}
