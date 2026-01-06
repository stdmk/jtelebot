package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.Emoji;
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
                responseText = Emoji.CHECK_MARK.getSymbol() + " UUID <b>${command.uuid.valid}</b>";
            } else {
                responseText = Emoji.DELETE.getSymbol() + " UUID <b>${command.uuid.invalid}</b>";
            }
        } else {
            responseText = "<code>" + UUID.randomUUID() + "</code>";
        }

        return returnResponse(new TextResponse(request.getMessage())
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
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
