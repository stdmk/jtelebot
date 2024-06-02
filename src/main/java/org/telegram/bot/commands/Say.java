package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.services.CommandWaitingService;

import java.util.List;

@RequiredArgsConstructor
@Component
public class Say implements Command {

    private final CommandWaitingService commandWaitingService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.say.commandwaitingstart}";
        } else {
            responseText = commandArgument;
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }
}
