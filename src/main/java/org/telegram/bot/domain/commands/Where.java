package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Where implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final UserService userService;
    private final ChatService chatService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public SendMessage parse(Update update) {
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
            responseText = "теперь напиши мне username того, кого хочешь найти";
        } else {
            User user = userService.get(textMessage);
            Chat chat = chatService.get(message.getChatId());

            //чтобы команда не срабатывала на каждое слово "Где"
            if (user == null) {
                return null;
            }

            log.debug("Request to get last message of user {} for chat {}", user, chat);
            UserStats userStats = userStatsService.get(chat, user);

            LastMessage lastMessage = userStats.getLastMessage();
            messageId = lastMessage.getMessageId();
            LocalDateTime dateOfMessage = lastMessage.getDate();
            ZoneId zoneId = ZoneId.systemDefault();

            responseText = "последний раз пользователя <b>" + getLinkToUser(user, true) +
                    "</b> я видел " + formatDateTime(dateOfMessage) + " (" + zoneId.getId() + ")\n" +
                    "Молчит уже " + deltaDatesToString(dateOfMessage, LocalDateTime.now());
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }
}
