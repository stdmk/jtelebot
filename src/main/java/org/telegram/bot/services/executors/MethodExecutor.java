package org.telegram.bot.services.executors;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

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
     * @param message handling message.
     */
    void executeMethod(PartialBotApiMethod<?> method, Message message);
}
