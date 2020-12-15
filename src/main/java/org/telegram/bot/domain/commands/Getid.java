package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Getid implements CommandParent<SendMessage> {

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        StringBuilder responseText = new StringBuilder();
        Long chatId = message.getChatId();

        if (chatId < 0) {
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
