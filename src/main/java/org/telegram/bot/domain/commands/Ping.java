package org.telegram.bot.domain.commands;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Component
public class Ping implements CommandParent<SendMessage> {
    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (cutCommandInText(message.getText()) != null) {
            return null;
        }

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
