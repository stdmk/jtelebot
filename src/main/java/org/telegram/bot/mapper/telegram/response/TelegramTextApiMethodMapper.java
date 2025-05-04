package org.telegram.bot.mapper.telegram.response;

import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

public interface TelegramTextApiMethodMapper {
    Class<? extends BotResponse> getMappingClass();
    PartialBotApiMethod<?> map(BotResponse botResponse);
}
