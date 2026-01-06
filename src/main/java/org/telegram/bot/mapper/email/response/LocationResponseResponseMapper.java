package org.telegram.bot.mapper.email.response;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.domain.model.response.LocationResponse;

@Component
public class LocationResponseResponseMapper implements EmailResponseMapper {

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return LocationResponse.class;
    }

    @Override
    public EmailResponse map(BotResponse botResponse) {
        LocationResponse locationResponse = (LocationResponse) botResponse;

        return new EmailResponse()
                .setText("latitude: " + locationResponse.getLatitude() + " longitude: " + locationResponse.getLongitude());
    }
}
