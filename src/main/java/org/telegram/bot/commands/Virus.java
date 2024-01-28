package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.virus.VirusScanApiKeyMissingException;
import org.telegram.bot.exception.virus.VirusScanException;
import org.telegram.bot.exception.virus.VirusScanNoResponseException;
import org.telegram.bot.providers.virus.VirusScanner;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@RequiredArgsConstructor
@Service
@Slf4j
public class Virus implements Command<SendMessage> {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final NetworkUtils networkUtils;
    private final SpeechService speechService;
    private final VirusScanner virusScanner;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        Integer messageIdToReply;
        bot.sendTyping(chatId);

        Document document = null;
        String textMessage = commandWaitingService.getText(message);
        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                if (repliedMessage.hasDocument()) {
                    document = repliedMessage.getDocument();
                } else {
                    try {
                        textMessage = TextUtils.findFirstUrlInText(repliedMessage.getText()).toString();
                    } catch (MalformedURLException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }
                messageIdToReply = repliedMessage.getMessageId();
            } else {
                if (message.hasDocument()) {
                    document = message.getDocument();
                }
                textMessage = cutCommandInText(message.getText());
                messageIdToReply = message.getMessageId();
            }
        } else {
            messageIdToReply = message.getMessageId();
        }

        String responseText;
        if (document != null) {
            responseText = sendFileToScan(document);
        } else if (textMessage != null) {
            responseText = sendUrlToScan(textMessage);
        } else {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.virus.commandwaitingstart}";
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(messageIdToReply);
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String sendFileToScan(Document document) {
        InputStream file;
        try {
            file = networkUtils.getInputStreamFromTelegramFile(document.getFileId());
        } catch (TelegramApiException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        try {
            return virusScanner.scan(file);
        } catch (VirusScanException e) {
            return handleException(e);
        }
    }

    private String sendUrlToScan(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        try {
            return virusScanner.scan(url);
        } catch (VirusScanException e) {
            return handleException(e);
        }
    }

    private String handleException(VirusScanException exception) {
        if (exception instanceof VirusScanApiKeyMissingException) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        } else if (exception instanceof VirusScanNoResponseException) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }

}
