package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class Length implements Command {

    private final Bot bot;
    private final BotStats botStats;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();
        bot.sendTyping(chatId);

        String commandArgument = commandWaitingService.getText(message);

        int length;
        if (commandArgument == null) {
            if (!message.hasAttachment()) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            try {
                length = getFileLength(message.getAttachments().get(0));
            } catch (TelegramApiException | IOException e) {
                log.error("Failed to get file from telegram", e);
                botStats.incrementErrors(request, e, "Failed to get file from telegram");
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        } else {
            length = commandArgument.length();
        }

        return returnResponse(new TextResponse(message)
                .setText("${command.length.responselength} <b>" + length + "</b> ${command.length.symbols}")
                .setResponseSettings(FormattingStyle.HTML));
    }

    private int getFileLength(Attachment attachment) throws IOException, TelegramApiException {
        checkMimeType(attachment.getMimeType());
        byte[] file = bot.getInputStreamFromTelegramFile(attachment.getFileId()).readAllBytes();
        return new String(file, StandardCharsets.UTF_8).length();
    }

    private void checkMimeType(String mimeType) {
        if (!mimeType.startsWith("text") && !mimeType.startsWith("application")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

}
