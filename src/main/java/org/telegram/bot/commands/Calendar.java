package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.providers.daysoff.DaysOffProvider;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.utils.DateUtils;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Calendar implements Command {

    private static final String API_URL = "https://date.nager.at/api/v2/publicholidays/";
    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile("\\d{2}.\\d{4}");
    private static final Pattern MONTH_NAME_YEAR_PATTERN = Pattern.compile("([а-яА-Яa-zA-Z]+)\\s(\\d{4})", Pattern.UNICODE_CHARACTER_CLASS);

    private final Map<Integer, Pair<LocalDate, List<PublicHoliday>>> holidaysData = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> monthValueNamesMap = new ConcurrentHashMap<>();

    private final Bot bot;
    private final UserCityService userCityService;
    private final InternationalizationService internationalizationService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final RestTemplate botRestTemplate;
    private final List<DaysOffProvider> daysOffProviderList;
    private final Clock clock;

    @PostConstruct
    private void postConstruct() {
        monthValueNamesMap.put(1, internationalizationService.getAllTranslations("command.calendar.jan"));
        monthValueNamesMap.put(2, internationalizationService.getAllTranslations("command.calendar.feb"));
        monthValueNamesMap.put(3, internationalizationService.getAllTranslations("command.calendar.mar"));
        monthValueNamesMap.put(4, internationalizationService.getAllTranslations("command.calendar.apr"));
        monthValueNamesMap.put(5, internationalizationService.getAllTranslations("command.calendar.may"));
        monthValueNamesMap.put(6, internationalizationService.getAllTranslations("command.calendar.jun"));
        monthValueNamesMap.put(7, internationalizationService.getAllTranslations("command.calendar.jul"));
        monthValueNamesMap.put(8, internationalizationService.getAllTranslations("command.calendar.aug"));
        monthValueNamesMap.put(9, internationalizationService.getAllTranslations("command.calendar.sep"));
        monthValueNamesMap.put(10, internationalizationService.getAllTranslations("command.calendar.oct"));
        monthValueNamesMap.put(11, internationalizationService.getAllTranslations("command.calendar.nov"));
        monthValueNamesMap.put(12, internationalizationService.getAllTranslations("command.calendar.dec"));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);

        String commandArgument = message.getCommandArgument();

        User user = message.getUser();
        Chat chat = message.getChat();

        LocalDate date;
        String responseText;
        Locale locale = getUserLocale(message, user);

        if (commandArgument == null) {
           date = LocalDate.now(clock).withDayOfMonth(1);
           responseText = printCalendarByDate(date, chat, user, locale, true);
        } else {
            date = getDateFromText(commandArgument);
            responseText = printCalendarByDate(date, chat, user, locale, false);
        }

        Keyboard keyboard = getKeyboard(date);

        if (message.isCallback()) {
            return returnResponse(new EditResponse(message)
                    .setText(responseText)
                    .setKeyboard(keyboard)
                    .setResponseSettings(new ResponseSettings().setFormattingStyle(FormattingStyle.HTML)));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setKeyboard(keyboard)
                .setResponseSettings(new ResponseSettings().setFormattingStyle(FormattingStyle.HTML)));
    }

    private Locale getUserLocale(Message message, User user) {
        String langCode = languageResolver.getChatLanguageCode(message, user);
        if (langCode == null) {
            return Locale.getDefault();
        }

        return Locale.forLanguageTag(langCode);
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
            Integer monthValue = getMonthValueByMonthName(monthNameYearMatcher.group(1));
            if (monthValue == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            int year = Integer.parseInt(monthNameYearMatcher.group(2));

            date = LocalDate.of(year, monthValue, 1);
        } else {
            Integer monthValue = getMonthValueByMonthName(text);
            LocalDate dateNow = LocalDate.now(clock);

            if (monthValue != null) {
                date = LocalDate.of(dateNow.getYear(), monthValue, 1);
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

    private Integer getMonthValueByMonthName(String name) {
        name = name.toLowerCase();

        for (Map.Entry<Integer, Set<String>> entry : monthValueNamesMap.entrySet()) {
            for (String data : entry.getValue()) {
                String[] monthNames = data.split(",");
                for (String monthName : monthNames) {
                    if (monthName.equals(name)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;
    }

    private String printCalendarByDate(LocalDate date, Chat chat, User user, Locale locale, boolean currentMonth) {
        ZoneId zoneIdOfUser = userCityService.getZoneIdOfUser(chat, user);
        if (zoneIdOfUser == null) {
            zoneIdOfUser = ZoneId.systemDefault();
        }

        ZonedDateTime zonedDateTime = date.atStartOfDay(zoneIdOfUser);
        String caption = "<b>" + date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + date.getYear() + "</b>\n";

        List<PublicHoliday> publicHolidays = getPublicHolidays(zonedDateTime.getYear(), locale);

        String holidays;
        Integer monthOfDate = zonedDateTime.getMonthValue();

        Predicate<PublicHoliday> filter;
        if (currentMonth) {
            filter = publicHoliday -> !date.isAfter(publicHoliday.getDate());
        } else {
            filter = publicHoliday -> monthOfDate.equals(publicHoliday.getDate().getMonthValue());
        }

        holidays = "<b>${command.calendar.holidayscaption}: </b>\n" + publicHolidays
                .stream()
                .filter(filter)
                .map(this::buildHolidayString)
                .collect(Collectors.joining("\n"));

        int daysInMonth = zonedDateTime.toLocalDate().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        int firstDayOfWeek = zonedDateTime.getDayOfWeek().getValue();
        List<Integer> daysOfInMonth = getDaysOfInMonth(publicHolidays, date.getYear(), monthOfDate, locale);

        String calendar = printCalendar(firstDayOfWeek, daysInMonth, daysOfInMonth);

        return caption + calendar + holidays;
    }

    private String buildHolidayString(PublicHoliday publicHoliday) {
        return "<b>" + DateUtils.formatDate(publicHoliday.getDate()) + "</b> — " + publicHoliday.getLocalName() + ".";
    }

    private List<Integer> getDaysOfInMonth(List<PublicHoliday> publicHolidays, int year, int month, Locale locale) {
        DaysOffProvider daysOffProvider = daysOffProviderList
                .stream()
                .filter(provider -> locale.equals(provider.getLocale()))
                .findFirst()
                .orElse(null);
        if (daysOffProvider != null) {
            List<Integer> daysOffInMonth = daysOffProvider.getDaysOffInMonth(year, month);
            if (!daysOffInMonth.isEmpty()) {
                return daysOffInMonth;
            }
        }

        return publicHolidays
                .stream()
                .map(Calendar.PublicHoliday::getDate)
                .filter(dateOfHoliday -> month == dateOfHoliday.getMonthValue())
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toList());
    }

    private String printCalendar(int firstDayOfWeek, int daysInMonth, List<Integer> daysOffInMonth) {
        StringBuilder buf = new StringBuilder();

        buf.append("${command.calendar.daysofweekstring}\n");
        buf.append("    ".repeat(firstDayOfWeek - 1));

        int i = 1;
        int j = firstDayOfWeek;
        while (i <= daysInMonth) {
            while (j <= 7 && i <= daysInMonth) {
                buf.append(String.format("%2d", i));

                if (j != 6 && j != 7 && daysOffInMonth.contains(i)) {
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

    private List<PublicHoliday> getPublicHolidays(int year, Locale locale) {
        List<PublicHoliday> holidayList;

        LocalDate dateNow = LocalDate.now(clock);
        Pair<LocalDate, List<PublicHoliday>> holidays = holidaysData.get(year);
        if (holidays == null || holidays.getFirst().isAfter(dateNow.plusMonths(1))) {
            holidayList = getPublicHolidaysFromApi(year, locale);
            holidaysData.put(year, Pair.of(dateNow, holidayList));
        } else {
            holidayList = holidays.getSecond();
        }

        return holidayList;
    }

    private List<PublicHoliday> getPublicHolidaysFromApi(int year, Locale locale) {
        ResponseEntity<PublicHoliday[]> responseEntity;
        try {
            responseEntity = botRestTemplate.getForEntity(API_URL + year + "/" + locale.getLanguage(), PublicHoliday[].class);
        } catch (RestClientException e) {
            return new ArrayList<>();
        }

        PublicHoliday[] publicHolidays = responseEntity.getBody();
        if (publicHolidays == null) {
            return new ArrayList<>();
        }

        return Arrays.asList(publicHolidays);
    }

    private Keyboard getKeyboard(LocalDate date) {
        return new Keyboard().setKeyboardButtonsList(List.of(
                new KeyboardButton()
                        .setName("${command.calendar.backbutton}" + Emoji.LEFT_ARROW.getSymbol())
                        .setCallback("/calendar_" + DateUtils.formatDateWithoutDay(date.minusMonths(1))),
                new KeyboardButton()
                        .setName("${command.calendar.forwardbutton}" + Emoji.RIGHT_ARROW.getSymbol())
                        .setCallback("/calendar_" + DateUtils.formatDateWithoutDay(date.plusMonths(1)))));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PublicHoliday {
        private LocalDate date;
        private String localName;
    }
}
