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
    NUMBER_OF_MESSAGES(Arrays.asList("месяц", "сообщений", "сообщения", "сообщение"), "getNumberOfMessages", "numberOfMessages"),
    NUMBER_OF_ALL_MESSAGES(Arrays.asList("все", "всё"), "getNumberOfAllMessages", "numberOfAllMessages"),
    NUMBER_OF_MESSAGES_PER_DAY(Arrays.asList("день", "сутки"), "getNumberOfMessagesPerDay", "numberOfMessagesPerDay"),
    NUMBER_OF_KARMA(Arrays.asList("карма", "кармы"), "getNumberOfKarma", "numberOfKarma"),
    NUMBER_OF_ALL_KARMA(Arrays.asList("кармавсе", "кармавсё"), "getNumberOfAllKarma", "numberOfAllKarma"),
    NUMBER_OF_STICKERS(Arrays.asList("стикеры", "стикер", "стикеров"), "getNumberOfStickers", "numberOfStickers"),
    NUMBER_OF_PHOTOS(Arrays.asList("изображения", "изображений", "изоражение"), "getNumberOfPhotos", "numberOfPhotos"),
    NUMBER_OF_ANIMATIONS(Arrays.asList("анимаций", "анимация"), "getNumberOfAnimations", "numberOfAnimations"),
    NUMBER_OF_AUDIO(Arrays.asList("музыка", "музыки"), "getNumberOfAudio", "numberOfAudio"),
    NUMBER_OF_DOCUMENTS(Arrays.asList("документы", "документ", "документов"), "getNumberOfDocuments", "numberOfDocuments"),
    NUMBER_OF_VIDEOS(Collections.singletonList("видео"), "getNumberOfVideos", "numberOfVideos"),
    NUMBER_OF_VIDEO_NOTES(Arrays.asList("видеосообщений", "видеосообщение", "видеосообщения"), "getNumberOfVideoNotes", "numberOfVideoNotes"),
    NUMBER_OF_VOICES(Arrays.asList("голосовых", "голосовые", "голосовое"), "getNumberOfAudio", "numberOfAudio"),
    NUMBER_OF_COMMANDS(Arrays.asList("команд", "команда"), "getNumberOfCommands", "numberOfCommands"),
    NUMBER_OF_GOODNESS(Arrays.asList("доброты", "доброта", "добра"), "getNumberOfGoodness", "numberOfGoodness"),
    NUMBER_OF_WICKEDNESS(Arrays.asList("злоботы", "злобота", "злоба", "злобы"), "getNumberOfWickedness", "numberOfWickedness"),
    ;

    private final List<String> paramNames;
    private final String method;
    private final String fieldName;

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
