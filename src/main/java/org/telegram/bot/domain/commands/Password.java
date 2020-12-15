package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Password implements CommandParent<SendMessage> {

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        int symbolsCount = 8;
        String text = getTextMessage(update);

        if (text != null) {
            try {
                symbolsCount = Integer.parseInt(getTextMessage(update));
            } catch (NumberFormatException e) {
                speechService.getRandomMessageByTag("wrongInput");
            }
        }

        if (symbolsCount < 1) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        if (symbolsCount > 1024) {
            throw new BotException("Не, ну по-моему это уже перебор");
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(new RandomString(symbolsCount).nextString());
        sendMessage.setReplyToMessageId(update.getMessage().getMessageId());

        return sendMessage;
    }
}
