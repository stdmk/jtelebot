package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Getid implements CommandParent<SendMessage> {

    @Override
    public SendMessage parse(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        StringBuilder responseText = new StringBuilder();
        Long chatId = message.getChatId();

        if (chatId < 0) {
            responseText.append("Айди этого чата: `").append(chatId).append("`\n");
        }

        responseText.append("Твой айди: `").append(message.getFrom().getId()).append("`");

        return new SendMessage()
                .setChatId(chatId)
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(responseText.toString());
    }
}
