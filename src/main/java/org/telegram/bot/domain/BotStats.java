package org.telegram.bot.domain;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.WorkParam;
import org.telegram.bot.services.WorkParamService;
import org.telegram.bot.services.config.PropertiesConfig;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.getDuration;

@Component
@Scope("singleton")
@Getter
public class BotStats {
    private final WorkParamService workParamService;

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

    private Integer wolframRequests;

    private static final String TOTAL_RECEIVED_MESSAGES = "totalReceivedMessages";
    private static final String TOTAL_RUNNING_TIME = "totalRunningTime";
    private static final String TOTAL_COMMANDS_PROCESSED = "totalCommandsProcessed";
    private static final String GOOGLE_REQUESTS = "googleRequests";
    private static final String WOLFRAM_REQUESTS = "wolframRequests";
    private final List<String> botStatsFieldsToSave = Arrays.asList(TOTAL_RECEIVED_MESSAGES, TOTAL_RUNNING_TIME, TOTAL_COMMANDS_PROCESSED, GOOGLE_REQUESTS, WOLFRAM_REQUESTS);

    public BotStats(WorkParamService workParamService, PropertiesConfig propertiesConfig) {
        this.workParamService = workParamService;
        this.botToken = propertiesConfig.getTelegramBotApiToken();

        List<WorkParam> workParamList = workParamService.get(this.botToken);
        this.botStartDateTime = LocalDateTime.now();
        this.receivedMessages = 0;
        this.commandsProcessed = 0;
        this.errors = 0;
        this.screenshots = 0;
        Arrays.asList(TOTAL_RECEIVED_MESSAGES, TOTAL_COMMANDS_PROCESSED).forEach(field -> setTotalBaseField(workParamList, field));
        setTotalRunningTime(workParamList);
        setGoogleRequests(workParamList);
        setWolframRequests(workParamList);
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

    public void incrementWorlframRequests() {
        this.wolframRequests = this.wolframRequests - 1;
    }

    public void resetGoogleRequests() {
        this.googleRequests = 100;
    }

    public void resetWolframRequests() {
        this.wolframRequests = 1000;
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
        this.totalRunningTime = this.totalRunningTime + getDuration(this.lastTotalRunningCheck, dateTimeNow);
        this.lastTotalRunningCheck = dateTimeNow;
        return this.totalRunningTime;
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

    private void setWolframRequests(List<WorkParam> workParamList) {
        WorkParam workParam = getWorkParamByName(workParamList, WOLFRAM_REQUESTS);
        if (workParam == null) {
            this.wolframRequests = 1000;
        } else {
            this.wolframRequests = Integer.parseInt(workParam.getValue());
        }
    }

    private void setTotalRunningTime(List<WorkParam> workParamList) {
        this.lastTotalRunningCheck = LocalDateTime.now();
        WorkParam workParam = getWorkParamByName(workParamList, TOTAL_RUNNING_TIME);
        if (workParam == null) {
            this.totalRunningTime = getDuration(this.botStartDateTime, this.lastTotalRunningCheck);
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
