package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;

import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class Download implements CommandParent<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private static final String DEFAULT_FILE_NAME = "file";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());

            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setText("теперь напиши мне что нужно скачать");

            return sendMessage;
        } else {
            bot.sendUploadPhoto(message.getChatId());
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
                fileFromUrl = networkUtils.getFileFromUrlWithLimit(url);
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
}
