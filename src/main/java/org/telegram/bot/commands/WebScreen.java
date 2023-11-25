package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebScreen implements Command<PartialBotApiMethod<?>> {

    private static final String DIMENSION = "1350x950";
    private static final String TIMEOUT_MS = "5000";
    private static final String API_URL = "https://api.screenshotmachine.com?" +
            "device=desktop" +
            "&dimension=" + DIMENSION + "" +
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
    public PartialBotApiMethod<?> parse(Update update) {
        String token = propertiesConfig.getScreenshotMachineToken();
        if (StringUtils.isEmpty(token)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        bot.sendUploadPhoto(message.getChatId());
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        URL url;
        if (textMessage == null) {
            Message replyToMessage = message.getReplyToMessage();
            if (replyToMessage != null) {
                if (replyToMessage.hasText()) {
                    url = findFirstUrlInText(replyToMessage.getText());
                } else {
                    commandWaitingService.add(message, this.getClass());
                    return getResponseForEmptyMessage(message);
                }
            } else {
                commandWaitingService.add(message, this.getClass());
                return getResponseForEmptyMessage(message);
            }
        } else {
            url = findFirstUrlInText(textMessage);
        }

        log.debug("Request to get screen of url: {}", url);
        SendPhoto sendPhoto = new SendPhoto();

        InputStream screen;
        try {
            screen = networkUtils.getFileFromUrlWithLimit(API_URL + "&key=" + token + "&url=" + url);
        } catch (IOException e) {
            log.debug("Error getting screen ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        botStats.incrementScreenshots();

        sendPhoto.setPhoto(new InputFile(screen, "webscreen.png"));
        sendPhoto.setReplyToMessageId(message.getMessageId());
        sendPhoto.setChatId(message.getChatId().toString());

        return sendPhoto;
    }

    private URL findFirstUrlInText(String text) {
        String stringUrl;

        int i = text.indexOf("http");
        if (i < 0) {
            stringUrl = "http://" + text;
        } else {
            text = text.substring(i);
            int spaceIndex = text.indexOf(" ");
            if (spaceIndex < 0) {
                stringUrl = text;
            } else {
                stringUrl = text.substring(0, spaceIndex);
            }
        }

        try {
            return new URL(stringUrl);
        } catch (MalformedURLException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private SendMessage getResponseForEmptyMessage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("${command.webscreen.commandwaitingstart}");

        return sendMessage;
    }
}
