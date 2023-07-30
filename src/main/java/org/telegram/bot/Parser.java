package org.telegram.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Component
public class Parser {
    @Async
    public CompletableFuture<PartialBotApiMethod<?>> parseAsync(Update update, CommandParent<?> command) {
        return new AsyncResult<PartialBotApiMethod<?>>(command.parse(update)).completable();
    }

}
