package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.LocationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationResponseResponseMapperTest {

    private final LocationResponseResponseMapper locationResponseResponseMapper = new LocationResponseResponseMapper();

    @Test
    void getMappingClassTest() {
        Class<LocationResponse> expected = LocationResponse.class;
        Class<? extends BotResponse> actual = locationResponseResponseMapper.getMappingClass();
        assertEquals(expected, actual);
    }

    @Test
    void mapTest() {
        double latitude = 1.23;
        double longitude = 3.21;
        LocationResponse locationResponse = new LocationResponse(new Message().setChat(new Chat()))
                .setLatitude(latitude)
                .setLongitude(longitude);

        EmailResponse emailResponse = locationResponseResponseMapper.map(locationResponse);

        assertEquals("latitude: " + latitude + " longitude: " + longitude, emailResponse.getText());
    }

}