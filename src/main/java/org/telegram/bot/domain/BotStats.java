package org.telegram.bot.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.WorkParam;
import org.telegram.bot.services.WorkParamService;
import org.telegram.bot.services.config.PropertiesConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.getDuration;

@Component
@Scope("singleton")
@Getter
@Slf4j
public class BotStats {
    private final WorkParamService workParamService;

    private final PropertiesConfig propertiesConfig;

    private final String botToken;

    private final LocalDateTime botStartDateTime;

    private Integer receivedMessages;

    private Long totalReceivedMessages;

    @Getter(value = AccessLevel.NONE)
    private Long totalRunningTime;

    @Getter(value = AccessLevel.NONE)
    private LocalDateTime lastTotalRunningCheck;

    private Integer commandsProcessed;

    private Long totalCommandsProcessed;

    private Integer errors;

    private Integer screenshots;

    private Integer googleRequests;

    private Integer kinopoiskRequests;

    private Integer wolframRequests;

    private Integer russianPostRequests;

    private Long lastTvUpdate;

    private Long lastTracksUpdate;

    private static final String TOTAL_RECEIVED_MESSAGES = "totalReceivedMessages";
    private static final String TOTAL_RUNNING_TIME = "totalRunningTime";
    private static final String TOTAL_COMMANDS_PROCESSED = "totalCommandsProcessed";
    private static final String GOOGLE_REQUESTS = "googleRequests";
    private static final String KINOPOISK_REQUESTS = "kinopoiskRequests";
    private static final String WOLFRAM_REQUESTS = "wolframRequests";
    private static final String RUSSIAN_POST_REQUESTS = "russianPostRequests";
    private static final String LAST_TV_UPDATE = "lastTvUpdate";
    private static final String LAST_TRACKS_UPDATE = "lastTracksUpdate";
    private final List<String> botStatsFieldsToSave = Arrays.asList(
                                                            TOTAL_RECEIVED_MESSAGES,
                                                            TOTAL_RUNNING_TIME,
                                                            TOTAL_COMMANDS_PROCESSED,
                                                            GOOGLE_REQUESTS,
                                                            WOLFRAM_REQUESTS,
                                                            RUSSIAN_POST_REQUESTS,
                                                            LAST_TV_UPDATE,
                                                            LAST_TRACKS_UPDATE);

    public BotStats(WorkParamService workParamService, PropertiesConfig propertiesConfig) {
        this.workParamService = workParamService;
        this.propertiesConfig = propertiesConfig;
        this.botToken = propertiesConfig.getTelegramBotApiToken();

        List<WorkParam> workParamList = workParamService.get(this.botToken);
        this.botStartDateTime = LocalDateTime.now();
        this.receivedMessages = 0;
        this.commandsProcessed = 0;
        this.errors = 0;
        this.screenshots = 0;
        Arrays.asList(TOTAL_RECEIVED_MESSAGES, TOTAL_COMMANDS_PROCESSED, LAST_TV_UPDATE, LAST_TRACKS_UPDATE)
                .forEach(field -> setTotalBaseField(workParamList, field));
        setTotalRunningTime(workParamList);
        setGoogleRequests(workParamList);
        setKinopoiskRequests(workParamList);
        setWolframRequests(workParamList);
        setRussianPostRequests(workParamList);
    }

    public void incrementReceivedMessages() {
        this.receivedMessages = this.receivedMessages + 1;
        this.totalReceivedMessages = this.totalReceivedMessages + 1L;
    }

    public void incrementCommandsProcessed() {
        this.commandsProcessed = this.commandsProcessed + 1;
        this.totalCommandsProcessed = this.totalCommandsProcessed + 1;
    }

    public void incrementErrors() {
        this.errors = this.errors + 1;
    }

    public void incrementScreenshots() {
        this.screenshots = this.screenshots + 1;
    }

    public void incrementGoogleRequests() {
        this.googleRequests = this.googleRequests - 1;
    }

    public void incrementKinopoiskRequests() {
        this.kinopoiskRequests = this.kinopoiskRequests - 1;
    }

    public void incrementWorlframRequests() {
        this.wolframRequests = this.wolframRequests - 1;
    }

    public void incrementRussianPostRequests() {
        this.russianPostRequests = this.russianPostRequests - 1;
    }

