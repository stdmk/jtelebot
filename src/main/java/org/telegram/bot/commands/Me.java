package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Me implements Command{
    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String commandArgument = message.getCommandArgument();
        if (commandArgument.startsWith("/me ")) {
            commandArgument.substring(4).trim();
            return returnResponse();
        }
        String username = request.getMessage().getUser().getUsername();
        return returnResponse(new TextResponse(request.getMessage())
                .setText("*@" + username + " " + commandArgument));

    }
}
