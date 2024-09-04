package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.utils.NetworkUtils;

import java.net.UnknownHostException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@RequiredArgsConstructor
@Component
public class Ping implements Command {

    private final Clock clock;
    private final NetworkUtils networkUtils;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String responseText;

        String commandArgument = message.getCommandArgument();
        if (commandArgument != null) {
            try {
                responseText = pingHost(commandArgument);
            } catch (UnknownHostException e) {
                responseText = "${command.ping.ipnotfound}";
            }
        } else {
            responseText = pingApi(message);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String pingApi(Message message) {
        LocalDateTime dateTimeNow = LocalDateTime.now(clock);
        LocalDateTime dateTimeOfMessage = message.getDateTime();

        ZoneOffset zoneOffSet = ZoneId.systemDefault().getRules().getOffset(dateTimeNow);
        long diff = dateTimeNow.toInstant(zoneOffSet).toEpochMilli() - dateTimeOfMessage.toInstant(zoneOffSet).toEpochMilli();

        return "${command.ping.caption}: " + diff + " ${command.ping.ms}.";
    }

    private String pingHost(String host) throws UnknownHostException {
        NetworkUtils.PingResult pingResult = networkUtils.pingHost(host);

        String hostAddress;
        if (pingResult.getHost().equals(pingResult.getIp())) {
            hostAddress = pingResult.getHost();
        } else {
            hostAddress = pingResult.getHost() + " (" + pingResult.getIp() + ")";
        }

        String emoji;
        String reachable;
        String duration;
        if (pingResult.isReachable()) {
            emoji = Emoji.CHECK_MARK_BUTTON.getSymbol();
            reachable = "${command.ping.reachable}";
            duration = "${command.ping.caption}: *" + pingResult.getDurationMillis() + "* ${command.ping.ms}.";
        } else {
            emoji = Emoji.DELETE.getSymbol();
            reachable = "${command.ping.unreachable}";
            duration = "";
        }

        return "${command.ping.host} " + hostAddress + ":\n"
                + emoji + "* " + reachable + "*\n"
                + duration;
    }

}
