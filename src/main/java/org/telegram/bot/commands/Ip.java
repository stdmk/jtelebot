package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.domain.model.whois.IpInfo;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.ip.IpInfoException;
import org.telegram.bot.exception.ip.IpInfoNoResponseException;
import org.telegram.bot.providers.ip.IpInfoProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.net.InetAddress;
import java.util.List;

@RequiredArgsConstructor
@Component
public class Ip implements Command {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final LanguageResolver languageResolver;
    private final IpInfoProvider ipInfoProvider;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.ip.commandwaitingstart}";
        } else {
            validateAddress(commandArgument);
            String lang = languageResolver.getChatLanguageCode(request);
            IpInfo ipInfo = getIpInfo(commandArgument, lang);
            responseText = buildResponseText(ipInfo);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setWebPagePreview(false)
                        .setFormattingStyle(FormattingStyle.HTML)));
    }

    private void validateAddress(String ip) {
        try {
            InetAddress.getByName(ip);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private IpInfo getIpInfo(String ip, String lang) {
        try {
            return ipInfoProvider.getData(ip, lang);
        } catch (IpInfoNoResponseException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        } catch (IpInfoException e) {
            throw new BotException(e.getMessage());
        }
    }

    private String buildResponseText(IpInfo ipInfo) {
        StringBuilder buf = new StringBuilder();

        String type;
        if (ipInfo.getType() == null) {
            type = "";
        } else {
            type = "(" + ipInfo.getType() + ")";
        }

        buf.append("<b>").append(ipInfo.getIp()).append("</b> ").append(type).append("\n");

        appendIfNotNull(buf, ipInfo.getFlagEmoji(), " ");
        appendIfNotNull(buf, ipInfo.getCity(), " ");
        buf.append("(");
        appendIfNotNull(buf, ipInfo.getCountry(), ", ");
        appendIfNotNull(buf, ipInfo.getContinent(), "");
        buf.append(")\n");
        if (ipInfo.getCoordinates() != null) buf.append("/location").append("_").append(TextUtils.coordinatesToCommand(ipInfo.getCoordinates())).append("\n");
        buf.append("\n");

        appendIfNotNull(buf, ipInfo.getAsn(), "ASN", "\n");
        appendIfNotNull(buf, ipInfo.getOrg(), "ORG", "\n");
        appendIfNotNull(buf, ipInfo.getIsp(), "ISP", "\n");
        appendIfNotNull(buf, ipInfo.getDomain(), "\n");

        return buf.toString();
    }

    private void appendIfNotNull(StringBuilder buf, Object value, String separator) {
        if (value != null) {
            buf.append(value).append(separator);
        }
    }

    private void appendIfNotNull(StringBuilder buf, Object value, String caption, String separator) {
        if (value != null) {
            buf.append(caption).append(": <b>").append(value).append("</b>").append(separator);
        }
    }

}
