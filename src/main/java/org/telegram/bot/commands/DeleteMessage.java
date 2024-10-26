package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DeleteMessage implements Command {

    @Override
    public List<BotResponse> parse(BotRequest request) {
        List<BotResponse> result = new ArrayList<>(2);

        Message message = request.getMessage();
        if (!message.hasCommandArgument()) {
            result.add(new DeleteResponse(message));

            Message replyToMessage = message.getReplyToMessage();
            if (replyToMessage != null) {
                result.add(new DeleteResponse(message.getChat(), replyToMessage.getMessageId()));
            }

        }

        return result;
    }

}
