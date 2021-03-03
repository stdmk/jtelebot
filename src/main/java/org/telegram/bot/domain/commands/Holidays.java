package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.HolidayService;
import org.telegram.bot.services.SpeechService;
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
import static org.telegram.bot.utils.DateUtils.formatDate;

@Service
@AllArgsConstructor
public class Holidays implements CommandParent<SendMessage> {

    private final HolidayService holidayService;
    private final ChatService chatService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        StringBuilder buf = new StringBuilder();

        if (textMessage == null) {
            buf.append("<u>Ближайшие праздники:</u>\n");
            getCommingHolidays(chatService.get(message.getChatId())).forEach(holiday -> buf.append(buildStringOfHoliday(holiday)));
        } else {
            int i = textMessage.indexOf(".");
            if (i < 0) {
                long holidayId;
                try {
                    holidayId = Long.parseLong(textMessage.substring(1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                Holiday holiday = holidayService.get(holidayId);
                if (holiday == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                buf.append(getHolidayDetails(holiday));
            } else {
                LocalDate requestedDate;
                try {
                    requestedDate = LocalDate.of(
                            LocalDate.now().getYear(),
                            Integer.parseInt(textMessage.substring(0, i)),
                            Integer.parseInt(textMessage.substring(i + 1)));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                getHolidaysForDate(chatService.get(message.getChatId()), requestedDate).forEach(holiday -> buf.append(buildStringOfHoliday(holiday)));
            }
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
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<Holiday> getHolidaysForDate(Chat chat, LocalDate date) {
        return holidayService.get(chat)
                .stream()
                .filter(holiday -> holiday.getDate().getMonth().getValue() == date.getMonth().getValue() && holiday.getDate().getDayOfMonth() == date.getDayOfMonth())
                .collect(Collectors.toList());
    }

    private String getHolidayDetails(Holiday holiday) {
        String date;
        LocalDate storedDate = holiday.getDate();
        LocalDate dateOfHoliday = getDateOfHoliday(storedDate);

        if (storedDate.getYear() == 1) {
            date = formatDate(dateOfHoliday);
        } else {
            date = formatDate(storedDate);
        }

        return "<u>" + holiday.getName() + "</u>\n" +
                "<i>" + date + " " + getDayOfWeek(dateOfHoliday) + "</i> " + getNumberOfYear(storedDate, dateOfHoliday) + "\n" +
                "Автор: " + getLinkToUser(holiday.getUser(), true);
    }

    private String buildStringOfHoliday(Holiday holiday) {
        LocalDate storedDate = holiday.getDate();
        LocalDate dateOfHoliday = getDateOfHoliday(storedDate);

        return  "<b>" + getDayOfWeek(dateOfHoliday) +
                String.format("%02d",dateOfHoliday.getMonth().getValue()) + "." + String.format("%02d", dateOfHoliday.getDayOfMonth()) +
                " </b><i>" + holiday.getName() + "</i> "  + getNumberOfYear(storedDate, dateOfHoliday) + "\n" +
                "/holidays_" + holiday.getId() + "\n";
    }

    private String getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("ru")) + ".";
    }

    private String getNumberOfYear(LocalDate storedDate, LocalDate dateOfHoliday) {
        String numberOfYears = "";
        if (storedDate.getYear() != 1) {
            numberOfYears = "(" + (dateOfHoliday.getYear() - storedDate.getYear()) + " лет)";
        }

        return numberOfYears;
    }

    private LocalDate getDateOfHoliday(LocalDate date) {
        return LocalDate.of(LocalDate.now().getYear(), date.getMonth(), date.getDayOfMonth());
    }
}
