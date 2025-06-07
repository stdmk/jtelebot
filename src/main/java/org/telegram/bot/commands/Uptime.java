package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.providers.system.SystemInfoProvider;
import org.telegram.bot.repositories.TalkerPhraseRepository;
import org.telegram.bot.services.BotStats;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.*;
import static org.telegram.bot.utils.TextUtils.formatFileSize;
import static org.telegram.bot.utils.TextUtils.formatLongValue;

@Component
@RequiredArgsConstructor
public class Uptime implements Command {

    private final Bot bot;
    private final BotStats botStats;
    private final TalkerPhraseRepository talkerPhraseRepository;
    private final SystemInfoProvider systemInfoProvider;
    private final Clock clock;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        if (message.hasCommandArgument()) {
            return returnResponse();
        }
        bot.sendTyping(message.getChatId());

        LocalDateTime dateTimeNow = LocalDateTime.now(clock);
        LocalDateTime botDateTimeStart = botStats.getBotStartDateTime();

        StringBuilder buf = new StringBuilder();
        buf.append("<b>${command.uptime.launch}:</b>\n").append(formatDateTime(botDateTimeStart)).append("\n");
        buf.append("<b>${command.uptime.uptime}:</b>\n").append(durationToString(botDateTimeStart, dateTimeNow)).append("\n");
        buf.append("<b>${command.uptime.totaltime}:</b>\n").append(durationToString(botStats.getTotalRunningTime())).append("\n");

        long heapMaxSize = systemInfoProvider.getMaxMemory() / 1024 / 1024;
        long heapSize = systemInfoProvider.getTotalMemory() / 1024 / 1024;
        long heapOccupiedSize = (systemInfoProvider.getTotalMemory() - systemInfoProvider.getFreeMemory()) / 1024 / 1024;
        buf.append("<b>Heap:</b>\n").append(heapOccupiedSize).append("/").append(heapSize).append("/").append(heapMaxSize).append(" мб.\n");

        buf.append("<b><u>${command.uptime.statistic}:</u></b>\n");
        buf.append("${command.uptime.incomingmessages}: <b>").append(botStats.getReceivedMessages()).append("</b> (").append(formatLongValue(botStats.getTotalReceivedMessages())).append(")\n");
        buf.append("${command.uptime.talkerphrases}: <b>").append(talkerPhraseRepository.countByChat(message.getChat())).append("</b> (").append(formatLongValue(talkerPhraseRepository.count())).append(")\n");
        buf.append("${command.uptime.commandsprocessed}: <b>").append(botStats.getCommandsProcessed()).append("</b> (").append(formatLongValue(botStats.getTotalCommandsProcessed())).append(")\n");
        buf.append("${command.uptime.googlerequests}: <b>").append(botStats.getGoogleRequests()).append("</b>\n");
        buf.append("${command.uptime.postrequests}: <b>").append(botStats.getRussianPostRequests()).append("</b>\n");
        buf.append("${command.uptime.wolframrequests}: <b>").append(botStats.getWolframRequests()).append("</b>\n");
        buf.append("${command.uptime.movierequests}: <b>").append(botStats.getKinopoiskRequests()).append("</b>\n");
        buf.append("${command.uptime.unexpectederrors}: <b>").append(botStats.getErrors()).append("</b>\n");
        buf.append("${command.uptime.tvupdate}: <b>").append(formatShortDateTime(Instant.ofEpochMilli(botStats.getLastTvUpdate()))).append("</b>\n");
        buf.append("${command.uptime.trackcodeupdate}: <b>").append(formatShortDateTime(Instant.ofEpochMilli(botStats.getLastTracksUpdate()))).append("</b>\n");
        buf.append("${command.uptime.dbsize}: <b>").append(formatFileSize(systemInfoProvider.getDbFileSize())).append(" </b>\n");
        buf.append("${command.uptime.freeondisk}: <b>").append(formatFileSize(systemInfoProvider.getFreeSystemSpace())).append(" </b>\n");

        return returnResponse(new TextResponse(message)
                .setText(buf.toString())
                .setResponseSettings(FormattingStyle.HTML));
    }

}
