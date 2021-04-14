package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.formatLongValue;

@Component
@AllArgsConstructor
public class Uptime implements CommandParent<SendMessage> {

    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        StringBuilder buf = new StringBuilder();

        if (textMessage != null) {
            return null;
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime botDateTimeStart = botStats.getBotStartDateTime();

        buf.append("<b>Запуск:</b>\n").append(formatDateTime(botDateTimeStart)).append("\n");
        buf.append("<b>Работаю без перерыва:</b>\n").append(deltaDatesToString(botDateTimeStart, dateTimeNow)).append("\n");
        buf.append("<b>Общее время наработки:</b>\n").append(deltaDatesToString(botStats.getTotalRunningTime())).append("\n");

        long heapMaxSize = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long heapOccupiedSize = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        buf.append("<b>Heap:</b>\n").append(heapOccupiedSize).append("/").append(heapSize).append("/").append(heapMaxSize).append(" мб.\n");

        buf.append("<b><u>Статистика:</u></b>\n");
        buf.append("Принято сообщений: <b>").append(botStats.getReceivedMessages()).append("</b> (").append(formatLongValue(botStats.getTotalReceivedMessages())).append(")\n");
        buf.append("Обработано команд: <b>").append(botStats.getCommandsProcessed()).append("</b> (").append(formatLongValue(botStats.getTotalCommandsProcessed())).append(")\n");
        buf.append("Гуглозапросов: <b>").append(botStats.getGoogleRequests()).append("</b>\n");
        buf.append("Вольфрамозапросов: <b>").append(botStats.getWolframRequests()).append("</b>\n");
        buf.append("Непредвиденных ошибок: <b>").append(botStats.getErrors()).append("</b>\n");
        buf.append("Обновление ТВ: <b>").append(formatDate(Instant.ofEpochMilli(botStats.getLastTvUpdate()))).append("</b>\n");
        buf.append("Размер БД: <b>").append(new File("db.mv.db").length() / 1024 / 1024).append(" мб</b>\n");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(buf.toString());

        return sendMessage;
    }
}
