package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;

@Component
@AllArgsConstructor
public class Logs implements CommandParent<SendDocument> {

    @Override
    public SendDocument parse(Update update) throws Exception {
        Long chatId = update.getMessage().getChatId();
        if (chatId < 0) {
            chatId = update.getMessage().getFrom().getId().longValue();
        }

        File logs = new File("logs/log.log");
        if (!logs.exists()) {
            throw new BotException("Error: unable to find log.log file");
        }

        return new SendDocument()
                .setChatId(chatId)
                .setDocument(logs);
    }
}
