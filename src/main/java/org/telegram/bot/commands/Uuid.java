package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.Command;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class Uuid implements Command<SendMessage> {

    @Override
    public SendMessage parse(Update update) {
        if (cutCommandInText(getMessageFromUpdate(update).getText()) != null) {
            return null;
        }

        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText("`" + UUID.randomUUID() + "`");
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());
        sendMessage.enableMarkdown(true);

        return sendMessage;
    }
}
