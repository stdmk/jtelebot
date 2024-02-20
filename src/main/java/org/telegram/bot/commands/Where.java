package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Where implements Command<SendMessage> {

    private final Bot bot;
    private final SpeechService speechService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (message.getChatId() > 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        Integer messageId = message.getMessageId();
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.where.commandwaitingstart}";
        } else {
            User user = userService.get(textMessage);
            Chat chat = new Chat().setChatId(message.getChatId());

            if (user == null) {
                return Collections.emptyList();
            }

            bot.sendTyping(message.getChatId());
            log.debug("Request to get last message of user {} for chat {}", user, chat);
            UserStats userStats = userStatsService.get(chat, user);

            LastMessage lastMessage = userStats.getLastMessage();
            messageId = lastMessage.getMessageId();
            LocalDateTime dateOfMessage = lastMessage.getDate();
            ZoneId zoneId = ZoneId.systemDefault();

            responseText = "${command.where.lasttime} <b>" + getLinkToUser(user, true) +
                    "</b> ${command.where.saw} " + formatDateTime(dateOfMessage) + " (" + zoneId.getId() + ")\n" +
                    "${command.where.silent} " + deltaDatesToString(dateOfMessage, LocalDateTime.now());
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return returnOneResult(sendMessage);
    }
}
