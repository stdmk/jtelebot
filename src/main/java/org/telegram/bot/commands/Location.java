package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.LocationResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.coordinates.Coordinates;
import org.telegram.bot.utils.coordinates.CoordinatesUtils;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Location implements Command {

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        String commandArgument = message.getCommandArgument();

        if (commandArgument == null) {
            return returnResponse();
        }
        bot.sendLocation(chatId);

        Coordinates coordinates = CoordinatesUtils.parseCoordinates(commandArgument);
        if (coordinates == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return returnResponse(new LocationResponse(message)
                .setLatitude(coordinates.latitude())
                .setLongitude(coordinates.longitude()));
    }

}
