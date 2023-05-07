package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.commands.Echo;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.LastMessage;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.MathUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TalkerQuestionsTimer extends TimerParent {

    private final TalkerDegreeService talkerDegreeService;
    private final UserStatsService userStatsService;
    private final Echo echo;
    private final Bot bot;
    private final BotStats botStats;

    private final Map<Long, LocalDateTime> lastAlertBotMessageMap = new ConcurrentHashMap<>();

    @Override
    @Scheduled(fixedRate = 300000)
    public void execute() {
        talkerDegreeService.getAllWithChatIdleParam().forEach(talkerDegree -> {
            LocalDateTime dateTimeNow = LocalDateTime.now();
            Chat chat = talkerDegree.getChat();

            List<UserStats> userStatsListOfChat = userStatsService.getActiveUserStatsListForChat(chat);

            LocalTime timeNow = dateTimeNow.toLocalTime();
            if (timeNow.isAfter(LocalTime.of(23, 0)) || timeNow.isBefore(LocalTime.of(7, 0))) {
                return;
            }

            LocalDateTime lastChatMessageDateTime = userStatsListOfChat
                    .stream()
                    .map(UserStats::getLastMessage)
                    .map(LastMessage::getDate)
                    .max(LocalDateTime::compareTo)
                    .orElse(dateTimeNow);

            LocalDateTime lastAlertBotMessage = lastAlertBotMessageMap.get(chat.getChatId());
            if (lastAlertBotMessage != null && lastAlertBotMessage.isAfter(lastChatMessageDateTime)) {
                lastChatMessageDateTime = lastAlertBotMessage;
            }

            if (lastChatMessageDateTime.plusMinutes(talkerDegree.getChatIdleMinutes()).isBefore(dateTimeNow)) {
                UserStats userStats = userStatsListOfChat.get(MathUtils.getRandomInRange(0, userStatsListOfChat.size() - 1));
                LastMessage lastMessage = userStats.getLastMessage();
                Long chatId = userStats.getChat().getChatId();

                String question = echo.getQuestionForText(lastMessage.getText(), chatId);
                if (question == null) {
                    return;
                }

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setReplyToMessageId(lastMessage.getMessageId());
                sendMessage.setText(question);

                try {
                    bot.execute(sendMessage);
                    lastAlertBotMessageMap.put(chatId, dateTimeNow);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    botStats.incrementErrors(sendMessage, e, "ошибка отправки сообщения с вопросом от бота");
                }
            }
        });
    }

}
