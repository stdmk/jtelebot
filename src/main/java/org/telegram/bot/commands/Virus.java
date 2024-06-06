package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.virus.VirusScanApiKeyMissingException;
import org.telegram.bot.exception.virus.VirusScanException;
import org.telegram.bot.exception.virus.VirusScanNoResponseException;
import org.telegram.bot.providers.virus.VirusScanner;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class Virus implements Command {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final NetworkUtils networkUtils;
    private final SpeechService speechService;
    private final VirusScanner virusScanner;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        Integer messageIdToReply;
        bot.sendTyping(chatId);

        Attachment attachment = null;

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                if (repliedMessage.hasAttachment()) {
                    attachment = repliedMessage.getAttachments().get(0);
                } else {
                    try {
                        commandArgument = TextUtils.findFirstUrlInText(repliedMessage.getText()).toString();
                    } catch (MalformedURLException e) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }
                messageIdToReply = repliedMessage.getMessageId();
            } else {
                if (message.hasAttachment()) {
                    attachment = message.getAttachments().get(0);
                }
                commandArgument = message.getCommandArgument();
                messageIdToReply = message.getMessageId();
            }
        } else {
            messageIdToReply = message.getMessageId();
        }

        String responseText;
        if (attachment != null) {
            responseText = sendFileToScan(attachment);
        } else if (commandArgument != null) {
            responseText = sendUrlToScan(commandArgument);
        } else {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.virus.commandwaitingstart}";
        }

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(messageIdToReply)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setFormattingStyle(FormattingStyle.HTML)
                        .setWebPagePreview(false)));
    }

    private String sendFileToScan(Attachment attachment) {
        String scanResult;
        try (InputStream file = bot.getInputStreamFromTelegramFile(attachment.getFileId())) {
            scanResult = virusScanner.scan(file);
        } catch (IOException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        } catch (VirusScanException e) {
            return handleException(e);
        }

        return scanResult;
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
