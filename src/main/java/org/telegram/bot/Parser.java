package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Command;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.mapper.telegram.response.ResponseMapper;
import org.telegram.bot.services.executors.MethodExecutor;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class Parser {

    private final ResponseMapper responseMapper;
    private final List<MethodExecutor> methodExecutors;
    private final BotStats botStats;
    private final Map<String, MethodExecutor> methodExecutorMap = new ConcurrentHashMap<>();

    public Parser(ResponseMapper responseMapper, @Lazy List<MethodExecutor> methodExecutors, BotStats botStats) {
        this.responseMapper = responseMapper;
        this.methodExecutors = methodExecutors;
        this.botStats = botStats;
    }

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
    public void executeAsync(BotRequest botRequest, List<BotResponse> responseList) {
        if (responseList == null || responseList.isEmpty()) {
            return;
        }

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
