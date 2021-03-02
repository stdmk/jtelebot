package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.HolidayService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Service
@AllArgsConstructor
public class Holidays implements CommandParent<SendMessage> {

    private final HolidayService holidayService;
    private final ChatService chatService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        StringBuilder buf = new StringBuilder();

        if (textMessage == null) {
            buf.append("<b><u>Ближайшие праздники:</u></b>\n");
            getCommingHolidays(chatService.get(message.getChatId())).forEach(holiday -> buf.append(buildStringOfHoliday(holiday)));
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    private List<Holiday> getCommingHolidays(Chat chat) {
        final LocalDate dateNow = LocalDate.now();

        return holidayService.get(chat)
                .stream()
                .filter(holiday -> holiday.getDate().getMonth().getValue() >= dateNow.getMonth().getValue() && holiday.getDate().getDayOfMonth() >= dateNow.getDayOfMonth())
                .sorted(Comparator.comparing(holiday -> holiday.getDate().getDayOfYear()))
                .collect(Collectors.toList());
    }

    private String buildStringOfHoliday(Holiday holiday) {
        LocalDate date = holiday.getDate();
        String dayOfWeek = holiday.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("ru"));

        return "<b>" + date.getMonth().getValue() + "." + String.format("%02d", date.getDayOfMonth()) + " (" + dayOfWeek + ") </b> — " + holiday.getName() + " (" + getLinkToUser(holiday.getUser(), true) + ")";
    }
}
