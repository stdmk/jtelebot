package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
@Slf4j
public class Random implements Command {

    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String responseText;
        if (!message.hasCommandArgument()) {
            responseText = tossACoin();
        } else {
            String commandArgument = message.getCommandArgument();
            if (commandArgument.indexOf(" ") < 1) {
                responseText = getRandomUntil(commandArgument);
            } else {
                String[] strings = commandArgument.split(" ");
                if (strings.length == 2) {
                    responseText = getRandomBetweenTwoArguments(strings);
                } else {
                    responseText = getRandomInArray(strings);
                }
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }

    private String tossACoin() {
        if (getRandomInRange(0, 2) == 1) {
            return "${command.random.heads}";
        } else {
            return "${command.random.tails}";
        }
    }

    private String getRandomUntil(String commandArgument) {
        long limit;
        try {
            limit = Long.parseLong(commandArgument);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (Long.MAX_VALUE == limit) {
            limit = limit - 1;
        } else if (Long.MIN_VALUE == limit) {
            limit = limit + 1;
        }

        if (limit < 0) {
            return getRandomInRange(limit, 0).toString();
        } else if (limit == 0) {
            return getRandomInRange(0, Long.MAX_VALUE).toString();
        } else {
            return getRandomInRange(0, limit).toString();
        }

    }

    private String getRandomBetweenTwoArguments(String[] strings) {
        long min;
        long max;
        try {
            min = Long.parseLong(strings[0]);
            max = Long.parseLong(strings[1]);
            return getRandomInRange(min, max).toString();
        } catch (NumberFormatException e) {
            return strings[getRandomInRange(0, 1)];
        }
    }

    private String getRandomInArray(String[] strings) {
        return strings[getRandomInRange(0, strings.length)];
    }

}