    public void resetGoogleRequests() {
        this.googleRequests = 100;
    }

    public void resetKinopoiskRequests() {
        this.kinopoiskRequests = 200;
    }

    public void resetWolframRequests() {
        this.wolframRequests = 1000;
    }

    public void resetRussianPostRequests() {
        this.russianPostRequests = propertiesConfig.getRussianPostRequestsLimit();
    }

    public void saveStats() {
        List<WorkParam> workParamList = workParamService.get(botToken, botStatsFieldsToSave);

        this.totalRunningTime = getTotalRunningTime();

        List<WorkParam> updatedWorkParamList = botStatsFieldsToSave
                .stream()
                .map(botStatsField -> {
                    String value = null;
                    try {
                        value = this.getClass().getDeclaredField(botStatsField).get(this).toString();
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    return setWorkParamByName(workParamList, botStatsField, value);
                })
                .collect(Collectors.toList());

        workParamService.save(updatedWorkParamList);
    }

    public Long getTotalRunningTime() {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        this.totalRunningTime = this.totalRunningTime + getDuration(this.lastTotalRunningCheck, dateTimeNow).toMillis();
        this.lastTotalRunningCheck = dateTimeNow;
        return this.totalRunningTime;
    }

    public void setLastTvUpdate(Instant lastTvUpdate) {
        this.lastTvUpdate = lastTvUpdate.toEpochMilli();
    }

    public void setLastTracksUpdate(Instant lastTracksUpdate) {
        this.lastTracksUpdate = lastTracksUpdate.toEpochMilli();
    }

    private void setTotalBaseField(List<WorkParam> workParamList, String fieldName) {
        WorkParam workParam = getWorkParamByName(workParamList, fieldName);
        long value;
        if (workParam == null) {
            value = 0L;
        } else {
            value = Long.parseLong(workParam.getValue());
        }

        try {
            this.getClass().getDeclaredField(fieldName).set(this, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setGoogleRequests(List<WorkParam> workParamList) {
        WorkParam workParam = getWorkParamByName(workParamList, GOOGLE_REQUESTS);
        if (workParam == null) {
            this.googleRequests = 100;
        } else {
            this.googleRequests = Integer.parseInt(workParam.getValue());
        }
    }

    private void setKinopoiskRequests(List<WorkParam> workParamList) {
        WorkParam workParam = getWorkParamByName(workParamList, GOOGLE_REQUESTS);
        if (workParam == null) {
            this.kinopoiskRequests = 200;
        } else {
            this.kinopoiskRequests = Integer.parseInt(workParam.getValue());
        }
    }

    private void setWolframRequests(List<WorkParam> workParamList) {
        WorkParam workParam = getWorkParamByName(workParamList, WOLFRAM_REQUESTS);
        if (workParam == null) {
            this.wolframRequests = 1000;
        } else {
            this.wolframRequests = Integer.parseInt(workParam.getValue());
        }
    }

    private void setRussianPostRequests(List<WorkParam> workParamList) {
        WorkParam workParam = getWorkParamByName(workParamList, RUSSIAN_POST_REQUESTS);
        if (workParam == null) {
            this.russianPostRequests = 100;
        } else {
            this.russianPostRequests = Integer.parseInt(workParam.getValue());
        }
    }

    private void setTotalRunningTime(List<WorkParam> workParamList) {
        this.lastTotalRunningCheck = LocalDateTime.now();
        WorkParam workParam = getWorkParamByName(workParamList, TOTAL_RUNNING_TIME);
        if (workParam == null) {
            this.totalRunningTime = getDuration(this.botStartDateTime, this.lastTotalRunningCheck).toMillis();
        } else {
            this.totalRunningTime = Long.parseLong(workParam.getValue());
        }
    }

    private WorkParam setWorkParamByName(List<WorkParam> workParamList, String name, String value) {
        WorkParam workParam = workParamList.stream().filter(workParam1 -> workParam1.getName().equals(name)).findFirst().orElse(new WorkParam(this.botToken, name));
        workParam.setValue(value);

        return workParam;
    }

    private WorkParam getWorkParamByName(List<WorkParam> workParamList, String name) {
        return workParamList.stream().filter(workParam -> workParam.getName().equals(name)).findFirst().orElse(null);
    }
}
