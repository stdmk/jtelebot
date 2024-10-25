package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.utils.TelegramUtils;

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
public class Where implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (TelegramUtils.isPrivateChat(message.getChat())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.COMMAND_FOR_GROUP_CHATS));
        }

        Integer messageId = message.getMessageId();

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.where.commandwaitingstart}";
        } else {
            User user = userService.get(commandArgument);
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

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(messageId)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }
}
