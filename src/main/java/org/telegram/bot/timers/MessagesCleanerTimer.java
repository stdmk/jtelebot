package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Today;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.ChatResultsSettingsService;
import org.telegram.bot.services.MessageService;
import org.telegram.bot.services.MessageStatsService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagesCleanerTimer extends TimerParent {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setNotification(false)
            .setWebPagePreview(false);

    private final ChatResultsSettingsService chatResultsSettingsService;
    private final Today today;
    private final Bot bot;
    private final MessageService messageService;
    private final MessageStatsService messageStatsService;

    @Value("${today.messageExpirationDays:2}")
    private Integer messageExpirationDays;

    @Override
    @Scheduled(cron = "0 55 23 * * ?")
    public void execute() {
        chatResultsSettingsService.getAllEnabled().forEach(chatResultsSettings -> sendResults(chatResultsSettings.getChat()));
        clearData();
    }

    private void sendResults(Chat chat) {
        String responseText = today.getResponseText(chat);
        if (responseText == null) {
            return;
        }

        bot.sendMessage(new TextResponse()
                .setChatId(chat.getChatId())
                .setText(responseText)
                .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
    }

    private void clearData() {
        LocalDateTime expirationDateTime = LocalDateTime.now().minusDays(messageExpirationDays);
        messageStatsService.removeAll(expirationDateTime);
        messageService.removeAll(expirationDateTime);
    }

}
