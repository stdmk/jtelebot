package org.telegram.bot.mapper.email.response;

import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;

public interface EmailResponseMapper {
    Class<? extends BotResponse> getMappingClass();
    EmailResponse map(BotResponse botResponse);
}
