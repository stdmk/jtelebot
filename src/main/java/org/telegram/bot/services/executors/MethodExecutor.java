package org.telegram.bot.services.executors;

import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * Telegram method handler.
 */
public interface MethodExecutor {

    List<String> notErrorExceptionMessages = List.of(
            "Can't find end of the entity starting at byte offset",
            "new message content and reply markup are exactly the same as a current content and reply markup of the message"
    );

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

    default boolean isError(TelegramApiException e) {
        String errorMessage = e.getMessage();
        return notErrorExceptionMessages.stream().noneMatch(errorMessage::contains);
    }
}
