package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Echo implements CommandParent<SendMessage> {
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());
        sendMessage.setText(speechService.getRandomMessageByTag("echo"));

        return sendMessage;
    }
}
