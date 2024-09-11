package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.DeleteResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;

import java.util.ArrayList;
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

        String username = message.getUser().getUsername();
        return returnResponse(
                new TextResponse(request.getMessage())
                .setText("* @" + username + " " + commandArgument),
                new DeleteResponse(message));

    }


}
