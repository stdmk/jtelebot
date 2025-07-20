package org.telegram.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Command;
import org.telegram.bot.commands.MessageAnalyzer;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.telegram.response.ResponseMapper;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.executors.MethodExecutor;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
@Slf4j
public class Parser {

    private final ResponseMapper responseMapper;
    private final List<MethodExecutor> methodExecutors;
    private final BotStats botStats;
    @Lazy
    private final List<MessageAnalyzer> messageAnalyzerList;
    private final CommandPropertiesService commandPropertiesService;
    private final UserService userService;

    private final Map<String, MethodExecutor> methodExecutorMap = new ConcurrentHashMap<>();

    @Async
    public void parseAsync(BotRequest botRequest, Command command) {
        if (botRequest == null) {
            return;
        }

        List<BotResponse> responseList = new ArrayList<>(1);
        try {
            responseList.addAll(command.parse(botRequest));
        } catch (Exception e) {
            BotResponse botResponse = handleException(botRequest, e);
            if (botResponse != null) {
                responseList.add(botResponse);
            }
        } finally {
            responseMapper.toTelegramMethod(responseList)
                    .forEach(method -> getExecutor(method.getMethod()).executeMethod(method, botRequest));

            botStats.incrementCommandsProcessed();
        }
    }


    @Async
    public void analyzeMessageAsync(BotRequest botRequest, AccessLevel userAccessLevel) {
        messageAnalyzerList.forEach(messageAnalyzer -> {
            CommandProperties analyzerCommandProperties = commandPropertiesService.getCommand(messageAnalyzer.getClass());
            if (analyzerCommandProperties == null
                    || userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), analyzerCommandProperties.getAccessLevel())) {

                List<BotResponse> botResponses = new ArrayList<>(1);
                try {
                    botResponses = messageAnalyzer.analyze(botRequest);
                } catch (Exception e) {
                    BotResponse botResponse = handleException(botRequest, e);
                    if (botResponse != null) {
                        botResponses.add(botResponse);
                    }
                }

                if (botResponses != null && !botResponses.isEmpty()) {
                    this.executeAsync(botRequest, botResponses);
                }
            }
        });
    }

    private TextResponse handleException(BotRequest botRequest, Throwable e) {
        BotException botException;
        if (e instanceof BotException be1) {
            botException = be1;
        } else if (e.getCause() instanceof BotException cause) {
            botException = cause;
        } else {
            botStats.incrementErrors(botRequest, e, "Unexpected general error: ");
            log.error("Unexpected error: ", e);
            return null;
        }

        Message message = botRequest.getMessage();

        return new TextResponse()
                .setReplyToMessageId(message.getMessageId())
                .setChatId(message.getChatId())
                .setText(botException.getMessage());
    }

    @Async
    public void executeAsync(BotRequest botRequest, @NotEmpty List<BotResponse> responseList) {
        responseMapper.toTelegramMethod(responseList)
                .forEach(method -> getExecutor(method.getMethod()).executeMethod(method, botRequest));

        botStats.incrementCommandsProcessed();
    }

    @Async
    public void executeAsync(BotResponse response) {
        if (response == null) {
            return;
        }

        PartialBotApiMethod<?> method = responseMapper.toTelegramMethod(response);
        getExecutor(method.getMethod()).executeMethod(method);
        botStats.incrementCommandsProcessed();
    }

    private MethodExecutor getExecutor(String methodName) {
        return methodExecutorMap.computeIfAbsent(methodName, key -> methodExecutors
                .stream()
                .filter(methodExecutor -> methodName.equals(methodExecutor.getMethod()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing executor for " + methodName)));
    }

}
