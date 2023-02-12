package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;

import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class Download implements CommandParent<SendDocument> {

    private final NetworkUtils networkUtils;
    private final SpeechService speechService;

    private static final String DEFAULT_FILE_NAME = "file";

    @Override
    public SendDocument parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        if (textMessage == null || textMessage.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String url;
        String fileName;

        int spaceIndex = textMessage.indexOf(" ");
        if (spaceIndex > 0) {
            String firstArg = textMessage.substring(0, spaceIndex);
            String secondArg = textMessage.substring(spaceIndex + 1);

            if (isThatUrl(firstArg)) {
                url = firstArg;
                fileName = secondArg;
            } else {
                if (isThatUrl(secondArg)) {
                    url = secondArg;
                    fileName = firstArg;
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }
        } else {
            if (isThatUrl(textMessage)) {
                url = textMessage;
                fileName = TextUtils.getFileNameFromUrl(url);
                if (fileName == null) {
                    fileName = DEFAULT_FILE_NAME;
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        InputStream fileFromUrl;
        try {
            fileFromUrl = networkUtils.getFileFromUrl(url, 5000000);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE));
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(message.getChatId().toString());
        sendDocument.setReplyToMessageId(message.getMessageId());
        sendDocument.setDocument(new InputFile(fileFromUrl, fileName));

        return sendDocument;
    }
}
