package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Getid extends CommandParent<SendMessage> {
    @Override
    public SendMessage parse(Update update) {
        Long chatId = update.getMessage().getChatId();
        StringBuilder responseText = new StringBuilder();
        if (chatId < 0) {
            responseText.append("Айди этого чата: `").append(chatId).append("`\n");
        }

        responseText.append("Твой айди: `").append(update.getMessage().getFrom().getId()).append("`");

        return new SendMessage()
                .setChatId(chatId)
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode("Markdown")
                .setText(responseText.toString());
    }
}
