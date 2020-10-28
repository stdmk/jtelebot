package org.telegram.bot;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.exception.BotException;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@AllArgsConstructor
public class Parser extends Thread {

    private final Logger log = LoggerFactory.getLogger(Parser.class);

    private final Bot bot;
    private final CommandParent command;
    private final Update update;

    @Override
    public void run() {
        if (command == null) {
            return;
        }
        log.debug("Find a command {}", command.toString());

        try {
            PartialBotApiMethod method = command.parse(update);
            if (method instanceof SendMessage) {
                log.info("To " + update.getMessage().getChatId() + ": " + ((SendMessage) method).getText());
                bot.execute((SendMessage) method);
            }
            else if (method instanceof SendPhoto) {
                log.info("To " + update.getMessage().getChatId() + ": sended photo " + ((SendPhoto) method).getCaption());
                bot.execute((SendPhoto) command.parse(update));
            }
        } catch (TelegramApiException e) {
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (BotException botException) {
            try {
                bot.execute(new SendMessage()
                        .setReplyToMessageId(update.getMessage().getMessageId())
                        .setChatId(update.getMessage().getChatId())
                        .setText(botException.getMessage()));
            } catch (TelegramApiException e) {
                log.error("Error: cannot send response: {}", e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
