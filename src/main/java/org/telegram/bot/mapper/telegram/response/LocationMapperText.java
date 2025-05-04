package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.LocationResponse;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;

@RequiredArgsConstructor
@Component
public class LocationMapperText implements TelegramTextApiMethodMapper {

    @Override
    public Class<? extends BotResponse> getMappingClass() {
        return LocationResponse.class;
    }

    public SendLocation map(BotResponse botResponse) {
        LocationResponse locationResponse = (LocationResponse) botResponse;

        SendLocation sendLocation = new SendLocation(locationResponse.getChatId().toString(), locationResponse.getLatitude(), locationResponse.getLongitude());
        sendLocation.setReplyToMessageId(locationResponse.getReplyToMessageId());

        return sendLocation;
    }

}
