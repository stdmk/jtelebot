package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;
import static org.telegram.bot.utils.TextUtils.removeCapital;

@Component
@RequiredArgsConstructor
@Slf4j
public class Top implements Command {

    private static final long MIN_SPACES_AFTER_NUMBER_OF_MESSAGE_COUNT = 6L;

    private final Bot bot;
    private final UserStatsService userStatsService;
    private final UserService userService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;

    private final Map<String, Set<String>> topListParamValuesMap = new ConcurrentHashMap<>();
    private String topListMonthlyParam;

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

        this.topListMonthlyParam = internationalizationService.internationalize("${command.top.list.monthly}", null);
    }

    @Override
    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String responseText;
        Chat chat = request.getMessage().getChat();

        User user;
        String commandArgument = message.getCommandArgument();
        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();

            Long userId;
            userId = Objects.requireNonNullElse(repliedMessage, message).getUser().getUserId();
            user = new User().setUserId(userId);

            log.debug("Request to get top of user {} for chat {}", user, chat);
            responseText = getTopOfUser(chat, user);
        } else {
            user = userService.get(commandArgument);
            if (user != null) {
                log.debug("Request to get top of user {} for chat {}", user, chat);
                responseText = getTopOfUser(chat, user);
            } else {
                log.debug("Request to get top of users for chat {} by param {}", chat, commandArgument);
                try {
                    responseText = getTopListOfUsers(chat, commandArgument);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)
                        .setNotification(false)));
    }

    public TextResponse getTopByChat(Chat chat) throws InvocationTargetException, IllegalAccessException {
        return new TextResponse()
                .setChatId(chat.getChatId())
                .setText(getTopListOfUsers(chat, topListMonthlyParam) + "\n${command.top.monthlyclearcaption}")
                .setResponseSettings(FormattingStyle.HTML);
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
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getSymbol();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getSymbol();
        }

        fieldsOfStats.put(Emoji.EMAIL.getSymbol() + "${command.top.userstats.messages}", userStats.getNumberOfMessages().toString());
        fieldsOfStats.put(karmaEmoji + "${command.top.userstats.karma}", userStats.getNumberOfKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getSymbol() + "${command.top.userstats.kindness}", userStats.getNumberOfGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getSymbol() + "${command.top.userstats.wickedness}", userStats.getNumberOfWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getSymbol() + "${command.top.userstats.stickers}", userStats.getNumberOfStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getSymbol() + "${command.top.userstats.images}", userStats.getNumberOfPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getSymbol() + "${command.top.userstats.animations}", userStats.getNumberOfAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getSymbol() + "${command.top.userstats.music}", userStats.getNumberOfAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getSymbol() + "${command.top.userstats.documents}", userStats.getNumberOfDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getSymbol() + "${command.top.userstats.videos}", userStats.getNumberOfVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getSymbol() + "${command.top.userstats.videomessages}", userStats.getNumberOfVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getSymbol() + "${command.top.userstats.voices}", userStats.getNumberOfVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getSymbol() + "${command.top.userstats.commands}", userStats.getNumberOfCommands().toString());

        buf.append("<b>").append(getLinkToUser(userStats.getUser(), true)).append("</b>\n").append("<u>${command.top.permonth}:</u>\n");

        fieldsOfStats.entrySet()
                .stream()
                .filter(entry -> !valueForSkip.equals(entry.getValue()))
                .forEach(entry -> buf.append(entry.getKey()).append(": <b>").append(entry.getValue()).append("</b>\n"));
        buf.append("\n");

        fieldsOfStats.clear();
        buf.append("<u>${command.top.userstats.total}:</u>\n");

        if (userStats.getNumberOfAllKarma() >= 0) {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HALO.getSymbol();
        } else {
            karmaEmoji = Emoji.SMILING_FACE_WITH_HORNS.getSymbol();
        }

        fieldsOfStats.put(Emoji.EMAIL.getSymbol() + "${command.top.userstats.messages}", userStats.getNumberOfAllMessages().toString());
        fieldsOfStats.put(karmaEmoji + "${command.top.userstats.karma}", userStats.getNumberOfAllKarma().toString());
        fieldsOfStats.put(Emoji.RED_HEART.getSymbol() + "${command.top.userstats.kindness}", userStats.getNumberOfAllGoodness().toString());
        fieldsOfStats.put(Emoji.BROKEN_HEART.getSymbol() + "${command.top.userstats.wickedness}", userStats.getNumberOfAllWickedness().toString());
        fieldsOfStats.put(Emoji.PICTURE.getSymbol() + "${command.top.userstats.stickers}", userStats.getNumberOfAllStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getSymbol() + "${command.top.userstats.images}", userStats.getNumberOfAllPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getSymbol() + "${command.top.userstats.animations}", userStats.getNumberOfAllAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getSymbol() + "${command.top.userstats.music}", userStats.getNumberOfAllAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getSymbol() + "${command.top.userstats.documents}", userStats.getNumberOfAllDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getSymbol() + "${command.top.userstats.videos}", userStats.getNumberOfAllVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getSymbol() + "${command.top.userstats.videomessages}", userStats.getNumberOfAllVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getSymbol() + "${command.top.userstats.voices}", userStats.getNumberOfAllVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getSymbol() + "${command.top.userstats.commands}", userStats.getNumberOfAllCommands().toString());

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
    private String getTopListOfUsers(Chat chat, String param) throws InvocationTargetException, IllegalAccessException {
        log.debug("Request to top by {} for chat {}", param, chat);

        final String loweredParam = param.toLowerCase(Locale.ROOT);
        String methodName = getMethodNameByParam(loweredParam);
        if (methodName == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Set<String> totalParams = topListParamValuesMap.get("getNumberOfAllMessages")
                .stream()
                .map(csv -> csv.split(","))
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        Set<String> dailyParams = topListParamValuesMap.get("getNumberOfMessagesPerDay")
                .stream()
                .map(csv -> csv.split(","))
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        boolean equalsTotalParam = totalParams.contains(loweredParam);
        boolean endsWithTotalParam = totalParams.stream().anyMatch(loweredParam::endsWith);
        boolean equalsDailyParam = dailyParams.contains(loweredParam);
        boolean endWithDailyParam = dailyParams.stream().anyMatch(loweredParam::endsWith);
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
        int spacesAfterNumberOfMessageCount = getMinSpacesAfterNumberOfMessageCount(method, userStatsList);

        StringBuilder responseText = new StringBuilder("<b>${command.top.list.caption} ").append(param).append(":</b>\n");
        int counter = 1;
        long total = 0L;

        for (UserStats userStats : userStatsList) {
            long value = Long.parseLong(method.invoke(userStats).toString());

            if (value != 0) {
                total = total + value;
                User user = userStats.getUser();
                String username = user.getUsername();

                responseText
                        .append(getLinkToUser(user, true, "@")).append(" ")
                        .append("<code>")
                        .append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter + ")"))
                        .append(String.format("%-" + spacesAfterNumberOfMessageCount + "s", value))
                        .append(username)
                        .append("</code>\n");

                counter = counter + 1;
            }
        }

        responseText.append("${command.top.list.totalcaption}: <b>").append(total).append("</b>");

        return responseText.toString();
    }

    private String getMethodNameByParam(String receivedParam) {
        if (receivedParam == null) {
            return null;
        }

        receivedParam = receivedParam.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, Set<String>> entry : topListParamValuesMap.entrySet()) {
            for (String data : entry.getValue()) {
                String[] params = data.split(",");
                for (String param : params) {
                    if (receivedParam.startsWith(param)) {
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
    private Integer getMinSpacesAfterNumberOfMessageCount(Method method, List<UserStats> userStatsList) throws InvocationTargetException, IllegalAccessException {
        List<Long> values = new ArrayList<>(userStatsList.size());
        for (UserStats userStats : userStatsList) {
            Long parseLong = Long.parseLong(method.invoke(userStats).toString());
            values.add(parseLong);
        }

        int maxValueLength = values
                .stream()
                .max(Long::compareTo)
                .orElse(MIN_SPACES_AFTER_NUMBER_OF_MESSAGE_COUNT)
                .toString()
                .length();

        boolean valuesHasNegative = values.stream().anyMatch(value -> value < 0);
        if (valuesHasNegative) {
            int minValueLength = values
                    .stream()
                    .min(Long::compareTo)
                    .orElse(MIN_SPACES_AFTER_NUMBER_OF_MESSAGE_COUNT)
                    .toString()
                    .length();

            if (minValueLength > maxValueLength) {
                return minValueLength + 1;
            }
        }

        return maxValueLength + 1;
    }

}
