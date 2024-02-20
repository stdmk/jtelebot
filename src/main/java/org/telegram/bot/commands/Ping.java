package org.telegram.bot.commands;

import org.springframework.stereotype.Component;
import org.telegram.bot.domain.Command;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Component
public class Ping implements Command<SendMessage> {
    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (cutCommandInText(message.getText()) != null) {
            return Collections.emptyList();
        }

        ZoneId zoneId = ZoneId.of("UTC");

        LocalDateTime dateTimeNow = LocalDateTime.now(zoneId);
        LocalDateTime dateTimeOfMessage = LocalDateTime.ofInstant(Instant.ofEpochSecond(message.getDate()), zoneId);

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(dateTimeNow);
        float diff = (float) (dateTimeNow.toInstant(zoneOffSet).toEpochMilli() - dateTimeOfMessage.toInstant(zoneOffSet).toEpochMilli()) / 1000;

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("${command.ping.caption}: " + diff + " ${command.ping.seconds}.");

        return returnOneResult(sendMessage);
    }
}
