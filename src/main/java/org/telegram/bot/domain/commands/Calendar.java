package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.DateUtils.MonthName;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Calendar implements CommandParent<SendMessage> {

    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;

    private static final Locale LOCALE = new Locale("ru");
    private static final String API_URL = "https://date.nager.at/api/v2/publicholidays/";
    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile("\\d{2}.\\d{4}");
    private static final Pattern MONTH_NAME_YEAR_PATTERN = Pattern.compile("([а-яА-Я]+)\\s(\\d{4})", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());

        LocalDate date;
        String responseText;
        if (textMessage == null) {
           date = LocalDate.now();
           responseText = printCalendarByDate(date, message.getChatId(), message.getFrom().getId(), true);
        } else {
            date = getDateFromText(textMessage);
            responseText = printCalendarByDate(date, message.getChatId(), message.getFrom().getId(), false);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private LocalDate getDateFromText(String text) {
        LocalDate date;
        Matcher monthYearMatcher = MONTH_YEAR_PATTERN.matcher(text);
        Matcher monthNameYearMatcher = MONTH_NAME_YEAR_PATTERN.matcher(text);

        if (monthYearMatcher.find()) {
            try {
                date = LocalDate.parse(
                        "01." + text.substring(monthYearMatcher.start(), monthYearMatcher.end()), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } else if (monthNameYearMatcher.find()) {
            MonthName monthName = MonthName.getByName(monthNameYearMatcher.group(1));
            if (monthName == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            int year;
            try {
                year = Integer.parseInt(monthNameYearMatcher.group(2));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            date = LocalDate.of(year, monthName.getMonthValue(), 1);
        } else {
            MonthName monthName = MonthName.getByName(text);
            LocalDate dateNow = LocalDate.now();

            if (monthName != null) {
                date = LocalDate.of(dateNow.getYear(), monthName.getMonthValue(), 1);
            } else {
                int month;
                try {
                    month = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                date = LocalDate.of(dateNow.getYear(), month, 1);
            }
        }

        return date;
    }

    private String printCalendarByDate(LocalDate date, Long chatId, Long userId, boolean currentMonth) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUser(new Chat().setChatId(chatId), new User().setUserId(userId));
        if (zoneIdOfUser == null) {
            zoneIdOfUser = ZoneId.systemDefault();
        }

        ZonedDateTime zonedDateTime = date.atStartOfDay(zoneIdOfUser);
        String caption = "<b>" + date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, LOCALE) + " " + date.getYear() + "</b>\n";

        List<PublicHoliday> publicHolidays = getPublicHolidays(zonedDateTime.getYear());

        String holidays;
        Integer monthOfDate = zonedDateTime.getMonthValue();

        Predicate<PublicHoliday> filter;
        if (currentMonth) {
            filter = publicHoliday -> !date.isAfter(publicHoliday.getDate());
        } else {
            filter = publicHoliday -> monthOfDate.equals(publicHoliday.getDate().getMonthValue());
        }

        holidays = "<b>Праздники: </b>\n" + publicHolidays
                .stream()
                .filter(filter)
                .map(this::buildHolidayString)
                .collect(Collectors.joining("\n"));

        int daysInMonth = zonedDateTime.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        int firstDayOfWeek = zonedDateTime.getDayOfWeek().getValue();
        List<Integer> holidaysInMonth = publicHolidays
                .stream()
                .map(PublicHoliday::getDate)
                .filter(dateOfHoliday -> monthOfDate.equals(dateOfHoliday.getMonthValue()))
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toList());

        String calendar = printCalendar(firstDayOfWeek, daysInMonth, holidaysInMonth);

        return caption + calendar + holidays;
    }

    private String buildHolidayString(PublicHoliday publicHoliday) {
        return "<b>" + DateUtils.formatDate(publicHoliday.getDate()) + "</b> — " + publicHoliday.getLocalName() + ".";
    }

    private String printCalendar(int firstDayOfWeek, int daysInMonth, List<Integer> holidaysInMonth) {
        StringBuilder buf = new StringBuilder();

        buf.append("ПН  ВТ  СР  ЧТ  ПТ  СБ  ВС\n");
        buf.append("    ".repeat(firstDayOfWeek - 1));

        int i = 1;
        int j = firstDayOfWeek;
        while (i <= daysInMonth) {
            while (j <= 7 && i <= daysInMonth) {
                buf.append(String.format("%2d", i));

                if (holidaysInMonth.contains(i)) {
                    buf.append("* ");
                } else {
                    buf.append("  ");
                }

                j++;
                i++;
            }
            j = 1;
            buf.append("\n");
        }

        return "<code>" + buf + "</code>\n";
    }

    private List<PublicHoliday> getPublicHolidays(int year) {
        ResponseEntity<PublicHoliday[]> responseEntity;
        try {
            responseEntity = botRestTemplate.getForEntity(API_URL + year + "/" + LOCALE.getLanguage(), PublicHoliday[].class);
        } catch (RestClientException e) {
            return new ArrayList<>();
        }

        PublicHoliday[] publicHolidays = responseEntity.getBody();
        if (publicHolidays == null) {
            return new ArrayList<>();
        }

        return Arrays.asList(publicHolidays);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PublicHoliday {
        private LocalDate date;
        private String localName;
    }
}
