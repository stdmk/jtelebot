package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Getid implements CommandParent<SendMessage> {

    private final UserService userService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = getTextMessage(update);
        StringBuilder responseText = new StringBuilder();
        Long chatId = message.getChatId();

        if (textMessage != null) {
            log.debug("Request to getting telegram id of {}", textMessage);
            User user = userService.get(textMessage);
            if (user == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            responseText.append("Айди ").append(getLinkToUser(user, false)).append(": `").append(user.getUserId()).append("`\n");
        }

        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage != null) {
            org.telegram.telegrambots.meta.api.objects.User repliedUser = repliedMessage.getFrom();
            log.debug("Request to getting telegram id of {}", repliedUser);
            User user = userService.get(repliedUser.getId());
            responseText.append("Айди ").append(user.getUsername()).append(": `").append(user.getUserId()).append("`\n");
        }

        if (chatId < 0) {
            log.debug("Request to getting telegram id of public chat {}", message.getChat());
            responseText.append("Айди этого чата: `").append(chatId).append("`\n");
        }

        responseText.append("Твой айди: `").append(message.getFrom().getId()).append("`");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText.toString());

        return sendMessage;
    }
}
