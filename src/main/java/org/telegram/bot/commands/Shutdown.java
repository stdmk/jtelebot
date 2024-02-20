package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Shutdown implements Command<SendMessage> {

    private final Bot bot;
    private final SendMessageExecutor sendMessageExecutor;
    private final ConfigurableApplicationContext configurableApplicationContext;
    private final BotStats botStats;
    private final FileManagerTimer fileManagerTimer;

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);

        if (cutCommandInText(message.getText()) != null) {
            return Collections.emptyList();
        }

        bot.sendTyping(update);
        log.debug("From {} received command to shutdown", update.getMessage().getFrom());

        try {
            botStats.saveStats();
            fileManagerTimer.deleteAllFiles();
        } catch (Exception e) {
            log.error("Failed to shutdown normally: {}", e.getMessage());
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("${command.shutdown.switchingoff}...");

        sendMessageExecutor.executeMethod(sendMessage);

        configurableApplicationContext.close();
        System.exit(0);

        return Collections.emptyList();
    }
}
