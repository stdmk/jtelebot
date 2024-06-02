package org.telegram.bot.commands;

import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;

import java.util.List;

public interface MessageAnalyzer {
    List<BotResponse> analyze(BotRequest request);
}
