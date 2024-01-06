package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
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
public class Download implements Command<PartialBotApiMethod<?>> {

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

        Long chatId = message.getChatId();
        if (textMessage == null) {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());

            SendMessage sendMessage = new SendMessage();
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText("${command.download.commandwaitingstart}");

            return sendMessage;
        } else {
            bot.sendUploadDocument(chatId);

            FileParams fileParams = getFileParams(textMessage);

            InputStream fileFromUrl;
            try {
                fileFromUrl = networkUtils.getFileFromUrlWithLimit(fileParams.getUrl());
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE));
            }

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setReplyToMessageId(message.getMessageId());
            sendDocument.setDocument(new InputFile(fileFromUrl, fileParams.getName()));

            return sendDocument;
        }
    }

    private FileParams getFileParams(String text) {
        String url;
        String fileName;

        int spaceIndex = text.indexOf(" ");
        if (spaceIndex > 0) {
            String firstArg = text.substring(0, spaceIndex);
            String secondArg = text.substring(spaceIndex + 1);

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
            if (isThatUrl(text)) {
                url = text;
                fileName = TextUtils.getFileNameFromUrl(url);
                if (fileName == null) {
                    fileName = DEFAULT_FILE_NAME;
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return new FileParams(url, fileName);
    }

    @Value
    private static class FileParams {
        String url;
        String name;
    }
}
