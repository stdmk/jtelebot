package org.telegram.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.MessageAnalyzer;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class MessageAnalyzerExecutor {

    @Lazy
    private final List<MessageAnalyzer> messageAnalyzerList;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;
    private final Parser parser;

    @Async
    public void analyzeMessageAsync(BotRequest botRequest, AccessLevel userAccessLevel) {
        messageAnalyzerList.forEach(messageAnalyzer -> {
            CommandProperties analyzerCommandProperties = commandPropertiesService.getCommand(messageAnalyzer.getClass());
            if (analyzerCommandProperties == null
                    || userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), analyzerCommandProperties.getAccessLevel())) {
                List<BotResponse> botResponses = messageAnalyzer.analyze(botRequest);
                if (botResponses != null && !botResponses.isEmpty()) {
                    parser.executeAsync(botRequest, botResponses);
                }
            }
        });
    }

}
