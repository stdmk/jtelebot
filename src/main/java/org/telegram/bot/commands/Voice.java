package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.SpeechParseException;
import org.telegram.bot.exception.SpeechSynthesizeException;
import org.telegram.bot.providers.sber.SaluteSpeechSynthesizer;
import org.telegram.bot.providers.sber.SpeechParser;
import org.telegram.bot.providers.sber.SpeechSynthesizer;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.executors.SendMessageExecutor;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TelegramUtils.getMessage;

@RequiredArgsConstructor
@Service
@Slf4j
public class Voice implements Command<SendVoice>, TextAnalyzer {

    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final NetworkUtils networkUtils;
    private final SpeechSynthesizer speechSynthesizer;
    private final SpeechParser speechParser;
    private final LanguageResolver languageResolver;
    private final SendMessageExecutor sendMessageExecutor;
    private final BotStats botStats;
    private final Bot bot;

    @Override
    public SendVoice parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());

        String textMessage = commandWaitingService.getText(message);
        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }
        Integer messageIdToReply = message.getMessageId();

        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                textMessage = repliedMessage.getText();
                messageIdToReply = repliedMessage.getMessageId();
            } else {
                log.debug("Empty request. Turning on command waiting");
                commandWaitingService.add(message, this.getClass());
                String responseText = "${command.voice.commandwaitingstart}\n";
                String availableVoices = Arrays.stream(SaluteSpeechVoice.values())
                        .map(saluteSpeechVoice -> saluteSpeechVoice.getName() + "(" + saluteSpeechVoice.getLangCode() + ")\n")
                        .collect(Collectors.joining());
                throw new BotException(responseText + availableVoices);
            }
        }

        if (!StringUtils.hasLength(textMessage)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String lang = languageResolver.getChatLanguageCode(update);

        SaluteSpeechVoice saluteSpeechVoice = null;
        String[] words = textMessage.split(" ");
        if (words.length >= 2) {
            saluteSpeechVoice = SaluteSpeechVoice.getByName(words[0]);
        }

        byte[] voice;
        try {
            if (saluteSpeechVoice == null) {
                voice = speechSynthesizer.synthesize(textMessage, lang);
            } else {
                textMessage = textMessage.replaceFirst(words[0], "").trim();
                voice = ((SaluteSpeechSynthesizer) speechSynthesizer).synthesize(textMessage, lang, saluteSpeechVoice);
            }
        } catch (SpeechSynthesizeException e) {
            throw new BotException(e.getMessage());
        }

        SendVoice sendVoice = new SendVoice();
        sendVoice.setChatId(message.getChatId().toString());
        sendVoice.setReplyToMessageId(messageIdToReply);
        sendVoice.setVoice(new InputFile(new ByteArrayInputStream(voice), "voice"));

        return sendVoice;
    }

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
