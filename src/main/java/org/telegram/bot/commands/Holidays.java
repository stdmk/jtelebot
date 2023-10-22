package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Holiday;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.HolidayService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.getDayOfWeek;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;
import static org.telegram.bot.utils.DateUtils.formatDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class Holidays implements Command<SendMessage> {

    private final Bot bot;
    private final HolidayService holidayService;
    private final LanguageResolver languageResolver;
    private final SpeechService speechService;
    private final Clock clock;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String languageCode = languageResolver.getChatLanguageCode(message);
        String responseText;

        if (textMessage == null) {
            log.debug("Request to get coming holidays");
            responseText = getComingHolidays(new Chat().setChatId(message.getChatId()), languageCode);
        } else if (textMessage.startsWith("_")) {
            long holidayId;
            try {
                holidayId = Long.parseLong(textMessage.substring(1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            log.debug("Request to get Holiday by id {}", holidayId);
            Holiday holiday = holidayService.get(holidayId);
            if (holiday == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = getHolidayDetails(holiday, languageCode);
        } else {
            int i = textMessage.indexOf(".");
            if (i < 0) {
                log.debug("Request to search holidays by text: {}", textMessage);
                responseText = findHolidaysByText(new Chat().setChatId(message.getChatId()), textMessage, languageCode);
            } else {
                LocalDate requestedDate;
                try {
                    requestedDate = LocalDate.of(
                            LocalDate.now(clock).getYear(),
                            Integer.parseInt(textMessage.substring(i + 1)),
                            Integer.parseInt(textMessage.substring(0, i)));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                log.debug("Request to get holidays for date: {}", formatDate(requestedDate));
                responseText = getHolidaysForDate(new Chat().setChatId(message.getChatId()), requestedDate, languageCode);
                if (responseText == null) {
                    responseText = "${command.holidays.noholidays}";
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

    /**
     * Getting list of coming holidays.
     *
     * @param chat chat in which to search.
     * @param lang user's language.
     * @return formatted text with holidays.
     */
    private String getComingHolidays(Chat chat, String lang) {
        StringBuilder buf = new StringBuilder("<u>${command.holidays.caption}:</u>\n");
        final LocalDate dateNow = LocalDate.now(clock);

        holidayService.get(chat)
                .stream()
                .filter(holiday -> holiday.getDate().getDayOfYear() >= dateNow.getDayOfYear())
                .sorted(Comparator.comparing(holiday -> holiday.getDate().getDayOfYear()))
                .limit(10)
                .forEach(holiday -> buf.append(buildStringOfHoliday(holiday, true, lang)));

        return buf.toString();
    }

    /**
     * Getting list of holidays to specific date.
     *
     * @param chat chat in which to search.
     * @param date date by which will search.
     * @param lang user's language.
     * @return formatted text with holidays.
     */
    public String getHolidaysForDate(Chat chat, LocalDate date, String lang) {
        List<Holiday> holidayList = holidayService.get(chat)
                .stream()
                .filter(holiday -> holiday.getDate().getMonth().getValue() == date.getMonth().getValue()
                        && holiday.getDate().getDayOfMonth() == date.getDayOfMonth())
                .collect(Collectors.toList());
        if (holidayList.isEmpty()) {
            return null;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<u>").append(formatDate(date)).append("</u>").append(" (").append(getDayOfWeek(date, lang)).append(")\n");

        holidayList.forEach(holiday -> buf.append(buildStringOfHoliday(holiday, false, lang)));

        return buf.toString();
    }

    /**
     * Finding holidays by text.
     *
     * @param chat chat in which to search.
     * @param name name of searching holiday.
     * @param lang user's language.
     * @return formatted text with holidays.
     */
    private String findHolidaysByText(Chat chat, String name, String lang) {
        StringBuilder buf = new StringBuilder("<u>${command.holidays.searchresults}:</u>\n");
        holidayService.get(chat, name).forEach(holiday -> buf.append(buildStringOfHoliday(holiday, true, lang)));

        return buf.toString();
    }

    /**
     * Getting details of Holiday.
     *
     * @param holiday Holiday entity.
     * @param lang user's language.
     * @return formatted text with details of Holiday.
     * @see Holiday
     */
    private String getHolidayDetails(Holiday holiday, String lang) {
        String date;
        LocalDate storedDate = holiday.getDate();
        LocalDate dateOfHoliday = getDateOfHoliday(storedDate);

        if (storedDate.getYear() == 1) {
            date = formatDate(dateOfHoliday);
        } else {
            date = formatDate(storedDate);
        }

        return "<u>" + holiday.getName() + "</u>\n" +
                "<i>" + date + " " + getDayOfWeek(dateOfHoliday, lang) + "</i> " + getNumberOfYear(storedDate, dateOfHoliday) + "\n" +
                "${command.holidays.author}: " + getLinkToUser(holiday.getUser(), true);
    }

    /**
     * Getting formatted string of short information about Holiday.
     *
     * @param holiday Holiday entity.
     * @param withDayOfWeek flag to add day of week.
     * @param lang user's language.
     * @return formatted string with short info about Holiday
     * @see Holiday
     */
    private String buildStringOfHoliday(Holiday holiday, Boolean withDayOfWeek, String lang) {
        LocalDate storedDate = holiday.getDate();
        LocalDate dateOfHoliday = getDateOfHoliday(storedDate);
        String dayOfWeek;
        if (withDayOfWeek) {
            dayOfWeek = getDayOfWeek(dateOfHoliday, lang) + " ";
        } else {
            dayOfWeek = "";
        }

        return "<b>" + dayOfWeek +
                String.format("%02d", dateOfHoliday.getDayOfMonth()) + "." + String.format("%02d",dateOfHoliday.getMonth().getValue()) +
                " </b><i>" + holiday.getName() + "</i> "  + getNumberOfYear(storedDate, dateOfHoliday) + "\n" +
                "/holidays_" + holiday.getId() + "\n";
    }

    /**
     * Getting postfix with number of years.
     *
     * @param storedDate date of creating the holiday.
     * @param currentDateOfHoliday current celebration date.
     * @return postfix with number of years.
     */
    private String getNumberOfYear(LocalDate storedDate, LocalDate currentDateOfHoliday) {
        String numberOfYears = "";
        if (storedDate.getYear() != 1) {
            String postfix;
            String years = String.valueOf(currentDateOfHoliday.getYear() - storedDate.getYear());

            if (years.endsWith("11") || years.endsWith("12") ||  years.endsWith("13") ||  years.endsWith("14") ||  years.endsWith("15") ||  years.endsWith("16") ||  years.endsWith("17") ||  years.endsWith("18") ||  years.endsWith("19")) {
                postfix = " ${command.holidays.years2})";
            } else if (years.endsWith("1")) {
                postfix = " ${command.holidays.years1})";
            } else if (years.endsWith("2") || (years.endsWith("3") || (years.endsWith("4")))) {
                postfix = " ${command.holidays.yearsparentcase})";
            } else {
                postfix = " ${command.holidays.years2})";
            }

            numberOfYears = "(" + years + postfix;
        }

        return numberOfYears;
    }

    /**
     * Getting current celebration date of holiday.
     *
     * @param date date of creating the holiday
     * @return current celebration date.
     */
    private LocalDate getDateOfHoliday(LocalDate date) {
        return LocalDate.of(LocalDate.now(clock).getYear(), date.getMonth(), date.getDayOfMonth());
    }
}
