package org.telegram.bot.services;

import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.response.TextResponse;

public interface SpyModeService {
    TextResponse generateResponse(User user, String textMessage);
}
