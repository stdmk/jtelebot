package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class Uuid implements CommandParent<SendMessage> {

    @Override
    public SendMessage parse(Update update) {

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText("`" + UUID.randomUUID() + "`");
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }
}
