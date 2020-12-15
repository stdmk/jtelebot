package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;

@Component
@AllArgsConstructor
public class Logs implements CommandParent<SendDocument> {

    @Override
    public SendDocument parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        if (chatId < 0) {
            chatId = message.getFrom().getId().longValue();
        }

        File logs = new File("logs/log.log");
        if (!logs.exists()) {
            throw new BotException("Error: unable to find log.log file");
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());
        sendDocument.setReplyToMessageId(message.getMessageId());
        sendDocument.setDocument(new InputFile(logs, "logs"));

        return sendDocument;
    }
}
