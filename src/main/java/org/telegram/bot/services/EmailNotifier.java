package org.telegram.bot.services;

import org.telegram.bot.domain.model.request.BotRequest;

public interface EmailNotifier {
    void notify(BotRequest botRequest);
}
