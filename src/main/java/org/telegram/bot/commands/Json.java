package org.telegram.bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class Json implements Command {

    private final ObjectMapper jsonMapper;
    private final SpeechService speechService;
    private final Bot bot;
    private final BotStats botStats;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String commandArgument = message.getCommandArgument();

        String responseText;
        String fileName = null;
        if (commandArgument == null) {
            if (!message.hasAttachment() || !MessageContentType.FILE.equals(message.getMessageContentType())) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Attachment attachment = message.getAttachments().get(0);
            fileName = attachment.getName();

            byte[] file;
            if (attachment.getFile() != null) {
                file = attachment.getFile();
            } else {
                try {
                    file = bot.getBytesTelegramFile(attachment.getFileId());
                } catch (TelegramApiException | IOException e) {
                    log.error("Failed to get file from telegram", e);
                    botStats.incrementErrors(request, e, "Failed to get file from telegram");
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
                }
            }

            try {
                responseText = format(new String(file, StandardCharsets.UTF_8));
            } catch (JsonProcessingException e) {
                fileName = null;
                responseText = "`" + e.getMessage() + "`";
            }
        } else {
            try {
                responseText = "<pre><code class=\"language-json\">\n" + format(commandArgument) + "</code></pre>";
            } catch (JsonProcessingException e) {
                responseText = "`" + e.getMessage() + "`";
            }
        }

        if (fileName != null) {
            return returnResponse(new FileResponse(message)
                    .addFile(new File(
                            FileType.FILE, responseText.getBytes(StandardCharsets.UTF_8), fileName)));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private String format(String json) throws JsonProcessingException {
        String result;

        JsonNode jsonNode = jsonMapper.readValue(json, JsonNode.class);
        if (TextUtils.hasLineBreaks(json)) {
            result = jsonNode.toString();
        } else {
            result = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        }

        return result;
    }

}
