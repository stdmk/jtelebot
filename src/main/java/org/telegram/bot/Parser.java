package org.telegram.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.executors.MethodExecutor;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.telegram.bot.utils.TelegramUtils.getMessage;

@Component
@Slf4j
public class Parser {

    private final List<MethodExecutor> methodExecutors;
    private final BotStats botStats;

    public Parser(@Lazy List<MethodExecutor> methodExecutors, BotStats botStats) {
        this.methodExecutors = methodExecutors;
        this.botStats = botStats;
    }

    @Async
    public void parseAsync(Update update, Command<?> command) {
        PartialBotApiMethod<?> method = null;
        try {
            method = command.parse(update);
        } catch (Exception e) {
            method = handleException(update, e);
        } finally {
            executeMethod(update, method);
        }
    }

    private PartialBotApiMethod<?> handleException(Update update, Throwable e) {
        BotException botException;
        if (e instanceof BotException) {
            botException = (BotException) e;
        } else if (e.getCause() instanceof BotException) {
            botException = (BotException) e.getCause();
        } else {
            botStats.incrementErrors(update, e, "неожиданная верхнеуровневая ошибка");
            log.error("Unexpected error: ", e);
            return null;
        }

        Message message = getMessage(update);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(botException.getMessage());

        return sendMessage;
    }

    private void executeMethod(Update update, PartialBotApiMethod<?> method) {
        if (method == null) {
            return;
        }

        Message message = getMessage(update);

        methodExecutors
                .stream()
                .filter(methodExecutor -> methodExecutor.getMethod().equals(method.getMethod()))
                .findFirst()
                .ifPresentOrElse(
                        methodExecutor -> methodExecutor.executeMethod(method, message),
                        () -> log.error("Missing executor for {}", method.getMethod()));

        botStats.incrementCommandsProcessed();
    }

}
