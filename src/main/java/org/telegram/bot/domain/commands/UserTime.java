package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.telegram.bot.utils.DateUtils.formatTime;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTime implements CommandParent<SendMessage> {

    private final UserService userService;
    private final ChatService chatService;
    private final UserCityService userCityService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        User user;
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                user = userService.get(repliedMessage.getFrom().getId());
            } else {
                user = userService.get(message.getFrom().getId());
            }
        } else {
            user = userService.get(textMessage);
        }

        log.debug("Request to get time of user {}", user);
        UserCity userCity = userCityService.get(user, chatService.get(message.getChatId()));
        if (userCity == null) {
            responseText = "У " + getLinkToUser(user, false) + " не задан город";
        } else {
            String dateTimeNow = formatTime(ZonedDateTime.now(ZoneId.of(userCity.getCity().getTimeZone())));
            responseText = "У " + getLinkToUser(user, false) + " сейчас *" + dateTimeNow + "*";
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }
}