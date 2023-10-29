package org.telegram.bot.services.executors;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Telegram method handler.
 */
public interface MethodExecutor {

    /**
     * Get the name of the handled method.
     *
     * @return name of the method.
     */
    String getMethod();

    /**
     * Execute telegram method.
     *
     * @param method handling method.
     * @param update handling update.
     */
    void executeMethod(PartialBotApiMethod<?> method, Update update);

    void executeMethod(PartialBotApiMethod<?> method);
}
