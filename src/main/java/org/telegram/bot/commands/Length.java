package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class Length implements Command<SendMessage> {

    private final Bot bot;
    private final BotStats botStats;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final NetworkUtils networkUtils;

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);

        String textMessage = commandWaitingService.getText(message);
        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        int length;
        if (textMessage == null) {
            if (!message.hasDocument()) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            length = getFileLength(message.getDocument());
        } else {
            length = textMessage.length();
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText("${command.length.responselength} <b>" + length + "</b> ${command.length.symbols}");


        return returnOneResult(sendMessage);
    }

    private int getFileLength(Document document) {
        checkMimeType(document.getMimeType());

        byte[] file;
        try {
            file = networkUtils.getFileFromTelegram(document.getFileId());
        } catch (TelegramApiException | IOException e) {
            log.error("Failed to get file from telegram", e);
            botStats.incrementErrors(document, e, "Failed to get file from telegram");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return new String(file, StandardCharsets.UTF_8).length();
    }

    private void checkMimeType(String mimeType) {
        if (!mimeType.startsWith("text") && !mimeType.startsWith("application")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

}
