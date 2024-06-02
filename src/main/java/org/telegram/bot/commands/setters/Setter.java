package org.telegram.bot.commands.setters;

import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.enums.AccessLevel;

public interface Setter<T extends BotResponse> {
    boolean canProcessed(String command);
    AccessLevel getAccessLevel();
    T set(BotRequest update, String commandText);
}
