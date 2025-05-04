package org.telegram.bot.mapper.telegram.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.LocationResponse;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationMapperTest {

    private final LocationMapperText locationMapper = new LocationMapperText();

    @Test
    void mapTest() {
        final Integer replyToMessageId = 12345;
        final Long chatId = 123L;
        final Double latitude = 123.123;
        final Double longitude = 321.321;

        Message message = new Message()
                .setMessageId(replyToMessageId)
                .setChat(new Chat().setChatId(chatId));
        LocationResponse locationResponse = new LocationResponse(message)
                .setLatitude(latitude)
                .setLongitude(longitude);

        SendLocation sendLocation = locationMapper.map(locationResponse);

        assertEquals(chatId.toString(), sendLocation.getChatId());
        assertEquals(latitude, sendLocation.getLatitude());
        assertEquals(longitude, sendLocation.getLongitude());
        assertEquals(replyToMessageId, sendLocation.getReplyToMessageId());
    }

}