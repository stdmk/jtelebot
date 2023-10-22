package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ErrorService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Errors implements Command<PartialBotApiMethod<?>> {

    private final Bot bot;
    private final ErrorService errorService;
    private final SpeechService speechService;
    private final BotStats botStats;

    private static final String CLEAR_ERRORS_COMMAND = "_clear";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        if (textMessage != null) {
            if (textMessage.startsWith(CLEAR_ERRORS_COMMAND)) {
                errorService.clear();
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else if (textMessage.startsWith("_")) {
                long errorId;
                try {
                    errorId = Long.parseLong(textMessage.substring(1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                Error error = errorService.get(errorId);
                if (error == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(message.getChatId());
                sendDocument.setReplyToMessageId(message.getMessageId());
                sendDocument.setDocument(getDataFromError(error));

                return sendDocument;
            } else {
                return null;
            }
        } else {
            log.debug("Request to get list of errors");

            StringBuilder buf = new StringBuilder("<b>${command.errors.errorsscaption}:</b>\n");
            errorService.getAll().forEach(error ->
                    buf.append(DateUtils.formatDateTime(error.getDateTime())).append(" — ").append(error.getComment())
                            .append(" /errors_").append(error.getId()).append("\n"));
            buf.append("\n${command.errors.errorsclear} — /errors" + CLEAR_ERRORS_COMMAND);

            responseText = buf.toString();
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private InputFile getDataFromError(Error error) {
        try {
            ByteArrayOutputStream zip = new ByteArrayOutputStream();

            ZipOutputStream out = new ZipOutputStream(zip);
            ZipEntry request = new ZipEntry("request.json");
            out.putNextEntry(request);
            byte[] requestData = error.getRequest().getBytes();
            out.write(requestData, 0, requestData.length);
            out.closeEntry();

            ZipEntry response = new ZipEntry("response.json");
            out.putNextEntry(response);
            byte[] responseData = error.getResponse().getBytes();
            out.write(responseData, 0, responseData.length);
            out.closeEntry();

            ZipEntry stacktrace = new ZipEntry("stacktrace.txt");
            out.putNextEntry(stacktrace);
            byte[] stacktraceData = error.getStacktrace().getBytes();
            out.write(stacktraceData, 0, stacktraceData.length);
            out.closeEntry();

            out.close();

            return new InputFile(new ByteArrayInputStream(zip.toByteArray()), "error" + error.getId() + ".zip");
        } catch (IOException e) {
            botStats.incrementErrors(error, e, "error when building zip file from Error");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }
}
