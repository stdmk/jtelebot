package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.DelayCommand;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import javax.annotation.PostConstruct;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
@Slf4j
public class Delay implements Command {

    private static final int MAX_DELAY_YEARS = 1;

    private static final Pattern FULL_DATE_FULL_TIME_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern FULL_DATE_SHORT_TIME_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2})\\.(\\d{4}) (\\d{2}):(\\d{2})");
    private static final Pattern SHORT_DATE_FULL_TIME_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern SHORT_DATE_SHORT_TIME_PATTERN = Pattern.compile("^(\\d{2})\\.(\\d{2}) (\\d{2}):(\\d{2})");
    private static final Pattern FULL_TIME_PATTERN = Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{2})");
    private static final Pattern SHORT_TIME_PATTERN = Pattern.compile("^(\\d{2}):(\\d{2})");
    private static final Pattern ISO_TIME_DURATION_PATTERN = Pattern.compile("^PT(\\d+H)?(\\d+M)?(\\d+S)?");

    public static final DateTimeFormatter FULL_DATE_FULL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    public static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Map<Pattern, Function<String, LocalDateTime>> DATE_TIME_GETTER_LIST = new LinkedHashMap<>();

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;
    private final CommandPropertiesService commandPropertiesService;
    private final DisableCommandService disableCommandService;
    private final DelayCommandService delayCommandService;
    private final UserService userService;
    private final UserCityService userCityService;
    private final Bot bot;
    private final ObjectMapper objectMapper;

    private Pattern simpleDurationSecondsPattern;
    private Pattern simpleDurationMinutesPattern;
    private Pattern simpleDurationHoursPattern;

    @PostConstruct
    private void postConstruct() {
        DATE_TIME_GETTER_LIST.put(FULL_DATE_FULL_TIME_PATTERN, text -> LocalDateTime.parse(text, FULL_DATE_FULL_TIME_FORMATTER));
        DATE_TIME_GETTER_LIST.put(FULL_DATE_SHORT_TIME_PATTERN, text -> LocalDateTime.parse(normalizeTime(text), FULL_DATE_FULL_TIME_FORMATTER));
        DATE_TIME_GETTER_LIST.put(SHORT_DATE_FULL_TIME_PATTERN, text -> LocalDateTime.parse(normalizeDate(text), FULL_DATE_FULL_TIME_FORMATTER));
        DATE_TIME_GETTER_LIST.put(SHORT_DATE_SHORT_TIME_PATTERN, text -> LocalDateTime.parse(normalizeDateTime(text), FULL_DATE_FULL_TIME_FORMATTER));
        DATE_TIME_GETTER_LIST.put(FULL_TIME_PATTERN, text -> LocalDateTime.parse(addDate(text), FULL_DATE_FULL_TIME_FORMATTER));
        DATE_TIME_GETTER_LIST.put(SHORT_TIME_PATTERN, text -> LocalDateTime.parse(addDate(normalizeTime(text)), FULL_DATE_FULL_TIME_FORMATTER));

        String simpleDurationTemplate = "^(\\d+)(%s)";

        String secondSymbols = String.join("|", internationalizationService.getAllTranslations("command.delay.second"));
        simpleDurationSecondsPattern = Pattern.compile(String.format(simpleDurationTemplate, secondSymbols));

        String minutesSymbols = String.join("|", internationalizationService.getAllTranslations("command.delay.minutes"));
        simpleDurationMinutesPattern = Pattern.compile(String.format(simpleDurationTemplate, minutesSymbols));

        String hoursSymbols = String.join("|", internationalizationService.getAllTranslations("command.delay.hours"));
        simpleDurationHoursPattern = Pattern.compile(String.format(simpleDurationTemplate, hoursSymbols));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);
        String commandArgument = message.getCommandArgument();

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.delay.commandwaitingstart}";
        } else {
            delayCommand(commandArgument, request);
            responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }

    private void delayCommand(String text, BotRequest request) {
        Message message = request.getMessage();
        ZoneId zoneId = userCityService.getZoneIdOfUserOrDefault(message);

        ArgumentAndDateTime argumentAndDateTime = Optional.ofNullable(getDateTimeByPattern(text, zoneId))
                .or(() -> Optional.ofNullable(getDateTimeByDuration(text)))
                .or(() -> Optional.ofNullable(getDateTimeBySimpleDuration(text)))
                .orElseThrow(() -> new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)));

        LocalDateTime dateTime = argumentAndDateTime.dateTime;
        validateDateTime(dateTime);

        String command = text.replaceFirst(argumentAndDateTime.argument, "").trim();
        validateCommand(message.getChat(), message.getUser(), command);
        request.getMessage().setText(command);

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize request: {}", e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        delayCommandService.save(new DelayCommand().setDateTime(dateTime).setRequestJson(requestJson));
    }

    private void validateDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.isAfter(now.plusYears(MAX_DELAY_YEARS))) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private void validateCommand(Chat chat, User user, String command) {
        CommandProperties commandProperties = commandPropertiesService.findCommandInText(command, bot.getBotUsername());
        if (commandProperties == null || disableCommandService.get(chat, commandProperties) != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        
        if (!userService.isUserHaveAccessForCommand(user, commandProperties.getAccessLevel())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }
    }

    private ArgumentAndDateTime getDateTimeByPattern(String text, ZoneId zoneId) {
        for (Map.Entry<Pattern, Function<String, LocalDateTime>> entry : DATE_TIME_GETTER_LIST.entrySet()) {
            Pattern pattern = entry.getKey();
            Function<String, LocalDateTime> getDateTimeFunction = entry.getValue();

            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    return new ArgumentAndDateTime(
                            matcher.group(),
                            getDateTimeFunction.apply(text.substring(matcher.start(), matcher.end()))
                                    .atZone(ZoneId.systemDefault())
                                    .withZoneSameInstant(zoneId)
                                    .toLocalDateTime());
                } catch (Exception e) {
                    log.error("Failed to parse dateTime from {}: {}", text, e.getMessage());
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }
            }
        }

        return null;
    }

    private ArgumentAndDateTime getDateTimeByDuration(String text) {
        Matcher matcher = ISO_TIME_DURATION_PATTERN.matcher(text);
        if (matcher.find()) {
            Duration duration;
            try {
                duration = Duration.parse(text.substring(matcher.start(), matcher.end()));
            } catch (Exception e) {
                return null;
            }

            return new ArgumentAndDateTime(matcher.group(), LocalDateTime.now().plusSeconds(duration.getSeconds()));
        }

        return null;
    }

    private ArgumentAndDateTime getDateTimeBySimpleDuration(String text) {
        Matcher matcher;

        try {
            matcher = simpleDurationSecondsPattern.matcher(text);
            if (matcher.find()) {
                return new ArgumentAndDateTime(matcher.group(), LocalDateTime.now().plusSeconds(Long.parseLong(matcher.group(1))));
            }

            matcher = simpleDurationMinutesPattern.matcher(text);
            if (matcher.find()) {
                return new ArgumentAndDateTime(matcher.group(), LocalDateTime.now().plusMinutes(Long.parseLong(matcher.group(1))));
            }

            matcher = simpleDurationHoursPattern.matcher(text);
            if (matcher.find()) {
                return new ArgumentAndDateTime(matcher.group(), LocalDateTime.now().plusHours(Long.parseLong(matcher.group(1))));
            }
        } catch (Exception e) {
            log.error("Failed to parse dateTime from {}: {}", text, e.getMessage());
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return null;
    }

    private record ArgumentAndDateTime (String argument, LocalDateTime dateTime) {}

    private static String normalizeDateTime(String text) {
        return normalizeTime(normalizeDate(text));
    }

    private static String normalizeDate(String dateTime) {
        int spaceIndex = dateTime.indexOf(" ");
        String dateWithoutYear = dateTime.substring(0, spaceIndex);
        String dateWithYear = dateWithoutYear + "." + Year.now();
        return dateTime.replaceFirst(dateWithoutYear, dateWithYear);
    }

    private static String normalizeTime(String dateTime) {
        return dateTime + ":00";
    }

    private static String addDate(String time) {
        return FULL_DATE_FORMATTER.format(LocalDate.now()) + " " + time;
    }

}
