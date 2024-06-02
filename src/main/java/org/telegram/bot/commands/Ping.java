package org.telegram.bot.commands;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class Ping implements Command {

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime dateTimeOfMessage = message.getDateTime();

        ZoneOffset zoneOffSet = ZoneId.systemDefault().getRules().getOffset(dateTimeNow);
        float diff = (float) (dateTimeNow.toInstant(zoneOffSet).toEpochMilli() - dateTimeOfMessage.toInstant(zoneOffSet).toEpochMilli()) / 1000;

        return returnResponse(new TextResponse(message)
                .setText("${command.ping.caption}: " + diff + " ${command.ping.seconds}."));
    }
}
