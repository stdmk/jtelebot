package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;

@Component
@AllArgsConstructor
public class NewYear implements CommandParent<SendMessage> {

    private final UserService userService;
    private final ChatService chatService;
    private final UserCityService userCityService;

    @Override
    public SendMessage parse(Update update) {
        final String defaultTimeZone = "GMT+03:00";

        Message message = getMessageFromUpdate(update);
        Chat chat = chatService.get(message.getChatId());
        User user = userService.get(message.getFrom().getId());
        ZoneId userTimeZone;

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null) {
            userTimeZone = ZoneId.of(defaultTimeZone);
        } else {
            userTimeZone = ZoneId.of(userCity.getCity().getTimeZone());
        }

        ZonedDateTime dateTimeNow = ZonedDateTime.now(userTimeZone);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("До нового года осталось: *" + deltaDatesToString(dateTimeNow.toLocalDateTime(), dateTimeNow.plusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay()) +
                            "* (" + userTimeZone.toString() + ")");

        return sendMessage;
    }
}
