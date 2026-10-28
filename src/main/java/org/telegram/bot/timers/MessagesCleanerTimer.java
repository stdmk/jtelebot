package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.ChatGPT;
import org.telegram.bot.commands.Reactions;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.Message;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagesCleanerTimer extends TimerParent {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setNotification(false)
            .setWebPagePreview(false);

    private final ChatResultsSettingsService chatResultsSettingsService;
    private final Reactions reactions;
    private final ChatGPT chatGPT;
    private final Bot bot;
    private final MessageService messageService;
    private final MessageStatsService messageStatsService;
    private final ReactionDayStatsService reactionDayStatsService;
    private final CustomReactionDayStatsService customReactionDayStatsService;

    @Value("${today.messageExpirationDays:2}")
    private Integer messageExpirationDays;

    @Override
    @Scheduled(cron = "0 59 23 * * ?")
    public void execute() {
        chatResultsSettingsService.getAllEnabled().forEach(chatResultsSettings -> {
            Chat chat = chatResultsSettings.getChat();
            sendResults(chat);
            sendDailyDigest(chat);
        });
        clearData();
    }

    private void sendDailyDigest(Chat chat) {
        if (!chatGPT.isConfigured()) {
            return;
        }

        LocalDate today = LocalDate.now();
        try {
            List<Message> messages = messageService.get(
                    chat,
                    today.atStartOfDay(),
                    today.plusDays(1).atStartOfDay());
            String digest = chatGPT.getDailyDigest(chat, messages);
            if (digest == null || digest.isBlank()) {
                return;
            }

            bot.sendMessage(new TextResponse()
                    .setChatId(chat.getChatId())
                    .setText("<b>${command.chatgpt.dailydigest.title}</b>\n" + StringEscapeUtils.escapeHtml4(digest))
                    .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
        } catch (Exception e) {
            log.error("Failed to generate daily digest for chat {}", chat.getChatId(), e);
        }
    }

    private void sendResults(Chat chat) {
        String responseText = reactions.getTodayTop(chat);
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

        reactionDayStatsService.removeAll();
        customReactionDayStatsService.removeAll();
    }

}
