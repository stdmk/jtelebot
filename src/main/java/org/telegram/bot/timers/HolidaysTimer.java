package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Holidays;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidaysTimer extends TimerParent {

    private final SendMessageExecutor sendMessageExecutor;
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

                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chat.getChatId().toString());
                    sendMessage.enableHtml(true);
                    sendMessage.disableWebPagePreview();
                    sendMessage.setText(textMessage);

                    return sendMessage;
                })
                .filter(Objects::nonNull)
                .forEach(sendMessageExecutor::executeMethod);
    }
}
