package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.telegram.bot.utils.TextUtils.startsWithElementInList;
import static org.telegram.bot.utils.TextUtils.removeCapital;

@Component
@AllArgsConstructor
public class Top implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Top.class);

    private final UserStatsService userStatsService;
    private final UserService userService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        Chat chat = chatService.get(message.getChatId());

        //TODO переделать на айди, если пользователь без юзернейма
        User user;
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                user = userService.get(repliedMessage.getFrom().getId());
            } else {
                user = userService.get(message.getFrom().getId());
            }
            responseText = getTopOfUser(chat, user);
        } else {
            user = userService.get(textMessage);
            if (user != null) {
                responseText = getTopOfUser(chat, user);
            } else {
                responseText = getTopListOfUsers(chat, textMessage);
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    public SendMessage getTopByChat(Chat chat) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getChatId().toString());
        sendMessage.enableHtml(true);
        try {
            sendMessage.setText(getTopListOfUsers(chat, getSortParamByName("месяц") + "\nСтатистика за месяц сброшена"));
        } catch (BotException ignored) {

        }

        return sendMessage;
    }

    private String getTopOfUser(Chat chat, User user) throws BotException {
        log.debug("Request to get top of user by username {}", user.getUsername());

        Map<String, String> fieldsOfStats = new LinkedHashMap<>();
        StringBuilder buf = new StringBuilder();
        String valueForSkip = "0";
        UserStats userStats = userStatsService.get(chat, user);
        if (userStats == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.USER_NOT_FOUND));
        }

        String karmaEmoji;
        if (userStats.getNumberOfKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getEmoji();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getEmoji();
        }

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "Сообщений", userStats.getNumberOfMessages().toString());
        fieldsOfStats.put(karmaEmoji + "Карма", userStats.getNumberOfKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getEmoji() + "Доброта", userStats.getNumberOfGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getEmoji() + "Злобота", userStats.getNumberOfWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "Стикеров", userStats.getNumberOfStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "Изображений", userStats.getNumberOfPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "Анимаций", userStats.getNumberOfAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "Музыки", userStats.getNumberOfAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "Документов", userStats.getNumberOfDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "Видео", userStats.getNumberOfVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "Видеосообщений", userStats.getNumberOfVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "Голосовых", userStats.getNumberOfVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "Команд", userStats.getNumberOfCommands().toString());

        buf.append("<b>").append(userStats.getUser().getUsername()).append("</b>\n").append("<u>За месяц:</u>\n");
        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": <b>").append(value).append("</b>\n");
            }
        });
        buf.append("\n");

        fieldsOfStats.clear();
        buf.append("<u>Всего:</u>\n");

        if (userStats.getNumberOfAllKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getEmoji();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getEmoji();
        }

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "Сообщений", userStats.getNumberOfAllMessages().toString());
        fieldsOfStats.put(karmaEmoji + "Карма", userStats.getNumberOfAllKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getEmoji() + "Доброта", userStats.getNumberOfAllGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getEmoji() + "Злобота", userStats.getNumberOfAllWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "Стикеров", userStats.getNumberOfAllStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "Изображений", userStats.getNumberOfAllPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "Анимаций", userStats.getNumberOfAllAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "Музыки", userStats.getNumberOfAllAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "Документов", userStats.getNumberOfAllDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "Видео", userStats.getNumberOfAllVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "Видеосообщений", userStats.getNumberOfAllVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "Голосовых", userStats.getNumberOfAllVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "Команд", userStats.getNumberOfAllCommands().toString());

        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": <b>").append(value).append("</b>\n");
            }
        });

        return buf.toString();
    }

    private String getTopListOfUsers(Chat chat, String param) throws BotException {
        log.debug("Request to top by {} for chat {}", param, chat);

        SortParam sortParam = getSortParamByName(param);
        if (!param.equals("все") && !param.equals("всё") && (param.endsWith("всё") || param.endsWith("все"))) {
            String name = sortParam.getMethod().getName();
            name = name.substring(0, 11) + "All" + name.substring(11);
            try {
                sortParam.setMethod(UserStats.class.getMethod(name));
            } catch (NoSuchMethodException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        if (sortParam == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String sortedField = removeCapital(sortParam.getMethod().getName().substring(3));
        List<UserStats> userStatsList = userStatsService.getSortedUserStatsListForChat(chat, sortedField, 30);

        int spacesAfterSerialNumberCount = String.valueOf(userStatsList.size()).length() + 2;
        int spacesAfterNumberOfMessageCount = getSpacesAfterNumberOfMessageCount(sortParam, userStatsList);

        StringBuilder responseText = new StringBuilder("<b>Топ ").append(sortParam.getParamNames().get(0)).append(":</b>\n<code>\n");
        AtomicInteger counter = new AtomicInteger(1);
        Method finalMethod = sortParam.getMethod();

        userStatsList.forEach(userStats -> {
            long value = 0;
            try {
                value = Long.parseLong(finalMethod.invoke(userStats).toString());
            } catch (IllegalAccessException | InvocationTargetException e) {
                try {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                } catch (BotException botException) {
                    botException.printStackTrace();
                }
            }

            if (value != 0) {
                responseText
                        .append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter.getAndIncrement() + ")"))
                        .append(String.format("%-" + spacesAfterNumberOfMessageCount + "s", value))
                        .append(userStats.getUser().getUsername()).append("\n");
            }
        });

        return responseText.append("</code>").toString();
    }

    private SortParam getSortParamByName(String name) {
        List<SortParam> sortParamList = new ArrayList<>();
        try {
            sortParamList.add(new SortParam(Arrays.asList("месяц", "сообщений", "сообщения", "сообщение"), UserStats.class.getMethod("getNumberOfMessages")));
            sortParamList.add(new SortParam(Arrays.asList("все", "всё"), UserStats.class.getMethod("getNumberOfAllMessages")));
            sortParamList.add(new SortParam(Arrays.asList("карма", "кармы"), UserStats.class.getMethod("getNumberOfKarma")));
            sortParamList.add(new SortParam(Arrays.asList("стикеры", "стикер", "стикеров"), UserStats.class.getMethod("getNumberOfStickers")));
            sortParamList.add(new SortParam(Arrays.asList("изображения", "изображений", "изоражение"), UserStats.class.getMethod("getNumberOfPhotos")));
            sortParamList.add(new SortParam(Arrays.asList("анимаций", "анимация"), UserStats.class.getMethod("getNumberOfAnimations")));
            sortParamList.add(new SortParam(Arrays.asList("музыка", "музыки"), UserStats.class.getMethod("getNumberOfAudio")));
            sortParamList.add(new SortParam(Arrays.asList("документы", "документ", "документов"), UserStats.class.getMethod("getNumberOfDocuments")));
            sortParamList.add(new SortParam(Collections.singletonList("видео"), UserStats.class.getMethod("getNumberOfVideos")));
            sortParamList.add(new SortParam(Arrays.asList("видеосообщений", "видеосообщение", "видеосообщения"), UserStats.class.getMethod("getNumberOfVideoNotes")));
            sortParamList.add(new SortParam(Arrays.asList("голосовых", "голосовые", "голосовое"), UserStats.class.getMethod("getNumberOfAudio")));
            sortParamList.add(new SortParam(Arrays.asList("команд", "команда"), UserStats.class.getMethod("getNumberOfCommands")));
            sortParamList.add(new SortParam(Arrays.asList("доброты", "доброта", "добра"), UserStats.class.getMethod("getNumberOfGoodness")));
            sortParamList.add(new SortParam(Arrays.asList("злоботы", "злобота", "злоба", "злобы"), UserStats.class.getMethod("getNumberOfWickedness")));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return sortParamList
                .stream()
                .filter(sortParamValue -> startsWithElementInList(name.toLowerCase(Locale.ROOT), sortParamValue.getParamNames()))
                .findFirst()
                .orElse(null);
    }

    private Integer getSpacesAfterNumberOfMessageCount(SortParam sortParam, List<UserStats> userStatsList) {
        return userStatsList
                .stream()
                .map(userStats -> {
                    long value = 0;
                    try {
                        value = Long.parseLong(sortParam.getMethod().invoke(userStats).toString());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return value;
                })
                .max(Long::compareTo)
                .orElse(6L)
                .toString()
                .length() + 2;
    }

    @Data
    @AllArgsConstructor
    private static class SortParam {
        private List<String> paramNames;
        private Method method;
    }
}
