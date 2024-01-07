package org.telegram.bot.domain;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface MessageAnalyzer {
    void analyze(Update update);
}
