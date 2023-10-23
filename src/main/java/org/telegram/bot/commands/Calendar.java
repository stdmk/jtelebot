package org.telegram.bot.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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
public class Calendar implements Command<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final UserCityService userCityService;
    private final InternationalizationService internationalizationService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final RestTemplate botRestTemplate;
    private final Map<Integer, Pair<LocalDate, List<PublicHoliday>>> holidaysData = new ConcurrentHashMap<>(new ConcurrentHashMap<>());
    private final Map<Integer, Set<String>> monthValueNamesMap = new ConcurrentHashMap<>();
    private final Clock clock;

    private static final String API_URL = "https://date.nager.at/api/v2/publicholidays/";
    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile("\\d{2}.\\d{4}");
    private static final Pattern MONTH_NAME_YEAR_PATTERN = Pattern.compile("([а-яА-Яa-zA-Z]+)\\s(\\d{4})", Pattern.UNICODE_CHARACTER_CLASS);

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
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);

        String textMessage;
        Long userId;
        boolean callback;
        if (update.hasCallbackQuery()) {
            textMessage = cutCommandInText(update.getCallbackQuery().getData());
            userId = update.getCallbackQuery().getFrom().getId();
            callback = true;
        } else {
            textMessage = cutCommandInText(message.getText());
            userId = message.getFrom().getId();
            callback = false;
        }

        User user = new User().setUserId(userId);
        Chat chat = new Chat().setChatId(chatId);

        LocalDate date;
        String responseText;
        Locale locale = getUserLocale(message);

        if (textMessage == null) {
           date = LocalDate.now(clock).withDayOfMonth(1);
           responseText = printCalendarByDate(date, chat, user, locale, true);
        } else {
            date = getDateFromText(textMessage);
            responseText = printCalendarByDate(date, chat, user, locale, false);
        }

        if (callback) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(message.getMessageId());
            editMessage.enableHtml(true);
            editMessage.setText(responseText);
            editMessage.setReplyMarkup(getKeyboard(date));

            return editMessage;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableHtml(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setReplyMarkup(getKeyboard(date));
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private Locale getUserLocale(Message message) {
        String langCode = languageResolver.getChatLanguageCode(message);
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

        buf.append("${command.calendar.daysofweekstring}\n");
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

    private InlineKeyboardMarkup getKeyboard(LocalDate date) {
        final String command = "/calendar_";

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("${command.calendar.backbutton}" + Emoji.LEFT_ARROW.getEmoji());
        backButton.setCallbackData(command + DateUtils.formatDateWithoutDay(date.minusMonths(1)));

        InlineKeyboardButton forwardButton = new InlineKeyboardButton();
        forwardButton.setText("${command.calendar.forwardbutton}" + Emoji.RIGHT_ARROW.getEmoji());
        forwardButton.setCallbackData(command + DateUtils.formatDateWithoutDay(date.plusMonths(1)));

        return new InlineKeyboardMarkup(List.of(List.of(backButton, forwardButton)));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PublicHoliday {
        private LocalDate date;
        private String localName;
    }
}
