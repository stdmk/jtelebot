package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class Top implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Top.class);

    private final UserStatsService userStatsService;
    private final UserService userService;
    private final SpeechService speechService;

    private static final List<String> PARAMS = Arrays.asList("месяц", "все", "всё");

    @Override
    public SendMessage parse(Update update) throws BotException {
        String textMessage = cutCommandInText(update.getMessage().getText());
        String responseText;

        //TODO переделать на айди, если пользователь без юзернейма
        if (textMessage == null) {
            responseText = getTopOfUsername(update.getMessage().getChatId(), update.getMessage().getFrom().getUserName());
        } else {
            if (PARAMS.contains(textMessage)) {
                responseText = getTopListOfUsers(update.getMessage().getChatId(), textMessage);
            } else {
                responseText = getTopOfUsername(update.getMessage().getChatId(), textMessage);
            }
        }

        return new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(responseText);
    }

    public SendMessage getTopByChatId(Long chatId) {
        return new SendMessage()
                .setChatId(chatId)
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(getTopListOfUsers(chatId, PARAMS.get(0)));
    }

    @Transactional
    private String getTopOfUsername(Long chatId, String username) throws BotException {
        log.debug("Request to get top of user by username {}", username);
        User user = userService.get(username);
        if (user == null) {
            throw new BotException(speechService.getRandomMessageByTag("userNotFount"));
        }

        HashMap<String, String> fieldsOfStats = new HashMap<>();
        StringBuilder buf = new StringBuilder();
        String valueForSkip = "0";
        UserStats userStats = userStatsService.get(chatId, user.getUserId());
        if (userStats == null) {
            throw new BotException(speechService.getRandomMessageByTag("userNotFount"));
        }

        fieldsOfStats.put("Сообщений", userStats.getNumberOfMessages().toString());
        fieldsOfStats.put("Стикеров", userStats.getNumberOfStickers().toString());
        fieldsOfStats.put("Изображений", userStats.getNumberOfPhotos().toString());
        fieldsOfStats.put("Анимаций", userStats.getNumberOfAnimations().toString());
        fieldsOfStats.put("Музыки", userStats.getNumberOfAudio().toString());
        fieldsOfStats.put("Документов", userStats.getNumberOfDocuments().toString());
        fieldsOfStats.put("Видео", userStats.getNumberOfVideos().toString());
        fieldsOfStats.put("Видеосообщений", userStats.getNumberOfVideoNotes().toString());
        fieldsOfStats.put("Голосовых", userStats.getNumberOfVoices().toString());
        fieldsOfStats.put("Команд", userStats.getNumberOfCommands().toString());

        buf.append("*").append(userStats.getUser().getUsername()).append("*\n");
        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": ").append(value).append("\n");
            }
        });
        buf.append("-----------------------------\n");

        fieldsOfStats = new HashMap<>();
        buf.append("*Всего:*\n");

        fieldsOfStats.put("Сообщений", userStats.getNumberOfAllMessages().toString());
        fieldsOfStats.put("Стикеров", userStats.getNumberOfAllStickers().toString());
        fieldsOfStats.put("Изображений", userStats.getNumberOfAllPhotos().toString());
        fieldsOfStats.put("Анимаций", userStats.getNumberOfAllAnimations().toString());
        fieldsOfStats.put("Музыки", userStats.getNumberOfAllAudio().toString());
        fieldsOfStats.put("Документов", userStats.getNumberOfAllDocuments().toString());
        fieldsOfStats.put("Видео", userStats.getNumberOfAllVideos().toString());
        fieldsOfStats.put("Видеосообщений", userStats.getNumberOfAllVideoNotes().toString());
        fieldsOfStats.put("Голосовых", userStats.getNumberOfAllVoices().toString());
        fieldsOfStats.put("Команд", userStats.getNumberOfAllCommands().toString());

        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": ").append(value).append("\n");
            }
        });

        return buf.toString();
    }

    private String getTopListOfUsers(Long chatId, String param) {
        log.debug("Request to top by {} for chat {}", param, chatId);
        StringBuilder responseText = new StringBuilder();
        List<UserStats> userList = userStatsService.getUsersByChatId(chatId);

        int spacesAfterSerialNumberCount = String.valueOf(userList.size()).length() + 2;
        int spacesAfterNuberOfMessageCount = userList.stream()
                .map(UserStats::getNumberOfMessages)
                .max(Integer::compareTo)
                .orElse(6)
                .toString()
                .length() + 1;

        if (param.equals(PARAMS.get(0))) {
            userList = userList.stream()
                    .filter(userStats -> userStats.getNumberOfMessages() != 0)
                    .sorted(Comparator.comparing(UserStats::getNumberOfMessages).reversed())
                    .collect(Collectors.toList());
        }
        else if (param.equals(PARAMS.get(1)) || param.equals(PARAMS.get(2))) {
            userList = userList.stream()
                    .filter(userStats -> userStats.getNumberOfAllMessages() != 0)
                    .sorted(Comparator.comparing(UserStats::getNumberOfAllMessages).reversed())
                    .collect(Collectors.toList());
        }

        AtomicInteger counter = new AtomicInteger(1);
        responseText.append("*Топ по общению за ").append(param).append(":*\n```\n");
        userList.forEach(userStats ->
                responseText.append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter.getAndIncrement() + ")"))
                .append(String.format("%-" + spacesAfterNuberOfMessageCount + "s", userStats.getNumberOfMessages().toString()))
                .append(userStats.getUser().getUsername()).append("\n"));

        return responseText.append("```").toString();
    }
}
