package org.telegram.bot.timers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.DelayCommand;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.DelayCommandService;
import org.telegram.bot.services.UserStatsService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
@Slf4j
public class DelayCommandTimer extends TimerParent {

    private final Bot bot;
    private final BotStats botStats;
    private final ObjectMapper objectMapper;
    private final DelayCommandService delayCommandService;
    private final UserStatsService userStatsService;

    @Override
    @Scheduled(fixedRate = 5000)
    public void execute() {
        LocalDateTime dateTimeNow = LocalDateTime.now();

        for (DelayCommand delayCommand : delayCommandService.getAllBeforeDateTime(dateTimeNow)) {
            if (dateTimeNow.isAfter(delayCommand.getDateTime())) {
                BotRequest botRequest;
                try {
                    botRequest = objectMapper.readValue(delayCommand.getRequestJson(), BotRequest.class);
                } catch (JsonProcessingException e) {
                    String errorText = "Failed to deserialize BotRequest: " + e.getMessage();
                    log.error(errorText);
                    botStats.incrementErrors(delayCommand.getRequestJson(), e, errorText);

                    delayCommandService.remove(delayCommand);
                    continue;
                }

                Message message = botRequest.getMessage();
                userStatsService.incrementUserStatsCommands(message.getChat(), message.getUser());
                bot.processRequest(botRequest);

                delayCommandService.remove(delayCommand);
            }
        }
    }
}
