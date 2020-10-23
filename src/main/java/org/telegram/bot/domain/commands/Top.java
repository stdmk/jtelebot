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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class Top extends CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Top.class);

    private final UserStatsService userStatsService;
    private final UserService userService;
    private final SpeechService speechService;

    private static final List<String> PARAMS = Arrays.asList("месяц", "все", "всё");

    @Override
    public SendMessage parse(Update update) throws BotException {
        String textMessage = TextUtils.cutCommandInText(update.getMessage().getText());
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

    private String getTopOfUsername(Long chatId, String username) throws BotException {
        log.debug("Request to get top of user by username {}", username);
        User user = userService.get(username);
        if (user == null) {
            throw new BotException(speechService.getRandomMessageByTag("userNotFount"));
        }

        UserStats userStats = userStatsService.get(chatId, user.getUserId());

        return "*" + user.getUsername() + "*\n" +
                "Сообщений: " + userStats.getNumberOfMessages() +
                "Всего сообщений: " + userStats.getNumberOfAllMessages();
    }

    private String getTopListOfUsers(Long chatId, String param) {
        log.debug("Request to top by {} for chat {}", param, chatId);
        StringBuilder responseText = new StringBuilder();
        List<UserStats> userList = userStatsService.getUsersByChatId(chatId);

        if (param.equals(PARAMS.get(0))) {
            userList = userList.stream()
                    .sorted(Comparator.comparing(UserStats::getNumberOfMessages).reversed())
                    .collect(Collectors.toList());
        }
        else if (param.equals(PARAMS.get(1)) || param.equals(PARAMS.get(2))) {
            userList = userList.stream()
                    .sorted(Comparator.comparing(UserStats::getNumberOfAllMessages).reversed())
                    .collect(Collectors.toList());
        }

        AtomicInteger counter = new AtomicInteger(1);
        responseText.append("*Топ по общению за ").append(param).append(":*\n```\n");
        userList.forEach(userStats -> responseText.append(counter.getAndIncrement()).append(") ")
                .append(userStats.getNumberOfMessages()).append(" ")
                .append(userStats.getUser().getUsername()).append("\n"));

        return responseText.append("```").toString();
    }
}
