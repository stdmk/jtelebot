package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.request.MessageKind;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.SaluteSpeechVoice;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.exception.speech.SpeechSynthesizeException;
import org.telegram.bot.exception.speech.SpeechSynthesizeNoApiResponseException;
import org.telegram.bot.exception.speech.TooLongSpeechException;
import org.telegram.bot.providers.sber.SaluteSpeechSynthesizer;
import org.telegram.bot.providers.sber.SpeechParser;
import org.telegram.bot.providers.sber.SpeechSynthesizer;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class Voice implements Command, MessageAnalyzer {

    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechSynthesizer speechSynthesizer;
    private final SpeechParser speechParser;
    private final LanguageResolver languageResolver;
    private final BotStats botStats;
    private final Bot bot;
    private final ObjectCopier objectCopier;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        Integer messageIdToReply = message.getMessageId();

        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                commandArgument = repliedMessage.getText();
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

        if (TextUtils.isEmpty(commandArgument)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String lang = languageResolver.getChatLanguageCode(request);

        SaluteSpeechVoice saluteSpeechVoice = null;
        String[] words = commandArgument.split(" ");
        if (words.length >= 2) {
            saluteSpeechVoice = SaluteSpeechVoice.getByName(words[0]);
        }

        byte[] voice;
        try {
            if (saluteSpeechVoice == null) {
                voice = speechSynthesizer.synthesize(commandArgument, lang);
            } else {
                commandArgument = commandArgument.replaceFirst(words[0], "").trim();
                voice = ((SaluteSpeechSynthesizer) speechSynthesizer).synthesize(commandArgument, lang, saluteSpeechVoice);
            }
        } catch (SpeechSynthesizeNoApiResponseException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        } catch (SpeechSynthesizeException e) {
            throw new BotException(e.getMessage());
        }

        return returnResponse(new FileResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(messageIdToReply)
                .addFile(new File(FileType.VOICE, new ByteArrayInputStream(voice), "voice")));
    }

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();

        return Optional.ofNullable(message.getAttachments())
                .filter(attachment -> MessageContentType.VOICE.equals(message.getMessageContentType()))
                .map(attachments -> attachments
                        .stream()
                        .findFirst()
                        .map(voice -> {
                            byte[] file;
                            try {
                                file = bot.getInputStreamFromTelegramFile(voice.getFileId()).readAllBytes();
                            } catch (TelegramApiException | IOException e) {
                                log.error("Failed to get file from telegram", e);
                                botStats.incrementErrors(request, e, "Failed to get file from telegram");
                                return null;
                            }

                            String response;
                            try {
                                response = speechParser.parse(file, voice.getDuration());
                            } catch (TooLongSpeechException tle) {
                                if (message.getChatId().equals(message.getUser().getUserId())) {
                                    response = "${command.voice.speechistoolong}";
                                } else {
                                    return null;
                                }
                            } catch (SpeechParseException e) {
                                log.error("Failed to parse voice file", e);
                                botStats.incrementErrors(request, e, "Failed to parse voice file");
                                return null;
                            }

                            checkTextForCommands(response, request);

                            return response;
                        }))
                .map(optionalText -> optionalText
                        .map(textOfVoice -> returnResponse(new TextResponse(message).setText(textOfVoice)))
                        .orElse(returnResponse()))
                .orElse(returnResponse());
    }

    private void checkTextForCommands(String text, BotRequest botRequest) {
        String command = normalizeCommand(text);

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(command, bot.getBotUsername());
        if (commandProperties != null) {
            BotRequest newBotRequest = objectCopier.copyObject(botRequest, BotRequest.class);
            if (newBotRequest != null) {
                newBotRequest.getMessage()
                        .setMessageKind(MessageKind.COMMON)
                        .setMessageContentType(MessageContentType.TEXT)
                        .setText(command);
                bot.processRequestWithoutAnalyze(newBotRequest);
            }
        }
    }

    private String normalizeCommand(String command) {
        if (command.endsWith(".")) {
            command = command.substring(0, command.length() - 1);
        }

        return command.trim();
    }

}
