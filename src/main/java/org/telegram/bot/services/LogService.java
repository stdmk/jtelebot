package org.telegram.bot.services;

import org.telegram.bot.domain.model.request.Message;

public interface LogService {
    void log(Message message);
}
