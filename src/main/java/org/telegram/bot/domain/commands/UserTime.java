package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.services.CityService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTime implements CommandParent<SendMessage> {

    private final Bot bot;
    private final UserService userService;
    private final UserCityService userCityService;
    private final CityService cityService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        User user;
        Integer repliedMessageTime = null;
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                repliedMessageTime = repliedMessage.getDate();
                user = userService.get(repliedMessage.getFrom().getId());
            } else {
                user = userService.get(message.getFrom().getId());
            }
        } else {
            user = userService.get(textMessage);
        }

        UserCity userCity = userCityService.get(user, new Chat().setChatId(message.getChatId()));
        if (userCity == null) {
            City city = cityService.get(textMessage);
            if (city == null) {
                if (user == null) {
                    log.debug("Unable to find user or city {}", textMessage);
                    return null;
                }
                responseText = "У " + getLinkToUser(user, false) + " не задан город";
            } else {
                log.debug("Request to get time of city {}", city.getNameEn());
                String dateTimeNow = formatTime(ZonedDateTime.now(ZoneId.of(city.getTimeZone())));
                responseText = "В городе " + city.getNameRu() + " сейчас: *" + dateTimeNow + "*";
            }
        } else {
            log.debug("Request to get time of user {}", user);

            ZoneId userZoneId = ZoneId.of(userCity.getCity().getTimeZone());
            String dateTimeNow = formatTime(ZonedDateTime.now(userZoneId));
            responseText = "У " + getLinkToUser(user, false) + " сейчас *" + dateTimeNow + "*";
            if (repliedMessageTime != null) {
                String pastDateTime = formatDateTime(unixTimeToLocalDateTime(repliedMessageTime, userZoneId));
                responseText = responseText + "\n" + "На тот момент было: *" + pastDateTime + "*";
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }
}