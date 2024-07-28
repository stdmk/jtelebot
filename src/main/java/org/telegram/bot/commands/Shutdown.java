package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.timers.FileManagerTimer;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Shutdown implements Command {

    private final Bot bot;
    private final ConfigurableApplicationContext configurableApplicationContext;
    private final BotStats botStats;
    private final FileManagerTimer fileManagerTimer;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        bot.sendTyping(request.getMessage().getChatId());
        log.debug("From {} received command to shutdown", message.getUser());

        try {
            botStats.saveStats();
        } catch (Exception e) {
            log.error("Failed to shutdown normally: {}", e.getMessage());
        }

        try {
            fileManagerTimer.deleteAllFiles();
        } catch (Exception e) {
            log.error("Failed to shutdown normally: {}", e.getMessage());
        }

        bot.sendMessage(new TextResponse(message)
                .setText("${command.shutdown.switchingoff}..."));

        configurableApplicationContext.close();
        System.exit(0);

        return returnResponse();
    }
}
