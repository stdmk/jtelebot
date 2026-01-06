package org.telegram.bot.services.executors.email;

import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;

import java.util.List;

/**
 * Telegram method handler.
 */
public interface EmailExecutor {

    /**
     * Execute telegram method.
     *
     * @param botResponses handling responses.
     * @param request handling request.
     */
    void execute(List<BotResponse> botResponses, BotRequest request);

    void execute(BotResponse botResponse);

}
