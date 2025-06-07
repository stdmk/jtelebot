package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.ErrorService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Errors implements Command {

    private final Bot bot;
    private final ErrorService errorService;
    private final SpeechService speechService;
    private final BotStats botStats;

    private static final String CLEAR_ERRORS_COMMAND = "_clear";

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = message.getCommandArgument();
        String responseText;

        if (commandArgument != null) {
            if (commandArgument.startsWith(CLEAR_ERRORS_COMMAND)) {
                errorService.clear();
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else if (commandArgument.startsWith("_")) {
                long errorId;
                try {
                    errorId = Long.parseLong(commandArgument.substring(1));
                } catch (NumberFormatException e) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                Error error = errorService.get(errorId);
                if (error == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }

                return returnResponse(new FileResponse(message)
                        .addFile(new File(FileType.FILE, getDataFromError(error), "error" + error.getId() + ".zip")));
            } else {
                return Collections.emptyList();
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

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    private InputStream getDataFromError(Error error) {
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

            return new ByteArrayInputStream(zip.toByteArray());
        } catch (IOException e) {
            botStats.incrementErrors(error, e, "error when building zip file from Error");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }
    }
}
