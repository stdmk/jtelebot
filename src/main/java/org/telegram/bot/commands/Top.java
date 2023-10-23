package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.removeCapital;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Top implements Command<SendMessage> {

    private final Bot bot;
    private final UserStatsService userStatsService;
    private final UserService userService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;

    private final Map<String, Set<String>> topListParamValuesMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        topListParamValuesMap.put("getNumberOfMessagesPerDay", internationalizationService.getAllTranslations("command.top.list.daily"));
        topListParamValuesMap.put("getNumberOfMessages", internationalizationService.getAllTranslations("command.top.list.monthly"));
        topListParamValuesMap.put("getNumberOfAllMessages", internationalizationService.getAllTranslations("command.top.list.total"));
        topListParamValuesMap.put("getNumberOfKarma", internationalizationService.getAllTranslations("command.top.list.karma"));
        topListParamValuesMap.put("getNumberOfStickers", internationalizationService.getAllTranslations("command.top.list.stickers"));
        topListParamValuesMap.put("getNumberOfPhotos", internationalizationService.getAllTranslations("command.top.list.photos"));
        topListParamValuesMap.put("getNumberOfAnimations", internationalizationService.getAllTranslations("command.top.list.animations"));
        topListParamValuesMap.put("getNumberOfAudio", internationalizationService.getAllTranslations("command.top.list.audio"));
        topListParamValuesMap.put("getNumberOfDocuments", internationalizationService.getAllTranslations("command.top.list.documents"));
        topListParamValuesMap.put("getNumberOfVideos", internationalizationService.getAllTranslations("command.top.list.videos"));
        topListParamValuesMap.put("getNumberOfVideoNotes", internationalizationService.getAllTranslations("command.top.list.videonotes"));
        topListParamValuesMap.put("getNumberOfVoices", internationalizationService.getAllTranslations("command.top.list.voices"));
        topListParamValuesMap.put("getNumberOfCommands", internationalizationService.getAllTranslations("command.top.list.commands"));
        topListParamValuesMap.put("getNumberOfGoodness", internationalizationService.getAllTranslations("command.top.list.goodness"));
        topListParamValuesMap.put("getNumberOfWickedness", internationalizationService.getAllTranslations("command.top.list.wickedness"));
    }

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        Chat chat = new Chat().setChatId(message.getChatId());

        User user;
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();

            Long userId;
            userId = Objects.requireNonNullElse(repliedMessage, message).getFrom().getId();
            user = new User().setUserId(userId);

            log.debug("Request to get top of user {} for chat {}", user, chat);
            responseText = getTopOfUser(chat, user);
        } else {
            user = userService.get(textMessage);
            if (user != null) {
                log.debug("Request to get top of user {} for chat {}", user, chat);
                responseText = getTopOfUser(chat, user);
            } else {
                log.debug("Request to get top of users for chat {} by param {}", chat, textMessage);
                responseText = getTopListOfUsers(chat, textMessage);
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);
        sendMessage.setDisableNotification(true);

        return sendMessage;
    }

    public SendMessage getTopByChat(Chat chat) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getChatId().toString());
        sendMessage.enableHtml(true);
        try {
            sendMessage.setText(getTopListOfUsers(chat, "monthly") + "\n${command.top.monthlyclearcaption}");
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

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "${command.top.userstats.messages}", userStats.getNumberOfMessages().toString());
        fieldsOfStats.put(karmaEmoji + "${command.top.userstats.karma}", userStats.getNumberOfKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getEmoji() + "${command.top.userstats.kindness}", userStats.getNumberOfGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getEmoji() + "${command.top.userstats.wickedness}", userStats.getNumberOfWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "${command.top.userstats.stickers}", userStats.getNumberOfStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "${command.top.userstats.images}", userStats.getNumberOfPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "${command.top.userstats.animations}", userStats.getNumberOfAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "${command.top.userstats.music}", userStats.getNumberOfAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "${command.top.userstats.documents}", userStats.getNumberOfDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "${command.top.userstats.videos}", userStats.getNumberOfVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "${command.top.userstats.videomessages}", userStats.getNumberOfVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "${command.top.userstats.voices}", userStats.getNumberOfVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "${command.top.userstats.commands}", userStats.getNumberOfCommands().toString());

        buf.append("<b>").append(getLinkToUser(userStats.getUser(), true)).append("</b>\n").append("<u>${command.top.permonth}:</u>\n");
        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": <b>").append(value).append("</b>\n");
            }
        });
        buf.append("\n");

        fieldsOfStats.clear();
        buf.append("<u>${command.top.userstats.total}:</u>\n");

        if (userStats.getNumberOfAllKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getEmoji();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getEmoji();
        }

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "${command.top.userstats.messages}", userStats.getNumberOfAllMessages().toString());
        fieldsOfStats.put(karmaEmoji + "${command.top.userstats.karma}", userStats.getNumberOfAllKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getEmoji() + "${command.top.userstats.kindness}", userStats.getNumberOfAllGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getEmoji() + "${command.top.userstats.wickedness}", userStats.getNumberOfAllWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "${command.top.userstats.stickers}", userStats.getNumberOfAllStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "${command.top.userstats.images}", userStats.getNumberOfAllPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "${command.top.userstats.animations}", userStats.getNumberOfAllAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "${command.top.userstats.music}", userStats.getNumberOfAllAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "${command.top.userstats.documents}", userStats.getNumberOfAllDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "${command.top.userstats.videos}", userStats.getNumberOfAllVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "${command.top.userstats.videomessages}", userStats.getNumberOfAllVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "${command.top.userstats.voices}", userStats.getNumberOfAllVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "${command.top.userstats.commands}", userStats.getNumberOfAllCommands().toString());

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

        String methodName = getMethodNameByParam(param);
        if (methodName == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        boolean equalsTotalParam = topListParamValuesMap.get("getNumberOfAllMessages").stream().anyMatch(totalParam -> totalParam.equals(param));
        boolean endsWithTotalParam = topListParamValuesMap.get("getNumberOfAllMessages").stream().anyMatch(totalParam -> totalParam.endsWith(param));
        boolean equalsDailyParam = topListParamValuesMap.get("getNumberOfMessagesPerDay").stream().anyMatch(dailyParam -> dailyParam.equals(param));
        boolean endWithDailyParam = topListParamValuesMap.get("getNumberOfMessagesPerDay").stream().anyMatch(dailyParam -> dailyParam.endsWith(param));
        if (!equalsTotalParam && endsWithTotalParam) {
            methodName = methodName.substring(0, 11) + "All" + methodName.substring(11);
        } else if (!equalsDailyParam && endWithDailyParam) {
            methodName = methodName + "PerDay";
        }

        String sortedField = removeCapital(methodName.substring(3));
        List<UserStats> userStatsList;

        if ("numberOfKarma".equals(sortedField)) {
            userStatsList = userStatsService.getSortedUserStatsListWithKarmaForChat(chat, sortedField, 30, false);
        } else if ("numberOfAllKarma".equals(sortedField)) {
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

        StringBuilder responseText = new StringBuilder("<b>${command.top.list.caption} ").append(param).append(":</b>\n");
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
                User user = userStats.getUser();
                String username = user.getUsername();

                responseText
                        .append(TextUtils.getLinkToUser(user, true, "@")).append(" ")
                        .append("<code>")
                        .append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter.getAndIncrement() + ")"))
                        .append(String.format("%-" + spacesAfterNumberOfMessageCount + "s", value))
                        .append(username)
                        .append("</code>\n");
            }
        });
        responseText.append("${command.top.list.totalcaption}: <b>").append(total.get()).append("</b>");

        return responseText.toString();
    }

    private String getMethodNameByParam(String receivedParam) {
        receivedParam = receivedParam.toLowerCase();

        for (Map.Entry<String, Set<String>> entry : topListParamValuesMap.entrySet()) {
            for (String data : entry.getValue()) {
                String[] params = data.split(",");
                for (String param : params) {
                    if (param.equals(receivedParam)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;

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
