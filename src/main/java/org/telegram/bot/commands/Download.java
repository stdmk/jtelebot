package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;

import java.io.InputStream;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class Download implements Command {

    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private static final String DEFAULT_FILE_NAME = "file";

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        Long chatId = message.getChatId();
        if (commandArgument == null) {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.download.commandwaitingstart}"));
        } else {
            bot.sendUploadDocument(chatId);

            FileParams fileParams = getFileParams(commandArgument);

            InputStream fileFromUrl;
            try {
                fileFromUrl = networkUtils.getFileFromUrlWithLimit(fileParams.getUrl());
            } catch (Exception e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE));
            }

            return returnResponse(new FileResponse(message)
                    .addFile(new File(FileType.FILE, fileFromUrl, fileParams.getName())));
        }
    }

    private FileParams getFileParams(String text) {
        String url;
        String fileName;

        int spaceIndex = text.indexOf(" ");
        if (spaceIndex > 0) {
            String firstArg = text.substring(0, spaceIndex);
            String secondArg = text.substring(spaceIndex + 1);

            if (isThatUrl(firstArg)) {
                url = firstArg;
                fileName = secondArg;
            } else {
                if (isThatUrl(secondArg)) {
                    url = secondArg;
                    fileName = firstArg;
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }
        } else {
            if (isThatUrl(text)) {
                url = text;
                fileName = TextUtils.getFileNameFromUrl(url);
                if (fileName == null) {
                    fileName = DEFAULT_FILE_NAME;
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return new FileParams(url, fileName);
    }

    @Value
    private static class FileParams {
        String url;
        String name;
    }
}
