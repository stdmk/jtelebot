package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
@AllArgsConstructor
public class Ping implements CommandParent<SendMessage> {
    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        ZoneId zoneId = ZoneId.of("UTC");

        LocalDateTime dateTimeNow = LocalDateTime.now(zoneId);
        LocalDateTime dateTimeOfMessage = LocalDateTime.ofInstant(Instant.ofEpochSecond(message.getDate()), zoneId);

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(dateTimeNow);
        float diff = (float) (dateTimeNow.toInstant(zoneOffSet).toEpochMilli() - dateTimeOfMessage.toInstant(zoneOffSet).toEpochMilli()) / 1000;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("Пинг: " + diff + " с.");

        return sendMessage;
    }
}
