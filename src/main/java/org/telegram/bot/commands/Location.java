package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.coordinates.Coordinates;
import org.telegram.bot.utils.coordinates.CoordinatesUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Location implements Command<SendLocation> {

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<SendLocation> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = cutCommandInText(message.getText());

        if (textMessage == null) {
            return Collections.emptyList();
        }
        bot.sendLocation(chatId);

        Coordinates coordinates = CoordinatesUtils.parseCoordinates(textMessage);
        if (coordinates == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        SendLocation sendLocation = new SendLocation();
        sendLocation.setReplyToMessageId(message.getMessageId());
        sendLocation.setChatId(chatId);
        sendLocation.setLatitude(coordinates.getLatitude());
        sendLocation.setLongitude(coordinates.getLongitude());

        return returnOneResult(sendLocation);
    }

}
