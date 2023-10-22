package org.telegram.bot.services.executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.services.InternalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
@Service
@Slf4j
public class SendVideoExecutor implements MethodExecutor {

    private final Bot bot;
    private final BotStats botStats;
    private final LanguageResolver languageResolver;
    private final InternalizationService internalizationService;

    @Override
    public String getMethod() {
        return SendVideo.PATH;
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method, Message message) {
        String lang = languageResolver.getChatLanguageCode(message);
        SendVideo sendVideo = internalizationService.internalize((SendVideo) method, lang);
        log.info("To " + message.getChatId() + ": " + sendVideo.getCaption());

        try {
            bot.execute(sendVideo);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(message, method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(message, method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }
    }

    @Override
    public void executeMethod(PartialBotApiMethod<?> method) {
        SendVideo sendVideo = (SendVideo) method;
        String chatId = sendVideo.getChatId();

        String lang = languageResolver.getChatLanguageCode(chatId);

        sendVideo = internalizationService.internalize(sendVideo, lang);

        try {
            bot.execute(sendVideo);
        } catch (TelegramApiException e) {
            botStats.incrementErrors(method, e, "error sending response");
            log.error("Error: cannot send response: {}", e.getMessage());
        } catch (Exception e) {
            botStats.incrementErrors(method, e, "unexpected error");
            log.error("Unexpected error: ", e);
        }

    }
}
