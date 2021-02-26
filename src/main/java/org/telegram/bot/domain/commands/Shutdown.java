package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@AllArgsConstructor
public class Shutdown implements CommandParent<SendMessage> {

    private final ConfigurableApplicationContext configurableApplicationContext;
    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) throws Exception {
        botStats.saveStats();

        configurableApplicationContext.close();
        System.exit(0);

        return null;
    }
}
