package org.telegram.bot.domain;

import org.telegram.bot.Bot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TextAnalyzer {
    void analyze(Bot bot, CommandParent<?> command, Update update);

    default void waitForThread(Thread thread, Message message, String text) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        message.setText(text);
    }
}
