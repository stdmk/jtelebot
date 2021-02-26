package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.telegram.bot.utils.NetworkUtils.getFileFromUrl;

@Component
@AllArgsConstructor
public class WebScreen implements CommandParent<SendPhoto> {

    private final Logger log = LoggerFactory.getLogger(WebScreen.class);

    private final PropertiesConfig propertiesConfig;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final BotStats botStats;

    @Override
    public SendPhoto parse(Update update) throws Exception {
        String token = propertiesConfig.getScreenshotMachineToken();
        if (token == null || token.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());

            throw new BotException("теперь напиши мне url-адрес");
        } else {
            URL url;

            if (!textMessage.startsWith("http")) {
                textMessage = "http://" + textMessage;
            }

            try {
                url = new URL(textMessage);
            } catch (MalformedURLException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            SendPhoto sendPhoto = new SendPhoto();
            String API_URL = "https://api.screenshotmachine.com?device=desktop&dimension=1350x950&format=png&cacheLimit=0&timeout=5000";

            InputStream screen;
            try {
                screen = getFileFromUrl(API_URL + "&key=" + token + "&url=" + url.toString());
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            botStats.incrementScreenshots();

            sendPhoto.setPhoto(new InputFile(screen, "webscreen.png"));
            sendPhoto.setReplyToMessageId(message.getMessageId());
            sendPhoto.setChatId(message.getChatId().toString());

            return sendPhoto;
        }
    }
}
