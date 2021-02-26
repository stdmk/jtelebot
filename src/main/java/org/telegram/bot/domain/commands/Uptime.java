package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;

import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.formatDateTime;

@Component
@AllArgsConstructor
public class Uptime implements CommandParent<SendMessage> {

    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) throws Exception {
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
        buf.append("<b>Общее время наработки</b>\n").append(deltaDatesToString(botStats.getTotalRunningTime())).append("\n");

        long heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long heapMaxSize = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        float heapPercent = (float) heapSize / (float) heapMaxSize;
        buf.append("<b>Heap:</b>\n").append(heapSize).append("/").append(heapMaxSize).append(" (").append(String.format("%.0f%%", heapPercent * 100)).append(")\n");

        buf.append("<b><u>Статистика:</u></b>\n");
        buf.append("Принято сообщений: <b>").append(botStats.getReceivedMessages()).append("</b> (").append(botStats.getTotalReceivedMessages()).append(")\n");
        buf.append("Обработано команд: <b>").append(botStats.getCommandsProcessed()).append("</b> (").append(botStats.getTotalCommandsProcessed()).append(")\n");
        buf.append("Гуглозапросов: <b>").append(botStats.getGoogleRequests()).append("</b>\n");
        buf.append("Вольфрамозапросов: <b>").append(botStats.getWolframRequests()).append("</b>\n");
        buf.append("Непредвиденных ошибок: <b>").append(botStats.getErrors()).append("</b>\n");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(buf.toString());

        return sendMessage;
    }
}
