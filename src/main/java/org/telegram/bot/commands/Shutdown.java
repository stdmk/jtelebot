package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
@Slf4j
public class Shutdown implements Command<SendMessage> {

    private final Bot bot;
    private final ConfigurableApplicationContext configurableApplicationContext;
    private final BotStats botStats;
    private final FileManagerTimer fileManagerTimer;

    @Override
    public SendMessage parse(Update update) {
        if (cutCommandInText(getMessageFromUpdate(update).getText()) != null) {
            return null;
        }

        bot.sendTyping(update);
        log.debug("From {} received command to shutdown", update.getMessage().getFrom());

        try {
            botStats.saveStats();
            fileManagerTimer.deleteAllFiles();
        } catch (Exception e) {
            log.error("Failed to shutdown normally: {}", e.getMessage());
        }

        configurableApplicationContext.close();
        System.exit(0);

        return null;
    }
}
