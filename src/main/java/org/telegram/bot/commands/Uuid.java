package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;

import java.util.List;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class Uuid implements Command {

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String responseText;

        String commandArgument = request.getMessage().getCommandArgument();
        if (commandArgument != null) {
            if (isValidUuid(commandArgument)) {
                responseText = "*${command.uuid.valid}*";
            } else {
                responseText = "*${command.uuid.invalid}*";
            }
        } else {
            responseText = "`" + UUID.randomUUID() + "`";
        }

        return returnResponse(new TextResponse(request.getMessage())
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private boolean isValidUuid(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

}
