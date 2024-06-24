package org.telegram.bot.services.executors;

import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * Telegram method handler.
 */
public interface MethodExecutor {

    List<String> notErrorExceptionMessages = List.of(
            "Can't find end of the entity starting at byte offset",
            "new message content and reply markup are exactly the same as a current content and reply markup of the message",
            "message to delete not found"
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
     * @param request handling request.
     */
    void executeMethod(PartialBotApiMethod<?> method, BotRequest request);

    void executeMethod(PartialBotApiMethod<?> method);

    default boolean isError(TelegramApiException e) {
        String errorMessage = e.getMessage();
        return notErrorExceptionMessages.stream().noneMatch(errorMessage::contains);
    }
}
