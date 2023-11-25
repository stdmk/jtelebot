package org.telegram.bot.commands.setters;

import org.telegram.bot.enums.AccessLevel;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Setter<T extends PartialBotApiMethod<?>> {

    boolean canProcessed(String command);
    AccessLevel getAccessLevel();
    T set(Update update, String commandText);

    default Message getMessageFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage();
        } else if (update.hasEditedMessage()) {
            return update.getEditedMessage();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage();
        }

        return null;
    }
}
