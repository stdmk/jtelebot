package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;

@Component
@AllArgsConstructor
public class Where implements CommandParent<SendMessage> {

    private final SpeechService speechService;
    private final UserStatsService userStatsService;

    @Override
    public SendMessage parse(Message message) throws Exception {
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage == null) {
            //TODO добавить ожидание команд
            throw new BotException("wrongInput");
        }

        UserStats userStats = userStatsService.get(message.getChatId(), message.getFrom().getId());
        if (userStats == null) {
            throw new BotException(speechService.getRandomMessageByTag("userNotFount"));
        }

        LocalDateTime dateOfMessage = userStats.getLastMessage().getDate();
        ZoneId zoneId = ZoneId.systemDefault();

        responseText = "последний раз пользователя *" + userStats.getUser().getUsername() +
                        "* я видел " + dateOfMessage + " ("+zoneId.getId()+").\n" +
                        "Молчит уже " + deltaDatesToString(LocalDateTime.now(), dateOfMessage);


        return new SendMessage()
                .setChatId(message.getChatId())
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText(responseText);
    }
}
