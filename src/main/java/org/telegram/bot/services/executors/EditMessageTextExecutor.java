package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class EditMessageTextExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    @Override
    public String getMethod() {
        return EditMessageText.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, BotRequest request) {
        Message message = request.getMessage();
        String lang = languageResolver.getChatLanguageCode(request);
        EditMessageText editMessageText = internationalizationService.internationalize((EditMessageText) method, lang);
        log.info("To " + message.getChatId() + ": edited message " + editMessageText.getText());

        try {
            bot.execute(editMessageText);
        } catch (TelegramApiException e) {
            if (isError(e)) {
                botStats.incrementErrors(request, method, e, "error sending response");
            }
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(request, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        EditMessageText editMessageText = (EditMessageText) method;

        String chatId = editMessageText.getChatId();
        String lang = languageResolver.getChatLanguageCode(chatId);

        editMessageText = internationalizationService.internationalize(editMessageText, lang);

        log.info("To " + chatId + ": edited message " + editMessageText.getText());

        try {
            bot.execute(editMessageText);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }
}
