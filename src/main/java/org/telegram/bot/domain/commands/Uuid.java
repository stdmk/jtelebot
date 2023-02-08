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

    @Override
    public SendMessage parse(Update update) {
        StringBuilder text = new StringBuilder();
        UUID uuid = UUID.randomUUID();
        text.append("`").append(uuid).append("`");
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(text.toString());
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }
}
