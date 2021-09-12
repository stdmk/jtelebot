package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.domain.enums.UserStatsParam;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.removeCapital;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
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
            sendMessage.setText(getTopListOfUsers(chat, "месяц") + "\nСтатистика за месяц сброшена");
        } catch (BotException ignored) {

        }

        return sendMessage;
    }

    /**
     * Getting user stats.
     *
     * @param chat Chat entity.
     * @param user User entity.
     * @return user stats.
     */
    private String getTopOfUser(Chat chat, User user) {
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

        buf.append("<b>").append(getLinkToUser(userStats.getUser(), true)).append("</b>\n").append("<u>За месяц:</u>\n");
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

    /**
     * Getting top of users for param of UserStats.
     *
     * @param chat Chat entity.
     * @param param param of user stats.
     * @return top of users.
     */
    private String getTopListOfUsers(Chat chat, String param) {
        log.debug("Request to top by {} for chat {}", param, chat);

        UserStatsParam sortParam = UserStatsParam.getParamByName(param);

        if (sortParam == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String methodName = sortParam.getMethod();
        if (!param.equals("все") && !param.equals("всё") && (param.endsWith("всё") || param.endsWith("все"))) {
            methodName = methodName.substring(0, 11) + "All" + methodName.substring(11);
        }

        String sortedField = removeCapital(methodName.substring(3));
        List<UserStats> userStatsList;

        if (UserStatsParam.NUMBER_OF_KARMA.getMethod().equals(sortedField)) {
            userStatsList = userStatsService.getSortedUserStatsListWithKarmaForChat(chat, sortedField, 30, false);
        } else if (UserStatsParam.NUMBER_OF_ALL_KARMA.getMethod().equals(sortedField)) {
            userStatsList = userStatsService.getSortedUserStatsListWithKarmaForChat(chat, sortedField, 30, true);
        } else {
            userStatsList = userStatsService.getSortedUserStatsListForChat(chat, sortedField, 30);
        }

        Method method;
        try {
           method = UserStats.class.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
        int spacesAfterSerialNumberCount = String.valueOf(userStatsList.size()).length() + 2;
        int spacesAfterNumberOfMessageCount = getSpacesAfterNumberOfMessageCount(method, userStatsList);

        StringBuilder responseText = new StringBuilder("<b>Топ ").append(sortParam.getParamNames().get(0)).append(":</b>\n<code>");
        AtomicInteger counter = new AtomicInteger(1);
        AtomicLong total = new AtomicLong(0L);

        userStatsList.forEach(userStats -> {
            long value;
            try {
                value = Long.parseLong(method.invoke(userStats).toString());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            if (value != 0) {
                total.set(total.get() + value);
                responseText
                        .append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter.getAndIncrement() + ")"))
                        .append(String.format("%-" + spacesAfterNumberOfMessageCount + "s", value))
                        .append(userStats.getUser().getUsername()).append("\n");
            }
        });
        responseText.append("</code>").append("Итого: <b>").append(total.get()).append("</b>");

        return responseText.toString();
    }

    /**
     * Getting spaces count after value of number param of top.
     *
     * @param method Method for getting value of UserStats param.
     * @param userStatsList list of UserStats.
     * @return count of spaces.
     */
    private Integer getSpacesAfterNumberOfMessageCount(Method method, List<UserStats> userStatsList) {
        List<Long> values = userStatsList
                .stream()
                .map(userStats -> {
                    long value;

                    try {
                        value = Long.parseLong(method.invoke(userStats).toString());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                    }

                    return value;
                })
                .collect(Collectors.toList());

        int maxValueLength = values.stream()
                .max(Long::compareTo)
                .orElse(6L)
                .toString()
                .length();

        boolean valuesHasNegative = values.stream().filter(value -> value < 0).findFirst().orElse(null) != null;
        if (valuesHasNegative) {
            int minValueLength = values.stream()
                    .min(Long::compareTo)
                    .orElse(6L)
                    .toString()
                    .length();

            if (minValueLength > maxValueLength) {
                return minValueLength + 1;
            }
        }

        return maxValueLength + 1;
    }
}
