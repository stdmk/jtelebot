package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.Holidays;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.LanguageResolver;

import java.time.LocalDate;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidaysTimer extends TimerParent {

    private final Bot bot;
    private final ChatService chatService;
    private final Holidays holidays;
    private final LanguageResolver languageResolver;

    @Override
    @Scheduled(cron = "0 0 5 * * ?")
    public void execute() {
        LocalDate dateNow = LocalDate.now();

        chatService.getChatsWithHolidays()
                .stream()
                .map(chat -> {
                    String lang = languageResolver.getChatLanguageCode(chat);
                    String textMessage = holidays.getHolidaysForDate(chat, dateNow, lang);
                    if (textMessage == null) {
                        return null;
                    }

                    return new TextResponse()
                            .setChatId(chat.getChatId())
                            .setText(textMessage)
                            .setResponseSettings(new ResponseSettings()
                                    .setWebPagePreview(false)
                                    .setFormattingStyle(FormattingStyle.HTML));
                })
                .filter(Objects::nonNull)
                .forEach(bot::sendMessage);
    }
}
