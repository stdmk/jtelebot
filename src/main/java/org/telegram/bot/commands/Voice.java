package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.exception.SpeechParseException;
import org.telegram.bot.providers.SpeechParser;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

import static org.telegram.bot.utils.TelegramUtils.getMessage;

@RequiredArgsConstructor
@Service
@Slf4j
public class Voice implements TextAnalyzer {

    private final NetworkUtils networkUtils;
    private final SpeechParser speechParser;
    private final SendMessageExecutor sendMessageExecutor;
    private final BotStats botStats;

    @Override
    public void analyze(Update update) {
        Message message = getMessage(update);
        if (message.hasVoice()) {
            org.telegram.telegrambots.meta.api.objects.Voice voice = message.getVoice();
            byte[] file;

            try {
                file = networkUtils.getFileFromTelegram(voice.getFileId());
            } catch (TelegramApiException e) {
                log.error("Failed to get voice-file from Telegram", e);
                botStats.incrementErrors(update, e, "Failed to get voice-file from Telegram");
                return;
            } catch (IOException e) {
                log.error("Failed to get bytes from inputstream of voice file", e);
                botStats.incrementErrors(update, e, "Failed to get bytes from inputstream of voice file");
                return;
            }

            String response;
            try {
                response = speechParser.parse(file, voice.getDuration());
            } catch (SpeechParseException e) {
                log.error("Failed to parse voice file", e);
                botStats.incrementErrors(update, e, "Failed to parse voice file");
                return;
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText(response);

            sendMessageExecutor.executeMethod(sendMessage);
        }
    }
}
