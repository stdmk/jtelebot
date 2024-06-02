package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebScreen implements Command {

    private static final String DIMENSION = "1350x950";
    private static final String TIMEOUT_MS = "5000";
    private static final String API_URL = "https://api.screenshotmachine.com?" +
            "device=desktop" +
            "&dimension=" + DIMENSION +
            "&format=png" +
            "&cacheLimit=0" +
            "&timeout=" + TIMEOUT_MS;

    private final Bot bot;
    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final BotStats botStats;
    private final NetworkUtils networkUtils;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        String token = propertiesConfig.getScreenshotMachineToken();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = request.getMessage();
        bot.sendUploadPhoto(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        URL url;
        if (commandArgument == null) {
            Message replyToMessage = message.getReplyToMessage();
            if (replyToMessage != null) {
                if (replyToMessage.hasText()) {
                    url = findFirstUrlInText(replyToMessage.getText());
                } else {
                    commandWaitingService.add(message, this.getClass());
                    return returnResponse(new TextResponse(message).setText("${command.webscreen.commandwaitingstart}"));
                }
            } else {
                commandWaitingService.add(message, this.getClass());
                return returnResponse(new TextResponse(message).setText("${command.webscreen.commandwaitingstart}"));
            }
        } else {
            url = findFirstUrlInText(commandArgument);
        }

        log.debug("Request to get screen of url: {}", url);
        InputStream screen;
        try {
            screen = networkUtils.getFileFromUrlWithLimit(API_URL + "&key=" + token + "&url=" + url);
        } catch (IOException e) {
            log.debug("Error getting screen ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementScreenshots();

        return returnResponse(new FileResponse(message)
                .addFile(new File(FileType.IMAGE, screen, "webscreen.png")));
    }

    private URL findFirstUrlInText(String text) {
        try {
            return TextUtils.findFirstUrlInText(text);
        } catch (MalformedURLException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

}
