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
        String responseText;

        if (textMessage == null) {
            responseText = getCommingHolidays(chatService.get(message.getChatId()));
        } else if (textMessage.startsWith("_")) {
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

            responseText = getHolidayDetails(holiday);
        } else {
            int i = textMessage.indexOf(".");
            if (i < 0) {
                responseText = getHolidaysSearch(chatService.get(message.getChatId()), textMessage);
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

                responseText = getHolidaysForDate(chatService.get(message.getChatId()), requestedDate);
                if (responseText == null) {
                    responseText = "Праздники на эту дату отсутствуют";
                }
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String getCommingHolidays(Chat chat) {
        StringBuilder buf = new StringBuilder("<u>Ближайшие праздники:</u>\n");
        final LocalDate dateNow = LocalDate.now();

        holidayService.get(chat)
                .stream()
                .filter(holiday -> holiday.getDate().getMonth().getValue() >= dateNow.getMonth().getValue() && holiday.getDate().getDayOfMonth() >= dateNow.getDayOfMonth())
                .sorted(Comparator.comparing(holiday -> holiday.getDate().getDayOfYear()))
                .limit(10)
                .forEach(holiday -> buf.append(buildStringOfHoliday(holiday, true)));

        return buf.toString();
    }

    public String getHolidaysForDate(Chat chat, LocalDate date) {
        List<Holiday> holidayList = holidayService.get(chat);
        if (holidayList.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<u>").append(formatDate(date)).append("</u>").append(" (").append(getDayOfWeek(date)).append(")\n");

        holidayList
                .stream()
                .filter(holiday -> holiday.getDate().getMonth().getValue() == date.getMonth().getValue() && holiday.getDate().getDayOfMonth() == date.getDayOfMonth())
                .forEach(holiday -> buf.append(buildStringOfHoliday(holiday, false)));

        return buf.toString();
    }

    private String getHolidaysSearch(Chat chat, String name) {
        StringBuilder buf = new StringBuilder("<u>Результаты поиска:</u>\n");
        holidayService.get(chat, name).forEach(holiday -> buf.append(buildStringOfHoliday(holiday, true)));

        return buf.toString();
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

    private String buildStringOfHoliday(Holiday holiday, Boolean withDayOfWeek) {
        LocalDate storedDate = holiday.getDate();
        LocalDate dateOfHoliday = getDateOfHoliday(storedDate);
        String dayOfWeek;
        if (withDayOfWeek) {
            dayOfWeek = getDayOfWeek(dateOfHoliday) + " ";
        } else {
            dayOfWeek = "";
        }

        return "<b>" + dayOfWeek +
                String.format("%02d", dateOfHoliday.getDayOfMonth()) + "." + String.format("%02d",dateOfHoliday.getMonth().getValue()) +
                " </b><i>" + holiday.getName() + "</i> "  + getNumberOfYear(storedDate, dateOfHoliday) + "\n" +
                "/holidays_" + holiday.getId() + "\n";
    }

    private String getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("ru")) + ".";
    }

    private String getNumberOfYear(LocalDate storedDate, LocalDate dateOfHoliday) {
        String numberOfYears = "";
        if (storedDate.getYear() != 1) {
            String postfix;
            String years = String.valueOf(dateOfHoliday.getYear() - storedDate.getYear());

            if (years.endsWith("11") || years.endsWith("12") ||  years.endsWith("13") ||  years.endsWith("14") ||  years.endsWith("15") ||  years.endsWith("16") ||  years.endsWith("17") ||  years.endsWith("18") ||  years.endsWith("19")) {
                postfix = " лет)";
            } else if (years.endsWith("1")) {
                postfix = " год)";
            } else if (years.endsWith("2") || (years.endsWith("3") || (years.endsWith("4")))) {
                postfix = " года)";
            } else {
                postfix = " лет)";
            }

            numberOfYears = "(" + years + postfix;
        }

        return numberOfYears;
    }

    private LocalDate getDateOfHoliday(LocalDate date) {
        return LocalDate.of(LocalDate.now().getYear(), date.getMonth(), date.getDayOfMonth());
    }
}
