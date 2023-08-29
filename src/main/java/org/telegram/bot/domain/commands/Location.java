package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.coordinates.Coordinates;
import org.telegram.bot.utils.coordinates.CoordinatesUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@RequiredArgsConstructor
@Component
@Slf4j
public class Location implements CommandParent<SendLocation> {

    private final SpeechService speechService;

    @Override
    public SendLocation parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        String textMessage = cutCommandInText(message.getText());

        if (textMessage == null) {
            return null;
        }

        Coordinates coordinates = CoordinatesUtils.parseCoordinates(textMessage);
        if (coordinates == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        SendLocation sendLocation = new SendLocation();
        sendLocation.setReplyToMessageId(message.getMessageId());
        sendLocation.setChatId(chatId);
        sendLocation.setLatitude(coordinates.getLatitude());
        sendLocation.setLongitude(coordinates.getLongitude());

        return sendLocation;
    }

}
