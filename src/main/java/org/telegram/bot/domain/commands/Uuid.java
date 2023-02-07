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

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class Uuid implements CommandParent<SendMessage> {

    private final UserService userService;
    private final SpeechService speechService;


    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = getTextMessage(update);
        StringBuilder responseText = new StringBuilder();
        UUID uuid = UUID.randomUUID();


        if (textMessage != null) {
            log.debug("Request to getting telegram id of {}", textMessage);
            User user = userService.get(textMessage);
            if (user == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        responseText.append("`").append(uuid).append("`");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText.toString());


        return sendMessage;
    }
}
