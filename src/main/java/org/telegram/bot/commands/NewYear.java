package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.services.UserCityService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.telegram.bot.utils.DateUtils.durationToString;

@Component
@RequiredArgsConstructor
public class NewYear implements Command<SendMessage> {

    private static final String DEFAULT_TIME_ZONE = "GMT+03:00";

    private final Bot bot;
    private final UserCityService userCityService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        if (cutCommandInText(message.getText()) != null) {
            return null;
        }
        bot.sendTyping(message.getChatId());

        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());
        ZoneId userTimeZone;

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null) {
            userTimeZone = ZoneId.of(DEFAULT_TIME_ZONE);
        } else {
            userTimeZone = ZoneId.of(userCity.getCity().getTimeZone());
        }

        ZonedDateTime dateTimeNow = ZonedDateTime.now(userTimeZone);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText("${command.newyear.caption}: *" + durationToString(dateTimeNow.toLocalDateTime(), dateTimeNow.plusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay()) +
                            "* (" + userTimeZone + ")");

        return sendMessage;
    }
}
