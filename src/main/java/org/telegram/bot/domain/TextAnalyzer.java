package org.telegram.bot.domain;

import org.telegram.bot.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TextAnalyzer {
    void analyze(Bot bot, CommandParent<?> command, Update update);
}
