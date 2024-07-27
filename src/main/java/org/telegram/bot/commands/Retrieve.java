package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class Retrieve implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        if (!message.hasCommandArgument()) {
            return returnResponse();
        }

        bot.sendTyping(message.getChatId());

        String commandArgument = message.getCommandArgument();
        int spaceIndex = commandArgument.indexOf(" ");
        if (spaceIndex < 0) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        URL url;
        Pattern pattern;
        try {
            url = new URL(commandArgument.substring(0, spaceIndex));
            pattern = Pattern.compile(commandArgument.substring(spaceIndex + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return mapToTextResponseList(retrieveData(url, pattern), message, new ResponseSettings());
    }

    private List<String> retrieveData(URL url, Pattern pattern) {
        String raw;
        try {
            raw = networkUtils.readStringFromURL(url);
        } catch (Exception e) {
            throw new BotException(e.getMessage());
        }

        List<String> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(raw);
        while (matcher.find()) {
            StringBuilder buf = new StringBuilder();

            int groupCount = matcher.groupCount();
            if (groupCount == 0) {
                buf.append(matcher.group(0));
            } else {
                for (int i = 1; i <= groupCount; i++) {
                    buf.append(matcher.group(i)).append("\n");
                }
            }

            buf.append("\n");
            results.add(buf.toString());
        }

        if (results.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }

        return results;
    }

}
